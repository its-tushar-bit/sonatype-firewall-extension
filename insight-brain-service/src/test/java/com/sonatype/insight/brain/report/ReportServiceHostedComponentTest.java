/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.repository.hosted.HostedComponentScanQueueConsumer;
import com.sonatype.insight.brain.repository.hosted.HostedReportFileBuilder;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for hosted repository component report support in CLM-39917.
 * File building logic lives in HostedReportFileBuilder; isHostedScan is tested via ReportService.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportServiceHostedComponentTest
{
  @Mock
  private RepositoryComponentDAO repositoryComponentDAO;

  @Mock
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.policy.PolicyDAO policyDAO;

  // Only the two fields above are exercised in these tests — all others remain null mocks
  @Mock
  private com.sonatype.insight.brain.cpematching.CpeMatchingConfigurationService cpeMatchingConfigurationService;

  @Mock
  private com.sonatype.insight.brain.dashboard.H2ApplicationRiskService applicationRiskService;

  @Mock
  private com.sonatype.insight.brain.dataaccess.ApplicationDAO applicationDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.OrganizationDAO organizationDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory componentLoaderFactory;

  @Mock
  private com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO hashComponentIdentifierDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO innerSourceApplicationDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.innersource.InnerSourceVersionDAO innerSourceVersionDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.license.LicenseDAO licenseDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO licenseOverrideDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO licenseThreatGroupDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager clusterLockManager;

  @Mock
  private com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO multiLicenseDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO repositoryDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  @Mock
  private com.sonatype.insight.brain.git.pullrequestcreationservice.AutomatedPullRequestCreationService automatedPullRequestCreationService;

  @Mock
  private com.sonatype.insight.brain.hds.ScanUploadService scanUploadService;

  @Mock
  private com.sonatype.insight.brain.product.license.ProductLicense productLicense;

  @Mock
  private com.sonatype.insight.brain.proprietary.ProprietaryConfigService proprietaryConfigService;

  @Mock
  private com.sonatype.insight.brain.report.ReportDataStore reportDataStore;

  @Mock
  private com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils sbomMetadataUtils;

  @Mock
  private com.sonatype.insight.brain.scan.datastore.ScanPersistenceService scanPersistenceService;

  @Mock
  private com.sonatype.insight.brain.service.Configuration configuration;

  @Mock
  private com.sonatype.insight.brain.telemetry.TelemetrySender telemetrySender;

  @Mock
  private com.sonatype.insight.brain.telemetry.TelemetryUtils telemetryUtils;

  @Mock
  private com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO thirdPartyComponentDAO;

  @Mock
  private com.sonatype.insight.brain.thirdparty.ThirdPartyDataService thirdPartyDataService;

  @Mock
  private com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Mock
  private com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  @Mock
  private jakarta.inject.Provider<com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator> repositoryPolicyEvaluatorProvider;

  @Mock
  private LifecycleReportPersistenceService lifecycleReportPersistenceService;

  @Mock
  private HostedComponentScanQueueConsumer hostedComponentScanQueueConsumer;

  @InjectMocks
  private ReportService reportService;

  @Before
  public void setUp() {
    // Post CLM-41904 merge: reevaluateHostedComponent wraps evaluate → mirror → saveOverlay →
    // persist inside a ClusterLock, then persistHostedComponentReevaluation opens a
    // TransactionContext on policyEvaluationDAO. Stub both lenient()ly so tests that never reach
    // that code path (component/repository/application lookup failures) aren't rejected by
    // Mockito for unused stubbings.
    ClusterLock clusterLock = Mockito.mock(ClusterLock.class);
    Mockito.lenient()
        .when(clusterLockManager.createForPolicyEvaluation(
            Mockito.any(Application.class), Mockito.anyString()))
        .thenReturn(clusterLock);
    TransactionContext tx = Mockito.mock(TransactionContext.class);
    Mockito.lenient().when(policyEvaluationDAO.createTransactionContext()).thenReturn(tx);
    // resolveComponentUnknownPolicy opens a tx on policyDAO and lists applicable policies; stub it
    // to return none so the synthetic go-outer row falls back to the default (these tests don't
    // exercise a customized Component-Unknown policy).
    TransactionContext policyTx = Mockito.mock(TransactionContext.class);
    Mockito.lenient().when(policyDAO.createTransactionContext()).thenReturn(policyTx);
    Mockito.lenient()
        .when(policyDAO.getApplicableByOwnerIdWithHierarchy(Mockito.any(), Mockito.anyString()))
        .thenReturn(java.util.List.of());
  }

  // ---- isHostedRepositoryComponent ----

  @Test
  public void isHostedRepositoryComponent_returnsTrueWhenComponentExists() {
    when(repositoryComponentDAO.getByScanId("scan1")).thenReturn(newComponent("repo1", "lib.jar", "abc123"));

    assertThat(reportService.isHostedRepositoryComponent("scan1")).isTrue();
  }

  @Test
  public void isHostedRepositoryComponent_returnsFalseWhenNotFound() {
    when(repositoryComponentDAO.getByScanId("none")).thenReturn(null);

    assertThat(reportService.isHostedRepositoryComponent("none")).isFalse();
  }

  // ---- HostedReportFileBuilder: policythreats.json ----

  @Test
  public void build_policyThreats_noViolations_emptyAaData() throws Exception {
    byte[] result = HostedReportFileBuilder.build("policythreats.json",
        newComponent("r", "lib.jar", "abc"), List.of());

    String json = new String(result);
    assertThat(json).contains("\"version\":5");
    assertThat(json).contains("\"aaData\":[]");
  }

  @Test
  public void build_policyThreats_withViolation_populatesGroup() throws Exception {
    RepositoryComponent comp = newComponent("r", "lib.jar", "abc123");
    RepositoryPolicyViolation v = newViolation("v1", "policy-1", "No Risky Libs", 8, false);

    byte[] result = HostedReportFileBuilder.build("policythreats.json", comp, List.of(v));

    String json = new String(result);
    assertThat(json).contains("\"policyId\":\"policy-1\"");
    assertThat(json).contains("\"policyThreatLevel\":8");
    assertThat(json).contains("\"hash\":\"abc123\"");
    assertThat(json).contains("\"activeViolations\"");
    assertThat(json).contains("\"waivedViolations\"");
  }

  @Test
  public void build_policyThreats_waivedViolation_separatedCorrectly() throws Exception {
    RepositoryComponent comp = newComponent("r", "lib.jar", "abc");
    RepositoryPolicyViolation active = newViolation("v1", "p1", "A", 7, false);
    RepositoryPolicyViolation waived = newViolation("v2", "p2", "B", 5, true);

    byte[] result = HostedReportFileBuilder.build("policythreats.json", comp, List.of(active, waived));

    String json = new String(result);
    assertThat(json).contains("\"waived\":true");
    assertThat(json).contains("\"waived\":false");
  }

  @Test
  public void build_policyThreats_nullComponent_emptyAaData() throws Exception {
    byte[] result = HostedReportFileBuilder.build("policythreats.json", null, List.of());
    assertThat(new String(result)).contains("\"aaData\":[]");
  }

  // ---- HostedReportFileBuilder: bom.json ----

  @Test
  public void build_bom_withComponent_containsHashAndPathname() throws Exception {
    RepositoryComponent comp = newComponent("r", "commons-text-1.9.jar", "def456");
    comp.setDisplayName("commons-text : 1.9");

    byte[] result = HostedReportFileBuilder.build("bom.json", comp, List.of());

    String json = new String(result);
    assertThat(json).contains("\"hash\":\"def456\"");
    assertThat(json).contains("commons-text : 1.9");
    assertThat(json).contains("commons-text-1.9.jar");
  }

  @Test
  public void build_bom_nullComponent_emptyAaData() throws Exception {
    assertThat(new String(HostedReportFileBuilder.build("bom.json", null, List.of())))
        .contains("\"aaData\":[]");
  }

  // ---- HostedReportFileBuilder: data.json ----

  @Test
  public void build_data_exactMatch_knownCountIsOne() throws Exception {
    RepositoryComponent comp = newComponent("r", "lib.jar", "abc");
    comp.setMatchStateId("exact");

    String json = new String(HostedReportFileBuilder.build("data.json", comp, List.of()));

    assertThat(json).contains("\"totalArtifactCount\":1");
    assertThat(json).contains("\"knownArtifactCount\":1");
    assertThat(json).contains("\"exactlyMatchedComponentCount\":1");
  }

  @Test
  public void build_data_unknownMatch_knownCountIsZero() throws Exception {
    RepositoryComponent comp = newComponent("r", "lib.jar", "abc");
    comp.setMatchStateId("unknown");

    String json = new String(HostedReportFileBuilder.build("data.json", comp, List.of()));

    assertThat(json).contains("\"totalArtifactCount\":1");
    assertThat(json).contains("\"knownArtifactCount\":0");
  }

  @Test
  public void build_data_nullComponent_allCountsZero() throws Exception {
    assertThat(new String(HostedReportFileBuilder.build("data.json", null, List.of())))
        .contains("\"totalArtifactCount\":0");
  }

  // ---- HostedReportFileBuilder: static files ----

  @Test
  public void build_summaryJson_containsExpectedKeys() throws Exception {
    assertThat(new String(HostedReportFileBuilder.build("summary.json", null, List.of())))
        .contains("totalComponentCount");
  }

  @Test
  public void build_dependenciesJson_containsDependencyTree() throws Exception {
    assertThat(new String(HostedReportFileBuilder.build("dependencies.json", null, List.of())))
        .contains("dependencyTree");
  }

  @Test
  public void build_securityJson_emptyAaData() throws Exception {
    assertThat(new String(HostedReportFileBuilder.build("security.json", null, List.of())))
        .contains("\"aaData\":[]");
  }

  // Risk-total dedup tests live in HostedReportFileBuilderTest, where HostedReportFileBuilder.totalRisk
  // now dedups by (hash, constraintFactsId).

  // ---- metadata-path reshape wiring ----

  @Mock
  private LifecycleReport lifecycleReport;

  /**
   * Guards that the metadata path reshapes violations before computing totalRisk: an npm outer row
   * whose identity is mirrored by an inner row must be dropped, so the reported risk counts the
   * shared finding once (14) rather than twice (28).
   */
  @Test
  public void getReportMetadata_npmOuterMirror_totalRiskUsesReshapedViolations() throws Exception {
    String pubId = "app-pub";
    String scanId = "scan-npm";
    String repoId = "repo-npm";
    String outerPath = "axios-0.18.0.tgz";

    Application application = new Application();
    application.setId("app-id");
    application.setPublicId(pubId);
    application.setOrganizationId("org-id");
    when(applicationDAO.getByPublicIdNotNull(pubId)).thenReturn(application);
    when(organizationDAO.getByIdNotNull("org-id")).thenReturn(new Organization());

    when(reportDataStore.getLifecycleReport(application, scanId)).thenReturn(lifecycleReport);
    when(lifecycleReport.exists()).thenReturn(true);
    ReportEntry dataEntry = new ReportEntry("data.json", 0L,
        "{\"policyComponentCount\":1,\"globals\":{}}".getBytes());
    lenient().when(lifecycleReport.getEntries(List.of("data.json", "template.properties", "summary.json")))
        .thenReturn(Map.of("data.json", dataEntry));

    PolicyEvaluation evaluation = org.mockito.Mockito.mock(PolicyEvaluation.class);
    when(evaluation.getStageTypeId()).thenReturn(BuildStageType.ID);
    when(evaluation.getScanTriggerType()).thenReturn(ScanTriggerType.REPOSITORY_MANAGER);
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId("app-id", scanId)).thenReturn(evaluation);

    RepositoryComponent comp = newComponent(repoId, outerPath, "file_sha1");
    when(repositoryComponentDAO.getByScanId(scanId)).thenReturn(comp);

    Repository repository = new Repository();
    repository.setFormat("npm");
    when(repositoryDAO.getById(repoId)).thenReturn(repository);

    ComponentIdentifier axios = newNpmIdentifier("axios", "0.18.0");
    RepositoryPolicyViolation outer =
        newInnerViolation(outerPath, "file_sha1", axios, "cve-1", 14);
    RepositoryPolicyViolation innerMirror =
        newInnerViolation(outerPath + "!/axios@0.18.0", "hds_hash", axios, "cve-1", 14);
    when(repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnameOrInnerPathnames(repoId, outerPath))
        .thenReturn(List.of(outer, innerMirror));

    ReportMetadataDTO metadata = reportService.getReportMetadataNoAuth(pubId, scanId);

    assertThat(metadata.getTotalRisk())
        .as("outer self-mirror dropped before totalRisk, so the shared finding counts once")
        .isEqualTo(14);
  }

  /**
   * Guards the recovery path (saveOverlayFiles): when a hosted scan's data.json lacks
   * policyComponentCount, getReportMetadataNoAuth regenerates the overlay. The persisted
   * policythreats.json must be built from the reshaped violation set, so an npm outer self-mirror
   * is dropped (one aaData entry, not two).
   */
  @Test
  public void getReportMetadata_recoveryPath_persistsReshapedPolicyThreats() throws Exception {
    String pubId = "app-pub";
    String scanId = "scan-npm";
    String repoId = "repo-npm";
    String outerPath = "axios-0.18.0.tgz";

    Application application = new Application();
    application.setId("app-id");
    application.setPublicId(pubId);
    application.setOrganizationId("org-id");
    when(applicationDAO.getByPublicIdNotNull(pubId)).thenReturn(application);
    when(applicationDAO.getByIdNotNull("app-id")).thenReturn(application);
    when(organizationDAO.getByIdNotNull("org-id")).thenReturn(new Organization());

    // data.json WITHOUT policyComponentCount => triggers the saveOverlayFiles recovery branch.
    ReportEntry dataEntry = new ReportEntry("data.json", 0L, "{\"globals\":{}}".getBytes());
    when(reportDataStore.getLifecycleReport(application, scanId)).thenReturn(lifecycleReport);
    when(lifecycleReport.exists()).thenReturn(true);
    lenient().when(lifecycleReport.getEntries(List.of("data.json", "template.properties", "summary.json")))
        .thenReturn(Map.of("data.json", dataEntry));
    lenient().when(lifecycleReport.getEntry("bom.json")).thenReturn(null);
    lenient().when(lifecycleReport.getEntry("data.json")).thenReturn(dataEntry);

    PolicyEvaluation evaluation = Mockito.mock(PolicyEvaluation.class);
    lenient().when(evaluation.getStageTypeId()).thenReturn(BuildStageType.ID);
    when(evaluation.getScanTriggerType()).thenReturn(ScanTriggerType.REPOSITORY_MANAGER);
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId("app-id", scanId)).thenReturn(evaluation);

    RepositoryComponent comp = newComponent(repoId, outerPath, "file_sha1");
    when(repositoryComponentDAO.getByScanId(scanId)).thenReturn(comp);

    Repository repository = new Repository();
    repository.setFormat("npm");
    when(repositoryDAO.getById(repoId)).thenReturn(repository);

    ComponentIdentifier axios = newNpmIdentifier("axios", "0.18.0");
    RepositoryPolicyViolation outer = newInnerViolation(outerPath, "file_sha1", axios, "cve-1", 14);
    RepositoryPolicyViolation innerMirror =
        newInnerViolation(outerPath + "!/axios@0.18.0", "hds_hash", axios, "cve-1", 14);
    when(repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnameOrInnerPathnames(repoId, outerPath))
        .thenReturn(List.of(outer, innerMirror));

    reportService.getReportMetadataNoAuth(pubId, scanId);

    ArgumentCaptor<java.io.InputStream> captor = ArgumentCaptor.forClass(java.io.InputStream.class);
    verify(lifecycleReportPersistenceService)
        .saveReportFile(eq("app-id"), eq(scanId), eq("policythreats.json"), captor.capture());
    String json = new String(captor.getValue().readAllBytes());
    int aaDataEntries = new com.fasterxml.jackson.databind.ObjectMapper()
        .readTree(json)
        .path("aaData")
        .size();
    assertThat(aaDataEntries)
        .as("recovery path persists reshaped policythreats.json: npm outer self-mirror dropped, one entry")
        .isEqualTo(1);
  }

  private static ComponentIdentifier newNpmIdentifier(String packageId, String version) {
    TreeMap<String, String> coords = new TreeMap<>();
    coords.put("packageId", packageId);
    coords.put("version", version);
    return new ComponentIdentifier("npm", coords);
  }

  private static RepositoryPolicyViolation newInnerViolation(
      String pathname,
      String hash,
      ComponentIdentifier ci,
      String constraintFactsId,
      int threatLevel)
  {
    RepositoryPolicyViolation v = newViolation("vid-" + pathname, "pol-sec", "Security-High", threatLevel, false);
    v.setPathname(pathname);
    v.setHash(hash);
    v.setComponentIdentifier(ci);
    v.setConstraintFactsId(constraintFactsId);
    return v;
  }

  // ---- CLM-42080: reevaluateHostedComponent invokes the nested-violation mirror ----
  //
  // The bug: before this fix, reevaluateHostedComponent updated only the outer artifact's
  // repository_policy_violation row via RepositoryPolicyEvaluator.evaluate. Any mirrored
  // inner-pathname rows (created on the initial-scan path by
  // HostedComponentScanQueueConsumer.mirrorNestedComponentViolationsFromApplicationEvaluation
  // when HDS couldn't identify the outer) stayed at whatever threat_level they carried at
  // initial scan time. After a policy severity edit + Re-Evaluate, the Application Report
  // showed a mix of new (outer) and stale (inner) severities for the same policy.
  //
  // Fix: reevaluateHostedComponent now invokes the same mirror method after the outer eval.
  // These tests lock in that contract at the wiring layer — the mirror's own behaviour is
  // covered by HostedComponentScanQueueConsumerTest.

  @Test
  public void reevaluateHostedComponent_invokesMirrorAfterOuterEval() {
    RepositoryComponent component = newComponent("repo1", "outer.zip", "outerhash123");
    Repository repository = new Repository();
    repository.setId("repo1");
    Application application = new Application();
    application.setId("app1");
    PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setStageTypeId("proxy");

    when(repositoryComponentDAO.getByScanId("scan1")).thenReturn(component);
    when(repositoryDAO.getById("repo1")).thenReturn(repository);
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId("app1", "scan1"))
        .thenReturn(policyEvaluation);
    when(applicationDAO.getByIdNotNull("app1")).thenReturn(application);
    when(repositoryPolicyEvaluatorProvider.get()).thenReturn(repositoryPolicyEvaluator);

    reportService.reevaluateHostedComponent("app1", "scan1");

    // Assert: mirror was called with primitives derived from the component + scan.
    // jobLogId=scanId (no queue entry on re-eval)
    // componentIdOrNull=null (re-eval has no NXRM queue entry to source componentId from)
    // stageTypeId comes from the last policy_evaluation row for this scan
    verify(hostedComponentScanQueueConsumer)
        .mirrorNestedComponentViolationsFromApplicationEvaluation(
            eq("scan1"),
            eq("repo1"),
            eq("outer.zip"),
            eq("outerhash123"),
            same(application),
            eq("scan1"),
            eq("proxy"),
            isNull());
  }

  @Test
  public void reevaluateHostedComponent_usesComplianceStageWhenNoPriorPolicyEvaluation() {
    // Regression guard: if getLastByOwnerIdAndScanId returns null (no prior primary eval
    // recorded — e.g. Re-Evaluate clicked on a scan whose policy_evaluation row hasn't landed
    // yet), reevaluateHostedComponent falls back to ComplianceStageType.ID. The mirror must
    // receive the same fallback so its ScanPolicyEvaluator.evaluate runs on the correct stage.
    RepositoryComponent component = newComponent("repoX", "archive.tar", "hashX");
    Repository repository = new Repository();
    repository.setId("repoX");
    Application application = new Application();
    application.setId("appX");

    when(repositoryComponentDAO.getByScanId("scanX")).thenReturn(component);
    when(repositoryDAO.getById("repoX")).thenReturn(repository);
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId("appX", "scanX")).thenReturn(null);
    when(applicationDAO.getByIdNotNull("appX")).thenReturn(application);
    when(repositoryPolicyEvaluatorProvider.get()).thenReturn(repositoryPolicyEvaluator);

    reportService.reevaluateHostedComponent("appX", "scanX");

    verify(hostedComponentScanQueueConsumer)
        .mirrorNestedComponentViolationsFromApplicationEvaluation(
            eq("scanX"),
            eq("repoX"),
            eq("archive.tar"),
            eq("hashX"),
            same(application),
            eq("scanX"),
            eq("compliance"),
            isNull());
  }

  @Test
  public void reevaluateHostedComponent_mirrorFailure_isSwallowedAndDoesNotBreakReeval() {
    // Regression guard for the fail-soft contract: the mirror method's own catch(Exception)
    // is defence-in-depth #1; this outer catch in ReportService.reevaluateHostedComponent is
    // defence-in-depth #2. Together they guarantee that a mirror failure never rolls back
    // the outer eval that has already persisted. The mirror is idempotent (delete-then-insert
    // inner rows), so the next re-eval retries.
    RepositoryComponent component = newComponent("repoY", "archive.zip", "hashY");
    Repository repository = new Repository();
    repository.setId("repoY");
    Application application = new Application();
    application.setId("appY");
    PolicyEvaluation policyEvaluation = new PolicyEvaluation();
    policyEvaluation.setStageTypeId("proxy");

    when(repositoryComponentDAO.getByScanId("scanY")).thenReturn(component);
    when(repositoryDAO.getById("repoY")).thenReturn(repository);
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId("appY", "scanY"))
        .thenReturn(policyEvaluation);
    when(applicationDAO.getByIdNotNull("appY")).thenReturn(application);
    when(repositoryPolicyEvaluatorProvider.get()).thenReturn(repositoryPolicyEvaluator);
    doThrow(new RuntimeException("simulated mirror crash"))
        .when(hostedComponentScanQueueConsumer)
        .mirrorNestedComponentViolationsFromApplicationEvaluation(
            anyString(), anyString(), anyString(), anyString(),
            any(Application.class), anyString(), anyString(), any());

    assertThatCode(() -> reportService.reevaluateHostedComponent("appY", "scanY"))
        .as("mirror failures must not propagate — outer eval has already persisted "
            + "and the mirror is idempotent (next re-eval retries)")
        .doesNotThrowAnyException();
  }

  @Test
  public void reevaluateHostedComponent_applicationLookupFailure_atMirrorStep_skipsMirrorCleanly() {
    RepositoryComponent component = newComponent("repoZ", "archive.zip", "hashZ");
    Repository repository = new Repository();
    repository.setId("repoZ");

    when(repositoryComponentDAO.getByScanId("scanZ")).thenReturn(component);
    when(repositoryDAO.getById("repoZ")).thenReturn(repository);
    when(applicationDAO.getByIdNotNull("appZ"))
        .thenThrow(new com.sonatype.insight.error.exception.NotFoundException(
            "Application not found: appZ"));

    // Post CLM-41904 merge: applicationDAO.getByIdNotNull is now called at the top of the
    // method (before the ClusterLock is acquired), so an app-lookup failure fails fast and
    // never reaches evaluate or the mirror step. Pre-merge, this test verified that the
    // mirror-step try/catch swallowed the failure. The observable contract (mirror not
    // invoked, NotFoundException surfaced to caller) is preserved.
    assertThatThrownBy(() -> reportService.reevaluateHostedComponent("appZ", "scanZ"))
        .isInstanceOf(com.sonatype.insight.error.exception.NotFoundException.class)
        .hasMessageContaining("appZ");

    verify(hostedComponentScanQueueConsumer, never())
        .mirrorNestedComponentViolationsFromApplicationEvaluation(
            anyString(), anyString(), anyString(), anyString(),
            any(Application.class), anyString(), anyString(), any());
  }

  @Test
  public void reevaluateHostedComponent_throwsWhenComponentMissing_mirrorNotCalled() {
    // Fail-fast guard: no repository_component row for this scanId → NotFoundException from
    // the existing null-check at ReportService. Mirror must NOT be invoked in that case
    // (the error surfaces to the caller before we get to the mirror).
    when(repositoryComponentDAO.getByScanId("missing-scan")).thenReturn(null);

    assertThatThrownBy(() -> reportService.reevaluateHostedComponent("app1", "missing-scan"))
        .isInstanceOf(com.sonatype.insight.error.exception.NotFoundException.class);

    verify(hostedComponentScanQueueConsumer, never())
        .mirrorNestedComponentViolationsFromApplicationEvaluation(
            anyString(), anyString(), anyString(), anyString(),
            any(Application.class), anyString(), anyString(), any());
  }

  // ---- helpers ----

  private static RepositoryComponent newComponent(String repositoryId, String pathname, String hash) {
    RepositoryComponent c = new RepositoryComponent();
    c.setRepositoryId(repositoryId);
    c.setPathname(pathname);
    c.setHash(hash);
    return c;
  }

  private static RepositoryPolicyViolation newViolation(
      String id,
      String policyId,
      String policyName,
      int threatLevel,
      boolean waived)
  {
    RepositoryPolicyViolation v = new RepositoryPolicyViolation();
    v.setId(id);
    v.setPolicyId(policyId);
    v.setPolicyName(policyName);
    v.setThreatLevel(threatLevel);
    v.setWaived(waived);
    return v;
  }

  private static RepositoryPolicyViolation newViolationForRiskSum(
      String id,
      String pathname,
      String hash,
      ComponentIdentifier ci,
      String policyId,
      int threatLevel)
  {
    RepositoryPolicyViolation v = newViolation(id, policyId, "Policy-" + policyId, threatLevel, false);
    v.setPathname(pathname);
    v.setHash(hash);
    v.setComponentIdentifier(ci);
    return v;
  }

  private static ComponentIdentifier ci(String format, String packageId, String version) {
    TreeMap<String, String> coords = new TreeMap<>();
    coords.put("packageId", packageId);
    coords.put("version", version);
    return new ComponentIdentifier(format, coords);
  }

  // ---- persistHostedComponentReevaluation (CLM-41904) ----

  private TransactionContext stubPersistPlumbing() {
    TransactionContext tx = mock(TransactionContext.class);
    when(policyEvaluationDAO.createTransactionContext()).thenReturn(tx);
    return tx;
  }

  private PolicyEvaluation firstTimeHostedRow(final String appId, final String stage, final String scanId) {
    return new PolicyEvaluation(appId, stage, scanId, false, false, "system",
        ScanTriggerType.HOSTED_REPOSITORY_SCANNING, null);
  }

  @Test
  public void persistHostedComponentReevaluation_firstTimeCall_insertsRowWithReevaluationFalse() {
    TransactionContext tx = stubPersistPlumbing();
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId(tx, "app-1", "scan-1")).thenReturn(null);

    reportService.persistHostedComponentReevaluation("app-1", "scan-1", "release");

    ArgumentCaptor<PolicyEvaluation> captor = ArgumentCaptor.forClass(PolicyEvaluation.class);
    verify(policyEvaluationDAO).insert(any(TransactionContext.class), captor.capture());
    PolicyEvaluation inserted = captor.getValue();
    assertThat(inserted.isReevaluation()).isFalse();
    assertThat(inserted.isForObsoleteScan()).isFalse();
    assertThat(inserted.getScanTriggerType()).isEqualTo(ScanTriggerType.REPOSITORY_MANAGER);
    verify(tx).commit();
  }

  @Test
  public void persistHostedComponentReevaluation_priorFirstTimeRowExists_insertsReevaluationRow() {
    TransactionContext tx = stubPersistPlumbing();
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId(tx, "app-1", "scan-1"))
        .thenReturn(firstTimeHostedRow("app-1", "release", "scan-1"));
    when(policyEvaluationDAO.getLastPrimaryByOwnerIdAndStageId(tx, "app-1", "release"))
        .thenReturn(firstTimeHostedRow("app-1", "release", "scan-1"));

    reportService.persistHostedComponentReevaluation("app-1", "scan-1", "release");

    ArgumentCaptor<PolicyEvaluation> captor = ArgumentCaptor.forClass(PolicyEvaluation.class);
    verify(policyEvaluationDAO).insert(any(TransactionContext.class), captor.capture());
    PolicyEvaluation inserted = captor.getValue();
    assertThat(inserted.getOwnerId()).isEqualTo("app-1");
    assertThat(inserted.getScanId()).isEqualTo("scan-1");
    assertThat(inserted.getStageTypeId()).isEqualTo("release");
    assertThat(inserted.isReevaluation()).isTrue();
    assertThat(inserted.isForMonitoring()).isFalse();
    assertThat(inserted.isForObsoleteScan()).isFalse();
    assertThat(inserted.getInitiator()).isEqualTo("system");
    assertThat(inserted.getScanTriggerType()).isEqualTo(ScanTriggerType.REPOSITORY_MANAGER);
    verify(tx).commit();
  }

  @Test
  public void persistHostedComponentReevaluation_repeatedReeval_insertsAnotherRow() {
    TransactionContext tx = stubPersistPlumbing();
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId(tx, "app-1", "scan-1"))
        .thenReturn(firstTimeHostedRow("app-1", "release", "scan-1"));
    when(policyEvaluationDAO.getLastPrimaryByOwnerIdAndStageId(tx, "app-1", "release"))
        .thenReturn(firstTimeHostedRow("app-1", "release", "scan-1"));

    reportService.persistHostedComponentReevaluation("app-1", "scan-1", "release");
    reportService.persistHostedComponentReevaluation("app-1", "scan-1", "release");

    verify(policyEvaluationDAO, times(2))
        .insert(any(TransactionContext.class), any(PolicyEvaluation.class));
  }

  @Test
  public void persistHostedComponentReevaluation_marksNewRowObsoleteWhenReEvalIsForOlderScan() {
    TransactionContext tx = stubPersistPlumbing();
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId(tx, "app-1", "older-scan"))
        .thenReturn(firstTimeHostedRow("app-1", "release", "older-scan"));
    PolicyEvaluation priorPrimary = firstTimeHostedRow("app-1", "release", "current-latest-scan");
    when(policyEvaluationDAO.getLastPrimaryByOwnerIdAndStageId(tx, "app-1", "release"))
        .thenReturn(priorPrimary);

    reportService.persistHostedComponentReevaluation("app-1", "older-scan", "release");

    ArgumentCaptor<PolicyEvaluation> captor = ArgumentCaptor.forClass(PolicyEvaluation.class);
    verify(policyEvaluationDAO).insert(any(TransactionContext.class), captor.capture());
    assertThat(captor.getValue().isReevaluation()).isTrue();
    assertThat(captor.getValue().isForObsoleteScan()).isTrue();
    assertThat(priorPrimary.isForObsoleteScan()).isFalse();
    verify(policyEvaluationDAO, never()).update(any(TransactionContext.class), any(PolicyEvaluation.class));
  }

  @Test
  public void persistHostedComponentReevaluation_firstTimeCall_doesNotQueryLastPrimary() {
    TransactionContext tx = stubPersistPlumbing();
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId(tx, "app-1", "scan-1")).thenReturn(null);

    reportService.persistHostedComponentReevaluation("app-1", "scan-1", "release");

    verify(policyEvaluationDAO, never()).getLastPrimaryByOwnerIdAndStageId(
        any(TransactionContext.class), any(String.class), any(String.class));
  }

  @Test
  public void persistHostedComponentReevaluation_propagatesRuntimeExceptionFromInsert() {
    stubPersistPlumbing();
    RuntimeException dbFailure = new IllegalStateException("db down");
    doThrow(dbFailure).when(policyEvaluationDAO)
        .insert(any(TransactionContext.class), any(PolicyEvaluation.class));

    assertThatThrownBy(() -> reportService.persistHostedComponentReevaluation("app-1", "scan-1", "release"))
        .isSameAs(dbFailure);
  }

  // Suresh comment #1: verify the transaction commits only on success. If insert throws, the
  // try-with-resources on TransactionContext must close (rollback-on-close) without a commit —
  // otherwise a future refactor could accidentally commit a half-written row.
  @Test
  public void persistHostedComponentReevaluation_doesNotCommitWhenInsertThrows() {
    TransactionContext tx = stubPersistPlumbing();
    doThrow(new IllegalStateException("db down")).when(policyEvaluationDAO)
        .insert(any(TransactionContext.class), any(PolicyEvaluation.class));

    assertThatThrownBy(() -> reportService.persistHostedComponentReevaluation("app-1", "scan-1", "release"))
        .isInstanceOf(IllegalStateException.class);

    verify(tx, never()).commit();
    verify(tx).close();
  }

  // ---- reevaluateHostedComponent (CLM-41904, cluster-lock widened per iam-ast comment) ----

  private RepositoryComponent stubReevaluatePlumbing(final String appId, final String scanId) {
    RepositoryComponent comp = newComponent("repo-1", "com/example/lib.jar", "hash-abc");
    when(repositoryComponentDAO.getByScanId(scanId)).thenReturn(comp);
    com.sonatype.insight.brain.model.repository.Repository repo =
        mock(com.sonatype.insight.brain.model.repository.Repository.class);
    when(repo.getId()).thenReturn("repo-1");
    when(repo.getFormat()).thenReturn("maven");
    when(repositoryDAO.getById("repo-1")).thenReturn(repo);
    Application application = mock(Application.class);
    when(applicationDAO.getByIdNotNull(appId)).thenReturn(application);
    when(clusterLockManager.createForPolicyEvaluation(application, scanId)).thenReturn(mock(ClusterLock.class));
    when(repositoryPolicyEvaluatorProvider.get()).thenReturn(repositoryPolicyEvaluator);
    // saveOverlayFiles reads the application report; return a null-friendly stub.
    when(reportDataStore.getLifecycleReport(any(), any())).thenReturn(null);
    when(policyEvaluationDAO.createTransactionContext()).thenReturn(mock(TransactionContext.class));
    return comp;
  }

  @Test
  public void reevaluateHostedComponent_acquiresClusterLockForApplicationAndScanId() {
    stubReevaluatePlumbing("app-1", "scan-1");
    Application application = applicationDAO.getByIdNotNull("app-1");
    ClusterLock lock = mock(ClusterLock.class);
    when(clusterLockManager.createForPolicyEvaluation(application, "scan-1")).thenReturn(lock);

    reportService.reevaluateHostedComponent("app-1", "scan-1");

    verify(clusterLockManager).createForPolicyEvaluation(application, "scan-1");
    verify(lock).lock();
    verify(lock).close();
  }

  // iam-ast comment #5: the widened lock must protect evaluate + saveOverlayFiles + persist.
  // Verify all three write-path calls happen while the lock is held (lock().lock() before,
  // close() after), so future refactors can't accidentally move work outside the critical section.
  @Test
  public void reevaluateHostedComponent_holdsClusterLockAcrossEvaluateSaveOverlayAndPersist() throws Exception {
    stubReevaluatePlumbing("app-1", "scan-1");
    Application application = applicationDAO.getByIdNotNull("app-1");
    ClusterLock lock = mock(ClusterLock.class);
    when(clusterLockManager.createForPolicyEvaluation(application, "scan-1")).thenReturn(lock);

    reportService.reevaluateHostedComponent("app-1", "scan-1");

    org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
        lock, repositoryPolicyEvaluator, lifecycleReportPersistenceService, policyEvaluationDAO);
    inOrder.verify(lock).lock();
    inOrder.verify(repositoryPolicyEvaluator)
        .evaluate(any(), any(), org.mockito.ArgumentMatchers.eq(false),
            org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.anyString());
    inOrder.verify(lifecycleReportPersistenceService)
        .saveReportFile(
            org.mockito.ArgumentMatchers.eq("app-1"), org.mockito.ArgumentMatchers.eq("scan-1"),
            org.mockito.ArgumentMatchers.eq("policythreats.json"), any());
    inOrder.verify(policyEvaluationDAO).insert(any(TransactionContext.class), any(PolicyEvaluation.class));
    inOrder.verify(lock).close();
  }

  // Suresh comment #3: if clusterLock.lock() throws (e.g. PostgresClusterLock connection failure),
  // no downstream work should run — no evaluate, no saveOverlayFiles, no policy_evaluation write.
  // The try-with-resources still closes the (partially-constructed) lock.
  @Test
  public void reevaluateHostedComponent_haltsBeforeAnyWriteWhenLockAcquisitionFails() throws Exception {
    stubReevaluatePlumbing("app-1", "scan-1");
    Application application = applicationDAO.getByIdNotNull("app-1");
    ClusterLock lock = mock(ClusterLock.class);
    RuntimeException lockFailure = new IllegalStateException("cluster lock connection failed");
    doThrow(lockFailure).when(lock).lock();
    when(clusterLockManager.createForPolicyEvaluation(application, "scan-1")).thenReturn(lock);

    assertThatThrownBy(() -> reportService.reevaluateHostedComponent("app-1", "scan-1")).isSameAs(lockFailure);

    verify(repositoryPolicyEvaluator, never()).evaluate(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    verify(lifecycleReportPersistenceService, never()).saveReportFile(
        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(), any());
    verify(policyEvaluationDAO, never()).insert(any(TransactionContext.class), any(PolicyEvaluation.class));
    verify(lock).close();
  }

  // Suresh comment #2: guard against a future refactor re-adding summary.json to the overlay-write
  // loop. summary.json is HDS-owned; overwriting it with the local builder's placeholder was the
  // exact bug that emptied Latest Evaluations (CLM-41904).
  @Test
  public void reevaluateHostedComponent_neverOverwritesSummaryJson() throws Exception {
    stubReevaluatePlumbing("app-1", "scan-1");

    reportService.reevaluateHostedComponent("app-1", "scan-1");

    verify(lifecycleReportPersistenceService, never()).saveReportFile(
        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.eq("summary.json"), any());
  }

  // CLM-42080 followup: mirror must run before the outer cluster lock is acquired.
  @Test
  public void reevaluateHostedComponent_mirrorRunsBeforeClusterLockAcquired() throws Exception {
    stubReevaluatePlumbing("app-1", "scan-1");
    Application application = applicationDAO.getByIdNotNull("app-1");
    ClusterLock lock = mock(ClusterLock.class);
    when(clusterLockManager.createForPolicyEvaluation(application, "scan-1")).thenReturn(lock);

    reportService.reevaluateHostedComponent("app-1", "scan-1");

    // stubReevaluatePlumbing leaves getLastByOwnerIdAndScanId unstubbed → null →
    // ReportService falls back to ComplianceStageType.ID ("compliance"), same as the sibling
    // reevaluateHostedComponent_usesComplianceStageWhenNoPriorPolicyEvaluation test.
    org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(hostedComponentScanQueueConsumer, lock);
    inOrder.verify(hostedComponentScanQueueConsumer)
        .mirrorNestedComponentViolationsFromApplicationEvaluation(
            eq("scan-1"),
            eq("repo-1"),
            eq("com/example/lib.jar"),
            eq("hash-abc"),
            same(application),
            eq("scan-1"),
            eq("compliance"),
            isNull());
    inOrder.verify(lock).lock();
    inOrder.verify(lock).close();
  }

  // Pins the fail-soft contract: even when the mirror throws, reevaluateHostedComponent must
  // still acquire the outer cluster lock, run the outer evaluate, and release the lock.
  @Test
  public void reevaluateHostedComponent_mirrorFailure_stillAcquiresLockAndRunsOuterEval() throws Exception {
    stubReevaluatePlumbing("app-1", "scan-1");
    Application application = applicationDAO.getByIdNotNull("app-1");
    ClusterLock lock = mock(ClusterLock.class);
    when(clusterLockManager.createForPolicyEvaluation(application, "scan-1")).thenReturn(lock);
    doThrow(new RuntimeException("simulated mirror crash"))
        .when(hostedComponentScanQueueConsumer)
        .mirrorNestedComponentViolationsFromApplicationEvaluation(
            anyString(), anyString(), anyString(), anyString(),
            any(Application.class), anyString(), anyString(), any());

    assertThatCode(() -> reportService.reevaluateHostedComponent("app-1", "scan-1"))
        .doesNotThrowAnyException();

    verify(lock).lock();
    verify(repositoryPolicyEvaluator).evaluate(any(), any(), org.mockito.ArgumentMatchers.eq(false),
        org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.anyString());
    verify(lock).close();
  }

  // ---- refreshHostedComponentAfterEvaluation (CLM-42136 shared helper) ----
  //
  // Direct unit tests on the public helper used by both the Re-Evaluate button and the CM flow
  // processor. Coverage locks in the flag-driven persist behavior and the fail-soft contract on
  // the mirror step so future refactors can't quietly change either.

  @Test
  public void refreshHostedComponentAfterEvaluation_persistFlagTrue_insertsPolicyEvaluationRow() {
    RepositoryComponent component = newComponent("repo1", "outer.zip", "outerhash123");
    Repository repository = new Repository();
    repository.setId("repo1");
    Application application = new Application();
    application.setId("app-1");

    reportService.refreshHostedComponentAfterEvaluation(
        component, repository, application, "app-1", "scan-1", "build", true);

    verify(hostedComponentScanQueueConsumer)
        .mirrorNestedComponentViolationsFromApplicationEvaluation(
            eq("scan-1"), eq("repo1"), eq("outer.zip"), eq("outerhash123"),
            same(application), eq("scan-1"), eq("build"), isNull());
    // Explicit persist call — Re-Evaluate button path.
    verify(policyEvaluationDAO).insert(any(TransactionContext.class), any(PolicyEvaluation.class));
  }

  @Test
  public void refreshHostedComponentAfterEvaluation_persistFlagFalse_skipsExplicitPolicyEvaluationInsert() {
    RepositoryComponent component = newComponent("repo1", "outer.zip", "outerhash123");
    Repository repository = new Repository();
    repository.setId("repo1");
    Application application = new Application();
    application.setId("app-1");

    reportService.refreshHostedComponentAfterEvaluation(
        component, repository, application, "app-1", "scan-1", "build", false);

    verify(hostedComponentScanQueueConsumer)
        .mirrorNestedComponentViolationsFromApplicationEvaluation(
            anyString(), anyString(), anyString(), anyString(),
            any(Application.class), anyString(), anyString(), any());
    // The explicit persist call this flag controls must NOT fire; NOTE: the mirror step itself
    // (via ScanPolicyEvaluator.evaluate) still inserts its own policy_evaluation row in the
    // real implementation — this test uses a mocked consumer so the mirror side-effect is a
    // no-op and we can assert cleanly that policyEvaluationDAO.insert isn't invoked from
    // refreshHostedComponentAfterEvaluation itself.
    verify(policyEvaluationDAO, never()).insert(any(TransactionContext.class), any(PolicyEvaluation.class));
  }

  /**
   * Fail-soft contract on the mirror step: a mirror crash must not skip the overlay-file save.
   * The mirror is idempotent — the next successful re-evaluation retries — but a stale overlay
   * would leave the UI showing pre-refresh threat levels indefinitely.
   */
  @Test
  public void refreshHostedComponentAfterEvaluation_mirrorFailureDoesNotBlockOverlaySave() {
    RepositoryComponent component = newComponent("repoY", "archive.zip", "hashY");
    Repository repository = new Repository();
    repository.setId("repoY");
    Application application = new Application();
    application.setId("appY");
    doThrow(new RuntimeException("simulated mirror crash"))
        .when(hostedComponentScanQueueConsumer)
        .mirrorNestedComponentViolationsFromApplicationEvaluation(
            anyString(), anyString(), anyString(), anyString(),
            any(Application.class), anyString(), anyString(), any());

    assertThatCode(() -> reportService.refreshHostedComponentAfterEvaluation(
        component, repository, application, "appY", "scanY", "build", true))
            .as("mirror failure must not propagate — outer eval has already persisted, "
                + "and the mirror is idempotent (next re-eval retries)")
            .doesNotThrowAnyException();
    // Persist still fires because the mirror failure was swallowed.
    verify(policyEvaluationDAO).insert(any(TransactionContext.class), any(PolicyEvaluation.class));
  }

  /**
   * Overlay-save failure is fatal to the Re-Evaluate button path: a stale {@code policythreats.json}
   * would leave the UI showing pre-refresh threat levels, so the checked exception is wrapped in
   * a 500. persistHostedComponentReevaluation must NOT run afterwards — advancing the "Triggered
   * by" timestamp on top of a stale overlay would be worse than not advancing it at all.
   */
  @Test
  public void refreshHostedComponentAfterEvaluation_overlayCheckedException_wrappedAndPersistNotCalled() throws Exception {
    RepositoryComponent component = newComponent("repo1", "outer.zip", "outerhash");
    Repository repository = new Repository();
    repository.setId("repo1");
    Application application = new Application();
    application.setId("app-1");
    when(repositoryComponentDAO.getByScanId("scan-1")).thenReturn(component);
    when(applicationDAO.getByIdNotNull("app-1")).thenReturn(application);
    // Force saveOverlayFiles to throw a checked Exception via lifecycleReportPersistenceService
    // (its saveReportFile declares throws IOException).
    doThrow(new java.io.IOException("simulated disk failure writing policythreats.json"))
        .when(lifecycleReportPersistenceService)
        .saveReportFile(anyString(), anyString(), anyString(), any());

    assertThatThrownBy(() -> reportService.refreshHostedComponentAfterEvaluation(
        component, repository, application, "app-1", "scan-1", "build", true))
            .isInstanceOf(jakarta.ws.rs.InternalServerErrorException.class)
            .hasMessageContaining("scan-1");
    verify(policyEvaluationDAO, never()).insert(any(TransactionContext.class), any(PolicyEvaluation.class));
  }

  /**
   * Unchecked exceptions from saveOverlayFiles must propagate as-is so the caller sees the
   * original type rather than a wrapped 500. The CM flow processor relies on this to record its
   * drop metric with the underlying failure type.
   */
  @Test
  public void refreshHostedComponentAfterEvaluation_overlayRuntimeException_propagatesAsIs() throws Exception {
    RepositoryComponent component = newComponent("repo1", "outer.zip", "outerhash");
    Repository repository = new Repository();
    repository.setId("repo1");
    Application application = new Application();
    application.setId("app-1");
    when(repositoryComponentDAO.getByScanId("scan-1")).thenReturn(component);
    when(applicationDAO.getByIdNotNull("app-1")).thenReturn(application);
    IllegalStateException runtimeBoom = new IllegalStateException("disk full");
    doThrow(runtimeBoom)
        .when(lifecycleReportPersistenceService)
        .saveReportFile(anyString(), anyString(), anyString(), any());

    assertThatThrownBy(() -> reportService.refreshHostedComponentAfterEvaluation(
        component, repository, application, "app-1", "scan-1", "build", true))
            .isSameAs(runtimeBoom);
    verify(policyEvaluationDAO, never()).insert(any(TransactionContext.class), any(PolicyEvaluation.class));
  }
}
