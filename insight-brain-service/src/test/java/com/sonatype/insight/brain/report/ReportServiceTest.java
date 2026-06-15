/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.cpematching.CpeMatchingConfigurationService;
import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceVersionDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.innersource.InnerSourceCleanupPendingService;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.pullrequestcreationservice.AutomatedPullRequestCreationService;
import com.sonatype.insight.brain.hds.ScanUploadService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.CpeResultsTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyBillOfMaterialsRowDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyDataService;
import com.sonatype.insight.brain.thirdparty.ThirdPartyLicenseRowDTO;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.utils.ScanHelper;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.model.license.LicenseOverrideStatus.OVERRIDDEN;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DATA_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DEPENDENCIES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.INDEX_HTML;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.LICENSES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.POLICY_THREATS;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.SECURITY_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.SUMMARY_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.THIRD_PARTY_BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.THIRD_PARTY_LICENSE_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.THIRD_PARTY_SECURITY_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.cpematching.CpeMatchingConfigurationService;
import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceVersionDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.pullrequestcreationservice.AutomatedPullRequestCreationService;
import com.sonatype.insight.brain.hds.ScanUploadService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.sbom.SbomResultsMerger;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceService;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.CpeResultsTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyBillOfMaterialsRowDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyDataService;
import com.sonatype.insight.brain.thirdparty.ThirdPartyLicenseRowDTO;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.utils.ScanHelper;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;

