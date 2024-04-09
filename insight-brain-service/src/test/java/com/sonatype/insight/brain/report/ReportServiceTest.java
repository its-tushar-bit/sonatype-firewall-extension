/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyBillOfMaterialsRowDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyDataService;
import com.sonatype.insight.brain.thirdparty.ThirdPartyLicenseRowDTO;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.model.ItemContentType;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import de.schlichtherle.truezip.file.TFile;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportServiceTest
    extends AbstractComponentTest
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Inject
  private InsightWork insightWork;

  private Application app;

  private final String scanId = "ReportServiceTestScanId";

  @Inject
  private Configuration configuration;

  @Inject
  private ThirdPartyDataService thirdPartyDataService;

  @Inject
  private ApplicationRiskService applicationRiskService;

  @Inject
  private TestProductLicense productLicense;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  private ComponentLoaderFactory componentLoaderFactory;

  // No default constructor, can't use @Spy
  private ThirdPartyDataService thirdPartyDataServiceSpy;

  @Mock
  private TelemetrySender telemetrySender;

  @Mock
  private TelemetryUtils telemetryUtils;

  @Mock
  private RepositoryMatcher repositoryMatcher;

  /**
   * To be configured/mocked by each test.
   */
  private ReportDownloader reportDownloader;

  @Mock
  private SbomMetadataUtils sbomMetadataUtils;

  @Before
  public void before() {
    thirdPartyDataServiceSpy = spy(thirdPartyDataService);
    app = tempEntity.newApplicationWithParent();
  }

  private ReportService createReportService() {
    return new ReportService(insightWork, reportDownloader, policyEvaluationDAO, configuration,
        applicationDAO, organizationDAO, thirdPartyDataServiceSpy, telemetrySender, telemetryUtils, repositoryMatcher,
        applicationRiskService, productLicense, sbomMetadataUtils);
  }

  @Test
  public void testFetchReport_Exists() throws Exception {
    createReportFile();

    ReportService reportService = createReportService();
    File report = reportService.fetchReport(app, scanId);
    assertThat(report).isNotNull();
    assertThat(report).isFile();
    assertThat(report.getName()).isEqualTo("report.zip");
    verify(thirdPartyDataServiceSpy, never()).deleteByScanId(eq(scanId));
  }

  @Test
  public void testFetchReport_DoesNotExist() throws Exception {
    MockReportDownloader mockReportDownloader = new MockReportDownloader();
    mockReportDownloader.mockDownloadReport(scanId, "/ReportServiceTest/report");
    reportDownloader = mockReportDownloader.getMock();

    ReportService reportService = createReportService();
    when(thirdPartyDataServiceSpy.getScanData(scanId))
        .thenReturn(new ThirdPartyApplicationReportDTO());

    File report = reportService.fetchReport(app, scanId);
    assertThat(report).isNotNull();
    assertThat(report).isFile();
    assertThat(report.getName()).isEqualTo("report.zip");
    verify(reportDownloader).downloadReport(eq(scanId), any(File.class), eq(2100), eq(5));
  }

  @Test
  public void testFetchReport_ThirdPartyDataMatchesUnknownComponents() throws Exception {
    MockReportDownloader mockReportDownloader = new MockReportDownloader();
    mockReportDownloader.mockDownloadReport(scanId, "/ReportServiceTest/report-with-third-party-data");
    reportDownloader = mockReportDownloader.getMock();
    ReportService reportService = createReportService();

    File reportFile = reportService.fetchReport(app, scanId);

    // Verify bom.json
    ComponentLoader componentLoader = componentLoaderFactory.createComponentLoader(app);
    ReportEntry licenseReportEntry = Report.getEntry(reportFile, Report.LICENSES_JSON_FILENAME);
    ReportEntry securityReportEntry = Report.getEntry(reportFile, Report.SECURITY_JSON_FILENAME);
    ReportEntry bomReportEntry = Report.getEntry(reportFile, Report.BOM_JSON_FILENAME);
    ReportEntry dependenciesReportEntry = Report.getEntry(reportFile, Report.DEPENDENCIES_JSON_FILENAME);
    List<Component> components = componentLoader
        .getAll(licenseReportEntry.buf, securityReportEntry.buf, bomReportEntry.buf, dependenciesReportEntry.buf);
    assertThat(components).hasSize(3);
    assertComponent(components.get(0), "964cd74171f427720480",
        ComponentIdentifier.createMavenCoordinates("apache-httpclient", "commons-httpclient", "3.1", "", "jar"),
        "test/commons-httpclient-3.1.jar", IdentificationSource.SONATYPE);
    assertThat(components.get(0).getAnalyzerFeatures()).usingRecursiveComparison()
        .isEqualTo(new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.HASH, "cli", true, true, true));
    assertComponent(components.get(1), "37b3ce40791bc2dd8068",
        new ComponentIdentifier("debian-9", ImmutableMap.of("name", "glibc", "version", "2.24-11+deb9u3")),
        "dependency:/test/clair-scanner-output.json/glibc:2.24-11+deb9u3", IdentificationSource.CLAIR);
    assertThat(components.get(1).getAnalyzerFeatures()).usingRecursiveComparison().isEqualTo(
        new AnalyzerFeatures(AnalysisSource.THIRD_PARTY, AnalysisType.COORDINATE, "cli", false, false, false));
    assertComponent(components.get(2), "cf085cd08ee27334c573",
        ComponentIdentifier.createPypiCoordinates("altgraph", "0.10.2", null, null),
        "dependency:/pkg:pypi\\altgraph@0.10.2", IdentificationSource.getOrMake("cyclonedx"));
    assertThat(components.get(2).getAnalyzerFeatures()).usingRecursiveComparison().isEqualTo(
        new AnalyzerFeatures(AnalysisSource.THIRD_PARTY, AnalysisType.COORDINATE, "cli", false, false, false,
            ItemContentType.SBOM.name()));

    // Verify security.json
    assertSecurityVulnerability(components.get(0), "cve", "CVE-2012-5783", 5.8F,
        "http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2012-5783");
    assertSecurityVulnerability(components.get(1), "CVE", "CVE-2017-16997", 10.0F,
        "https://security-tracker.debian.org/tracker/CVE-2017-16997");
    assertSecurityVulnerability(components.get(2), "NVD", "CVE-2018-7489", 9.8F,
        "https://nvd.nist.gov/vuln/detail/CVE-2018-7489");

    // Verify licenses.json
    assertLicenses(components.get(0), Collections.singleton("Apache-2.0"), Collections.singleton("No-Sources"));
    assertLicenses(components.get(1), Collections.singleton("UNSPECIFIED"), Collections.emptySet());
    assertLicenses(components.get(2), Collections.singleton("GPL-2.0"), Collections.emptySet());

    // Verify summary.json
    ReportEntry summaryReportEntry = Report.getEntry(reportFile, Report.SUMMARY_JSON_FILENAME);
    JsonNode summaryJsonNode = JsonUtils.parse(summaryReportEntry.buf);
    assertThat(summaryJsonNode.path("knownArtifactCount").asInt()).isEqualTo(3);

    // Verify data.json
    ReportEntry dataReportEntry = Report.getEntry(reportFile, Report.DATA_JSON_FILENAME);
    JsonNode dataJsonNode = JsonUtils.parse(dataReportEntry.buf);
    assertThat(dataJsonNode.path("exactlyMatchedComponentCount").asInt()).isEqualTo(3);
    assertThat(dataJsonNode.path("knownArtifactCount").asInt()).isEqualTo(3);
    assertThat(dataJsonNode.path("securityCounts"))
        .isEqualTo(JsonUtils.asTree(new int[]{1, 1, 0, 0, 0, 1, 0, 0, 0, 0}));
  }

  private void assertLicenses(
      Component component,
      Set<String> declaredLicenseIds,
      Set<String> observedLicenseIds)
  {
    assertThat(component.getDeclaredLicenseIds()).isEqualTo(declaredLicenseIds);
    assertThat(component.getObservedLicenseIds()).isEqualTo(observedLicenseIds);
    assertThat(component.getLicenseThreatGroups()).isNotEmpty();
  }

  private void assertSecurityVulnerability(
      Component component,
      String source,
      String refId,
      float severity,
      String url)
  {
    assertThat(component.getSecurityVulnerabilities()).hasSize(1);
    SecurityVulnerability securityVulnerability = component.getSecurityVulnerabilities().get(0);
    assertSecurityVulnerability(securityVulnerability, source, refId, severity, url);
  }

  private void assertSecurityVulnerability(
      final SecurityVulnerability securityVulnerability,
      final String source,
      final String refId,
      final float severity,
      final String url)
  {
    assertThat(securityVulnerability.getSource()).isEqualTo(source);
    assertThat(securityVulnerability.getRefId()).isEqualTo(refId);
    assertThat(securityVulnerability.getSeverity()).isEqualTo(severity);
    assertThat(securityVulnerability.getUrl()).isEqualTo(url);
  }

  private void assertComponent(
      Component component,
      String hash,
      ComponentIdentifier componentIdentifier,
      String pathname,
      IdentificationSource identificationSource)
  {
    assertThat(component.getHash()).isEqualTo(hash);
    assertThat(component.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(component.getDisplayName())
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    assertThat(component.getPathnames()).isEqualTo(Collections.singletonList(pathname));
    assertThat(component.getIdentificationSource()).isEqualTo(identificationSource);
    assertThat(component.getMatchState()).isEqualTo(MatchState.EXACT);
  }

  @Test
  public void testGetReport_Exists() throws Exception {
    createReportFile();
    ReportService reportService = createReportService();
    File report = reportService.getReport(app.getId(), scanId);
    assertThat(report).isNotNull();
    assertThat(report).isFile();
    assertThat(report.getName()).isEqualTo("report.zip");
  }

  @Test
  public void testGetReport_DoesNotExist() {
    ReportService reportService = createReportService();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> reportService.getReport(app.getId(), scanId))
        .withMessage("Could not find a report with ID ReportServiceTestScanId");
  }

  @Test
  public void testGetReport_DoesNotExistAndEvaluationExist() {
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(), scanId);
    ReportService reportService = createReportService();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> reportService.getReport(app.getId(), scanId))
        .withMessageContaining("The report for application ID " + app.getId() + " and scan ID " + scanId
            + " does not exist. Usually this means the report was deemed obsolete according "
            + "to the data retention policies and hence purged to the trash.");
  }

  @Test
  public void testGetReportMetadataWithoutDeveloperDashboardFeature() throws Exception {
    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    String commitHash = "0b1bbd94b2edbacd441f170ecd59a178e334868f";

    productLicense.setMissingFeatures(LicensedFeature.DEVELOPER_DASHBOARD);

    // ReportResource.getReport requires a report.zip to exist when evaluations exist
    createReportFile(app.getId(), scanId1, zipReportDir("/ReportResourceTest/report-expanded_coverage_false"));
    // use an older data.json to make sure they still work
    createReportFile(app.getId(), scanId2, zipReportDir("/ReportResourceTest/report"));

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId1);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId2, commitHash);

    Policy appPolicy1 = tempEntity.newPolicy(app.getId(), "app owned policy1", 5);
    tempEntity.newPolicyViolation(eval1, appPolicy1, appPolicy1.getThreatLevel() + 1,
            PolicyThreatCategory.SECURITY, "Group1", "Artifact1", "Version1");

    Policy appPolicy2 = tempEntity.newPolicy(app.getId(), "app owned policy2", 8);
    tempEntity.newPolicyViolation(eval2, appPolicy2, appPolicy2.getThreatLevel() + 1,
            PolicyThreatCategory.SECURITY, "Group1", "Artifact1", "Version1");

    ReportService reportService = createReportService();

    // Verify Response for scan 1
    ReportMetadataDTO metadata = reportService.getReportMetadata(app.getPublicId(), scanId1);
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getApplication().getOrganizationId()).isEqualTo(app.getOrganizationId());
    assertThat(metadata.getApplication().getOrganization()).isNotNull();
    assertThat(metadata.getApplication().getOrganization().getId()).isEqualTo(app.getOrganizationId());
    assertThat(metadata.getReportTitle()).isEqualTo("Build Report");
    assertThat(metadata.getReportTime()).isEqualTo(eval1.getTime());
    assertThat(metadata.getScanTriggerType()).isEqualTo(eval1.getScanTriggerType().getDisplayName());
    assertThat(metadata.getStageId()).isEqualTo("build");
    assertThat(metadata.getCommitHash()).isNull();
    assertThat(metadata.getInitiator()).isEqualTo(CurrentUser.SYSTEM);
    assertThat(metadata.isForMonitoring()).isFalse();
    assertThat(metadata.isReevaluation()).isFalse();
    assertThat(metadata.getTotalRisk()).isEqualTo(-1);

    // Verify Response for scan 2
    metadata = reportService.getReportMetadata(app.getPublicId(), scanId2);
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getApplication().getOrganizationId()).isEqualTo(app.getOrganizationId());
    assertThat(metadata.getApplication().getOrganization()).isNotNull();
    assertThat(metadata.getApplication().getOrganization().getId()).isEqualTo(app.getOrganizationId());
    assertThat(metadata.getReportTitle()).isEqualTo("Release Report");
    assertThat(metadata.getReportTime()).isEqualTo(eval2.getTime());
    assertThat(metadata.getScanTriggerType()).isEqualTo(eval2.getScanTriggerType().getDisplayName());
    assertThat(metadata.getStageId()).isEqualTo("release");
    assertThat(metadata.getCommitHash()).isEqualTo(commitHash);
    assertThat(metadata.getInitiator()).isEqualTo(CurrentUser.SYSTEM);
    assertThat(metadata.isForMonitoring()).isFalse();
    assertThat(metadata.isReevaluation()).isFalse();
    assertThat(metadata.getTotalRisk()).isEqualTo(-1);

    // Verify response for monitoring/re-evaluation
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId2,
        true /* isReevaluation */, true/* isForMonitoring */, new Date(System.currentTimeMillis() + 1));
    metadata = reportService.getReportMetadata(app.getPublicId(), scanId2);
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getApplication().getOrganizationId()).isEqualTo(app.getOrganizationId());
    assertThat(metadata.getApplication().getOrganization()).isNotNull();
    assertThat(metadata.getApplication().getOrganization().getId()).isEqualTo(app.getOrganizationId());
    assertThat(metadata.getReportTitle()).isEqualTo("Build Report");
    assertThat(metadata.getReportTime()).isEqualTo(eval3.getTime());
    assertThat(metadata.getScanTriggerType()).isEqualTo(eval3.getScanTriggerType().getDisplayName());
    assertThat(metadata.getStageId()).isEqualTo("build");
    assertThat(metadata.getCommitHash()).isNull();
    assertThat(metadata.getInitiator()).isEqualTo(CurrentUser.SYSTEM);
    assertThat(metadata.isForMonitoring()).isTrue();
    assertThat(metadata.isReevaluation()).isTrue();
    assertThat(metadata.getTotalRisk()).isEqualTo(-1);

    // Unknown scan id
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> reportService.getReportMetadata(app.getPublicId(), "12345678"))
        .withMessage("Could not find a report with ID 12345678");
  }

  @Test
  public void testGetReportMetadataWithDeveloperDashboardFeature() throws Exception {
    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    String commitHash = "0b1bbd94b2edbacd441f170ecd59a178e334868f";

    // ReportResource.getReport requires a report.zip to exist when evaluations exist
    createReportFile(app.getId(), scanId1, zipReportDir("/ReportResourceTest/report-expanded_coverage_false"));
    // use an older data.json to make sure they still work
    createReportFile(app.getId(), scanId2, zipReportDir("/ReportResourceTest/report"));

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId1);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId2, commitHash);

    Policy appPolicy1 = tempEntity.newPolicy(app.getId(), "app owned policy1", 5);
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, appPolicy1, appPolicy1.getThreatLevel() + 1,
            PolicyThreatCategory.SECURITY, "Group1", "Artifact1", "Version1");

    Policy appPolicy2 = tempEntity.newPolicy(app.getId(), "app owned policy2", 8);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, appPolicy2, appPolicy2.getThreatLevel() + 1,
            PolicyThreatCategory.SECURITY, "Group1", "Artifact1", "Version1");

    ReportService reportService = createReportService();

    // Verify Response for scan 1
    ReportMetadataDTO metadata = reportService.getReportMetadata(app.getPublicId(), scanId1);
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getApplication().getOrganizationId()).isEqualTo(app.getOrganizationId());
    assertThat(metadata.getApplication().getOrganization()).isNotNull();
    assertThat(metadata.getApplication().getOrganization().getId()).isEqualTo(app.getOrganizationId());
    assertThat(metadata.getReportTitle()).isEqualTo("Build Report");
    assertThat(metadata.getReportTime()).isEqualTo(eval1.getTime());
    assertThat(metadata.getScanTriggerType()).isEqualTo(eval1.getScanTriggerType().getDisplayName());
    assertThat(metadata.getStageId()).isEqualTo("build");
    assertThat(metadata.getCommitHash()).isNull();
    assertThat(metadata.getInitiator()).isEqualTo(CurrentUser.SYSTEM);
    assertThat(metadata.isForMonitoring()).isFalse();
    assertThat(metadata.isReevaluation()).isFalse();
    assertThat(metadata.getTotalRisk()).isEqualTo(violation1.getThreatLevel());

    // Verify Response for scan 2
    metadata = reportService.getReportMetadata(app.getPublicId(), scanId2);
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getApplication().getOrganizationId()).isEqualTo(app.getOrganizationId());
    assertThat(metadata.getApplication().getOrganization()).isNotNull();
    assertThat(metadata.getApplication().getOrganization().getId()).isEqualTo(app.getOrganizationId());
    assertThat(metadata.getReportTitle()).isEqualTo("Release Report");
    assertThat(metadata.getReportTime()).isEqualTo(eval2.getTime());
    assertThat(metadata.getScanTriggerType()).isEqualTo(eval2.getScanTriggerType().getDisplayName());
    assertThat(metadata.getStageId()).isEqualTo("release");
    assertThat(metadata.getCommitHash()).isEqualTo(commitHash);
    assertThat(metadata.getInitiator()).isEqualTo(CurrentUser.SYSTEM);
    assertThat(metadata.isForMonitoring()).isFalse();
    assertThat(metadata.isReevaluation()).isFalse();
    assertThat(metadata.getTotalRisk()).isEqualTo(violation2.getThreatLevel());

    // Verify response for monitoring/re-evaluation
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId2,
            true /* isReevaluation */, true/* isForMonitoring */, new Date(System.currentTimeMillis() + 1));
    metadata = reportService.getReportMetadata(app.getPublicId(), scanId2);
    assertThat(metadata.getApplication().getId()).isEqualTo(app.getId());
    assertThat(metadata.getApplication().getOrganizationId()).isEqualTo(app.getOrganizationId());
    assertThat(metadata.getApplication().getOrganization()).isNotNull();
    assertThat(metadata.getApplication().getOrganization().getId()).isEqualTo(app.getOrganizationId());
    assertThat(metadata.getReportTitle()).isEqualTo("Build Report");
    assertThat(metadata.getReportTime()).isEqualTo(eval3.getTime());
    assertThat(metadata.getScanTriggerType()).isEqualTo(eval3.getScanTriggerType().getDisplayName());
    assertThat(metadata.getStageId()).isEqualTo("build");
    assertThat(metadata.getCommitHash()).isNull();
    assertThat(metadata.getInitiator()).isEqualTo(CurrentUser.SYSTEM);
    assertThat(metadata.isForMonitoring()).isTrue();
    assertThat(metadata.isReevaluation()).isTrue();
    assertThat(metadata.getTotalRisk()).isEqualTo(violation1.getThreatLevel());

    // Unknown scan id
    assertThatExceptionOfType(NotFoundException.class)
            .isThrownBy(() -> reportService.getReportMetadata(app.getPublicId(), "12345678"))
            .withMessage("Could not find a report with ID 12345678");
  }

  @Test
  public void testGetReportMetadata_expandedCoverage() throws Exception {
    createReportFile(app.getId(), scanId, zipReportDir("/ReportResourceTest/report-expanded_coverage"));
    ReportService reportService = createReportService();

    String applicationPublicId = app.getPublicId();
    assertThatThrownBy(() -> reportService.getReportMetadata(applicationPublicId, scanId))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Expanded Coverage (XC) is no longer supported. " +
            "We have incorporated support for all languages that were maintained in XC in Lifecycle");
  }

  @Test
  public void testGetReportMetadata_ScanLabelForNVS() throws Exception {
    createReportFile(app.getId(), scanId, zipReportDir("/" + getClass().getSimpleName() + "/report-scan_label"));
    ReportService reportService = createReportService();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);

    ReportMetadataDTO metadata = reportService.getReportMetadata(app.getPublicId(), scanId);
    assertThat(metadata).isNotNull();
    assertThat(metadata.getApplication().getName()).isEqualTo("My Awesome Artifact");
    assertThat(metadata.getReportTitle()).isEqualTo("Report");
  }

  @Test
  public void testIncludeThirdPartyData() throws Exception {
    final File reportZip = zipReportDir("/ReportServiceTest/report");

    ThirdPartyApplicationReportDTO dto = new ThirdPartyApplicationReportDTO();
    final ComponentIdentifier coord = ComponentIdentifier.createRpmCoordinates("n1", "v1", "a1");
    dto.billOfMaterials.add(new ThirdPartyBillOfMaterialsRowDTO(coord, "hash1"));
    dto.securityRows.add(new ThirdPartyHealthCheckReportSecurityRowDTO(coord, "hash1"));
    dto.licenseRows.add(new ThirdPartyLicenseRowDTO(coord, "hash1"));

    createReportService().includeThirdPartyData(reportZip, dto);

    assertThatReportZipContains(reportZip, "thirdparty-bom.json");
    assertThatReportZipContains(reportZip, "thirdparty-security.json");
    assertThatReportZipContains(reportZip, "thirdparty-license.json");
  }

  @Test
  public void testProcessThirdPartyData_withInfrastructureAsCodeMergedWithExisting() throws Exception {
    final File reportZip = zipReportDir("/ReportServiceTest/report-with-third-party-iac");
    createReportFile(app.getId(), scanId, reportZip);
    ReportService reportService = createReportService();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);

    ThirdPartyApplicationReportDTO dto = new ThirdPartyApplicationReportDTO();

    ComponentIdentifier coord = new ComponentIdentifier("sbom",
        ImmutableMap.of("group", "group1", "artifactId", "existing1", "version", "1.0"));
    dto.billOfMaterials.add(new ThirdPartyBillOfMaterialsRowDTO(coord, "existing1"));
    dto.securityRows.add(new ThirdPartyHealthCheckReportSecurityRowDTO(coord, "existing1"));

    when(thirdPartyDataServiceSpy.getScanData(scanId)).thenReturn(dto);
    reportService.processThirdPartyData(scanId, reportZip, "app-id");

    assertThat(dto.billOfMaterials).hasSize(3);
    assertThat(dto.billOfMaterials.get(0).componentIdentifier.getFormat()).isEqualTo("sbom");
    assertThat(dto.billOfMaterials.get(1).componentIdentifier.getFormat()).isEqualTo("terraform");

    assertThat(dto.securityRows).hasSize(13);
    assertThat(dto.securityRows.get(0).componentIdentifier.getFormat()).isEqualTo("sbom");
    assertThat(dto.securityRows.get(1).componentIdentifier.getFormat()).isEqualTo("terraform");
  }

  @Test
  public void testProcessThirdPartyData_withContainerContent() throws Exception {
    MockReportDownloader mockReportDownloader = new MockReportDownloader();
    mockReportDownloader.mockDownloadReport(scanId, "/ReportServiceTest/report-with-container-content");
    reportDownloader = mockReportDownloader.getMock();
    ReportService reportService = createReportService();

    File reportFile = reportService.fetchReport(app, scanId);

    ComponentLoader componentLoader = componentLoaderFactory.createComponentLoader(app);
    ReportEntry licenseReportEntry = Report.getEntry(reportFile, Report.LICENSES_JSON_FILENAME);
    ReportEntry securityReportEntry = Report.getEntry(reportFile, Report.SECURITY_JSON_FILENAME);
    ReportEntry bomReportEntry = Report.getEntry(reportFile, Report.BOM_JSON_FILENAME);
    ReportEntry dependenciesReportEntry = Report.getEntry(reportFile, Report.DEPENDENCIES_JSON_FILENAME);
    List<Component> components = componentLoader
        .getAll(licenseReportEntry.buf, securityReportEntry.buf, bomReportEntry.buf, dependenciesReportEntry.buf);
    assertThat(components).hasSize(9);

    // get a component with a vulnerability and verify it
    Component component = components.get(0);
    assertComponent(component, "9cd309492780e10b8349",
        ComponentIdentifier.createContainerCoordinates("alpine:3.6.5", "apk-tools", "2.7.6-r0"),
        "dependency:/pkg:generic\\alpine%3A3.6.5\\apk-tools@2.7.6-r0?qualifier=container",
        IdentificationSource.SONATYPE_CONTAINER);

    List<SecurityVulnerability> securityVulnerabilities = component.getSecurityVulnerabilities();
    assertSecurityVulnerability(securityVulnerabilities.get(0), "Sonatype", "CVE-2021-30139", 7.5F,
        "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-30139");
  }

  @Test
  public void testProcessThirdPartyData_SBOMManagerEnabled_reportNotDeleted() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final File reportZip = zipReportDir("/ReportServiceTest/report-with-third-party-iac");
    createReportFile(app.getId(), scanId, reportZip);
    ReportService reportService = createReportService();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);

    ThirdPartyApplicationReportDTO dto = new ThirdPartyApplicationReportDTO();

    ComponentIdentifier coord = new ComponentIdentifier("sbom",
        ImmutableMap.of("group", "group1", "artifactId", "existing1", "version", "1.0"));
    dto.billOfMaterials.add(new ThirdPartyBillOfMaterialsRowDTO(coord, "existing1"));
    dto.securityRows.add(new ThirdPartyHealthCheckReportSecurityRowDTO(coord, "existing1"));

    when(thirdPartyDataServiceSpy.getScanData(scanId)).thenReturn(dto);
    reportService.processThirdPartyData(scanId, reportZip, "app-id");

    assertThat(dto.billOfMaterials).hasSize(3);
    assertThat(dto.billOfMaterials.get(0).componentIdentifier.getFormat()).isEqualTo("sbom");
    assertThat(dto.billOfMaterials.get(1).componentIdentifier.getFormat()).isEqualTo("terraform");

    assertThat(dto.securityRows).hasSize(13);
    assertThat(dto.securityRows.get(0).componentIdentifier.getFormat()).isEqualTo("sbom");
    assertThat(dto.securityRows.get(1).componentIdentifier.getFormat()).isEqualTo("terraform");

    verify(thirdPartyDataServiceSpy, never()).deleteByScanId(eq(scanId));
  }

  @Test
  public void testProcessThirdPartyData_MaxSbomLimitNotReached_reportNotDeleted() throws Exception {
    final File reportZip = zipReportDir("/ReportServiceTest/report");
    createReportFile(app.getId(), scanId, reportZip);
    ReportService reportService = createReportService();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan("scanRequestId", scanId, thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata(app.getId(), "1", thirdPartyFile);
    String sbomApplicationPath = tempDir.getRoot().toPath()
        .relativize(insightWork.getSbomDir(sbomMetadata.getApplicationId()).toPath()).normalize().toString();
    File sbomFile = tempDir.newFile(sbomApplicationPath + File.separator + sbomMetadata.getFilename());
    sbomFile.deleteOnExit();
    assertThat(sbomFile).exists();

    ThirdPartyApplicationReportDTO dto = new ThirdPartyApplicationReportDTO();
    final ComponentIdentifier coord = ComponentIdentifier.createRpmCoordinates("n1", "v1", "a1");
    dto.billOfMaterials.add(new ThirdPartyBillOfMaterialsRowDTO(coord, "hash1"));
    dto.securityRows.add(new ThirdPartyHealthCheckReportSecurityRowDTO(coord, "hash1"));
    dto.licenseRows.add(new ThirdPartyLicenseRowDTO(coord, "hash1"));
    when(thirdPartyDataServiceSpy.getScanData(scanId)).thenReturn(dto);

    reportService.processThirdPartyData(scanId, reportZip, app.getId());

    assertThat(sbomMetadataUtils.hasMaxSbomLimitBeenReached()).isFalse();
    verify(thirdPartyDataServiceSpy, never()).deleteByScanId(scanId);
    assertThat(sbomFile).exists();
    assertThat(sbomFile.delete()).isTrue();
  }

  @Test
  public void testProcessThirdPartyData_MaxSbomLimitReached_reportDeleted() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final File reportZip = zipReportDir("/ReportServiceTest/report-with-third-party-iac");
    createReportFile(app.getId(), scanId, reportZip);
    ReportService reportService = createReportService();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan("scanRequestId", scanId, thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata(app.getId(), "1", thirdPartyFile);
    String sbomApplicationPath = tempDir.getRoot().toPath()
        .relativize(insightWork.getSbomDir(sbomMetadata.getApplicationId()).toPath()).normalize().toString();
    File sbomFile = tempDir.newFile(sbomApplicationPath + File.separator + sbomMetadata.getFilename());
    sbomFile.deleteOnExit();
    assertThat(sbomFile).exists();

    ThirdPartyApplicationReportDTO dto = new ThirdPartyApplicationReportDTO();

    ComponentIdentifier coord = new ComponentIdentifier("sbom",
        ImmutableMap.of("group", "group1", "artifactId", "existing1", "version", "1.0"));
    dto.billOfMaterials.add(new ThirdPartyBillOfMaterialsRowDTO(coord, "existing1"));
    dto.securityRows.add(new ThirdPartyHealthCheckReportSecurityRowDTO(coord, "existing1"));

    when(thirdPartyDataServiceSpy.getScanData(scanId)).thenReturn(dto);
    when(sbomMetadataUtils.hasMaxSbomLimitBeenReached()).thenReturn(true);

    reportService.processThirdPartyData(scanId, reportZip, app.getId());

    verify(thirdPartyDataServiceSpy, times(1)).deleteByScanId(scanId);
    assertThat(sbomFile).doesNotExist();
  }

  @Test
  public void testGetBomForPolicyEvaluation() throws URISyntaxException, IOException {
    createReportFile(app.getId(), "SCAN_ID", zipReportDir("/ReportServiceTest/report"));
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "SCAN_ID");

    ReportEntry reportEntry = createReportService().getBomForPolicyEvaluation(policyEvaluation);

    assertThat(reportEntry).isNotNull();
    assertThat(reportEntry.buf).isNotNull();
    assertThat(reportEntry.buf).isNotEmpty();
  }

  @Test
  public void testGetBomForPolicyEvaluation_NoBomFile() throws URISyntaxException, IOException {
    createReportFile(app.getId(), "SCAN_ID", zipReportDir("/ReportServiceTest/report-missing-bom-json"));
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "SCAN_ID");

    ReportEntry reportEntry = createReportService().getBomForPolicyEvaluation(policyEvaluation);

    assertThat(reportEntry).isNull();
  }

  @Test
  public void testGetBomForPolicyEvaluation_NullPolicyEvaluation() throws IOException {
    ReportEntry reportEntry = createReportService().getBomForPolicyEvaluation(null);

    assertThat(reportEntry).isNull();
  }

  @Test
  public void testGetBomForPolicyEvaluation_NoPolicyEvaluation() {
    PolicyEvaluation policyEvaluation =
        new PolicyEvaluation(app.getId(), BuildStageType.ID, "SCAN_ID", CurrentUser.SYSTEM, ScanTriggerType.CLI);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> createReportService().getBomForPolicyEvaluation(policyEvaluation));
  }

  private void assertThatReportZipContains(File zipFile, final String thirdPartyFile) {
    assertThat(Stream.of(new TFile(zipFile).listFiles()).anyMatch(f -> f.getName().endsWith(thirdPartyFile)))
        .isTrue();
  }

  private void createReportFile() throws IOException, URISyntaxException {
    createReportFile(app.getId(), scanId, zipReportDir("/ReportServiceTest/report"));
  }

  private void createReportFile(String appId, String scanId, File reportFile) throws IOException {
    FileUtils.copyFile(reportFile, insightWork.getReportFile(appId, scanId));
  }

  private File zipReportDir(String reportResourceName) throws URISyntaxException {
    return Paths.get(ReportHelper.zipReport(reportResourceName, tempDir).toURI()).toFile();
  }
}
