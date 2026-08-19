/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.scan.model.ClientScanType;

import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers {@link ReportService#reUploadScanToHds(Owner, String, String)} for a
 * {@link HostedRepositoryComponent} owner: it must resolve the policy evaluation and the stored scan by the
 * HRC's own id, upload the scan under the HRC owner, and materialize the regenerated report back under the
 * canonical scan id.
 */
@ExtendWith(MockitoExtension.class)
public class ReportServiceReUploadScanToHdsTest
{
  private static final String HRC_ID = "hrc-id-1";

  private static final String CANONICAL_SCAN_ID = "canonical-scan-id";

  private static final String TEMP_SCAN_ID = "temp-scan-id";

  private static final String CLIENT_USER_AGENT = "Nexus-Repository-Manager/3.70.0";

  @Mock
  private PolicyEvaluationDAO policyEvaluationDAO;

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
  private com.sonatype.insight.brain.dataaccess.policy.PolicyDAO policyDAO;

  @Mock
  private com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

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
  private ReportDataStore reportDataStore;

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
  private com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @InjectMocks
  private ReportService reportService;

  private HostedRepositoryComponent hrc;

  @BeforeEach
  public void setUp() {
    hrc = new HostedRepositoryComponent("repo-id-1", "acme-lib-1.0.0.tgz", "sha1-abc");
    hrc.setId(HRC_ID);
  }

  @Test
  public void reUploadScanToHds_hrcOverload_preservesCanonicalScanId() throws Exception {
    PolicyEvaluation policyEvaluation = newPolicyEvaluation();
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId(HRC_ID, CANONICAL_SCAN_ID)).thenReturn(policyEvaluation);

    ScanEntity scanEntity = mock(ScanEntity.class);
    when(scanPersistenceService.getScan(HRC_ID, CANONICAL_SCAN_ID)).thenReturn(scanEntity);

    ScanReceipt scanReceipt = mock(ScanReceipt.class);
    when(scanReceipt.getScanId()).thenReturn(TEMP_SCAN_ID);
    // scanContext (arg 8) is non-null: the owner-agnostic method builds one from the scan's third-party
    // SBOM metadata, which for an HRC is absent — so the context carries isValid=true and no
    // container-image specification. Asserted below rather than matched loosely.
    when(scanUploadService.upload(same(scanEntity), same(hrc), eq(BuildStageType.ID),
        eq(ClientScanType.SONATYPE), eq(CLIENT_USER_AGENT), any(), isNull(), any(), anyBoolean()))
            .thenReturn(scanReceipt);

    LifecycleReport originalReport = mock(LifecycleReport.class);
    lenient().when(reportDataStore.getLifecycleReport(same(hrc), eq(CANONICAL_SCAN_ID))).thenReturn(originalReport);
    lenient().when(originalReport.exists()).thenReturn(false);

    PolicyEvaluation result = reportService.reUploadScanToHds(hrc, CANONICAL_SCAN_ID, CLIENT_USER_AGENT);

    assertThat(result).isSameAs(policyEvaluation);
    verify(scanReceipt).waitForReport();
    // The regenerated report is downloaded under the temporary scan id HDS assigned...
    verify(reportDataStore).downloadReport(same(hrc), eq(TEMP_SCAN_ID), any());
    // ...then moved back under the canonical scan id, scoped to the HRC's own owner id.
    verify(reportDataStore).moveLifecycleReport(HRC_ID, TEMP_SCAN_ID, CANONICAL_SCAN_ID);

    // An HRC has no third-party SBOM metadata, so the shared method must upload a context that asserts
    // nothing about SBOM validity — not one that carries a container-image specification.
    ArgumentCaptor<ScanContext> contextCaptor = ArgumentCaptor.forClass(ScanContext.class);
    verify(scanUploadService).upload(any(), same(hrc), anyString(), any(), anyString(), any(), isNull(),
        contextCaptor.capture(), anyBoolean());
    assertThat(contextCaptor.getValue().containerImageSbomSpecification()).isNull();
    assertThat(contextCaptor.getValue().isValid()).isTrue();
  }

  @Test
  public void reUploadScanToHds_hrcOverload_throwsWhenNoPolicyEvaluationExists() {
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId(HRC_ID, CANONICAL_SCAN_ID)).thenReturn(null);

    assertThatThrownBy(() -> reportService.reUploadScanToHds(hrc, CANONICAL_SCAN_ID, CLIENT_USER_AGENT))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining(CANONICAL_SCAN_ID);
  }

  @Test
  public void reUploadScanToHds_hrcOverload_looksUpTelemetryUnderHrcOwnerId() throws Exception {
    PolicyEvaluation policyEvaluation = newPolicyEvaluation();
    when(policyEvaluationDAO.getLastByOwnerIdAndScanId(HRC_ID, CANONICAL_SCAN_ID)).thenReturn(policyEvaluation);

    ScanEntity scanEntity = mock(ScanEntity.class);
    when(scanPersistenceService.getScan(HRC_ID, CANONICAL_SCAN_ID)).thenReturn(scanEntity);

    ScanReceipt scanReceipt = mock(ScanReceipt.class);
    when(scanReceipt.getScanId()).thenReturn(TEMP_SCAN_ID);
    when(scanUploadService.upload(any(), any(), anyString(), any(), anyString(), any(), isNull(), any(),
        anyBoolean())).thenReturn(scanReceipt);

    LifecycleReport originalReport = mock(LifecycleReport.class);
    lenient().when(reportDataStore.getLifecycleReport(same(hrc), eq(CANONICAL_SCAN_ID))).thenReturn(originalReport);
    lenient().when(originalReport.exists()).thenReturn(false);

    reportService.reUploadScanToHds(hrc, CANONICAL_SCAN_ID, CLIENT_USER_AGENT);

    // Telemetry is attributed to the HRC, not to a synthetic application.
    verify(telemetryUtils).buildThirdPartyScanTelemetryData(eq(HRC_ID), any(), eq(BuildStageType.ID),
        eq(ScanTriggerType.HOSTED_REPOSITORY_SCANNING), eq(CLIENT_USER_AGENT));
  }

  private PolicyEvaluation newPolicyEvaluation() {
    PolicyEvaluation policyEvaluation = mock(PolicyEvaluation.class);
    when(policyEvaluation.getStageTypeId()).thenReturn(BuildStageType.ID);
    when(policyEvaluation.getScanTriggerType()).thenReturn(ScanTriggerType.HOSTED_REPOSITORY_SCANNING);
    when(policyEvaluation.getClientScanType()).thenReturn(ClientScanType.SONATYPE);
    return policyEvaluation;
  }
}
