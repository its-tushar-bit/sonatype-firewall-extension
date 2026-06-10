/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.inject.Provider;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.insight.brain.dataaccess.repository.HostedComponentScanQueueDAO;
import com.sonatype.insight.brain.dataaccess.repository.HostedDeploymentBlockDAO;
import com.sonatype.insight.brain.hds.ScanUploader;
import com.sonatype.insight.brain.model.repository.HostedDeploymentBlock;
import com.sonatype.insight.brain.model.repository.HostedDeploymentBlockViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HostedComponentEvaluationService#evaluateSynchronously} (CLM-39870).
 * <p>
 * Covers orchestration, block persistence, allow-path suppression of block persistence, and
 * cleanup-on-every-exit-path semantics. Scan XML parsing and HDS round-trips are mocked — the
 * full integration path is covered separately by resource-level tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class HostedComponentEvaluationServiceSyncTest
{
  private static final String REPO_ID = "repo-1";

  private static final String COMPONENT_ID = "nxrm-component-1";

  private static final String PURL = "pkg:maven/com.acme/lib@1.2.3";

  private static final String CORRELATION_ID = "nxrm-upload-corr-001";

  private static final String REQUESTED_BY = "developer@example.com";

  private static final String CLIENT = "maven";

  private static final String EVALUATION_URL =
      "https://iq.example.com/assets/index.html#/hostedRepos/repo-1/components?repositoryPublicId=maven-releases";

  @Mock
  private HostedComponentScanStorageService storageService;

  @Mock
  private HostedComponentScanQueueDAO queueDAO;

  @Mock
  private HostedComponentScanQueueConsumer queueConsumer;

  @Mock
  private ScanPersistenceService scanPersistenceService;

  @Mock
  private ScanUploader scanUploader;

  @Mock
  private RepositoryPolicyEvaluator evaluator;

  @Mock
  private HostedDeploymentBlockDAO blockDAO;

  @Mock
  private HostedEvaluationUrlBuilder urlBuilder;

  @Mock
  private ScanEntity scanEntity;

  @Mock
  private TransactionContext tx;

  private HostedComponentEvaluationService service;

  private Repository repository;

  private File inboundScanFile;

  @Before
  public void setUp() throws Exception {
    HostedEvaluationResultMapper realMapper = new HostedEvaluationResultMapper();

    Provider<ScanUploader> scanUploaderProvider = () -> scanUploader;
    Provider<RepositoryPolicyEvaluator> evaluatorProvider = () -> evaluator;

    service = new HostedComponentEvaluationService(
        storageService,
        queueDAO,
        queueConsumer,
        scanPersistenceService,
        scanUploaderProvider,
        evaluatorProvider,
        blockDAO,
        realMapper,
        urlBuilder);

    repository = new Repository();
    repository.setId(REPO_ID);
    repository.setPublicId("maven-releases");
    repository.setFormat("maven2");

    when(storageService.storeScanFile(eq(REPO_ID), any(File.class))).thenReturn(scanEntity);
    when(scanEntity.getInputStream())
        .thenAnswer(inv -> new ByteArrayInputStream(scanXml().getBytes(StandardCharsets.UTF_8)));
    when(urlBuilder.build(repository)).thenReturn(EVALUATION_URL);
    when(blockDAO.createTransactionContext()).thenReturn(tx);

    inboundScanFile = File.createTempFile("inbound-scan", ".xml");
    inboundScanFile.deleteOnExit();

    ScanReceipt receipt = new ScanReceipt();
    receipt.setScanId("hds-scan-001");
    when(scanUploader.uploadForRepository(eq(scanEntity), eq(REPO_ID), eq("release"), anyString(), eq(false)))
        .thenReturn(receipt);
  }

  @Test
  public void evaluateSynchronously_allowPath_persistsViaEvaluator_doesNotPersistBlock() throws Exception {
    // First evaluator call (persist=false): returns allow.
    // Second evaluator call (persist=true): no-op mock, but we verify it was called.
    when(evaluator.evaluateForHostedEnforcement(eq(repository), any(), eq(false), anyString(), eq("release")))
        .thenReturn(allowedEvaluation());
    when(evaluator.evaluateForHostedEnforcement(eq(repository), any(), eq(true), anyString(), eq("release")))
        .thenReturn(allowedEvaluation());

    HostedEvaluationResult result = service.evaluateSynchronously(
        repository, COMPONENT_ID, PURL, null, inboundScanFile, CORRELATION_ID, REQUESTED_BY, CLIENT);

    assertThat(result.blocked()).isFalse();
    assertThat(result.blockingViolations()).isEmpty();
    assertThat(result.evaluationUrl()).isEqualTo(EVALUATION_URL);
    assertThat(result.correlationId()).isEqualTo(CORRELATION_ID);
    assertThat(result.componentId()).isEqualTo(COMPONENT_ID);

    // Evaluator called twice: verdict check (persist=false), then persisted (persist=true).
    verify(evaluator).evaluateForHostedEnforcement(eq(repository), any(), eq(false), anyString(),
        eq("release"));
    verify(evaluator).evaluateForHostedEnforcement(eq(repository), any(), eq(true), anyString(),
        eq("release"));
    // Block must NOT be persisted on allow.
    verify(blockDAO, never()).insertWithViolations(any(), any(), any());
    // Scan file cleaned up.
    verify(scanPersistenceService).deleteScan(scanEntity);
  }

  @Test
  public void evaluateSynchronously_blockPath_persistsToBlockTables_andDoesNotReRunEvaluatorForPersistence() throws Exception {
    when(evaluator.evaluateForHostedEnforcement(eq(repository), any(), eq(false), anyString(), eq("release")))
        .thenReturn(blockedEvaluation());

    HostedEvaluationResult result = service.evaluateSynchronously(
        repository, COMPONENT_ID, PURL, null, inboundScanFile, CORRELATION_ID, REQUESTED_BY, CLIENT);

    assertThat(result.blocked()).isTrue();
    assertThat(result.policyAction()).isEqualTo("FAIL");
    assertThat(result.highestThreatLevel()).isEqualTo(9);
    assertThat(result.blockingViolations()).hasSize(1);

    // First (and only) evaluator call: verdict only, persist=false.
    verify(evaluator, times(1)).evaluateForHostedEnforcement(
        eq(repository), any(), eq(false), anyString(), eq("release"));
    // Never called with persist=true on block path (would pollute repository_component).
    verify(evaluator, never()).evaluateForHostedEnforcement(
        eq(repository), any(), eq(true), anyString(), eq("release"));

    // Block + violations persisted to dedicated tables.
    ArgumentCaptor<HostedDeploymentBlock> blockCaptor = ArgumentCaptor.forClass(HostedDeploymentBlock.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<HostedDeploymentBlockViolation>> violationsCaptor = ArgumentCaptor.forClass(List.class);
    verify(blockDAO).insertWithViolations(eq(tx), blockCaptor.capture(), violationsCaptor.capture());

    HostedDeploymentBlock persistedBlock = blockCaptor.getValue();
    assertThat(persistedBlock.getId()).isNotBlank();
    assertThat(persistedBlock.getRepositoryId()).isEqualTo(REPO_ID);
    assertThat(persistedBlock.getPolicyAction()).isEqualTo("FAIL");
    assertThat(persistedBlock.getHighestThreatLevel()).isEqualTo(9);
    assertThat(persistedBlock.getEvaluationUrl()).isEqualTo(EVALUATION_URL);
    assertThat(persistedBlock.getCorrelationId()).isEqualTo(CORRELATION_ID);
    assertThat(persistedBlock.getRequestedBy()).isEqualTo(REQUESTED_BY);
    assertThat(persistedBlock.getDisplayName()).isEqualTo(PURL);
    assertThat(persistedBlock.getPathname()).isEqualTo("com/acme/lib/1.2.3/lib-1.2.3.jar");
    assertThat(persistedBlock.getHash()).isEqualTo("abc123def456ghi7");
    assertThat(persistedBlock.getBlockedTime()).isNotNull();

    List<HostedDeploymentBlockViolation> persistedViolations = violationsCaptor.getValue();
    assertThat(persistedViolations).hasSize(1);
    assertThat(persistedViolations.get(0).getPolicyName()).isEqualTo("Critical Security Policy");

    // Scan file cleaned up.
    verify(scanPersistenceService).deleteScan(scanEntity);
  }

  @Test
  public void evaluateSynchronously_allowPath_persistFailureDoesNotMaskVerdict() throws Exception {
    // CLM-39870 PR-2 review fix (allow path): if the second evaluator call (persist=true)
    // throws, NXRM must still receive the already-computed allow verdict. CM will recompute
    // on its next scan; an HTTP 500 here would risk NXRM rejecting a deployment that was
    // policy-allowed.
    when(evaluator.evaluateForHostedEnforcement(eq(repository), any(), eq(false), anyString(), eq("release")))
        .thenReturn(allowedEvaluation());
    when(evaluator.evaluateForHostedEnforcement(eq(repository), any(), eq(true), anyString(), eq("release")))
        .thenThrow(new RuntimeException("HDS unreachable on persist round-trip"));

    HostedEvaluationResult result = service.evaluateSynchronously(
        repository, COMPONENT_ID, PURL, null, inboundScanFile, CORRELATION_ID, REQUESTED_BY, CLIENT);

    // Verdict is still ALLOW — the persistence-step failure is logged and swallowed.
    assertThat(result.blocked()).isFalse();
    // Scan file still cleaned up.
    verify(scanPersistenceService).deleteScan(scanEntity);
  }

  @Test
  public void evaluateSynchronously_blockPath_persistFailureDoesNotMaskVerdict() throws Exception {
    // CLM-39870 PR-2 review fix: a DataAccessException during persistBlock must NOT propagate
    // as HTTP 500. The verdict (BLOCK) is already correct; failing on a bookkeeping write would
    // turn a policy-blocked deployment into an allow if NXRM treats 500 as fail-open.
    when(evaluator.evaluateForHostedEnforcement(eq(repository), any(), eq(false), anyString(), eq("release")))
        .thenReturn(blockedEvaluation());
    org.mockito.Mockito.doThrow(new RuntimeException("Aurora failover in progress"))
        .when(blockDAO)
        .insertWithViolations(any(), any(), any());

    HostedEvaluationResult result = service.evaluateSynchronously(
        repository, COMPONENT_ID, PURL, null, inboundScanFile, CORRELATION_ID, REQUESTED_BY, CLIENT);

    // Verdict is still BLOCK — the audit-write failure is logged and swallowed.
    assertThat(result.blocked()).isTrue();
    assertThat(result.policyAction()).isEqualTo("FAIL");
    assertThat(result.blockingViolations()).hasSize(1);
    // Scan file still cleaned up.
    verify(scanPersistenceService).deleteScan(scanEntity);
  }

  @Test
  public void evaluateSynchronously_nullRepository_throws_andCleansUpNothing() throws Exception {
    // Defense-in-depth guard: the resource layer always resolves the Repository before
    // calling this method (and 404s the request if the lookup fails), so the only way
    // null reaches the service is a programming error. Fail fast with IllegalArgumentException
    // before any IO/DB work happens.
    assertThatThrownBy(() -> service.evaluateSynchronously(
        null, COMPONENT_ID, PURL, null, inboundScanFile, CORRELATION_ID, REQUESTED_BY, CLIENT))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("repository");

    // Nothing to clean up (we didn't reach storeScanFile).
    verify(storageService, never()).storeScanFile(anyString(), any(File.class));
    verify(scanPersistenceService, never()).deleteScan(any());
  }

  @Test
  public void evaluateSynchronously_evaluatorThrows_stillCleansUpScanFile() throws Exception {
    when(evaluator.evaluateForHostedEnforcement(any(), any(), anyBoolean(), anyString(), anyString()))
        .thenThrow(new RuntimeException("HDS down"));

    assertThatThrownBy(() -> service.evaluateSynchronously(
        repository, COMPONENT_ID, PURL, null, inboundScanFile, CORRELATION_ID, REQUESTED_BY, CLIENT))
            .isInstanceOf(RuntimeException.class);

    // try/finally guarantees cleanup even when the evaluator explodes.
    verify(scanPersistenceService).deleteScan(scanEntity);
  }

  @Test
  public void evaluateSynchronously_scanUploadThrows_stillCleansUpScanFile() throws Exception {
    when(scanUploader.uploadForRepository(any(), anyString(), anyString(), anyString(), anyBoolean()))
        .thenThrow(new IOException("HDS network error"));

    assertThatThrownBy(() -> service.evaluateSynchronously(
        repository, COMPONENT_ID, PURL, null, inboundScanFile, CORRELATION_ID, REQUESTED_BY, CLIENT))
            .isInstanceOf(IOException.class);

    verify(scanPersistenceService).deleteScan(scanEntity);
    verify(evaluator, never()).evaluateForHostedEnforcement(any(), any(), anyBoolean(), anyString(), anyString());
  }

  @Test
  public void evaluateSynchronously_scanCleanupFailure_doesNotMaskEvaluationResult() throws Exception {
    when(evaluator.evaluateForHostedEnforcement(eq(repository), any(), eq(false), anyString(), eq("release")))
        .thenReturn(allowedEvaluation());
    when(evaluator.evaluateForHostedEnforcement(eq(repository), any(), eq(true), anyString(), eq("release")))
        .thenReturn(allowedEvaluation());
    doThrowOnDelete();

    HostedEvaluationResult result = service.evaluateSynchronously(
        repository, COMPONENT_ID, PURL, null, inboundScanFile, CORRELATION_ID, REQUESTED_BY, CLIENT);

    assertThat(result.blocked()).isFalse();
    // cleanup was attempted; its failure was logged and swallowed (not rethrown).
    verify(scanPersistenceService).deleteScan(scanEntity);
  }

  @Test
  public void evaluateSynchronously_unscannableScan_throwsUnscannableArtifactException_andCleansUp() throws IOException {
    // Scan XML with no <dir> element — ScanXmlParser.extractComponentInfo returns null,
    // matching the real-world case of a -sources.jar / -javadoc.jar / signature file.
    when(scanEntity.getInputStream())
        .thenAnswer(inv -> new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<scan version=\"2.24\">\n"
            + "<repository id=\"repo-1\" name=\"maven-releases\" format=\"maven2\"/>\n"
            + "</scan>").getBytes(StandardCharsets.UTF_8)));

    assertThatThrownBy(() -> service.evaluateSynchronously(
        repository, COMPONENT_ID, PURL, null, inboundScanFile, CORRELATION_ID, REQUESTED_BY, CLIENT))
            .isInstanceOf(UnscannableArtifactException.class)
            .hasMessageContaining(COMPONENT_ID);

    // We failed BEFORE reaching the evaluator — but AFTER storing the scan, so cleanup must run.
    verify(scanPersistenceService).deleteScan(scanEntity);
    verify(evaluator, never()).evaluateForHostedEnforcement(any(), any(), anyBoolean(), anyString(), anyString());
    verify(blockDAO, never()).insertWithViolations(any(), any(), any());
  }

  @Test
  public void evaluateSynchronously_honoursIncomingStage() throws Exception {
    when(evaluator.evaluateForHostedEnforcement(any(), any(), anyBoolean(), anyString(), anyString()))
        .thenReturn(allowedEvaluation());

    service.evaluateSynchronously(
        repository, COMPONENT_ID, PURL, "stage-release", inboundScanFile,
        CORRELATION_ID, REQUESTED_BY, CLIENT);

    verify(scanUploader).uploadForRepository(eq(scanEntity), eq(REPO_ID), eq("stage-release"), eq(CLIENT),
        eq(false));
    verify(evaluator).evaluateForHostedEnforcement(any(), any(), eq(false), eq(CLIENT), eq("stage-release"));
  }

  @Test
  public void evaluateSynchronously_blankIncomingStage_fallsBackToRelease() throws Exception {
    when(evaluator.evaluateForHostedEnforcement(any(), any(), anyBoolean(), anyString(), anyString()))
        .thenReturn(allowedEvaluation());

    service.evaluateSynchronously(
        repository, COMPONENT_ID, PURL, "  " /* blank */, inboundScanFile,
        CORRELATION_ID, REQUESTED_BY, CLIENT);

    verify(scanUploader).uploadForRepository(eq(scanEntity), eq(REPO_ID), eq("release"), eq(CLIENT),
        eq(false));
    verify(evaluator).evaluateForHostedEnforcement(any(), any(), eq(false), eq(CLIENT), eq("release"));
  }

  @Test
  public void evaluateSynchronously_unknownIncomingStage_throws400() {
    assertThatThrownBy(() -> service.evaluateSynchronously(
        repository, COMPONENT_ID, PURL, "not-a-real-stage", inboundScanFile,
        CORRELATION_ID, REQUESTED_BY, CLIENT))
            .isInstanceOf(com.sonatype.insight.error.exception.BadRequestException.class)
            .hasMessageContaining("not-a-real-stage");
  }

  @Test
  public void evaluateSynchronously_nonEnforceableStage_throws400() {
    for (String stageId : List.of("proxy", "develop", "compliance")) {
      assertThatThrownBy(() -> service.evaluateSynchronously(
          repository, COMPONENT_ID, PURL, stageId, inboundScanFile,
          CORRELATION_ID, REQUESTED_BY, CLIENT))
              .as("stage id '%s' must not be accepted for upload enforcement", stageId)
              .isInstanceOf(com.sonatype.insight.error.exception.BadRequestException.class)
              .hasMessageContaining(stageId);
    }
  }

  // --- test fixture helpers ---

  private static String scanXml() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<scan version=\"2.24\">\n"
        + "<repository id=\"repo-1\" name=\"maven-releases\" format=\"maven2\"/>\n"
        + "<dir path=\"com/acme/lib/1.2.3/lib-1.2.3.jar\" sha1=\"abc123def456ghi7\" sha512=\"ignored\">\n</dir>\n"
        + "</scan>";
  }

  private static RepositoryComponentEvaluationDataList allowedEvaluation() {
    RepositoryComponentEvaluationDataList list = new RepositoryComponentEvaluationDataList();
    RepositoryComponentEvaluationData data = new RepositoryComponentEvaluationData();
    // policyAlerts left empty — mapper treats this as "allow".
    list.componentEvalResults.add(data);
    return list;
  }

  private static RepositoryComponentEvaluationDataList blockedEvaluation() {
    // Uses the same fixture builders as HostedEvaluationResultMapperTest — one FAIL alert.
    RepositoryComponentEvaluationDataList list = new RepositoryComponentEvaluationDataList();
    RepositoryComponentEvaluationData data = new RepositoryComponentEvaluationData();
    data.policyAlerts = new java.util.ArrayList<>();
    data.policyAlerts.add(BlockedAlertFixture.build());
    list.componentEvalResults.add(data);
    return list;
  }

  private void doThrowOnDelete() throws IOException {
    org.mockito.Mockito.doThrow(new IOException("disk unreachable"))
        .when(scanPersistenceService)
        .deleteScan(scanEntity);
  }
}
