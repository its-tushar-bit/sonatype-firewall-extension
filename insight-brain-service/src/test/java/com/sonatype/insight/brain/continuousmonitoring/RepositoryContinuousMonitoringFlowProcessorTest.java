/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.continuousmonitoring.ContinuousMonitoringHostedRepoItemDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.repository.hosted.HostedRepositoryComponentResolver;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.scan.model.ClientScanType;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RepositoryContinuousMonitoringFlowProcessor} (CLM-40039 Section 6.3).
 * Confirms the processor (a) drops queue items whose state is no longer eligible — defense-in-depth
 * checks beyond the producer's filter — and (b) monitors each hosted-repository component sharing the
 * queued (repository, hash) pair independently, cloning its stored scan and re-evaluating it through
 * {@link ScanPolicyEvaluator#evaluateForMonitoring}, so one bad candidate never stops its siblings.
 */
@ExtendWith(MockitoExtension.class)
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
  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Mock
  private TransactionContext tx;

  @Mock
  private HostedRepositoryComponentResolver resolver;

  @Mock
  private ScanPersistenceService scanPersistenceService;

  @Mock
  private ScanUploader scanUploader;

  @Mock
  private ScanPolicyEvaluator scanPolicyEvaluator;

  @Mock
  private PolicyEvaluationDAO policyEvaluationDAO;

  private SimpleMeterRegistry meterRegistry;

  private RepositoryContinuousMonitoringFlowProcessor underTest;

  @BeforeEach
  public void setup() {
    lenient().when(hostedRepoItemDAO.createTransactionContext()).thenReturn(tx);
    lenient().when(hostedRepositoryComponentDAO.createTransactionContext()).thenReturn(tx);
    meterRegistry = new SimpleMeterRegistry();
    underTest =
        new RepositoryContinuousMonitoringFlowProcessor(hostedRepoItemDAO, repositoryDAO,
            hostedRepositoryComponentDAO, meterRegistry, resolver, scanPersistenceService, scanUploader,
            scanPolicyEvaluator, policyEvaluationDAO);
    stubEvaluationsFromComponentIds();
  }

  @Test
  public void getFlowTypeIsHostedRepo() {
    assertThat(underTest.getFlowType()).isEqualTo(ContinuousMonitoringFlowType.HOSTED_REPO);
  }

  @Test
  public void testProcess_dropsWhenSatelliteMissing() {
    when(hostedRepoItemDAO.getByQueueIds(any(TransactionContext.class), anyList())).thenReturn(List.of());

    underTest.process(queueItem());

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

    verify(repositoryDAO, never()).getById(any());
    assertDropMetric("satellite-null-component-hash", 1L);
  }

  @Test
  public void testProcess_dropsWhenRepositoryNoLongerExists() {
    stubSatellite();
    when(repositoryDAO.getById(REPO_ID)).thenReturn(null);

    underTest.process(queueItem());

    assertDropMetric("repository-deleted", 1L);
  }

  @Test
  public void testProcess_dropsWhenRepositoryIsNotHosted() {
    stubSatellite();
    Repository proxyRepo = repository(REPO_ID, RepositoryType.proxy, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(proxyRepo);

    underTest.process(queueItem());

    assertDropMetric("repository-not-hosted", 1L);
  }

  @Test
  public void testProcess_dropsWhenMonitoringTurnedOffSinceEnqueue() {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, false, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);

    underTest.process(queueItem());

    assertDropMetric("monitoring-disabled", 1L);
  }

  @Test
  public void testProcess_dropsWhenNoComponentsFoundForHash() {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of());

    underTest.process(queueItem());

    assertDropMetric("no-components-for-hash", 1L);
  }

  /**
   * A repository with no {@code format} is still monitored. The per-component path resolves an owner,
   * uploads the cloned scan and evaluates it — none of which takes a format — so a format-null
   * repository has no reason to be excluded. Dropping it here would silently disable monitoring for
   * those repositories on every cycle.
   */
  @Test
  public void process_repositoryWithoutFormat_isStillMonitored() throws Exception {
    stubSatellite();
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repository(REPO_ID, RepositoryType.hosted, true, null));
    HostedRepositoryComponent hrc = component(HASH, "a.tgz", "stage-release", "s-a");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of(hrc));
    ScanEntity tempScan = mock(ScanEntity.class);
    when(scanPersistenceService.getScan(any(), any())).thenReturn(mock(ScanEntity.class));
    when(scanPersistenceService.createTempScan(any())).thenReturn(tempScan);
    when(scanUploader.upload(eq(tempScan), eq(hrc), eq("stage-release"), any(), any(), eq(true)))
        .thenReturn(scanReceipt("fresh-1"));

    underTest.process(queueItem());

    verify(scanPolicyEvaluator).evaluateForMonitoring(eq(hrc), eq("fresh-1"), eq(new Stage("stage-release")),
        eq(ScanTriggerType.HOSTED_REPOSITORY_SCANNING), eq(ClientScanType.SONATYPE));
    assertThat(meterRegistry.getMeters()).isEmpty();
  }

  @Test
  public void testProcess_dropsWhenNoEvaluatableComponents() {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    // Candidates exist for the hash but neither has an evaluation to re-upload. There is no batch
    // pre-flight gate any more: each is dropped on its own, so the count is per component.
    HostedRepositoryComponent neitherEvaluated = component(HASH, "a.tgz", "stage-release", null);
    HostedRepositoryComponent norThisOne = component(HASH, "b.tgz", "stage-release", null);
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH))
        .thenReturn(List.of(neitherEvaluated, norThisOne));

    underTest.process(queueItem());

    assertDropMetric("cm-no-previous-evaluation", 2L);
  }

  @Test
  public void process_happyPath_perComponentUploadEvaluateAndPin() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    HostedRepositoryComponent hrc1 = component(HASH, "a.tgz", "stage-release", "s-a");
    HostedRepositoryComponent hrc2 = component(HASH, "b.tgz", "stage-release", "s-b");
    HostedRepositoryComponent hrc3 = component(HASH, "c.tgz", "stage-release", "s-c");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH))
        .thenReturn(List.of(hrc1, hrc2, hrc3));
    ScanEntity sourceScan = mock(ScanEntity.class);
    ScanEntity tempScan = mock(ScanEntity.class);
    when(scanPersistenceService.getScan(any(), any())).thenReturn(sourceScan);
    when(scanPersistenceService.createTempScan(any())).thenReturn(tempScan);
    when(scanUploader.upload(eq(tempScan), eq(hrc1), eq("stage-release"), any(), any(), eq(true)))
        .thenReturn(scanReceipt("fresh-1"));
    when(scanUploader.upload(eq(tempScan), eq(hrc2), eq("stage-release"), any(), any(), eq(true)))
        .thenReturn(scanReceipt("fresh-2"));
    when(scanUploader.upload(eq(tempScan), eq(hrc3), eq("stage-release"), any(), any(), eq(true)))
        .thenReturn(scanReceipt("fresh-3"));

    underTest.process(queueItem());

    verify(scanPolicyEvaluator).evaluateForMonitoring(eq(hrc1), eq("fresh-1"), eq(new Stage("stage-release")),
        eq(ScanTriggerType.HOSTED_REPOSITORY_SCANNING), eq(ClientScanType.SONATYPE));
    verify(scanPolicyEvaluator).evaluateForMonitoring(eq(hrc2), eq("fresh-2"), eq(new Stage("stage-release")),
        eq(ScanTriggerType.HOSTED_REPOSITORY_SCANNING), eq(ClientScanType.SONATYPE));
    verify(scanPolicyEvaluator).evaluateForMonitoring(eq(hrc3), eq("fresh-3"), eq(new Stage("stage-release")),
        eq(ScanTriggerType.HOSTED_REPOSITORY_SCANNING), eq(ClientScanType.SONATYPE));
    verify(resolver).pinOwnerComponent(hrc1, "fresh-1", "stage-release");
    verify(resolver).pinOwnerComponent(hrc2, "fresh-2", "stage-release");
    verify(resolver).pinOwnerComponent(hrc3, "fresh-3", "stage-release");
    assertThat(meterRegistry.getMeters()).isEmpty();
  }

  /**
   * Every primary evaluation deletes the scan file it supersedes, so the scan a cycle read the id for can
   * be gone by the time it copies. Following the owner's latest primary evaluation forward recovers the
   * cycle instead of dropping the component until the next day.
   */
  @Test
  public void process_sourceScanSupersededMidClone_retriesWithTheNewerScan() throws Exception {
    stubSatellite();
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repository(REPO_ID, RepositoryType.hosted, true, "maven2"));
    HostedRepositoryComponent hrc = component(HASH, "a.tgz", "stage-release", "stale-scan");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of(hrc));
    ScanEntity staleScan = mock(ScanEntity.class);
    ScanEntity newerScan = mock(ScanEntity.class);
    ScanEntity tempScan = mock(ScanEntity.class);
    when(scanPersistenceService.createTempScan(hrc.getId())).thenReturn(tempScan);
    when(scanPersistenceService.getScan(hrc.getId(), "stale-scan")).thenReturn(staleScan);
    when(scanPersistenceService.getScan(hrc.getId(), "newer-scan")).thenReturn(newerScan);
    // The file behind the id the caller held has already been deleted; the newer one copies fine.
    doThrow(new java.io.IOException("no such file")).when(scanPersistenceService)
        .copyScanFile(staleScan, tempScan);
    when(policyEvaluationDAO.getLastPrimaryByOwnerIdAndStageId(hrc.getId(), "stage-release"))
        .thenReturn(policyEvaluation("newer-scan"));
    when(scanUploader.upload(eq(tempScan), eq(hrc), eq("stage-release"), any(), any(), eq(true)))
        .thenReturn(scanReceipt("fresh-1"));

    underTest.process(queueItem());

    verify(scanPersistenceService).copyScanFile(newerScan, tempScan);
    verify(scanPolicyEvaluator).evaluateForMonitoring(eq(hrc), eq("fresh-1"), eq(new Stage("stage-release")),
        eq(ScanTriggerType.HOSTED_REPOSITORY_SCANNING), eq(ClientScanType.SONATYPE));
    assertThat(meterRegistry.getMeters()).isEmpty();
  }

  /**
   * The retry must terminate. When the unreadable scan is still the owner's latest primary evaluation
   * there is nothing newer to follow, so the component is dropped for this cycle rather than looping.
   */
  @Test
  public void process_sourceScanUnreadableAndStillLatest_dropsWithoutLooping() throws Exception {
    stubSatellite();
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repository(REPO_ID, RepositoryType.hosted, true, "maven2"));
    HostedRepositoryComponent hrc = component(HASH, "a.tgz", "stage-release", "s-a");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of(hrc));
    ScanEntity sourceScan = mock(ScanEntity.class);
    ScanEntity tempScan = mock(ScanEntity.class);
    when(scanPersistenceService.createTempScan(hrc.getId())).thenReturn(tempScan);
    when(scanPersistenceService.getScan(hrc.getId(), "s-a")).thenReturn(sourceScan);
    doThrow(new java.io.IOException("no such file")).when(scanPersistenceService)
        .copyScanFile(sourceScan, tempScan);
    when(policyEvaluationDAO.getLastPrimaryByOwnerIdAndStageId(hrc.getId(), "stage-release"))
        .thenReturn(policyEvaluation("s-a"));

    underTest.process(queueItem());

    verify(scanPersistenceService, times(1)).copyScanFile(sourceScan, tempScan);
    verify(scanUploader, never()).upload(any(), any(HostedRepositoryComponent.class), any(), any(), any(),
        anyBoolean());
    assertDropMetric("cm-clone-scan-failed", 1L);
  }

  /**
   * The pointer advance is only meaningful if a scan actually exists under the scanId it names. The clone
   * uploaded to HDS must therefore be finalized under the fresh scanId rather than deleted with the temp
   * entity: the next cycle clones the scan back by that id, and Manual Re-Evaluate reads it to re-upload.
   * Advancing the pointer while discarding the file strands monitoring *and* makes Re-Evaluate fail on a
   * missing scan — worse than never advancing it.
   */
  @Test
  public void process_finalizesTheClonedScanUnderTheFreshScanId() throws Exception {
    stubSatellite();
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repository(REPO_ID, RepositoryType.hosted, true, "maven2"));
    HostedRepositoryComponent hrc = component(HASH, "a.tgz", "stage-release", "s-a");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of(hrc));
    ScanEntity tempScan = mock(ScanEntity.class);
    when(scanPersistenceService.getScan(any(), any())).thenReturn(mock(ScanEntity.class));
    when(scanPersistenceService.createTempScan(any())).thenReturn(tempScan);
    when(scanUploader.upload(eq(tempScan), eq(hrc), eq("stage-release"), any(), any(), eq(true)))
        .thenReturn(scanReceipt("fresh-1"));

    underTest.process(queueItem());

    verify(scanPersistenceService).moveTempScan(tempScan, hrc.getId(), "fresh-1");
    verify(scanPersistenceService, never()).deleteScan(tempScan);
    assertThat(meterRegistry.getMeters()).isEmpty();
  }

  /**
   * Replaces the former {@code cm-scan-id-stamp-failed} case: there is no pointer to advance any more,
   * because the scan to clone is read from {@code policy_evaluation}. The property that still matters is
   * that a failure in one component's post-evaluation bookkeeping — here finalizing the clone — does not
   * stop the next component from being monitored.
   */
  @Test
  public void process_finalizeFailsForOneComponent_othersStillEvaluate() throws Exception {
    stubSatellite();
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repository(REPO_ID, RepositoryType.hosted, true, "maven2"));
    HostedRepositoryComponent hrc1 = component(HASH, "a.tgz", "stage-release", "s-a");
    HostedRepositoryComponent hrc2 = component(HASH, "b.tgz", "stage-release", "s-b");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH))
        .thenReturn(List.of(hrc1, hrc2));
    ScanEntity tempScan1 = mock(ScanEntity.class);
    ScanEntity tempScan2 = mock(ScanEntity.class);
    when(scanPersistenceService.getScan(any(), any())).thenReturn(mock(ScanEntity.class));
    when(scanPersistenceService.createTempScan(hrc1.getId())).thenReturn(tempScan1);
    when(scanPersistenceService.createTempScan(hrc2.getId())).thenReturn(tempScan2);
    when(scanUploader.upload(eq(tempScan1), eq(hrc1), eq("stage-release"), any(), any(), eq(true)))
        .thenReturn(scanReceipt("fresh-1"));
    when(scanUploader.upload(eq(tempScan2), eq(hrc2), eq("stage-release"), any(), any(), eq(true)))
        .thenReturn(scanReceipt("fresh-2"));
    doThrow(new java.io.IOException("move boom")).when(scanPersistenceService)
        .moveTempScan(tempScan1, hrc1.getId(), "fresh-1");

    underTest.process(queueItem());

    // Both were evaluated; only the first one's bookkeeping failed, and it was counted, not rethrown.
    verify(scanPolicyEvaluator).evaluateForMonitoring(eq(hrc1), eq("fresh-1"), eq(new Stage("stage-release")),
        eq(ScanTriggerType.HOSTED_REPOSITORY_SCANNING), eq(ClientScanType.SONATYPE));
    verify(scanPolicyEvaluator).evaluateForMonitoring(eq(hrc2), eq("fresh-2"), eq(new Stage("stage-release")),
        eq(ScanTriggerType.HOSTED_REPOSITORY_SCANNING), eq(ClientScanType.SONATYPE));
    verify(scanPersistenceService).moveTempScan(tempScan2, hrc2.getId(), "fresh-2");
    assertDropMetric("cm-unexpected-failure", 1L);
  }

  /**
   * No {@code temp-*} clone may outlive the cycle — one per component per cycle would grow without
   * bound. On the success path the clone is retired by being finalized under the fresh scanId, one per
   * component, so nothing is left behind and the scan the evaluation names is on disk. (The failure
   * paths retire it by deletion instead — see the tests below.)
   */
  @Test
  public void process_finalizesEachComponentsOwnClone() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    HostedRepositoryComponent hrc1 = component(HASH, "a.tgz", "stage-release", "s-a");
    HostedRepositoryComponent hrc2 = component(HASH, "b.tgz", "stage-release", "s-b");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of(hrc1, hrc2));
    ScanEntity tempScan1 = mock(ScanEntity.class);
    ScanEntity tempScan2 = mock(ScanEntity.class);
    when(scanPersistenceService.getScan(any(), any())).thenReturn(mock(ScanEntity.class));
    when(scanPersistenceService.createTempScan(hrc1.getId())).thenReturn(tempScan1);
    when(scanPersistenceService.createTempScan(hrc2.getId())).thenReturn(tempScan2);
    when(scanUploader.upload(any(), eq(hrc1), any(), any(), any(), eq(true))).thenReturn(scanReceipt("fresh-1"));
    when(scanUploader.upload(any(), eq(hrc2), any(), any(), any(), eq(true))).thenReturn(scanReceipt("fresh-2"));

    underTest.process(queueItem());

    // Each component's own clone is finalized under its own fresh scanId -- not just one of them, and
    // not some other entity. Neither is left as a temp entity, and neither is discarded.
    verify(scanPersistenceService).moveTempScan(tempScan1, hrc1.getId(), "fresh-1");
    verify(scanPersistenceService).moveTempScan(tempScan2, hrc2.getId(), "fresh-2");
    verify(scanPersistenceService, never()).deleteScan(tempScan1);
    verify(scanPersistenceService, never()).deleteScan(tempScan2);
    // The evaluation still succeeded: cleanup must not turn a good cycle into a drop.
    verify(resolver).pinOwnerComponent(hrc1, "fresh-1", "stage-release");
    verify(resolver).pinOwnerComponent(hrc2, "fresh-2", "stage-release");
    assertThat(meterRegistry.getMeters()).isEmpty();
  }

  /**
   * The failure branches use {@code continue}, which bypasses any cleanup placed at the end of the
   * loop body; the clone must still be deleted when a stage fails.
   */
  @Test
  public void process_deletesClonedScanWhenEvaluationFails() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    HostedRepositoryComponent hrc1 = component(HASH, "a.tgz", "stage-release", "s-a");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of(hrc1));
    ScanEntity tempScan = mock(ScanEntity.class);
    when(scanPersistenceService.getScan(any(), any())).thenReturn(mock(ScanEntity.class));
    when(scanPersistenceService.createTempScan(hrc1.getId())).thenReturn(tempScan);
    when(scanUploader.upload(any(), eq(hrc1), any(), any(), any(), eq(true))).thenReturn(scanReceipt("fresh-1"));
    doThrow(new java.io.IOException("evaluation boom")).when(scanPolicyEvaluator)
        .evaluateForMonitoring(eq(hrc1), any(), any(), any(), any());

    underTest.process(queueItem());

    verify(scanPersistenceService).deleteScan(tempScan);
    assertDropMetric("cm-evaluation-failed", 1L);
  }

  @Test
  public void process_singleComponentEvaluationFails_othersContinue() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    HostedRepositoryComponent hrc1 = component(HASH, "a.tgz", "stage-release", "s-a");
    HostedRepositoryComponent hrc2 = component(HASH, "b.tgz", "stage-release", "s-b");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of(hrc1, hrc2));
    when(scanPersistenceService.getScan(any(), any())).thenReturn(mock(ScanEntity.class));
    when(scanPersistenceService.createTempScan(any())).thenReturn(mock(ScanEntity.class));
    when(scanUploader.upload(any(), eq(hrc1), any(), any(), any(), eq(true))).thenReturn(scanReceipt("fresh-1"));
    when(scanUploader.upload(any(), eq(hrc2), any(), any(), any(), eq(true))).thenReturn(scanReceipt("fresh-2"));
    doThrow(new java.io.IOException("evaluation boom")).when(scanPolicyEvaluator)
        .evaluateForMonitoring(eq(hrc1), any(), any(), any(), any());

    underTest.process(queueItem());

    verify(scanPolicyEvaluator).evaluateForMonitoring(eq(hrc2), eq("fresh-2"), any(), any(), any());
    verify(resolver, never()).pinOwnerComponent(eq(hrc1), any(), any());
    verify(resolver).pinOwnerComponent(hrc2, "fresh-2", "stage-release");
    assertDropMetric("cm-evaluation-failed", 1L);
  }

  @Test
  public void process_tempScanCreateFails_dropsAndContinues() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    HostedRepositoryComponent hrc = component(HASH, "a.tgz", "stage-release", "s-a");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of(hrc));
    // No getScan stub: createTempScan is the first call in the per-component try block, so it throws
    // before the clone step ever reads the source scan.
    when(scanPersistenceService.createTempScan(any())).thenThrow(new java.io.IOException("temp-scan boom"));

    underTest.process(queueItem());

    assertDropMetric("cm-temp-scan-create-failed", 1L);
    verify(scanPersistenceService, never()).copyScanFile(any(), any());
    verify(scanUploader, never()).upload(any(), any(), any(), any(), any(), anyBoolean());
    verify(scanPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any(), any(), any());
  }

  @Test
  public void process_cloneScanFileFails_dropsAndContinues() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    HostedRepositoryComponent hrc = component(HASH, "a.tgz", "stage-release", "s-a");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of(hrc));
    when(scanPersistenceService.getScan(any(), any())).thenReturn(mock(ScanEntity.class));
    when(scanPersistenceService.createTempScan(any())).thenReturn(mock(ScanEntity.class));
    doThrow(new java.io.IOException("clone boom")).when(scanPersistenceService)
        .copyScanFile(any(), any());

    underTest.process(queueItem());

    assertDropMetric("cm-clone-scan-failed", 1L);
    verify(scanUploader, never()).upload(any(), any(), any(), any(), any(), anyBoolean());
    verify(scanPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any(), any(), any());
  }

  @Test
  public void process_uploadFails_dropsAndContinues() throws Exception {
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    HostedRepositoryComponent hrc = component(HASH, "a.tgz", "stage-release", "s-a");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of(hrc));
    when(scanPersistenceService.getScan(any(), any())).thenReturn(mock(ScanEntity.class));
    when(scanPersistenceService.createTempScan(any())).thenReturn(mock(ScanEntity.class));
    doThrow(new java.io.IOException("upload boom")).when(scanUploader)
        .upload(any(), any(), any(), any(), any(), anyBoolean());

    underTest.process(queueItem());

    assertDropMetric("cm-upload-failed", 1L);
    verify(scanPolicyEvaluator, never()).evaluateForMonitoring(any(), any(), any(), any(), any());
  }

  /**
   * Replaces the former {@code cm-resolver-failed} case: candidates now arrive as
   * hosted-repository components, so there is no get-or-create step left to fail. The equivalent
   * "this candidate cannot be monitored" condition is having no evaluation to re-upload, and it must
   * skip only its own component.
   */
  @Test
  public void process_componentWithNoPreviousEvaluation_dropsAndContinues() throws Exception {
    stubSatellite();
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repository(REPO_ID, RepositoryType.hosted, true, "maven2"));
    HostedRepositoryComponent neverEvaluated = component(HASH, "a.tgz", "stage-release", null);
    HostedRepositoryComponent evaluated = component(HASH, "b.tgz", "stage-release", "s-b");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH))
        .thenReturn(List.of(neverEvaluated, evaluated));
    ScanEntity tempScan = mock(ScanEntity.class);
    when(scanPersistenceService.getScan(any(), any())).thenReturn(mock(ScanEntity.class));
    when(scanPersistenceService.createTempScan(any())).thenReturn(tempScan);
    when(scanUploader.upload(eq(tempScan), eq(evaluated), any(), any(), any(), eq(true)))
        .thenReturn(scanReceipt("fresh-b"));

    underTest.process(queueItem());

    assertDropMetric("cm-no-previous-evaluation", 1L);
    verify(scanPolicyEvaluator).evaluateForMonitoring(eq(evaluated), eq("fresh-b"), any(), any(), any());
    verify(scanPolicyEvaluator, never()).evaluateForMonitoring(eq(neverEvaluated), any(), any(), any(), any());
  }

  /**
   * {@code last_policy_evaluation} is unique on {@code (owner_id, stage_type_id)}, so an artifact that has
   * been uploaded at more than one stage — the stage is per-request on the NXRM scan payload — has one row
   * per stage, and {@code getLastByOwnerIds} applies no stage filter and no {@code ORDER BY}. The cycle must
   * refresh the stage the artifact was evaluated at most recently rather than whichever row the query
   * happened to return, so the choice cannot depend on result order.
   * <p>
   * Returned oldest-last here: a merge that keeps the later element would pick the stale build row.
   */
  @Test
  public void process_ownerEvaluatedAtTwoStages_monitorsTheMostRecentStage() throws Exception {
    stubSatellite();
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repository(REPO_ID, RepositoryType.hosted, true, "maven2"));
    HostedRepositoryComponent hrc = component(HASH, "a.tgz", "stage-release", "s-release");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of(hrc));

    PolicyEvaluation newerRelease = policyEvaluation("s-release");
    newerRelease.setOwnerId(hrc.getId());
    newerRelease.setStageTypeId("stage-release");
    newerRelease.setTime(new Date(2_000L));
    PolicyEvaluation olderBuild = policyEvaluation("s-build");
    olderBuild.setOwnerId(hrc.getId());
    olderBuild.setStageTypeId("stage-build");
    olderBuild.setTime(new Date(1_000L));
    when(policyEvaluationDAO.getLastByOwnerIds(anySet())).thenReturn(List.of(newerRelease, olderBuild));

    ScanEntity tempScan = mock(ScanEntity.class);
    when(scanPersistenceService.getScan(any(), any())).thenReturn(mock(ScanEntity.class));
    when(scanPersistenceService.createTempScan(any())).thenReturn(tempScan);
    when(scanUploader.upload(any(), eq(hrc), any(), any(), any(), eq(true))).thenReturn(scanReceipt("fresh-a"));

    underTest.process(queueItem());

    verify(scanPolicyEvaluator).evaluateForMonitoring(
        eq(hrc), eq("fresh-a"), eq(new Stage("stage-release")), any(), any());
    verify(scanPolicyEvaluator, never()).evaluateForMonitoring(
        any(), any(), eq(new Stage("stage-build")), any(), any());
  }

  @Test
  public void process_pinOwnerComponentMiss_stillNoDropMetric() throws Exception {
    // resolver.pinOwnerComponent swallows misses internally (WARN + dedicated pin-miss metric on
    // HostedRepositoryComponentResolver); the caller here does not add its own drop metric when
    // pin returns cleanly.
    stubSatellite();
    Repository repo = repository(REPO_ID, RepositoryType.hosted, true, "maven2");
    when(repositoryDAO.getById(REPO_ID)).thenReturn(repo);
    HostedRepositoryComponent hrc = component(HASH, "a.tgz", "stage-release", "s-a");
    when(hostedRepositoryComponentDAO.getByRepositoryIdAndHash(tx, REPO_ID, HASH)).thenReturn(List.of(hrc));
    when(scanPersistenceService.getScan(any(), any())).thenReturn(mock(ScanEntity.class));
    when(scanPersistenceService.createTempScan(any())).thenReturn(mock(ScanEntity.class));
    when(scanUploader.upload(any(), eq(hrc), any(), any(), any(), eq(true))).thenReturn(scanReceipt("fresh-a"));

    underTest.process(queueItem());

    verify(resolver).pinOwnerComponent(hrc, "fresh-a", "stage-release");
    assertThat(meterRegistry.getMeters()).isEmpty();
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

  private static HostedRepositoryComponent component(final String hash, final String pathname, final String stage) {
    return component(hash, pathname, stage, null);
  }

  /**
   * Pure factory — deliberately does no stubbing, because call sites nest it inside
   * {@code thenReturn(...)} where stubbing would corrupt Mockito's in-progress stubbing.
   * <p>
   * The stage and the scan to re-upload now come from {@code policy_evaluation} rather than from the
   * candidate row, so the intended pair is encoded into the id and served by the single default set up
   * in {@link #stubEvaluationsFromComponentIds()}.
   */
  private static HostedRepositoryComponent component(
      final String hash,
      final String pathname,
      final String stage,
      final String scanId)
  {
    HostedRepositoryComponent c = new HostedRepositoryComponent(REPO_ID, pathname, hash);
    c.setId("hrc|" + pathname + "|" + (stage == null ? "" : stage) + "|" + (scanId == null ? "" : scanId));
    return c;
  }

  /**
   * One lenient default covering every candidate: the processor batches this call for the whole page, so
   * decode every id in the set and return one evaluation per candidate that was built with a scanId.
   * Candidates built without one contribute no row, which is what "never evaluated" looks like.
   */
  private void stubEvaluationsFromComponentIds() {
    lenient().when(policyEvaluationDAO.getLastByOwnerIds(anySet())).thenAnswer(inv -> {
      Set<String> ids = inv.getArgument(0);
      List<PolicyEvaluation> evaluations = new ArrayList<>();
      for (String id : ids) {
        String[] parts = id.split("\\|", -1);
        if (parts.length < 4 || parts[3].isEmpty()) {
          continue;
        }
        PolicyEvaluation evaluation = policyEvaluation(parts[3]);
        evaluation.setOwnerId(id);
        evaluation.setStageTypeId(parts[2].isEmpty() ? null : parts[2]);
        evaluations.add(evaluation);
      }
      return evaluations;
    });
  }

  private static ScanReceipt scanReceipt(final String scanId) {
    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId(scanId);
    return receipt;
  }

  private static PolicyEvaluation policyEvaluation(final String scanId) {
    PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setScanId(scanId);
    return policyEvaluation;
  }
}
