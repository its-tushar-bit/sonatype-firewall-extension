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
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.hosted.ApplicationForHostedRepositoryComponentService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @Mock
  private RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  @Mock
  private ReportService reportService;

  @Mock
  private ApplicationForHostedRepositoryComponentService applicationForHostedRepositoryComponentService;

  @Mock
  private TransactionContext tx;

  private SimpleMeterRegistry meterRegistry;

  private RepositoryContinuousMonitoringFlowProcessor underTest;

  @Before
  public void setup() {
    when(hostedRepoItemDAO.createTransactionContext()).thenReturn(tx);
    meterRegistry = new SimpleMeterRegistry();
    underTest =
        new RepositoryContinuousMonitoringFlowProcessor(hostedRepoItemDAO, repositoryDAO, proxyRepositoryComponentDAO,
            repositoryPolicyEvaluator, reportService, applicationForHostedRepositoryComponentService,
            meterRegistry);
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
    when(proxyRepositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of());

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any());
    assertDropMetric("no-components-for-hash", 1L);
  }

  @Test
  public void testProcess_dropsWhenRepositoryFormatIsMissing() {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, null);
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    when(proxyRepositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH))
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
    ProxyRepositoryComponent nullPath = component(HASH, null, null);
    ProxyRepositoryComponent nullHash = component(null, "/x.jar", null);
    when(proxyRepositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of(nullPath, nullHash));

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any());
    assertDropMetric("no-evaluatable-components", 1L);
  }

  @Test
  public void processBuildsEvaluationRequestWithContinuousMonitoringCause() {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    ProxyRepositoryComponent c1 = component(HASH, "lib/a.jar", "stage-release");
    ProxyRepositoryComponent c2 = component(HASH, "lib/b.jar", null);
    when(proxyRepositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of(c1, c2));

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
    ProxyRepositoryComponent good = component(HASH, "lib/good.jar", null);
    ProxyRepositoryComponent missingPath = component(HASH, null, null);
    when(proxyRepositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of(good, missingPath));

    underTest.process(queueItem());

    ArgumentCaptor<RepositoryComponentEvaluationDataRequestList> captor =
        ArgumentCaptor.forClass(RepositoryComponentEvaluationDataRequestList.class);
    verify(repositoryPolicyEvaluator).evaluateForMonitoring(any(Repository.class), captor.capture(),
        any(String.class));
    assertThat(captor.getValue().components).hasSize(1);
  }

  @Test
  public void processRefreshesOverlaysAfterEvaluation() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    ProxyRepositoryComponent c = component(HASH, "lib/a.jar", "stage-release", "scan-1");
    when(proxyRepositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of(c));
    Application app = application("app-1");
    when(applicationForHostedRepositoryComponentService.getOrCreateApplication(REPO_ID, "lib/a.jar"))
        .thenReturn(app);

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator).evaluateForMonitoring(any(Repository.class),
        any(RepositoryComponentEvaluationDataRequestList.class), any(String.class));
    // CM must NOT persist a new policy_evaluation row on every cycle (would bloat Latest Evaluations),
    // so persistPolicyEvaluationRow=false. ReportService does the mirror + overlay writes internally.
    verify(reportService).refreshHostedComponentAfterEvaluation(
        eq(c), eq(repo), eq(app), eq("app-1"), eq("scan-1"), eq("stage-release"), eq(false));
  }

  @Test
  public void processSkipsRefreshWhenScanIdIsNull() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    // Component was never NXRM-scanned so no scanId exists — nothing on disk to refresh.
    ProxyRepositoryComponent c = component(HASH, "lib/a.jar", null, null);
    when(proxyRepositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of(c));

    underTest.process(queueItem());

    verify(repositoryPolicyEvaluator).evaluateForMonitoring(any(Repository.class),
        any(RepositoryComponentEvaluationDataRequestList.class), any(String.class));
    verify(reportService, never()).refreshHostedComponentAfterEvaluation(
        any(), any(), any(), any(), any(), any(), anyBoolean());
    verify(applicationForHostedRepositoryComponentService, never()).getOrCreateApplication(any(), any());
  }

  @Test
  public void processSwallowsRefreshExceptionAndContinuesBatch() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    ProxyRepositoryComponent c1 = component(HASH, "lib/a.jar", null, "scan-1");
    ProxyRepositoryComponent c2 = component(HASH, "lib/b.jar", null, "scan-2");
    when(proxyRepositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of(c1, c2));
    Application app1 = application("app-1");
    Application app2 = application("app-2");
    when(applicationForHostedRepositoryComponentService.getOrCreateApplication(REPO_ID, "lib/a.jar"))
        .thenReturn(app1);
    when(applicationForHostedRepositoryComponentService.getOrCreateApplication(REPO_ID, "lib/b.jar"))
        .thenReturn(app2);
    doThrow(new RuntimeException("disk full")).when(reportService)
        .refreshHostedComponentAfterEvaluation(
            eq(c1), any(Repository.class), eq(app1), eq("app-1"), eq("scan-1"), any(), anyBoolean());

    underTest.process(queueItem());

    // First component blew up; second must still be attempted so a single bad component doesn't
    // poison the whole CM batch.
    verify(reportService).refreshHostedComponentAfterEvaluation(
        eq(c1), eq(repo), eq(app1), eq("app-1"), eq("scan-1"), any(), eq(false));
    verify(reportService).refreshHostedComponentAfterEvaluation(
        eq(c2), eq(repo), eq(app2), eq("app-2"), eq("scan-2"), any(), eq(false));
    assertDropMetric("overlay-refresh-failed", 1L);
  }

  /**
   * CLM-42136 (F1): {@code getOrCreateApplication} returns null when the repository has no
   * valid parent organization (root-org lookup would fail). We must not NPE on
   * {@code application.getId()} — instead skip the component with a dedicated drop metric
   * so operators can distinguish this case from a mid-refresh exception.
   */
  @Test
  public void processSkipsRefreshWhenApplicationCannotBeCreated() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    ProxyRepositoryComponent c = component(HASH, "lib/a.jar", null, "scan-1");
    when(proxyRepositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of(c));
    when(applicationForHostedRepositoryComponentService.getOrCreateApplication(REPO_ID, "lib/a.jar"))
        .thenReturn(null);

    underTest.process(queueItem());

    verify(reportService, never()).refreshHostedComponentAfterEvaluation(
        any(), any(), any(), any(), any(), any(), anyBoolean());
    assertDropMetric("overlay-refresh-no-application", 1L);
  }

  /**
   * CLM-42136 (F5): the refresh loop must record a drop metric when a component is missing
   * its scanId or pathname, rather than silently continuing. Previously the {@code continue}
   * was silent, leaving no signal for operators.
   */
  @Test
  public void processRecordsDropMetricWhenComponentMissingIdentifier() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    // Two components in the batch: one has a valid pathname but no scanId (never persisted to
    // a scan file yet), one is a valid target for refresh. Only the first should be dropped.
    ProxyRepositoryComponent noScanId = component(HASH, "lib/a.jar", null, null);
    ProxyRepositoryComponent good = component(HASH, "lib/b.jar", null, "scan-b");
    when(proxyRepositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of(noScanId, good));
    when(applicationForHostedRepositoryComponentService.getOrCreateApplication(REPO_ID, "lib/b.jar"))
        .thenReturn(application("app-b"));

    underTest.process(queueItem());

    assertDropMetric("overlay-refresh-missing-identifier", 1L);
    verify(reportService).refreshHostedComponentAfterEvaluation(
        eq(good), eq(repo), any(), eq("app-b"), eq("scan-b"), any(), eq(false));
    verify(reportService, never()).refreshHostedComponentAfterEvaluation(
        eq(noScanId), any(), any(), any(), any(), any(), anyBoolean());
  }

  @Test
  public void processRefreshesOverlaysForEachComponentInBatch() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    ProxyRepositoryComponent c1 = component(HASH, "lib/a.jar", null, "scan-1");
    ProxyRepositoryComponent c2 = component(HASH, "lib/b.jar", null, "scan-2");
    ProxyRepositoryComponent c3 = component(HASH, "lib/c.jar", null, "scan-3");
    when(proxyRepositoryComponentDAO.getByRepositoryIdAndHash(REPO_ID, HASH)).thenReturn(List.of(c1, c2, c3));
    Application app1 = application("app-1");
    Application app2 = application("app-2");
    Application app3 = application("app-3");
    when(applicationForHostedRepositoryComponentService.getOrCreateApplication(REPO_ID, "lib/a.jar"))
        .thenReturn(app1);
    when(applicationForHostedRepositoryComponentService.getOrCreateApplication(REPO_ID, "lib/b.jar"))
        .thenReturn(app2);
    when(applicationForHostedRepositoryComponentService.getOrCreateApplication(REPO_ID, "lib/c.jar"))
        .thenReturn(app3);

    underTest.process(queueItem());

    verify(reportService).refreshHostedComponentAfterEvaluation(
        eq(c1), eq(repo), eq(app1), eq("app-1"), eq("scan-1"), any(), eq(false));
    verify(reportService).refreshHostedComponentAfterEvaluation(
        eq(c2), eq(repo), eq(app2), eq("app-2"), eq("scan-2"), any(), eq(false));
    verify(reportService).refreshHostedComponentAfterEvaluation(
        eq(c3), eq(repo), eq(app3), eq("app-3"), eq("scan-3"), any(), eq(false));
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

  private static ProxyRepositoryComponent component(final String hash, final String pathname, final String stage) {
    return component(hash, pathname, stage, null);
  }

  private static ProxyRepositoryComponent component(
      final String hash,
      final String pathname,
      final String stage,
      final String scanId)
  {
    ProxyRepositoryComponent c = new ProxyRepositoryComponent();
    c.setRepositoryId(REPO_ID);
    c.setHash(hash);
    c.setPathname(pathname);
    c.setLastEvaluationStage(stage);
    c.setScanId(scanId);
    return c;
  }

  private static Application application(final String id) {
    Application app = new Application();
    app.setId(id);
    return app;
  }
}
