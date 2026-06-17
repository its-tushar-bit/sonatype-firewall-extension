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
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;

import org.junit.Before;
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

  @Before
  public void setup() {
    when(hostedRepoProcessor.getFlowType()).thenReturn(ContinuousMonitoringFlowType.HOSTED_REPO);
    when(configuration.getContinuousMonitoringWorkerThreads()).thenReturn(WORKER_THREADS);
    when(configuration.getMaxContinuousMonitoringRetries()).thenReturn(MAX_RETRIES);
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
    verify(queueDAO, never()).deleteById(anyString());
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

    verify(queueDAO).deleteById(QUEUE_ID);
  }

  @Test
  public void testUnacquireJobs_marksRetryForEachId() {
    underTest.unacquireJobs(Set.of("a", "b"));

    verify(queueDAO).markRetry(eq("a"), anyString());
    verify(queueDAO).markRetry(eq("b"), anyString());
  }

  @Test
  public void testPermanentlyFailJob_deletesQueueRow() {
    underTest.permanentlyFailJob(item(QUEUE_ID, 1), new RuntimeException("boom"));

    verify(queueDAO).deleteById(QUEUE_ID);
  }

  // --- retry policy branches (Section 7.1 / 7.2) ------------------------

  @Test
  public void testOnJobFailure_marksRetryForTransientUnderLimit() {
    underTest.onJobFailure(item(QUEUE_ID, 0), new ConnectException("connect refused"));

    verify(queueDAO).markRetry(eq(QUEUE_ID), anyString());
    verify(queueDAO, never()).deleteById(anyString());
  }

  @Test
  public void testOnJobFailure_deletesWhenRetryLimitExhausted() {
    // retryCount == 2 → currentAttempt == 3 == maxRetries → delete.
    // DBCP signals pool-timeout via SQLException whose message starts with "Cannot get a connection".
    underTest.onJobFailure(item(QUEUE_ID, MAX_RETRIES - 1),
        new SQLException("Cannot get a connection, pool error Timeout waiting for idle object"));

    verify(queueDAO).deleteById(QUEUE_ID);
    verify(queueDAO, never()).markRetry(anyString(), anyString());
  }

  @Test
  public void testOnJobFailure_deletesForNonRetryableException() {
    underTest.onJobFailure(item(QUEUE_ID, 0), new IllegalArgumentException("bad input"));

    verify(queueDAO).deleteById(QUEUE_ID);
    verify(queueDAO, never()).markRetry(anyString(), anyString());
  }

  @Test
  public void testOnJobFailureFor_interruptedExceptionMarksRetryWithoutDelete() {
    underTest.onJobFailure(item(QUEUE_ID, 0), new InterruptedException("shutting down"));

    verify(queueDAO).markRetry(eq(QUEUE_ID), anyString());
    verify(queueDAO, never()).deleteById(anyString());
  }

  /**
   * AT-013 — operators can override {@code maxContinuousMonitoringRetries} at runtime; the
   * retry-vs-delete decision must read the current configured value, not a startup snapshot.
   * Configures the override to 1 (so currentAttempt=1 already exhausts) and asserts the row is
   * deleted instead of re-queued for the same retryCount that would mark-retry under the default.
   */
  @Test
  public void testOnJobFailure_honorsRuntimeOverrideOfMaxRetries() {
    // With default MAX_RETRIES (3) and retryCount=0, a transient failure marks-retry.
    // After lowering the budget to 1, the same row's currentAttempt (=1) reaches the limit
    // and is deleted instead.
    when(configuration.getMaxContinuousMonitoringRetries()).thenReturn(1);

    underTest.onJobFailure(item(QUEUE_ID, 0), new ConnectException("connect refused"));

    verify(queueDAO).deleteById(QUEUE_ID);
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
}
