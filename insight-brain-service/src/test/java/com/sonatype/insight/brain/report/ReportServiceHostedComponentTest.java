/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.repository.hosted.HostedReportFileBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
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
  private ApplicationReportPersistenceService applicationReportPersistenceService;

  @InjectMocks
  private ReportService reportService;

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
}
