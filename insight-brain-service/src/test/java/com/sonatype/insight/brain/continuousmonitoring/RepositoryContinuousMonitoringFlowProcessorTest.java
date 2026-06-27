/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringHostedRepoItemDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.dataaccess.TransactionContext;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RepositoryContinuousMonitoringFlowProcessor} (CLM-40039 Section 6.3).
 * Confirms the processor (a) drops queue items whose state is no longer eligible — defense-in-depth
 * checks beyond the producer's filter — and (b) builds an evaluation request that carries all
 * components for the (repository, hash) pair through to {@link RepositoryPolicyEvaluator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryContinuousMonitoringFlowProcessorTest
{
  private static final String QUEUE_ID = "queue-123";

  private static final String REPO_ID = "repo-A";

  private static final String HASH = "deadbeef";

  @Mock
  private ContinuousMonitoringHostedRepoItemDAO hostedRepoItemDAO;

  @Mock
  private RepositoryDAO repositoryDAO;

  @Mock
  private RepositoryComponentDAO repositoryComponentDAO;

  @Mock
  private RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  @Mock
  private TransactionContext tx;

  private SimpleMeterRegistry meterRegistry;

  private RepositoryContinuousMonitoringFlowProcessor underTest;

  @Before
  public void setup() {
    when(hostedRepoItemDAO.createTransactionContext()).thenReturn(tx);
    meterRegistry = new SimpleMeterRegistry();
    underTest =
        new RepositoryContinuousMonitoringFlowProcessor(hostedRepoItemDAO, repositoryDAO, repositoryComponentDAO,
            repositoryPolicyEvaluator, meterRegistry);
  }

  @Test
  public void getFlowTypeIsHostedRepo() {
    assertThat(underTest.getFlowType()).isEqualTo(ContinuousMonitoringFlowType.HOSTED_REPO);
  }

  @Test
  public void testProcess_dropsWhenSatelliteMissing() {
    when(hostedRepoItemDAO.getByQueueIds(any(TransactionContext.class), anyList())).thenReturn(List.of());

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any());
    assertDropMetric("satellite-missing", 1L);
  }

  /**
   * CLM-40971 follow-up: defense-in-depth drop branches around null FK columns on the satellite
   * row. Schema declares them NOT NULL but a backup restore or partial migration could violate
   * that — the processor returns silently rather than NPE-ing the consumer.
   */
  @Test
  public void testProcess_dropsWhenSatelliteHasNullRepositoryId() {
    ContinuousMonitoringHostedRepoItem satellite = new ContinuousMonitoringHostedRepoItem();
    satellite.setQueueId(QUEUE_ID);
    satellite.setRepositoryId(null);
    satellite.setComponentHash(HASH);
    when(hostedRepoItemDAO.getByQueueIds(any(TransactionContext.class), anyList()))
        .thenReturn(List.of(satellite));

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any());
    verify(repositoryDAO, never()).getById(any());
    assertDropMetric("satellite-null-repository-id", 1L);
  }

  @Test
  public void testProcess_dropsWhenSatelliteHasNullComponentHash() {
    ContinuousMonitoringHostedRepoItem satellite = new ContinuousMonitoringHostedRepoItem();
    satellite.setQueueId(QUEUE_ID);
    satellite.setRepositoryId(REPO_ID);
    satellite.setComponentHash(null);
    when(hostedRepoItemDAO.getByQueueIds(any(TransactionContext.class), anyList()))
        .thenReturn(List.of(satellite));

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any());
    verify(repositoryDAO, never()).getById(any());
    assertDropMetric("satellite-null-component-hash", 1L);
  }

  @Test
  public void testProcess_dropsWhenRepositoryNoLongerExists() {
    stubSatellite();
    when(repositoryDAO.getById(REPO_ID)).thenReturn(null);

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any());
    assertDropMetric("repository-deleted", 1L);
  }

  @Test
  public void testProcess_dropsWhenRepositoryIsNotHosted() {
    stubSatellite();
    Repository proxyRepo = repository(REPO_ID, RepositoryType.proxy, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(proxyRepo);

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any());
    assertDropMetric("repository-not-hosted", 1L);
  }

  @Test
  public void testProcess_dropsWhenMonitoringTurnedOffSinceEnqueue() {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, false, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any());
    assertDropMetric("monitoring-disabled", 1L);
  }

  @Test
  public void testProcess_dropsWhenNoComponentsFoundForHash() {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    when(repositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of());

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any());
    assertDropMetric("no-components-for-hash", 1L);
  }

  @Test
  public void testProcess_dropsWhenRepositoryFormatIsMissing() {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, null);
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    when(repositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH))
        .thenReturn(List.of(component(HASH, "/x.jar", null)));

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any());
    assertDropMetric("repository-no-format", 1L);
  }

  @Test
  public void testProcess_dropsWhenNoEvaluatableComponents() {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    // Components exist for the hash, but every one is missing either hash or pathname so
    // request.components ends up empty — the 9th drop branch (no-evaluatable-components).
    RepositoryComponent nullPath = component(HASH, null, null);
    RepositoryComponent nullHash = component(null, "/x.jar", null);
    when(repositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of(nullPath, nullHash));

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any());
    assertDropMetric("no-evaluatable-components", 1L);
  }

  @Test
  public void processBuildsEvaluationRequestWithContinuousMonitoringCause() {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    RepositoryComponent c1 = component(HASH, "lib/a.jar", "stage-release");
    RepositoryComponent c2 = component(HASH, "lib/b.jar", null);
    when(repositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of(c1, c2));

    underTest.process(queueItem());

    ArgumentCaptor<RepositoryComponentEvaluationDataRequestList> captor =
        ArgumentCaptor.forClass(RepositoryComponentEvaluationDataRequestList.class);
    verify(repositoryPolicyEvaluator).evaluateForMonitoring(any(Repository.class), captor.capture(),
        any(String.class));
    RepositoryComponentEvaluationDataRequestList actual = captor.getValue();
    assertThat(actual.cause).isEqualTo(RepositoryPolicyEvaluator.CONTINUOUS_MONITORING_CAUSE);
    assertThat(actual.components).hasSize(2);
  }

  @Test
  public void processSkipsComponentsMissingHashOrPathname() {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    RepositoryComponent good = component(HASH, "lib/good.jar", null);
    RepositoryComponent missingPath = component(HASH, null, null);
    when(repositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of(good, missingPath));

    underTest.process(queueItem());

    ArgumentCaptor<RepositoryComponentEvaluationDataRequestList> captor =
        ArgumentCaptor.forClass(RepositoryComponentEvaluationDataRequestList.class);
    verify(repositoryPolicyEvaluator).evaluateForMonitoring(any(Repository.class), captor.capture(),
        any(String.class));
    assertThat(captor.getValue().components).hasSize(1);
  }

  // --- helpers -----------------------------------------------------------

  private static ContinuousMonitoringQueueItem queueItem() {
    return new ContinuousMonitoringQueueItem(QUEUE_ID, ContinuousMonitoringFlowType.HOSTED_REPO, 0L, new Date());
  }

  private void stubSatellite() {
    ContinuousMonitoringHostedRepoItem satellite = new ContinuousMonitoringHostedRepoItem(QUEUE_ID, REPO_ID, HASH);
    when(hostedRepoItemDAO.getByQueueIds(any(TransactionContext.class), anyList())).thenReturn(List.of(satellite));
  }

  private static Repository repository(
      final String id,
      final RepositoryType type,
      final boolean monitoringEnabled,
      final String format)
  {
    Repository repo = new Repository();
    repo.setId(id);
    repo.setRepositoryType(type);
    repo.setMonitoringEnabled(monitoringEnabled);
    repo.setFormat(format);
    return repo;
  }

  /**
   * Asserts that the named drop reason has been counted exactly {@code expected} times. Drives
   * the CLM-40971 observability requirement that each defense-in-depth drop branch lights up a
   * tagged counter, so operators can distinguish data-integrity patterns from real evaluations.
   */
  private void assertDropMetric(final String reason, final long expected) {
    assertThat(meterRegistry.counter(
        RepositoryContinuousMonitoringFlowProcessor.DROP_METRIC_NAME, "reason", reason).count())
            .as("drop metric reason=%s", reason)
            .isEqualTo((double) expected);
  }

  private static RepositoryComponent component(final String hash, final String pathname, final String stage) {
    RepositoryComponent c = new RepositoryComponent();
    c.setRepositoryId(REPO_ID);
    c.setHash(hash);
    c.setPathname(pathname);
    c.setLastEvaluationStage(stage);
    return c;
  }
}