public class ReportServiceTest
    extends AbstractComponentTest
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Inject
  private InsightWork insightWork;

  private Application app;

  private final String scanId = "ReportServiceTestScanId";

  private Set<Integer> depths(Integer... depths) {
    return Sets.newHashSet(depths);
  }

  @Inject
  private Configuration configuration;

  @Inject
  private ThirdPartyDataService thirdPartyDataService;

  @Inject
  private H2ApplicationRiskService applicationRiskService;

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

  @Inject
  private ReportDataStore reportDataStore;

  private ReportDataStore reportDataStoreSpy;

  @Mock
  private SbomMetadataUtils sbomMetadataUtils;

  private ReportDownloader reportDownloader;

  private MockReportDownloader mockReportDownloader;

  @Inject
  private MultiLicenseDAO multiLicenseDAO;

  @Inject
  private LicenseDAO licenseDao;

  @Inject
  private ThirdPartyComponentDAO thirdPartyComponentDAO;

  @Inject
  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  @Inject
  private HashComponentIdentifierDAO hashComponentIdentifierDAO;

  @Inject
  private LicenseOverrideDAO licenseOverrideDAO;

  @Inject
  private SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  @Inject
  private InnerSourceApplicationDAO innerSourceApplicationDAO;

  @Inject
  private InnerSourceVersionDAO innerSourceVersionDAO;

  @Inject
  private ProprietaryConfigService proprietaryConfigService;

  @Inject
  private FileApplicationReportPersistenceService applicationReportPersistenceService;

  @Inject
  private AutomatedPullRequestCreationService automatedPullRequestCreationService;

  @Inject
  private ScanPersistenceService scanPersistenceService;

  private AutomatedPullRequestCreationService automatedPullRequestCreationServiceSpy;

  @Mock
  private ScanUploadService mockScanUploadService;

  @Mock
  private CpeMatchingConfigurationService cpeMatchingConfigurationService;

  @Mock
  private RepositoryComponentDAO repositoryComponentDAO;

  @Mock
  private InnerSourceCleanupPendingService innerSourceCleanupPendingService;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Before
  public void before() {
    thirdPartyDataServiceSpy = createThirdPartyDataServiceSpy();
    automatedPullRequestCreationServiceSpy = createAutomatedPullRequestCreationServiceSpy();
    app = tempEntity.newApplicationWithParent();
    mockReportDownloader = new MockReportDownloader(tempDir);
    mockReportDownloader.setInsightWork(insightWork);
    reportDownloader = mockReportDownloader.getMock();
    reportDataStoreSpy = spy(new ReportDataStore(reportDownloader, configuration, applicationReportPersistenceService));
  }

  private ThirdPartyDataService createThirdPartyDataServiceSpy() {
    ThirdPartyDataService target = thirdPartyDataService;
    if (Mockito.mockingDetails(target).isMock()) {
      target = new ThirdPartyDataService(
          lookup(ThirdPartyFileCoordinateDAO.class),
          lookup(ThirdPartyFileDAO.class),
          lookup(ThirdPartyCoordinateSecurityDAO.class),
          lookup(ThirdPartyVulnerabilityExploitabilityExchangeDAO.class),
          lookup(ThirdPartyScanDAO.class),
          lookup(ThirdPartyCoordinateLicenseDAO.class),
          multiLicenseDAO,
          lookup(ThirdPartyVulnerabilityDAO.class),
          thirdPartyComponentDAO,
          lookup(ThirdPartySbomMetadataDAO.class),
          telemetrySender,
          telemetryUtils,
          lookup(SearchIndexManager.class),
          productLicense,
          () -> lookup(SbomResultsMerger.class),
          lookup(SbomPersistenceService.class));
    }
    return spy(target);
  }

  private AutomatedPullRequestCreationService createAutomatedPullRequestCreationServiceSpy() {
    if (Mockito.mockingDetails(automatedPullRequestCreationService).isMock()) {
      return automatedPullRequestCreationService;
    }
    return spy(automatedPullRequestCreationService);
  }

  @After
  public void after() {
    Mockito.reset(reportDownloader);
  }

  private ReportService createReportService() {
    return new ReportService(policyEvaluationDAO, configuration, applicationDAO, organizationDAO,
        thirdPartyDataServiceSpy, telemetrySender, telemetryUtils, repositoryMatcher, applicationRiskService,
        productLicense, sbomMetadataUtils, licenseDao, componentLoaderFactory, thirdPartyComponentDAO,
        licenseThreatGroupDAO, hashComponentIdentifierDAO, licenseOverrideDAO, securityVulnerabilityOverrideDAO,
        multiLicenseDAO, innerSourceApplicationDAO, innerSourceVersionDAO, proprietaryConfigService, reportDataStoreSpy,
        mockScanUploadService, automatedPullRequestCreationServiceSpy, cpeMatchingConfigurationService,
        scanPersistenceService, repositoryComponentDAO, null, null, null, null,
        innerSourceCleanupPendingService, thirdPartySbomMetadataDAO);
  }

  @Test
  public void testFetchReport_Exists() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);

    ReportService reportService = createReportService();
    ApplicationReport report = reportService.fetchReport(app, scanId, StageTypes.RELEASE.getId());
    assertThat(report).isNotNull();
    assertThat(report.exists()).isTrue();
    verify(thirdPartyDataServiceSpy, never()).deleteByScanId(eq(scanId));
  }

  @Test
  public void testFetchReport_DoesNotExist() throws Exception {
    mockReportDownloader.mockDownloadReport(scanId, "/ReportServiceTest/report");
    reportDownloader = mockReportDownloader.getMock();

    ReportService reportService = createReportService();
    when(thirdPartyDataServiceSpy.getScanData(scanId))
        .thenReturn(new ThirdPartyApplicationReportDTO());

    ApplicationReport report = reportService.fetchReport(app, scanId, StageTypes.RELEASE.getId());
    assertThat(report).isNotNull();
    assertThat(report.exists()).isTrue();
    verify(reportDownloader).downloadReport(any(ApplicationReport.class), eq(2100), eq(5));
  }

  @Test
  public void testFetchReport_CpeResultsMetrics() throws Exception {
    mockReportDownloader.mockDownloadReport(scanId, "/ReportServiceTest/report-with-cpe-results");
    when(cpeMatchingConfigurationService.isCpeDataMatchingEnabled(eq(app.getId()))).thenReturn(true);
    when(sbomMetadataUtils.hasSbomMetadata(scanId)).thenReturn(true);
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(RandomStringUtils.insecure().nextAlphanumeric(10), scanId, tpFile);
    tempEntity.newThirdPartySbomMetadata(tpFile.getId(), app.getId(), PENDING, tpFile.getFilename());
    ThirdPartyFileCoordinate tpFileCoord =
        tempEntity.newThirdPartyFileCoordinate(tpFile, "SBOM", "generic", "lintian", "1.23.14", "19442645c98283636b4a",
            "pkg:generic/debian/lintian@1.23.14?sbom_type=library", "c68bdc4f6f12b754bf3e6ccdb8ab284c6a13c021");
    tempEntity.newThirdPartyCoordinateSecurity(tpFileCoord, "CVE-22024-123456", "some description",
        "https://example.com", 5.5d, "high", "2.0");

    // when
    ReportService reportService = createReportService();
    ApplicationReport report = reportService.fetchReport(app, scanId, StageTypes.COMPLIANCE.getId());

    // Then
    assertThat(report).isNotNull();
    assertThat(report.exists()).isTrue();

    ArgumentCaptor<TelemetryData> telemetryDataCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySender).send(telemetryDataCaptor.capture());
    TelemetryData capturedTelemetryData = telemetryDataCaptor.getValue();
    // Verify TelemetryData purpose
    assertThat(capturedTelemetryData.getPurpose()).isEqualTo(TelemetryPurpose.CPE_RESULTS_METRICS);

    // Verify application_id is present
    Map<String, Object> attributes = capturedTelemetryData.getAttributes();
    assertThat(attributes.containsKey("application_id")).isTrue();

    // Verify CpeResultsTelemetry is present and has expected values
    CpeResultsTelemetry cpeResultsTelemetry = (CpeResultsTelemetry) attributes.get(CpeResultsTelemetry.ATTRIBUTE_NAME);
    assertThat(cpeResultsTelemetry).isNotNull();
    assertThat(cpeResultsTelemetry.getReportComponentTotal()).isEqualTo(3);
    assertThat(cpeResultsTelemetry.getCandidateFormatsCount()).isEqualTo(2);
    assertThat(cpeResultsTelemetry.getCpeMatchedComponentCount()).isEqualTo(2);
    assertThat(cpeResultsTelemetry.getCpeMatchedVulnerabilityCount()).isEqualTo(3);
    assertThat(cpeResultsTelemetry.getCpeUnMatchedVulnerabilityCount()).isEqualTo(1);
  }

  @Test
  public void testFetchReport_ThirdPartyDataMatchesUnknownComponents() throws Exception {
    mockReportDownloader.mockDownloadReport(scanId, "/ReportServiceTest/report-with-third-party-data");
    ReportService reportService = createReportService();

    ApplicationReport reportZip = reportService.fetchReport(app, scanId, StageTypes.RELEASE.getId());

    // Verify bom.json
    ComponentLoader componentLoader = componentLoaderFactory.createComponentLoader(app);
    ReportEntry licenseReportEntry = reportZip.getEntry(LICENSES_JSON.getName());
    ReportEntry securityReportEntry = reportZip.getEntry(SECURITY_JSON.getName());
    ReportEntry bomReportEntry = reportZip.getEntry(BOM_JSON.getName());
    ReportEntry dependenciesReportEntry = reportZip.getEntry(DEPENDENCIES_JSON.getName());
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
    assertThat(components.get(1).getAnalyzerFeatures()).usingRecursiveComparison()
        .isEqualTo(
            new AnalyzerFeatures(AnalysisSource.THIRD_PARTY, AnalysisType.COORDINATE, "cli", false, false, false));
    assertComponent(components.get(2), "cf085cd08ee27334c573",
        ComponentIdentifier.createPypiCoordinates("altgraph", "0.10.2", null, null),
        "dependency:/pkg:pypi\\altgraph@0.10.2", IdentificationSource.getOrMake("cyclonedx"));
    assertThat(components.get(2).getAnalyzerFeatures()).usingRecursiveComparison()
        .isEqualTo(
            new AnalyzerFeatures(AnalysisSource.THIRD_PARTY, AnalysisType.COORDINATE, "cli", false, false, false,
                ItemContentType.SBOM.name()));

    // Verify security.json
    assertSecurityVulnerability(components.get(0), "cve", "CVE-2012-5783", 5.8F,
        "http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2012-5783",
        SecurityVulnerabilityResearchType.DEEP_DIVE.getId(), SecurityVulnerabilityDetectionType.PRIMARY.getId(),
        IdentificationSource.SONATYPE.getId());
    assertSecurityVulnerability(components.get(1), "CVE", "CVE-2017-16997", 10.0F,
        "https://security-tracker.debian.org/tracker/CVE-2017-16997",
        SecurityVulnerabilityResearchType.DEEP_DIVE.getId(), SecurityVulnerabilityDetectionType.OTHER.getId(),
        IdentificationSource.SBOM.getId());
    assertSecurityVulnerability(components.get(2), "NVD", "CVE-2018-7489", 9.8F,
        "https://nvd.nist.gov/vuln/detail/CVE-2018-7489", SecurityVulnerabilityResearchType.DEEP_DIVE.getId(),
        SecurityVulnerabilityDetectionType.OTHER.getId(), IdentificationSource.SBOM.getId());

    // Verify licenses.json
    assertLicenses(components.get(0), Collections.singleton("Apache-2.0"), Collections.singleton("No-Sources"));
    assertLicenses(components.get(1), Collections.singleton("UNSPECIFIED"), Collections.emptySet());
    assertLicenses(components.get(2), Collections.singleton("GPL-2.0"), Collections.emptySet());

    // Verify summary.json
    ReportEntry summaryReportEntry = reportZip.getEntry(SUMMARY_JSON.getName());
    JsonNode summaryJsonNode = JsonUtils.parse(summaryReportEntry.buf);
    assertThat(summaryJsonNode.path("knownArtifactCount").asInt()).isEqualTo(3);

    // Verify data.json
    ReportEntry dataReportEntry = reportZip.getEntry(DATA_JSON.getName());
    JsonNode dataJsonNode = JsonUtils.parse(dataReportEntry.buf);
    assertThat(dataJsonNode.path("exactlyMatchedComponentCount").asInt()).isEqualTo(3);
    assertThat(dataJsonNode.path("knownArtifactCount").asInt()).isEqualTo(3);
    assertThat(dataJsonNode.path("securityCounts"))
        .isEqualTo(JsonUtils.asTree(new int[]{1, 1, 0, 0, 0, 1, 0, 0, 0, 0}));
  }

  @Test
  public void testFetchReport_EmbeddedComponentCountedAsPartial() throws Exception {
    mockReportDownloader.mockDownloadReport(scanId, "/ReportServiceTest/report-with-embedded-component");
    ReportService reportService = createReportService();

    ApplicationReport reportZip = reportService.fetchReport(app, scanId, StageTypes.RELEASE.getId());

    ReportEntry dataReportEntry = reportZip.getEntry(DATA_JSON.getName());
    JsonNode dataJsonNode = JsonUtils.parse(dataReportEntry.buf);
    assertThat(dataJsonNode.path("exactlyMatchedComponentCount").asInt()).isEqualTo(1);
    assertThat(dataJsonNode.path("partiallyMatchedComponentCount").asInt()).isEqualTo(2); // EMBEDDED + SIMILAR both
                                                                                          // partial
    assertThat(dataJsonNode.path("knownArtifactCount").asInt()).isEqualTo(3);

    ReportEntry summaryReportEntry = reportZip.getEntry(SUMMARY_JSON.getName());
    JsonNode summaryJsonNode = JsonUtils.parse(summaryReportEntry.buf);
    assertThat(summaryJsonNode.path("knownArtifactCount").asInt()).isEqualTo(3);
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
      String url,
      String researchType,
      String detectionType,
      String identificationSource)
  {
    assertThat(component.getSecurityVulnerabilities()).hasSize(1);
    SecurityVulnerability securityVulnerability = component.getSecurityVulnerabilities().get(0);
    assertSecurityVulnerability(securityVulnerability, source, refId, severity, url, researchType, detectionType,
        identificationSource);
  }

  private void assertSecurityVulnerability(
      final SecurityVulnerability securityVulnerability,
      final String source,
      final String refId,
      final float severity,
      final String url,
      final String researchType,
      final String detectionType,
      final String identificationSource)
  {
    assertThat(securityVulnerability.getSource()).isEqualTo(source);
    assertThat(securityVulnerability.getRefId()).isEqualTo(refId);
    assertThat(securityVulnerability.getSeverity()).isEqualTo(severity);
    assertThat(securityVulnerability.getUrl()).isEqualTo(url);
    assertThat(securityVulnerability.getResearchType().getId()).isEqualTo(researchType);
    assertThat(securityVulnerability.getDetectionType().getId()).isEqualTo(detectionType);
    assertThat(securityVulnerability.getIdentificationSource().getId()).isEqualTo(identificationSource);
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
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);
    ReportService reportService = createReportService();
    ApplicationReport appReport = reportService.getReport(app.getId(), scanId);
    assertThat(appReport).isNotNull();
    assertThat(appReport.exists()).isTrue();
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
  @Category(SlowTest.class)
  public void testGetReportMetadataWithoutDeveloperDashboardFeature() throws Exception {
    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    String commitHash = "0b1bbd94b2edbacd441f170ecd59a178e334868f";
    String branchName = "test-branch";

    productLicense.setProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);

    // ReportResource.getReport requires a report.zip to exist when evaluations exist
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportResourceTest/report-expanded_coverage_false",
        app.getId(), scanId1);
    // use an older data.json to make sure they still work
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId2);

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId1);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId2, commitHash,
        branchName);

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
    assertThat(metadata.getBranchName()).isNull();

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
    assertThat(metadata.getBranchName()).isEqualTo(branchName);

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
    assertThat(metadata.getBranchName()).isNull();

    // Unknown scan id
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> reportService.getReportMetadata(app.getPublicId(), "12345678"))
        .withMessage("Could not find a report with ID 12345678");
  }

  @Test
  @Category(SlowTest.class)
  public void testGetReportMetadataWithDeveloperDashboardFeature() throws Exception {
    final String scanId1 = "ScanId1";
    final String scanId2 = "ScanId2";
    String commitHash = "0b1bbd94b2edbacd441f170ecd59a178e334868f";
    String branchName = "test-branch";

    // ReportResource.getReport requires a report.zip to exist when evaluations exist
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportResourceTest/report-expanded_coverage_false",
        app.getId(), scanId1);
    // use an older data.json to make sure they still work
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId2);

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId1);
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId2, commitHash,
        branchName);

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
    assertThat(metadata.getBranchName()).isNull();

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
    assertThat(metadata.getBranchName()).isEqualTo(branchName);

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
    assertThat(metadata.getBranchName()).isNull();

    // Unknown scan id
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> reportService.getReportMetadata(app.getPublicId(), "12345678"))
        .withMessage("Could not find a report with ID 12345678");
  }

  @Test
  public void testGetReportMetadata_expandedCoverage() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportResourceTest/report-expanded_coverage",
        app.getId(), scanId);
    ReportService reportService = createReportService();

    String applicationPublicId = app.getPublicId();
    assertThatThrownBy(() -> reportService.getReportMetadata(applicationPublicId, scanId))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Expanded Coverage (XC) is no longer supported. " +
            "We have incorporated support for all languages that were maintained in XC in Lifecycle");
  }

  @Test
  public void testGetReportMetadata_ScanLabelForNVS() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report-scan_label", app.getId(), scanId);
    ReportService reportService = createReportService();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);

    ReportMetadataDTO metadata = reportService.getReportMetadata(app.getPublicId(), scanId);
    assertThat(metadata).isNotNull();
    assertThat(metadata.getApplication().getName()).isEqualTo("My Awesome Artifact");
    assertThat(metadata.getReportTitle()).isEqualTo("Report");
  }

  @Test
  public void testIncludeThirdPartyData() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);
    final ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, app, scanId);

    ThirdPartyApplicationReportDTO dto = new ThirdPartyApplicationReportDTO();
    final ComponentIdentifier coord = ComponentIdentifier.createRpmCoordinates("n1", "v1", "a1");
    dto.billOfMaterials.add(new ThirdPartyBillOfMaterialsRowDTO(coord, "hash1"));
    dto.securityRows.add(new ThirdPartyHealthCheckReportSecurityRowDTO(coord, "hash1"));
    dto.licenseRows.add(new ThirdPartyLicenseRowDTO(coord, "hash1"));

    createReportService().includeThirdPartyData(appReport, dto);

    assertThatReportFilesContains(appReport, "thirdparty-bom.json");
    assertThatReportFilesContains(appReport, "thirdparty-security.json");
    assertThatReportFilesContains(appReport, "thirdparty-license.json");
  }

  @Test
  public void testProcessThirdPartyData_withInfrastructureAsCodeMergedWithExisting() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report-with-third-party-iac",
        app.getId(), scanId);
    final ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, app, scanId);
    ReportService reportService = createReportService();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);

    ThirdPartyApplicationReportDTO dto = new ThirdPartyApplicationReportDTO();

    ComponentIdentifier coord = new ComponentIdentifier("sbom",
        ImmutableMap.of("group", "group1", "artifactId", "existing1", "version", "1.0"));
    dto.billOfMaterials.add(new ThirdPartyBillOfMaterialsRowDTO(coord, "existing1"));
    dto.securityRows.add(new ThirdPartyHealthCheckReportSecurityRowDTO(coord, "existing1"));

    when(thirdPartyDataServiceSpy.getScanData(scanId)).thenReturn(dto);
    reportService.processThirdPartyData(scanId, appReport, "app-id");

    assertThat(dto.billOfMaterials).hasSize(3);
    assertThat(dto.billOfMaterials.get(0).componentIdentifier.getFormat()).isEqualTo("sbom");
    assertThat(dto.billOfMaterials.get(1).componentIdentifier.getFormat()).isEqualTo("terraform");

    assertThat(dto.securityRows).hasSize(13);
    assertThat(dto.securityRows.get(0).componentIdentifier.getFormat()).isEqualTo("sbom");
    assertThat(dto.securityRows.get(1).componentIdentifier.getFormat()).isEqualTo("terraform");
  }

  @Test
  public void testProcessThirdPartyData_withContainerContent() throws Exception {
    mockReportDownloader.mockDownloadReport(scanId, "/ReportServiceTest/report-with-container-content");

    ReportService reportService = createReportService();

    ApplicationReport reportZip = reportService.fetchReport(app, scanId, StageTypes.RELEASE.getId());

    ComponentLoader componentLoader = componentLoaderFactory.createComponentLoader(app);
    ReportEntry licenseReportEntry = reportZip.getEntry(LICENSES_JSON.getName());
    ReportEntry securityReportEntry = reportZip.getEntry(SECURITY_JSON.getName());
    ReportEntry bomReportEntry = reportZip.getEntry(BOM_JSON.getName());
    ReportEntry dependenciesReportEntry = reportZip.getEntry(DEPENDENCIES_JSON.getName());
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
        "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-30139",
        SecurityVulnerabilityResearchType.DEEP_DIVE.getId(),
        SecurityVulnerabilityDetectionType.PRIMARY.getId(), IdentificationSource.SONATYPE.getId());
  }

  @Test
  public void testProcessThirdPartyData_Lifecycle_thirdPartyScanDataDeleted() throws Exception {
    productLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);
    final ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, app, scanId);
    ReportService reportService = createReportService();
    tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan("scanRequestId", scanId, thirdPartyFile);

    ThirdPartyApplicationReportDTO dto = new ThirdPartyApplicationReportDTO();
    final ComponentIdentifier coord = ComponentIdentifier.createRpmCoordinates("n1", "v1", "a1");
    dto.billOfMaterials.add(new ThirdPartyBillOfMaterialsRowDTO(coord, "hash1"));
    dto.securityRows.add(new ThirdPartyHealthCheckReportSecurityRowDTO(coord, "hash1"));
    dto.licenseRows.add(new ThirdPartyLicenseRowDTO(coord, "hash1"));
    when(thirdPartyDataServiceSpy.getScanData(scanId)).thenReturn(dto);

    reportService.processThirdPartyData(scanId, appReport, app.getId());

    assertThat(productLicense.hasFeature(LicensedFeature.SBOM_MANAGER)).isFalse();
    assertThat(sbomMetadataUtils.hasMaxActiveSbomLimitBeenReached()).isFalse();
    assertThat(policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId).getStageTypeId()).isNotEqualTo(
        ComplianceStageType.ID);
    verify(thirdPartyDataServiceSpy, times(1)).deleteByScanId(scanId);
  }

  @Test
  public void testProcessThirdPartyData_SBOMManagerEnabled_reportNotDeleted() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report-with-third-party-iac",
        app.getId(), scanId);
    final ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, app, scanId);
    ReportService reportService = createReportService();
    tempEntity.newPolicyEvaluation(app.getId(), ComplianceStageType.ID, scanId);

    ThirdPartyApplicationReportDTO dto = new ThirdPartyApplicationReportDTO();

    ComponentIdentifier coord = new ComponentIdentifier("sbom",
        ImmutableMap.of("group", "group1", "artifactId", "existing1", "version", "1.0"));
    dto.billOfMaterials.add(new ThirdPartyBillOfMaterialsRowDTO(coord, "existing1"));
    dto.securityRows.add(new ThirdPartyHealthCheckReportSecurityRowDTO(coord, "existing1"));

    when(thirdPartyDataServiceSpy.getScanData(scanId)).thenReturn(dto);
    when(sbomMetadataUtils.hasSbomMetadata(scanId)).thenReturn(true);
    reportService.processThirdPartyData(scanId, appReport, "app-id");

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
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);
    final ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, app, scanId);
    ReportService reportService = createReportService();
    tempEntity.newPolicyEvaluation(app.getId(), ComplianceStageType.ID, scanId);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan("scanRequestId", scanId, thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.createSbomMetadata(app.getId(), "1", thirdPartyFile, PENDING);
    File sbomFile = createSbomFile(sbomMetadata);
    assertThat(sbomFile).exists();

    ThirdPartyApplicationReportDTO dto = new ThirdPartyApplicationReportDTO();
    final ComponentIdentifier coord = ComponentIdentifier.createRpmCoordinates("n1", "v1", "a1");
    dto.billOfMaterials.add(new ThirdPartyBillOfMaterialsRowDTO(coord, "hash1"));
    dto.securityRows.add(new ThirdPartyHealthCheckReportSecurityRowDTO(coord, "hash1"));
    dto.licenseRows.add(new ThirdPartyLicenseRowDTO(coord, "hash1"));
    when(thirdPartyDataServiceSpy.getScanData(scanId)).thenReturn(dto);
    when(sbomMetadataUtils.hasSbomMetadata(scanId)).thenReturn(true);

    reportService.processThirdPartyData(scanId, appReport, app.getId());

    assertThat(sbomMetadataUtils.hasMaxActiveSbomLimitBeenReached()).isFalse();
    verify(thirdPartyDataServiceSpy, never()).deleteByScanId(scanId);
    assertThat(sbomFile).exists();
    assertThat(sbomFile.delete()).isTrue();
  }

  @Test
  public void testProcessThirdPartyData_MaxSbomLimitReached_reportDeleted() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report-with-third-party-iac",
        app.getId(), scanId);
    final ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, app, scanId);
    ReportService reportService = createReportService();
    tempEntity.newPolicyEvaluation(app.getId(), ComplianceStageType.ID, scanId);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan("scanRequestId", scanId, thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata(app.getId(), "1", thirdPartyFile, PENDING);
    File sbomFile = createSbomFile(sbomMetadata);
    assertThat(sbomFile).exists();

    ThirdPartyApplicationReportDTO dto = new ThirdPartyApplicationReportDTO();

    ComponentIdentifier coord = new ComponentIdentifier("sbom",
        ImmutableMap.of("group", "group1", "artifactId", "existing1", "version", "1.0"));
    dto.billOfMaterials.add(new ThirdPartyBillOfMaterialsRowDTO(coord, "existing1"));
    dto.securityRows.add(new ThirdPartyHealthCheckReportSecurityRowDTO(coord, "existing1"));

    when(thirdPartyDataServiceSpy.getScanData(scanId)).thenReturn(dto);
    when(sbomMetadataUtils.hasSbomMetadata(scanId)).thenReturn(true);
    when(sbomMetadataUtils.hasMaxActiveSbomLimitBeenReached()).thenReturn(true);

    reportService.processThirdPartyData(scanId, appReport, app.getId());

    verify(thirdPartyDataServiceSpy, times(1)).deleteByScanId(scanId);
    assertThat(sbomFile).doesNotExist();
  }

  @Test
  public void testGetBomForPolicyEvaluation() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), "SCAN_ID");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "SCAN_ID");

    ReportEntry reportEntry = createReportService().getBomForPolicyEvaluation(policyEvaluation);

    assertThat(reportEntry).isNotNull();
    assertThat(reportEntry.buf).isNotNull();
    assertThat(reportEntry.buf).isNotEmpty();
  }

  @Test
  public void testGetBomForPolicyEvaluation_NoBomFile() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report-missing-bom-json",
        app.getId(), "SCAN_ID");
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

  @Test
  public void testGetPolicyThreats_shouldThrowNotFoundExceptionGivenNoReportFile() {
    final ReportService reportService = createReportService();
    final String expectedErrorMessage = "Could not find a report with ID " + scanId;

    assertThatThrownBy(() -> reportService.getPolicyThreats(app.getPublicId(), scanId))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(expectedErrorMessage);
  }

  @Test
  public void testGetPolicyThreats_shouldThrowNotFoundExceptionGivenNoReportEntryForPolicyThreatFounds() throws Exception {
    final ReportService reportService = createReportService();
    final String expectedErrorMessage = String.format("Report policy threats entry is missing for the requested " +
        "application [%s] and scan ID [%s]", app.getPublicId(), scanId);

    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);
    ApplicationReport applicationReport = mock(ApplicationReport.class);
    doReturn(true).when(applicationReport).exists();
    doReturn(null).when(applicationReport).getEntry(any());
    doReturn(applicationReport).when(reportDataStoreSpy)
        .getApplicationReport(argThat(application -> application.getId().equals(app.getId())), eq(scanId));

    assertThatThrownBy(() -> reportService.getPolicyThreats(app.getPublicId(), scanId))
        .isInstanceOf(NotFoundException.class)
        .hasMessage(expectedErrorMessage);

  }

  @Test
  public void testGetPolicyThreats_shouldReturnPolicyThreatsGivenPolicyThreatFileInReport() throws Exception {
    final ReportService reportService = createReportService();
    final PolicyThreats givenPolicyThreatsStoredForReport = createPolicyThreat();

    final ReportEntry givenReportEntryReturned =
        new ReportEntry(POLICY_THREATS.getName(), 1L, (new ObjectMapper())
            .writeValueAsBytes(givenPolicyThreatsStoredForReport));

    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);

    ApplicationReport applicationReport = mock(ApplicationReport.class);
    doReturn(true).when(applicationReport).exists();
    doReturn(givenReportEntryReturned).when(applicationReport).getEntry(any());
    doReturn(applicationReport).when(reportDataStoreSpy).getApplicationReport(any(), any());

    final PolicyThreats result = reportService
        .getPolicyThreats(app.getPublicId(), scanId);

    assertThat(result.aaData).hasSize(1);
    assertThat(result.stageTypeId).isEqualTo("build");

    assertThat(result.aaData.get(0).componentIdentifier)
        .isEqualTo(givenPolicyThreatsStoredForReport.aaData.get(0).componentIdentifier);
    validatePolicyValidationOwner(result.aaData);
  }

  @Test
  public void testGetPolicyThreats_shouldReturnPolicyThreats_noStageTypeIdAndPolicyOwnerId() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir,
        "/ReportServiceTest/report-missing-stage-policy-owner-id-policythreats-json", app.getId(), scanId);
    ReportService reportService = createReportService();
    PolicyThreats policyThreats = reportService.getPolicyThreats(app.getPublicId(), scanId);
    assertThat(policyThreats).isNotNull();
    validatePolicyValidationOwner(policyThreats.aaData);
  }

  private void assertThatReportFilesContains(
      ApplicationReport appReport,
      final String thirdPartyFile) throws IOException
  {
    ReportEntry entry = appReport.getEntry(thirdPartyFile);
    assertThat(entry).isNotNull();
  }

  public static PolicyThreats createPolicyThreat() {
    final PolicyThreats policyThreats = new PolicyThreats();

    final PolicyThreats.Component component = createComponent();

    final PolicyThreats.PolicyViolation policyViolation = createPolicyViolation();
    policyThreats.stageTypeId = "build";

    component.activeViolations.add(policyViolation);
    component.allViolations.add(policyViolation);

    policyThreats.aaData.add(component);

    return policyThreats;
  }

  private void validatePolicyValidationOwner(List<PolicyThreats.Component> components) {
    String ownerId = "ROOT_ORGANIZATION_ID";
    String ownerType = OwnerType.APPLICATION.toString();
    components.stream()
        .flatMap(component -> component.allViolations.stream())
        .forEach(policyViolation -> {
          assertThat(policyViolation.policyOwnerId).isEqualTo(ownerId);
          assertThat(policyViolation.policyOwnerType).isEqualTo(ownerType);
        });
  }

  public static PolicyThreats.PolicyViolation createPolicyViolation() {
    final PolicyThreats.PolicyViolation policyViolation = new PolicyThreats.PolicyViolation();
    policyViolation.policyThreatLevel = 9;
    policyViolation.policyId = "some-policy-id";
    policyViolation.policyViolationId = "some-violation-id";
    policyViolation.policyOwnerType = OwnerType.APPLICATION.toString();
    policyViolation.policyOwnerId = "ROOT_ORGANIZATION_ID";
    return policyViolation;
  }

  public static PolicyThreats.Component createComponent() {
    final PolicyThreats.Component component = new PolicyThreats.Component();
    component.hash = "aaa";
    final Map<String, String> coordinate = new HashMap<>();
    coordinate.put("extension", "jar");
    coordinate.put("groupId", "com.sonatype");
    coordinate.put("artifactId", "test");
    coordinate.put("version", "1.1.1");

    component.componentIdentifier = new ComponentIdentifier("maven", coordinate);

    return component;
  }

  @Test
  public void testParseDependencyDepths_PreferNewStructure() throws Exception {
    JsonNode dependenciesJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/dependencies.json"));
    assertThat(dependenciesJson.path("gavDepths").isObject()).isTrue();

    Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier =
        ReportService.parseDependencyDepths(dependenciesJson);
    assertThat(depthsByIdentifier)
        .containsEntry(ComponentIdentifier.createMavenCoordinates("junit", "junit", "4.9", "", "jar"), depths(1));
    assertThat(depthsByIdentifier).containsEntry(
        ComponentIdentifier.createMavenCoordinates("org.slf4j", "slf4j-api", "1.6", "", "jar"), depths(1, 2, 3));
    assertThat(depthsByIdentifier).hasSize(2);
  }

  @Test
  public void testParseDependencyDepths_FallbackToOldStructure() throws Exception {
    JsonNode dependenciesJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/dependencies.json"));
    ((ObjectNode) dependenciesJson).remove("componentDepths");

    Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier =
        ReportService.parseDependencyDepths(dependenciesJson);
    assertThat(depthsByIdentifier).containsEntry(ComponentIdentifier.createMavenCoordinates("junit", "junit", "4.9"),
        depths(1));
    assertThat(depthsByIdentifier)
        .containsEntry(ComponentIdentifier.createMavenCoordinates("org.slf4j", "slf4j-api", "1.6"), depths(1, 2, 3));
    assertThat(depthsByIdentifier).hasSize(2);
  }

  @Test
  public void testHasAnyLicenseOverrides() {
    String applicationId = tempEntity.newApplicationWithParent().getId();

    boolean hasAnyLicenseOverrides = ReportService.hasAnyLicenseOverrides(licenseOverrideDAO, applicationId);

    assertThat(hasAnyLicenseOverrides).isFalse();

    ComponentIdentifier anameHawk111 = ComponentIdentifier.createAnameCoordinates("hawk", "", "1.1.1");
    tempEntity.newLicenseOverride(applicationId, anameHawk111, OVERRIDDEN, "Beerware");
    hasAnyLicenseOverrides = ReportService.hasAnyLicenseOverrides(licenseOverrideDAO, applicationId);

    assertThat(hasAnyLicenseOverrides).isTrue();
  }

  @Test
  public void testAugmentModified_NoLicenseOverrides() throws Exception {
    JsonNode bomJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/bom.json"));

    JsonNode bomJsonAugmented = bomJson.deepCopy();
    ReportService.augmentModified(new HashSet<>(), bomJsonAugmented);

    assertThat(bomJson).isEqualTo(bomJsonAugmented);
    assertThat(bomJsonAugmented.get("aaData").get(0).has("modified")).isFalse();
    assertThat(bomJsonAugmented.get("aaData").get(1).has("modified")).isFalse();
  }

  @Test
  public void testAugmentModified_AppLicenseOverride() throws Exception {
    ComponentIdentifier anameHawk111 = ComponentIdentifier.createAnameCoordinates("hawk", "", "1.1.1");
    tempEntity.newLicenseOverride(tempEntity.newApplicationWithParent().getId(), anameHawk111, OVERRIDDEN, "Beerware");
    JsonNode bomJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/bom.json"));

    JsonNode bomJsonAugmented = bomJson.deepCopy();
    ReportService.augmentModified(Sets.newHashSet(anameHawk111), bomJsonAugmented);

    assertThat(bomJson).isNotEqualTo(bomJsonAugmented);
    assertThat(bomJsonAugmented.get("aaData").get(0).has("modified")).isTrue();
    assertThat(bomJsonAugmented.get("aaData").get(1).has("modified")).isFalse();
  }

  @Test
  public void testAugmentModified_OrgLicenseOverrides() throws Exception {
    ComponentIdentifier npmHawk111 = ComponentIdentifier.createNpmCoordinates("hawk", "1.1.1");
    tempEntity.newLicenseOverride(tempEntity.newApplicationWithParent().getParentOwnerId(), npmHawk111,
        OVERRIDDEN, "Beerware");
    JsonNode bomJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/bom.json"));

    JsonNode bomJsonAugmented = bomJson.deepCopy();
    ReportService.augmentModified(Sets.newHashSet(npmHawk111), bomJsonAugmented);

    assertThat(bomJson).isNotEqualTo(bomJsonAugmented);
    assertThat(bomJsonAugmented.get("aaData").get(0).has("modified")).isFalse();
    assertThat(bomJsonAugmented.get("aaData").get(1).has("modified")).isTrue();
  }

  @Test
  public void testHideObservedLicenses_ObservedLicenseEnabled_NonMaven() throws Exception {
    JsonNode bomJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/bom.json"));
    License notSupportedLicense = licenseDao.getById(License.NOT_SUPPORTED_ID);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("package1", "version1");
    ObjectNode bomObjectAugmented = (ObjectNode) bomJson.get("aaData").get(1);
    ReportService.hideObservedLicenses(componentIdentifier, bomObjectAugmented, true, notSupportedLicense);

    ObjectMapper mapper = new ObjectMapper();
    ArrayNode observedLicensesArray = mapper.createArrayNode();
    observedLicensesArray.add("EPL-1.0");

    assertThat(bomObjectAugmented.get("observedLicenses")).isEqualTo(observedLicensesArray);
    assertThat(bomObjectAugmented.get("hiddenObservedLicenses").asText()).isEqualTo("false");
  }

  @Test
  public void testHideObservedLicenses_ObservedLicenseDisabled_NonMaven() throws Exception {
    JsonNode bomJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/bom.json"));
    License notSupportedLicense = licenseDao.getById(License.NOT_SUPPORTED_ID);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("package1", "version1");
    ObjectNode bomObjectAugmented = (ObjectNode) bomJson.get("aaData").get(1);
    ReportService.hideObservedLicenses(componentIdentifier, bomObjectAugmented, false, notSupportedLicense);

    ObjectMapper mapper = new ObjectMapper();
    ArrayNode arrayNode = mapper.createArrayNode();

    assertThat(bomObjectAugmented.get("observedLicenses"))
        .isEqualTo(arrayNode.add(licenseDao.getById(License.NOT_SUPPORTED_ID).getShortDisplayName()));
    assertThat(bomObjectAugmented.get("hiddenObservedLicenses").asText()).isEqualTo("true");
  }

  @Test
  public void testHideObservedLicenses_ObservedLicenseDisabled_Maven() throws Exception {
    JsonNode bomJson = new ObjectMapper().readTree(getClass().getResource("/ReportTest/dependencies.json"));
    License notSupportedLicense = licenseDao.getById(License.NOT_SUPPORTED_ID);

    ComponentIdentifier componentIdentifier = ComponentIdentifier
        .createMavenCoordinates("group1", "package1", "version1");
    ObjectNode bomObjectAugmented = (ObjectNode) bomJson.get("componentDepths").get(0);
    ReportService.hideObservedLicenses(componentIdentifier, bomObjectAugmented, false, notSupportedLicense);

    assertThat(bomObjectAugmented.get("hiddenObservedLicenses").asText()).isEqualTo("false");
  }

  @Test
  public void testAugmentDependenciesGraph_WithoutDependencyGraphNode() throws Exception {
    JsonNode dependenciesJson =
        new ObjectMapper().readTree(getClass().getResource("/ReportTest/dependencies.json"));

    JsonNode dependenciesJsonAugmented = dependenciesJson.deepCopy();
    ReportService.augmentDependenciesGraph(dependenciesJsonAugmented);

    assertThat(dependenciesJson).isEqualTo(dependenciesJsonAugmented);
  }

  @Test
  public void testAugmentDependenciesGraph_WithDependencyGraphNode() throws Exception {
    JsonNode dependenciesJson =
        new ObjectMapper().readTree(getClass().getResource("/ReportTest/dependenciesWithGraph.json"));
    JsonNode dependenciesJsonAugmented = dependenciesJson.deepCopy();

    ReportService.augmentDependenciesGraph(dependenciesJsonAugmented);

    assertThat(dependenciesJson).isNotEqualTo(dependenciesJsonAugmented);
    JsonNode dependencyGraphNode = dependenciesJsonAugmented.get("dependencyGraph");

    int expectedNumDirectDependencies = 15;
    assertThat(dependencyGraphNode.get(0).get("children")).hasSize(expectedNumDirectDependencies);
    assertThat(dependencyGraphNode.get(0).has("directDependency")).isFalse();
    for (int i = 0; i < expectedNumDirectDependencies; i++) {
      assertThat(dependencyGraphNode.get(0).get("children").get(i).get("directDependency").asBoolean()).isTrue();
    }

    int expectedTotalDependencies = 29;
    assertThat(dependencyGraphNode).hasSize(expectedTotalDependencies);
    int numDirect = 0;
    for (int i = 1; i < dependencyGraphNode.size(); i++) {
      assertThat(dependencyGraphNode.get(i).has("directDependency")).isTrue();
      if (dependencyGraphNode.get(i).get("directDependency").asBoolean()) {
        numDirect++;
      }
    }
    assertThat(numDirect).isEqualTo(expectedNumDirectDependencies);
  }

  @Test
  public void testReUploadScanReport_doNotReEvaluateIfScanIdDoesNotExist() {
    ReportService reportService = createReportService();
    String nonExistentScanId = "nonExistentScanId";
    String clientUserAgent = "userAgent";

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> reportService.reUploadScanToHds(app.getId(), nonExistentScanId, clientUserAgent))
        .withMessage("Policy evaluation for scan " + nonExistentScanId + " does not exist on the server.");
  }

  @Test
  public void testReUploadScanReport_shouldOverwriteOldToHds() throws IOException {
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ReportService reportService = createReportService();

    String clientUserAgent = "userAgent";
    String newScanId = "newScanId";
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(newScanId);
    doReturn(scanReceipt).when(mockScanUploadService)
        .upload(
            any(),
            any(),
            eq(StageTypes.BUILD.getId()),
            any(),
            eq(clientUserAgent),
            any(),
            any(),
            any(),
            anyBoolean());
    // Mock the new report so we don't have to get it from the real HDS
    ReportHelper.saveMockReport(insightWork, tempDir,
        "/ApplicationReportPersistenceServiceTest/report", app.getId(), newScanId);

    var bomFileBeforeReUpload = applicationReportPersistenceService
        .getReportEntity(app.getId(), scanId, BOM_JSON.getName());
    var indexFileBeforeReUpload = applicationReportPersistenceService
        .getReportEntity(app.getId(), scanId, INDEX_HTML.getName());
    assertThat(getEntityContents(bomFileBeforeReUpload)).isNotEqualTo("{}\n");
    assertThat(getEntityContents(indexFileBeforeReUpload)).isNotEqualTo("<html></html>");

    reportService.reUploadScanToHds(app.getId(), scanId, clientUserAgent);

    var bomFileAfterReUpload = applicationReportPersistenceService
        .getReportEntity(app.getId(), scanId, BOM_JSON.getName());
    var indexFileAfterReUpload = applicationReportPersistenceService
        .getReportEntity(app.getId(), scanId, INDEX_HTML.getName());
    assertThat(getEntityContents(bomFileAfterReUpload)).isEqualTo("{}\n");
    assertThat(getEntityContents(indexFileAfterReUpload)).isEqualTo("<html></html>");
  }

  @Test
  public void testReUploadScanReport_skipsIntegrationVersionValidation() throws IOException {
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ReportService reportService = createReportService();

    String browserUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:143.0) Gecko/20100101 Firefox/143.0";
    String newScanId = "newScanId";
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(newScanId);
    doReturn(scanReceipt).when(mockScanUploadService)
        .upload(
            any(),
            any(),
            eq(StageTypes.BUILD.getId()),
            any(),
            eq(browserUserAgent),
            any(),
            any(),
            any(),
            eq(true));
    ReportHelper.saveMockReport(insightWork, tempDir,
        "/ApplicationReportPersistenceServiceTest/report", app.getId(), newScanId);

    reportService.reUploadScanToHds(app.getId(), scanId, browserUserAgent);

    verify(mockScanUploadService).upload(
        any(),
        any(),
        eq(StageTypes.BUILD.getId()),
        any(),
        eq(browserUserAgent),
        any(),
        any(),
        any(),
        eq(true));
  }

  @Test
  public void testReUploadScanReport_containerImageScannerApiSetsCycloneDxSbomSpecification() throws IOException {
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId, false, false, false,
        new Date(), null, ScanTriggerType.SONATYPE_CONTAINER_IMAGE_SCANNER_API);
    ReportService reportService = createReportService();

    String clientUserAgent = "userAgent";
    String newScanId = "newScanId";
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(newScanId);
    doReturn(scanReceipt).when(mockScanUploadService)
        .upload(
            any(),
            any(),
            eq(StageTypes.BUILD.getId()),
            any(),
            eq(clientUserAgent),
            any(),
            any(),
            any(),
            eq(true));
    ReportHelper.saveMockReport(insightWork, tempDir,
        "/ApplicationReportPersistenceServiceTest/report", app.getId(), newScanId);

    reportService.reUploadScanToHds(app.getId(), scanId, clientUserAgent);

    verify(mockScanUploadService).upload(
        any(),
        any(),
        eq(StageTypes.BUILD.getId()),
        any(),
        eq(clientUserAgent),
        any(),
        any(),
        argThat(scanContext -> scanContext != null &&
            scanContext.containerImageSbomSpecification() == SbomSpecification.CYCLONEDX),
        eq(true));
  }

  @Test
  public void testReUploadScanReport_nonContainerScannerApiSetsNullSbomSpecification() throws IOException {
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId, false, false, false,
        new Date(), null, ScanTriggerType.CLI);
    ReportService reportService = createReportService();

    String clientUserAgent = "userAgent";
    String newScanId = "newScanId";
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(newScanId);
    doReturn(scanReceipt).when(mockScanUploadService)
        .upload(
            any(),
            any(),
            eq(StageTypes.BUILD.getId()),
            any(),
            eq(clientUserAgent),
            any(),
            any(),
            any(),
            eq(true));
    ReportHelper.saveMockReport(insightWork, tempDir,
        "/ApplicationReportPersistenceServiceTest/report", app.getId(), newScanId);

    reportService.reUploadScanToHds(app.getId(), scanId, clientUserAgent);

    verify(mockScanUploadService).upload(
        any(),
        any(),
        eq(StageTypes.BUILD.getId()),
        any(),
        eq(clientUserAgent),
        any(),
        any(),
        argThat(scanContext -> scanContext != null &&
            scanContext.containerImageSbomSpecification() == null),
        eq(true));
  }

  /**
   * Regression test for CLM-37563. The ScanContext passed to scanUploadService.upload during re-evaluation
   * must have isValid=true when the original scan has no SBOM metadata (or the SBOM was valid), so that
   * SbomResultHandler.parseBom validates the SBOM and processSbom invokes processDependencyGraph. Without
   * this, the dependency graph is not written into the scan file sent to HDS, the resulting dependencies.json
   * has no dependencyTree, and "View Dependency Tree" is greyed out on the report.
   */
  @Test
  public void testReUploadScanReport_setsIsValidTrueOnScanContextWhenNoSbomMetadata() throws IOException {
    runReUploadAndVerifyScanContextIsValid(true);
  }

  /**
   * For SBOMs that originally failed CycloneDX schema validation (only possible under
   * SKIP_SBOM_IMPORT_VALIDATION), re-evaluation must propagate isValid=false so SbomResultHandler.parseBom
   * takes the no-validation branch. Hardcoding isValid=true would route through parseAndValidateCycloneDx,
   * which has no SbomValidationException fallback, breaking re-evaluation for these customers.
   */
  @Test
  public void testReUploadScanReport_propagatesIsValidFalseFromSbomMetadata() throws IOException {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan("scanRequestId", scanId, thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata(app.getId(), "1", thirdPartyFile, PENDING);
    sbomMetadata.setIsValid(false);
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    runReUploadAndVerifyScanContextIsValid(false);
  }

  /**
   * SBOM metadata exists in the database with isValid=true: re-evaluation must propagate isValid=true so the
   * dependency-graph processing path is taken. Complements the null-metadata and isValid=false cases above.
   */
  @Test
  public void testReUploadScanReport_propagatesIsValidTrueFromSbomMetadata() throws IOException {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan("scanRequestId", scanId, thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.createSbomMetadata(app.getId(), "1", thirdPartyFile, PENDING);
    sbomMetadata.setIsValid(true);
    thirdPartySbomMetadataDAO.update(sbomMetadata);
    runReUploadAndVerifyScanContextIsValid(true);
  }

  private void runReUploadAndVerifyScanContextIsValid(boolean expectedIsValid) throws IOException {
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ReportService reportService = createReportService();

    String clientUserAgent = "userAgent";
    String newScanId = "newScanId";
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(newScanId);
    doReturn(scanReceipt).when(mockScanUploadService)
        .upload(
            any(),
            any(),
            eq(StageTypes.BUILD.getId()),
            any(),
            eq(clientUserAgent),
            any(),
            any(),
            any(),
            eq(true));
    ReportHelper.saveMockReport(insightWork, tempDir,
        "/ApplicationReportPersistenceServiceTest/report", app.getId(), newScanId);

    reportService.reUploadScanToHds(app.getId(), scanId, clientUserAgent);

    verify(mockScanUploadService).upload(
        any(),
        any(),
        eq(StageTypes.BUILD.getId()),
        any(),
        eq(clientUserAgent),
        any(),
        any(),
        argThat(scanContext -> scanContext != null && scanContext.isValid() == expectedIsValid),
        eq(true));
  }

  @Test
  public void testReUploadScanReport_preservesThirdPartyEntriesFromOriginalReport() throws IOException {
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);
    ReportHelper.saveMockReport(insightWork, tempDir,
        "/ReportServiceTest/report-with-third-party-license-data", app.getId(), scanId);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ReportService reportService = createReportService();

    String clientUserAgent = "userAgent";
    String newScanId = "newScanId";
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(newScanId);
    doReturn(scanReceipt).when(mockScanUploadService)
        .upload(any(), any(), eq(StageTypes.BUILD.getId()), any(), eq(clientUserAgent), any(), any(), any(),
            anyBoolean());
    // Use mockReportDownloader (not pre-saved) so downloadReportPostAction fires, allowing preserved entries to inject
    mockReportDownloader.mockDownloadReport(newScanId, "/ApplicationReportPersistenceServiceTest/report");

    String thirdPartyBomBefore = getEntityContents(
        applicationReportPersistenceService.getReportEntity(app.getId(), scanId, THIRD_PARTY_BOM_JSON.getName()));
    String thirdPartyLicenseBefore = getEntityContents(
        applicationReportPersistenceService.getReportEntity(app.getId(), scanId, THIRD_PARTY_LICENSE_JSON.getName()));
    String thirdPartySecurityBefore = getEntityContents(
        applicationReportPersistenceService.getReportEntity(app.getId(), scanId, THIRD_PARTY_SECURITY_JSON.getName()));

    reportService.reUploadScanToHds(app.getId(), scanId, clientUserAgent);

    assertThat(getEntityContents(
        applicationReportPersistenceService.getReportEntity(app.getId(), scanId, THIRD_PARTY_BOM_JSON.getName())))
            .isEqualTo(thirdPartyBomBefore);
    assertThat(getEntityContents(
        applicationReportPersistenceService.getReportEntity(app.getId(), scanId, THIRD_PARTY_LICENSE_JSON.getName())))
            .isEqualTo(thirdPartyLicenseBefore);
    assertThat(getEntityContents(
        applicationReportPersistenceService.getReportEntity(app.getId(), scanId, THIRD_PARTY_SECURITY_JSON.getName())))
            .isEqualTo(thirdPartySecurityBefore);
  }

  @Test
  public void testReUploadScanReport_nonSbomReportRunsStandardProcessThirdPartyDataPath() throws IOException {
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report", app.getId(), scanId);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ReportService reportService = createReportService();

    String clientUserAgent = "userAgent";
    String newScanId = "newScanId";
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(newScanId);
    doReturn(scanReceipt).when(mockScanUploadService)
        .upload(any(), any(), eq(StageTypes.BUILD.getId()), any(), eq(clientUserAgent), any(), any(), any(),
            anyBoolean());
    mockReportDownloader.mockDownloadReport(newScanId, "/ApplicationReportPersistenceServiceTest/report");

    reportService.reUploadScanToHds(app.getId(), scanId, clientUserAgent);

    verify(thirdPartyDataServiceSpy).getScanData(eq(newScanId));
  }

  @Test
  public void testReUploadScanReport_preservesIacComponentsAlongsideSbomEntries() throws IOException {
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report-with-third-party-iac",
        app.getId(), scanId);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ReportService reportService = createReportService();

    String clientUserAgent = "userAgent";
    String newScanId = "newScanId";
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(newScanId);
    doReturn(scanReceipt).when(mockScanUploadService)
        .upload(any(), any(), eq(StageTypes.BUILD.getId()), any(), eq(clientUserAgent), any(), any(), any(),
            anyBoolean());
    mockReportDownloader.mockDownloadReport(newScanId, "/ApplicationReportPersistenceServiceTest/report");

    reportService.reUploadScanToHds(app.getId(), scanId, clientUserAgent);

    String thirdPartyBomAfter = getEntityContents(
        applicationReportPersistenceService.getReportEntity(app.getId(), scanId, THIRD_PARTY_BOM_JSON.getName()));
    JsonNode bomData = new ObjectMapper().readTree(thirdPartyBomAfter);
    List<String> formats = new ArrayList<>();
    bomData.path("aaData").forEach(entry -> formats.add(entry.path("componentIdentifier").path("format").asText()));
    assertThat(formats).contains("terraform");
  }

  @Test
  public void testReUploadScanReport_preservedLicensesRetainDeclaredAndEffectiveValues() throws IOException {
    ScanHelper.createDummyScanFile(insightWork, app.getId(), scanId);
    ReportHelper.saveMockReport(insightWork, tempDir,
        "/ReportServiceTest/report-with-third-party-license-data", app.getId(), scanId);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    ReportService reportService = createReportService();

    String clientUserAgent = "userAgent";
    String newScanId = "newScanId";
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(newScanId);
    doReturn(scanReceipt).when(mockScanUploadService)
        .upload(any(), any(), eq(StageTypes.BUILD.getId()), any(), eq(clientUserAgent), any(), any(), any(),
            anyBoolean());
    mockReportDownloader.mockDownloadReport(newScanId, "/ApplicationReportPersistenceServiceTest/report");

    reportService.reUploadScanToHds(app.getId(), scanId, clientUserAgent);

    String licenseContents = getEntityContents(
        applicationReportPersistenceService.getReportEntity(app.getId(), scanId, THIRD_PARTY_LICENSE_JSON.getName()));
    JsonNode licenseData = new ObjectMapper().readTree(licenseContents);
    Map<String, JsonNode> byHash = new HashMap<>();
    licenseData.path("aaData").forEach(entry -> byHash.put(entry.path("hash").asText(), entry));

    assertThat(byHash.get("cf085cd08ee27334c573").path("declaredLicenses").get(0).path("id").asText())
        .isEqualTo("GPL-2.0");
    assertThat(byHash.get("964cd74171f427720480").path("effectiveLicenses").get(0).path("id").asText())
        .isEqualTo("AGPL-1.0");
  }

  @Test
  public void testFetchReport_automatedRemediationIsTriggered() throws IOException {
    // Create an InnerSource app
    Application innerSourceApp = tempEntity.newApplicationWithParent();
    ComponentIdentifier component = ComponentIdentifier.createMavenCoordinates(
        "dev.sonatype.test", "iq-sample-vulnerable-dependency", "1.0-SNAPSHOT", "", "jar");
    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(component);
    InnerSourceApplication innerSourceAppEntity = tempEntity.newInnerSourceApplication(
        packageUrl.getPackageUrl(), innerSourceApp);
    tempEntity.newInnerSourceVersion(innerSourceAppEntity, component.get(ComponentIdentifier.VERSION),
        StageTypes.SOURCE.getId());

    // Register a new non-major version of the InnerSource app, the release version
    ComponentIdentifier componentWithNewVersion = component.createAlternativeVersion("1.0");
    tempEntity.newInnerSourceVersion(innerSourceAppEntity, componentWithNewVersion.get(ComponentIdentifier.VERSION),
        StageTypes.RELEASE.getId());

    // Fetch the report
    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report-with-innersource-dependencies",
        app.getId(), scanId);
    ReportService reportService = createReportService();
    RemediationVersionDTO remediationVersionDTO =
        new RemediationVersionDTO("1.0", ApiVersionChangeOptionType.INNER_SOURCE_LATEST_NON_BREAKING);
    ApplicationReport report = reportService.fetchReport(app, scanId, StageTypes.RELEASE.getId());
    assertThat(report).isNotNull();
    assertThat(report.exists()).isTrue();

    // Verify the automated remediation pull request is created
    Stage stage = new Stage(StageTypes.RELEASE.getId(), StageTypes.RELEASE.getName());
    verify(automatedPullRequestCreationServiceSpy, times(1))
        .createAutomatedRemediationPullRequest(
            eq(app),
            eq(report.getScanId()),
            eq(stage),
            eq(component),
            argThat(remediationMatches(remediationVersionDTO)),
            eq(Collections.emptyList()),
            eq(true),
            any());
  }

  @Test
  public void testFetchReport_automatedRemediationIsTriggered_directInnerSourceDepMultipleVersions() throws Exception {
    // Create an InnerSource app
    Application innerSourceApp = tempEntity.newApplicationWithParent();
    ComponentIdentifier component = ComponentIdentifier.createMavenCoordinates(
        "commons-io", "commons-io", "2.17.0", "", "jar");
    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(component);
    InnerSourceApplication innerSourceAppEntity = tempEntity.newInnerSourceApplication(
        packageUrl.getPackageUrl(), innerSourceApp);
    tempEntity.newInnerSourceVersion(innerSourceAppEntity, component.get(ComponentIdentifier.VERSION),
        StageTypes.SOURCE.getId());

    ComponentIdentifier componentWithNewVersion = component.createAlternativeVersion("2.18.0");
    tempEntity.newInnerSourceVersion(innerSourceAppEntity, componentWithNewVersion.get(ComponentIdentifier.VERSION),
        StageTypes.RELEASE.getId());

    // Fetch the report
    ReportHelper.saveMockReport(insightWork, tempDir,
        "/ReportServiceTest/report-with-innersource-dependencies-direct-multiple-versions",
        app.getId(), scanId);
    ReportService reportService = createReportService();
    RemediationVersionDTO remediationVersionDTO =
        new RemediationVersionDTO("2.18.0", ApiVersionChangeOptionType.INNER_SOURCE_LATEST_NON_BREAKING);
    ApplicationReport report = reportService.fetchReport(app, scanId, StageTypes.RELEASE.getId());
    assertThat(report).isNotNull();
    assertThat(report.exists()).isTrue();

    // Verify the automated remediation pull request is created
    Stage stage = new Stage(StageTypes.RELEASE.getId(), StageTypes.RELEASE.getName());
    verify(automatedPullRequestCreationServiceSpy, times(1))
        .createAutomatedRemediationPullRequest(
            eq(app),
            eq(report.getScanId()),
            eq(stage),
            eq(component),
            argThat(remediationMatches(remediationVersionDTO)),
            eq(Collections.emptyList()),
            eq(true),
            any());
  }

  @Test
  public void testSetContainerScannerMode_FieldMissing_resultsInNull() throws Exception {
    ReportService reportService = createReportService();
    ApplicationReport appReport =
        new ApplicationReport(applicationReportPersistenceService, app, "scanId");

    ObjectNode summary = new ObjectMapper().createObjectNode();
    appReport.saveReportEntry(SUMMARY_JSON.getName(), summary);

    ReportMetadataDTO metadata = new ReportMetadataDTO();
    reportService.setContainerScannerMode(appReport.getEntry(SUMMARY_JSON.getName()), metadata);

    assertThat(metadata.getContainerScanningMode()).isNull();
  }

  @Test
  public void testSetContainerScannerMode_FieldPresent_setsSonatype() throws Exception {
    ReportService reportService = createReportService();
    ApplicationReport appReport =
        new ApplicationReport(applicationReportPersistenceService, app, "scanId");

    ObjectNode summary = new ObjectMapper().createObjectNode();
    summary.put("containerScanningMode", "sonatype");
    appReport.saveReportEntry(SUMMARY_JSON.getName(), summary);

    ReportMetadataDTO metadata = new ReportMetadataDTO();
    reportService.setContainerScannerMode(appReport.getEntry(SUMMARY_JSON.getName()), metadata);

    assertThat(metadata.getContainerScanningMode()).isEqualTo("sonatype");
  }

  private ArgumentMatcher<Supplier<Optional<RemediationVersionDTO>>> remediationMatches(
      RemediationVersionDTO expected)
  {
    return supplier -> {
      if (supplier == null) {
        return false;
      }
      Optional<RemediationVersionDTO> result = supplier.get();
      return result.isPresent()
          && expected.getVersion().equals(result.get().getVersion())
          && expected.getRemediationType().equals(result.get().getRemediationType());
    };
  }

  private File createSbomFile(ThirdPartySbomMetadata sbomMetadata) throws IOException {
    File sbomDir = insightWork.getSbomDir(sbomMetadata.getApplicationId());
    if (!sbomDir.exists()) {
      assertThat(sbomDir.mkdirs()).isTrue();
    }

    File sbomFile = new File(sbomDir, sbomMetadata.getFilename());
    if (!sbomFile.exists()) {
      assertThat(sbomFile.createNewFile()).isTrue();
    }
    sbomFile.deleteOnExit();
    return sbomFile;
  }

  private String getEntityContents(BaseReportEntity entity) throws IOException {
    try (var inputStream = entity.getInputStream()) {
      assertThat(inputStream).isNotNull();
      byte[] entityContents = inputStream.readAllBytes();
      return new String(entityContents, StandardCharsets.UTF_8);
    }
  }
}
