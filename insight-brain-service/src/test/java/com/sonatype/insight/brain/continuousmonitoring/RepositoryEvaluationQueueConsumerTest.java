/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringQueueItemDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RepositoryEvaluationQueueConsumer} (CLM-40039 Sections 6.2 / 7.1 / 7.2).
 * Confirms the consumer (a) dispatches to the correct flow processor, (b) honors the retry
 * policy on failure (transient under limit → markRetry; non-retryable or exhausted → delete;
 * interrupt → markRetry without rethrow because the executor is the interrupt observer), and
 * (c) rejects duplicate flow-processor bindings at construction time.
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryEvaluationQueueConsumerTest
{
  private static final String QUEUE_ID = "queue-id";

  private static final int MAX_RETRIES = 3;

  private static final int WORKER_THREADS = 4;

  @Mock
  private ShutdownHandler shutdownHandler;

  @Mock
  private ContinuousMonitoringQueueItemDAO queueDAO;

  @Mock
  private ContinuousMonitoringFlowProcessor hostedRepoProcessor;

  @Mock
  private Configuration configuration;

  private RepositoryEvaluationQueueConsumer underTest;

  @BeforeClass
  public static void installFeatureFlagShim() {
    HostedRepositoryEvaluationFeatureFlagTestRule.install();
  }

  @AfterClass
  public static void uninstallFeatureFlagShim() {
    HostedRepositoryEvaluationFeatureFlagTestRule.uninstall();
  }

  @Before
  public void setup() {
    when(hostedRepoProcessor.getFlowType()).thenReturn(ContinuousMonitoringFlowType.HOSTED_REPO);
    when(configuration.getContinuousMonitoringWorkerThreads()).thenReturn(WORKER_THREADS);
    when(configuration.getMaxContinuousMonitoringRetries()).thenReturn(MAX_RETRIES);
    // Reset to enabled at the start of every test so the configurationChanged tests below have a
    // deterministic baseline. Tests that need it OFF flip the feature flag explicitly.
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    underTest = new RepositoryEvaluationQueueConsumer(
        shutdownHandler, queueDAO, Set.of(hostedRepoProcessor), configuration, null);
  }

  // --- construction ------------------------------------------------------

  @Test
  public void testConstructor_rejectsDuplicateFlowProcessors() {
    ContinuousMonitoringFlowProcessor duplicate = mockProcessor(ContinuousMonitoringFlowType.HOSTED_REPO);
    Set<ContinuousMonitoringFlowProcessor> processors = new HashSet<>();
    processors.add(hostedRepoProcessor);
    processors.add(duplicate);

    assertThatThrownBy(() -> new RepositoryEvaluationQueueConsumer(
        shutdownHandler, queueDAO, processors, configuration, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicate ContinuousMonitoringFlowProcessor")
            .hasMessageContaining(ContinuousMonitoringFlowType.HOSTED_REPO.name());
  }

  // --- acquire / dispatch ------------------------------------------------

  @Test
  public void testAcquireJobs_delegatesToDaoForHostedRepoFlow() {
    ContinuousMonitoringQueueItem item = item(QUEUE_ID, 0);
    when(queueDAO.acquirePending(eq(ContinuousMonitoringFlowType.HOSTED_REPO), anyString(), eq(7)))
        .thenReturn(List.of(item));

    List<ContinuousMonitoringQueueItem> result = underTest.acquireJobs(7);

    assertThat(result).containsExactly(item);
  }

  @Test
  public void testExecuteJob_dispatchesToProcessorForFlow() throws Exception {
    ContinuousMonitoringQueueItem item = item(QUEUE_ID, 0);

    underTest.executeJob(item);

    verify(hostedRepoProcessor).process(item);
    verify(queueDAO, never()).deleteById(anyString(), anyString());
  }

  @Test
  public void testExecuteJob_throwsWhenNoProcessorRegisteredForFlow() throws Exception {
    // Build a consumer with no processors so the processor lookup returns null.
    RepositoryEvaluationQueueConsumer empty =
        new RepositoryEvaluationQueueConsumer(shutdownHandler, queueDAO, Set.of(), configuration, null);
    ContinuousMonitoringQueueItem item = item(QUEUE_ID, 0);

    assertThatThrownBy(() -> empty.executeJob(item))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No processor registered for flow");

    verify(hostedRepoProcessor, never()).process(any());
  }

  // --- success / unacquire / permanent failure --------------------------

  @Test
  public void testOnJobSuccess_deletesQueueRow() {
    underTest.onJobSuccess(item(QUEUE_ID, 0));

    verify(queueDAO).deleteById(eq(QUEUE_ID), anyString());
  }

  @Test
  public void testUnacquireJobs_unacquiresEachId() {
    underTest.unacquireJobs(Set.of("a", "b"));

    verify(queueDAO).unacquire("a");
    verify(queueDAO).unacquire("b");
    verify(queueDAO, never()).markRetry(anyString(), anyString());
  }

  @Test
  public void testPermanentlyFailJob_deletesQueueRow() {
    underTest.permanentlyFailJob(item(QUEUE_ID, 1), new RuntimeException("boom"));

    verify(queueDAO).deleteById(eq(QUEUE_ID), anyString());
  }

  // --- retry policy branches (Section 7.1 / 7.2) ------------------------

  @Test
  public void testOnJobFailure_marksRetryForTransientUnderLimit() {
    underTest.onJobFailure(item(QUEUE_ID, 0), new ConnectException("connect refused"));

    verify(queueDAO).markRetry(eq(QUEUE_ID), anyString());
    verify(queueDAO, never()).deleteById(anyString(), anyString());
  }

  @Test
  public void testOnJobFailure_deletesWhenRetryLimitExhausted() {
    // retryCount == 2 → currentAttempt == 3 == maxRetries → delete.
    // DBCP signals pool-timeout via SQLException whose message starts with "Cannot get a connection".
    underTest.onJobFailure(item(QUEUE_ID, MAX_RETRIES - 1),
        new SQLException("Cannot get a connection, pool error Timeout waiting for idle object"));

    verify(queueDAO).deleteById(eq(QUEUE_ID), anyString());
    verify(queueDAO, never()).markRetry(anyString(), anyString());
  }

  @Test
  public void testOnJobFailure_deletesForNonRetryableException() {
    underTest.onJobFailure(item(QUEUE_ID, 0), new IllegalArgumentException("bad input"));

    verify(queueDAO).deleteById(eq(QUEUE_ID), anyString());
    verify(queueDAO, never()).markRetry(anyString(), anyString());
  }

  /**
   * CLM-40971 C1: InterruptedException must unacquire (no retry_count bump) so 3 rolling
   * restarts during an in-flight job don't permanently delete it.
   */
  @Test
  public void testOnJobFailureFor_interruptedExceptionUnacquiresWithoutRetryBudget() {
    underTest.onJobFailure(item(QUEUE_ID, 0), new InterruptedException("shutting down"));

    verify(queueDAO).unacquire(QUEUE_ID);
    verify(queueDAO, never()).markRetry(anyString(), anyString());
    verify(queueDAO, never()).deleteById(anyString(), anyString());
  }

  /**
   * CLM-40971 I3: InterruptedException wrapped in RuntimeException (e.g. by a flow processor's
   * blocking call) must still be detected and routed to unacquire — otherwise the retry policy
   * burns retry budget on a worker-shutdown signal.
   */
  @Test
  public void testOnJobFailureFor_wrappedInterruptedExceptionUnacquires() {
    RuntimeException wrapped = new RuntimeException("eval failed",
        new RuntimeException("intermediate", new InterruptedException("shutting down")));

    underTest.onJobFailure(item(QUEUE_ID, 0), wrapped);

    verify(queueDAO).unacquire(QUEUE_ID);
    verify(queueDAO, never()).markRetry(anyString(), anyString());
    verify(queueDAO, never()).deleteById(anyString(), anyString());
  }

  /**
   * AT-013 — operators can override {@code maxContinuousMonitoringRetries} at runtime; the
   * retry-vs-delete decision must read the current configured value, not a startup snapshot.
   * Configures the override to 1 (so currentAttempt=1 already exhausts) and asserts the row is
   * deleted instead of re-queued for the same retryCount that would mark-retry under the default.
   * <p>
   * Doubles as the CLM-40971 boundary test for {@code maxRetries=1} — the property's documented
   * "single attempt, no retries" semantic — by demonstrating that the very first failure deletes
   * the row instead of re-queueing it.
   */
  @Test
  public void testOnJobFailure_honorsRuntimeOverrideOfMaxRetries() {
    // With default MAX_RETRIES (3) and retryCount=0, a transient failure marks-retry.
    // After lowering the budget to 1, the same row's currentAttempt (=1) reaches the limit
    // and is deleted instead.
    when(configuration.getMaxContinuousMonitoringRetries()).thenReturn(1);

    underTest.onJobFailure(item(QUEUE_ID, 0), new ConnectException("connect refused"));

    verify(queueDAO).deleteById(eq(QUEUE_ID), anyString());
    verify(queueDAO, never()).markRetry(anyString(), anyString());
  }

  // --- recovery and config-driven knobs ---------------------------------

  @Test
  public void testRecoverStaleJobs_delegatesToDao() {
    when(queueDAO.resetInProgressToPending(anyString())).thenReturn(2);

    underTest.recoverStaleJobs();

    // Worker-scoped reset: only this consumer's previous-incarnation rows should be touched.
    verify(queueDAO).resetInProgressToPending(anyString());
  }

  @Test
  public void testGetMaxRetries_rereadsConfigurationOnEachCall() {
    assertThat(underTest.getMaxRetries()).isEqualTo(MAX_RETRIES);

    when(configuration.getMaxContinuousMonitoringRetries()).thenReturn(7);
    assertThat(underTest.getMaxRetries()).isEqualTo(7);
  }

  @Test
  public void testGetWorkerThreadCount_rereadsConfigurationOnEachCall() {
    assertThat(underTest.getWorkerThreadCount()).isEqualTo(WORKER_THREADS);

    when(configuration.getContinuousMonitoringWorkerThreads()).thenReturn(16);
    assertThat(underTest.getWorkerThreadCount()).isEqualTo(16);
  }

  // --- defense-in-depth fallbacks for tickBatchSize and idleBackoffMs ---------------------
  // REST validates the bounds, but a value injected directly into the DB (backup restore,
  // manual UPDATE) must not blow up the consumer or stop dispatch silently. Each out-of-range
  // value must fall back to the default and log a warning.

  @Test
  public void testReadTickBatchSize_nullReturnsDefault() {
    when(configuration.getContinuousMonitoringTickBatchSize()).thenReturn(null);
    assertThat(RepositoryEvaluationQueueConsumer.readTickBatchSize(configuration))
        .isEqualTo(RepositoryEvaluationQueueConsumer.DEFAULT_TICK_BATCH_SIZE);
  }

  @Test
  public void testReadTickBatchSize_belowFloorFallsBackToDefault() {
    when(configuration.getContinuousMonitoringTickBatchSize()).thenReturn(0);
    assertThat(RepositoryEvaluationQueueConsumer.readTickBatchSize(configuration))
        .isEqualTo(RepositoryEvaluationQueueConsumer.DEFAULT_TICK_BATCH_SIZE);
  }

  @Test
  public void testReadTickBatchSize_aboveCeilingFallsBackToDefault() {
    when(configuration.getContinuousMonitoringTickBatchSize()).thenReturn(257);
    assertThat(RepositoryEvaluationQueueConsumer.readTickBatchSize(configuration))
        .isEqualTo(RepositoryEvaluationQueueConsumer.DEFAULT_TICK_BATCH_SIZE);
  }

  @Test
  public void testReadTickBatchSize_inRangeIsReturnedAsIs() {
    when(configuration.getContinuousMonitoringTickBatchSize()).thenReturn(32);
    assertThat(RepositoryEvaluationQueueConsumer.readTickBatchSize(configuration)).isEqualTo(32);
  }

  @Test
  public void testReadIdleBackoffMs_nullReturnsDefault() {
    when(configuration.getContinuousMonitoringIdleBackoffMs()).thenReturn(null);
    assertThat(RepositoryEvaluationQueueConsumer.readIdleBackoffMs(configuration))
        .isEqualTo(RepositoryEvaluationQueueConsumer.DEFAULT_IDLE_BACKOFF_MS);
  }

  @Test
  public void testReadIdleBackoffMs_negativeFallsBackToDefault() {
    when(configuration.getContinuousMonitoringIdleBackoffMs()).thenReturn(-1);
    assertThat(RepositoryEvaluationQueueConsumer.readIdleBackoffMs(configuration))
        .isEqualTo(RepositoryEvaluationQueueConsumer.DEFAULT_IDLE_BACKOFF_MS);
  }

  @Test
  public void testReadIdleBackoffMs_aboveCeilingFallsBackToDefault() {
    when(configuration.getContinuousMonitoringIdleBackoffMs()).thenReturn(10_001);
    assertThat(RepositoryEvaluationQueueConsumer.readIdleBackoffMs(configuration))
        .isEqualTo(RepositoryEvaluationQueueConsumer.DEFAULT_IDLE_BACKOFF_MS);
  }

  @Test
  public void testReadIdleBackoffMs_zeroIsValid() {
    when(configuration.getContinuousMonitoringIdleBackoffMs()).thenReturn(0);
    assertThat(RepositoryEvaluationQueueConsumer.readIdleBackoffMs(configuration)).isEqualTo(0L);
  }

  @Test
  public void testReadIdleBackoffMs_inRangeIsReturnedAsIs() {
    when(configuration.getContinuousMonitoringIdleBackoffMs()).thenReturn(500);
    assertThat(RepositoryEvaluationQueueConsumer.readIdleBackoffMs(configuration)).isEqualTo(500L);
  }

  // --- configurationChanged ---------------------------------------------
  // I6: when HOSTED_REPOSITORY_EVALUATION is toggled off at runtime the consumer must cancel its
  // ScheduledFuture; symmetric reschedule must fire on a poll-interval change. The previous
  // synthetic-delta approach is replaced by a real per-tenant snapshot — these tests verify that
  // configurationChanged passes the actual last-applied values to handleConfigurationChanged so
  // the base class's reschedule guard fires only on real changes.

  @Test
  public void testConfigurationChanged_featureToggleOffPassesRealPreviousEnabled() {
    when(configuration.getContinuousMonitoringPollIntervalMs()).thenReturn(60_000);
    RecordingConsumer recording = new RecordingConsumer();
    // Warm the snapshot to (60000, true) — feature flag is enabled per setup().
    recording.configurationChanged(Set.of(SystemConfigurationProperty.CONTINUOUS_MONITORING_POLL_INTERVAL_MS));
    recording.lastCall = null;

    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
    recording.configurationChanged(Set.of(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION));

    assertThat(recording.lastCall)
        .isEqualTo(new HandleCall(WORKER_THREADS, 60_000L, false, 60_000L, true));
  }

  @Test
  public void testConfigurationChanged_pollIntervalChangePassesRealPreviousInterval() {
    when(configuration.getContinuousMonitoringPollIntervalMs()).thenReturn(60_000);
    RecordingConsumer recording = new RecordingConsumer();
    recording.configurationChanged(Set.of(SystemConfigurationProperty.CONTINUOUS_MONITORING_POLL_INTERVAL_MS));
    recording.lastCall = null;

    when(configuration.getContinuousMonitoringPollIntervalMs()).thenReturn(120_000);
    recording.configurationChanged(Set.of(SystemConfigurationProperty.CONTINUOUS_MONITORING_POLL_INTERVAL_MS));

    assertThat(recording.lastCall)
        .isEqualTo(new HandleCall(WORKER_THREADS, 120_000L, true, 60_000L, true));
  }

  @Test
  public void testConfigurationChanged_unrelatedPropertyIsNoOp() {
    RecordingConsumer recording = new RecordingConsumer();
    recording.configurationChanged(Set.of("unrelated.property"));
    assertThat(recording.lastCall).isNull();
  }

  // --- helpers -----------------------------------------------------------

  private static ContinuousMonitoringQueueItem item(final String id, final int retryCount) {
    ContinuousMonitoringQueueItem item =
        new ContinuousMonitoringQueueItem(id, ContinuousMonitoringFlowType.HOSTED_REPO, 0L, new Date());
    item.setRetryCount(retryCount);
    return item;
  }

  private static ContinuousMonitoringFlowProcessor mockProcessor(final ContinuousMonitoringFlowType type) {
    ContinuousMonitoringFlowProcessor p = org.mockito.Mockito.mock(ContinuousMonitoringFlowProcessor.class);
    when(p.getFlowType()).thenReturn(type);
    return p;
  }

  /**
   * Captures the arguments passed to {@code handleConfigurationChanged} without running the
   * (private) base-class {@code reschedule()} — that would require a tenant-scoped scheduled
   * executor that this unit test deliberately doesn't wire. Subclassing is the only way to reach
   * the protected hook from a sibling package.
   */
  private final class RecordingConsumer
      extends RepositoryEvaluationQueueConsumer
  {
    HandleCall lastCall;

    RecordingConsumer() {
      super(shutdownHandler, queueDAO, Set.of(hostedRepoProcessor), configuration, null);
    }

    @Override
    protected void handleConfigurationChanged(
        final int newWorkerCount,
        final long newPollIntervalMs,
        final boolean enabled,
        final long oldPollIntervalMs,
        final boolean wasEnabled)
    {
      lastCall = new HandleCall(newWorkerCount, newPollIntervalMs, enabled, oldPollIntervalMs, wasEnabled);
    }
  }

  private record HandleCall(
      int newWorkerCount,
      long newPollIntervalMs,
      boolean enabled,
      long oldPollIntervalMs,
      boolean wasEnabled)
  {
  }
}
