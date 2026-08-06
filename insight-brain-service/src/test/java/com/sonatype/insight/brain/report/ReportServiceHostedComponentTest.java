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
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.repository.hosted.HostedComponentScanQueueConsumer;
import com.sonatype.insight.brain.repository.hosted.HostedReportFileBuilder;
import com.sonatype.insight.brain.security.SecurityAspectControl;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.After;
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
  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

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
  private com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

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
    SecurityAspectControl.disableEnforcement();

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

  @After
  public void tearDown() {
    SecurityAspectControl.enableEnforcement();
  }

  // ---- isHostedRepositoryComponent ----

  @Test
  public void isHostedRepositoryComponent_returnsTrueWhenComponentExists() {
    when(proxyRepositoryComponentDAO.getByScanId("scan1")).thenReturn(newComponent("repo1", "lib.jar", "abc123"));

    assertThat(reportService.isHostedRepositoryComponent("scan1")).isTrue();
  }

  @Test
  public void isHostedRepositoryComponent_returnsFalseWhenNotFound() {
    when(proxyRepositoryComponentDAO.getByScanId("none")).thenReturn(null);

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
    ProxyRepositoryComponent comp = newComponent("r", "lib.jar", "abc123");
    ProxyRepositoryPolicyViolation v = newViolation("v1", "policy-1", "No Risky Libs", 8, false);

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
    ProxyRepositoryComponent comp = newComponent("r", "lib.jar", "abc");
    ProxyRepositoryPolicyViolation active = newViolation("v1", "p1", "A", 7, false);
    ProxyRepositoryPolicyViolation waived = newViolation("v2", "p2", "B", 5, true);

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
    ProxyRepositoryComponent comp = newComponent("r", "commons-text-1.9.jar", "def456");
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
    ProxyRepositoryComponent comp = newComponent("r", "lib.jar", "abc");
    comp.setMatchStateId("exact");

    String json = new String(HostedReportFileBuilder.build("data.json", comp, List.of()));

    assertThat(json).contains("\"totalArtifactCount\":1");
    assertThat(json).contains("\"knownArtifactCount\":1");
    assertThat(json).contains("\"exactlyMatchedComponentCount\":1");
  }

  @Test
  public void build_data_unknownMatch_knownCountIsZero() throws Exception {
    ProxyRepositoryComponent comp = newComponent("r", "lib.jar", "abc");
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

    ProxyRepositoryComponent comp = newComponent(repoId, outerPath, "file_sha1");
    when(proxyRepositoryComponentDAO.getByScanId(scanId)).thenReturn(comp);

    Repository repository = new Repository();
    repository.setFormat("npm");
    when(repositoryDAO.getById(repoId)).thenReturn(repository);

    ComponentIdentifier axios = newNpmIdentifier("axios", "0.18.0");
    ProxyRepositoryPolicyViolation outer =
        newInnerViolation(outerPath, "file_sha1", axios, "cve-1", 14);
    ProxyRepositoryPolicyViolation innerMirror =
        newInnerViolation(outerPath + "!/axios@0.18.0", "hds_hash", axios, "cve-1", 14);
    when(proxyRepositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnameOrInnerPathnames(repoId, outerPath))
        .thenReturn(List.of(outer, innerMirror));

    ReportMetadataDTO metadata = reportService.getReportMetadataNoAuth(pubId, scanId);

    assertThat(metadata.getTotalRisk())
        .as("outer self-mirror dropped before totalRisk, so the shared finding counts once")
        .isEqualTo(14);
  }

  private static ComponentIdentifier newNpmIdentifier(String packageId, String version) {
    TreeMap<String, String> coords = new TreeMap<>();
    coords.put("packageId", packageId);
    coords.put("version", version);
    return new ComponentIdentifier("npm", coords);
  }

  private static ProxyRepositoryPolicyViolation newInnerViolation(
      String pathname,
      String hash,
      ComponentIdentifier ci,
      String constraintFactsId,
      int threatLevel)
  {
    ProxyRepositoryPolicyViolation v = newViolation("vid-" + pathname, "pol-sec", "Security-High", threatLevel, false);
    v.setPathname(pathname);
    v.setHash(hash);
    v.setComponentIdentifier(ci);
    v.setConstraintFactsId(constraintFactsId);
    return v;
  }

  // ---- helpers ----

  private static ProxyRepositoryComponent newComponent(String repositoryId, String pathname, String hash) {
    ProxyRepositoryComponent c = new ProxyRepositoryComponent();
    c.setRepositoryId(repositoryId);
    c.setPathname(pathname);
    c.setHash(hash);
    return c;
  }

  private static ProxyRepositoryPolicyViolation newViolation(
      String id,
      String policyId,
      String policyName,
      int threatLevel,
      boolean waived)
  {
    ProxyRepositoryPolicyViolation v = new ProxyRepositoryPolicyViolation();
    v.setId(id);
    v.setPolicyId(policyId);
    v.setPolicyName(policyName);
    v.setThreatLevel(threatLevel);
    v.setWaived(waived);
    return v;
  }

  private static ProxyRepositoryPolicyViolation newViolationForRiskSum(
      String id,
      String pathname,
      String hash,
      ComponentIdentifier ci,
      String policyId,
      int threatLevel)
  {
    ProxyRepositoryPolicyViolation v = newViolation(id, policyId, "Policy-" + policyId, threatLevel, false);
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

  // ---- refreshHostedComponentAfterEvaluation (CLM-42136 shared helper) ----
  //
  // Direct unit tests on the public helper used by both the Re-Evaluate button and the CM flow
  // processor. Coverage locks in the flag-driven persist behavior and the fail-soft contract on
  // the mirror step so future refactors can't quietly change either.

  @Test
  public void refreshHostedComponentAfterEvaluation_persistFlagTrue_insertsPolicyEvaluationRow() {
    ProxyRepositoryComponent component = newComponent("repo1", "outer.zip", "outerhash123");
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
    ProxyRepositoryComponent component = newComponent("repo1", "outer.zip", "outerhash123");
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
    ProxyRepositoryComponent component = newComponent("repoY", "archive.zip", "hashY");
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
    ProxyRepositoryComponent component = newComponent("repo1", "outer.zip", "outerhash");
    Repository repository = new Repository();
    repository.setId("repo1");
    Application application = new Application();
    application.setId("app-1");
    when(proxyRepositoryComponentDAO.getByScanId("scan-1")).thenReturn(component);
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
    ProxyRepositoryComponent component = newComponent("repo1", "outer.zip", "outerhash");
    Repository repository = new Repository();
    repository.setId("repo1");
    Application application = new Application();
    application.setId("app-1");
    when(proxyRepositoryComponentDAO.getByScanId("scan-1")).thenReturn(component);
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
