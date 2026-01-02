/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerability;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.FileApplicationReportPersistenceService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.CpeResultsTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Binder;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.AttachmentText;
import org.cyclonedx.model.Swid;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ThirdPartyDataServiceTest
    extends AbstractComponentTest
{
  public static final String SCAN_REQUEST_ID = "scan-request-id";

  @Inject
  private ThirdPartyDataService handler;

  @Inject
  private ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  private SearchIndexChangeDAO searchIndexChangeDAO;

  @Inject
  private InsightWork insightWork;

  @Inject
  private FileApplicationReportPersistenceService applicationReportPersistenceService;

  @Inject
  private TestProductLicense productLicense;

  private static final String SCAN_ID = "scanId";

  private TelemetrySender mockTelemetrySender;

  private Application application;

  private CpeResultsTelemetry mockCpeResultsTelemetry;

  @Override
  public void configure(Binder binder) {
    mockTelemetrySender = mock(TelemetrySender.class);
    mockCpeResultsTelemetry = new CpeResultsTelemetry();
    binder.bind(TelemetrySender.class).toInstance(mockTelemetrySender);
    super.configure(binder);
  }

  @Before
  public void createApplication() {
    application = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testGetScanData() {
    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "f1", "n1", "v1", "hash1", "pkg:f1/n1@v1");
    ThirdPartyFileCoordinate coord2 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "f2", "n2", "v2", "hash2", null);
    ThirdPartyFileCoordinate coord3 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "maven", "a", "v2", "hash3", "pkg:maven/g/a@v2?type=jar");
    ThirdPartyFileCoordinate coord4 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "nuget", "p", "v2", "hash4", "pkg:nuget/p@v2");
    ThirdPartyFileCoordinate coord5 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "npm", "p", "v2", "hash5", "pkg:npm/p@v2");
    ThirdPartyFileCoordinate coord6 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "pypi", "n2", "v2", "hash6", "pkg:pypi/n2@v2");
    ThirdPartyFileCoordinate coord7 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "golang", "n2", "v2", "hash7", "pkg:golang/n2@v2");
    ThirdPartyFileCoordinate coord8 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "rpm", "n2", "v2", "hash8", null);

    final ThirdPartyCoordinateSecurity sec1coord1 =
        tempEntity
            .newThirdPartyCoordinateSecurity(coord1, "r1", "desc1", "l1", 5f, null, "s1", "v:1", "sd1", "<dd>123</dd>",
                "m1", "<dd>r1</dd>", "<dd>a1</dd>", "SBOM");
    final ThirdPartyCoordinateSecurity sec2coord1 =
        tempEntity
            .newThirdPartyCoordinateSecurity(coord1, "r2", "desc2", "l2", 1f, null, "s2", "v:2", "sd2", "<dd>444</dd>",
                "m2", "<dd>r2</dd>", "<dd>a2</dd>", "SBOM");

    final ThirdPartyCoordinateSecurity sec1coord2 =
        tempEntity
            .newThirdPartyCoordinateSecurity(coord2, "r3", "desc3", "l3", 3f, null, "s3", "v:3", "sd3", "<dd>333</dd>",
                "m3", "<dd>r3</dd>", "<dd>a3</dd>", "SBOM");

    final ThirdPartyCoordinateLicense lic1coord1 =
        tempEntity.newThirdPartyCoordinateLicense(coord1, "Apache-2.0", "n1", "u1");

    final ThirdPartyCoordinateLicense lic2coord1 =
        tempEntity.newThirdPartyCoordinateLicense(coord1, "AFL-1.2", "n2", "u2");

    final ThirdPartyCoordinateLicense lic1coord2 =
        tempEntity.newThirdPartyCoordinateLicense(coord2, "Apache-2.0", "n2", "u2");

    tempEntity.newThirdPartyCoordinateLicense(coord1, "l3", "n3", "u3");
    tempEntity.newThirdPartyCoordinateLicense(coord2, "l2", "n2", "u2");

    final ThirdPartyApplicationReportDTO scanData = handler.getScanData(SCAN_ID);

    assertThat(scanData.billOfMaterials).hasSize(8);
    assertThat(scanData.securityRows).hasSize(3);
    assertThat(scanData.licenseRows).hasSize(8);

    assertBomContains(scanData.billOfMaterials, coord1, file);
    assertBomContains(scanData.billOfMaterials, coord2, file);
    assertBomContains(scanData.billOfMaterials, coord3, file);
    assertBomContains(scanData.billOfMaterials, coord4, file);
    assertBomContains(scanData.billOfMaterials, coord5, file);
    assertBomContains(scanData.billOfMaterials, coord6, file);
    assertBomContains(scanData.billOfMaterials, coord7, file);
    assertBomContains(scanData.billOfMaterials, coord8, file);
    assertSecurityRowsForComponent(scanData.securityRows, coord1, sec1coord1, sec2coord1);
    assertSecurityRowsForComponent(scanData.securityRows, coord2, sec1coord2);

    assertLicenseRowsForComponent(scanData.licenseRows, coord1, 1, lic1coord1, lic2coord1);
    assertLicenseRowsForComponent(scanData.licenseRows, coord2, 1, lic1coord2);
    assertLicenseNotProvided(scanData.licenseRows, coord3);
    assertLicenseNotProvided(scanData.licenseRows, coord4);
    assertLicenseNotProvided(scanData.licenseRows, coord5);
    assertLicenseNotProvided(scanData.licenseRows, coord6);
    assertLicenseNotProvided(scanData.licenseRows, coord7);
    assertLicenseNotProvided(scanData.licenseRows, coord8);
  }

  @Test
  public void testGetScanDataWithVex() {
    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "f1", "n1", "v1", "hash1", "pkg:f1/n1@v1");

    final ThirdPartyCoordinateSecurity sec1coord1 =
        tempEntity
            .newThirdPartyCoordinateSecurity(coord1, "r1", "desc1", "l1", 5f, null, "s1", "v:1", "sd1", "<dd>123</dd>",
                "m1", "<dd>r1</dd>", "<dd>a1</dd>", "SBOM");

    final ThirdPartyVulnerabilityExploitabilityExchange vex =
        tempEntity
            .newThirdPartyVulnerabilityExploitabilityExchange(sec1coord1, "r1", "resolved",
                "code_not_reachable", "will_not_fix,update", null);

    final ThirdPartyCoordinateLicense lic1coord1 =
        tempEntity.newThirdPartyCoordinateLicense(coord1, "Apache-2.0", "n1", "u1");

    tempEntity.newThirdPartyCoordinateLicense(coord1, "l3", "n3", "u3");

    final ThirdPartyApplicationReportDTO scanData = handler.getScanData(SCAN_ID);

    assertThat(scanData.billOfMaterials).hasSize(1);
    assertThat(scanData.securityRows).hasSize(1);
    assertThat(scanData.licenseRows).hasSize(1);

    assertBomContains(scanData.billOfMaterials, coord1, file);
    assertLicenseRowsForComponent(scanData.licenseRows, coord1, 1, lic1coord1);
    assertSecurityRowsForComponentWithVex(scanData.securityRows, coord1, sec1coord1, vex);
  }

  @Test
  public void testGetScanData_CpeAndSwid() throws JsonProcessingException {
    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    Swid expectedSwid = new Swid();
    expectedSwid.setTagId("swidgen-242eb18a-503e-ca37-393b-cf156ef09691_9.1.1");
    expectedSwid.setName("Acme Application");
    expectedSwid.setVersion("9.1.1");
    expectedSwid.setTagVersion(0);
    expectedSwid.setPatch(false);
    AttachmentText attachmentText = new AttachmentText();
    attachmentText.setEncoding("base64");
    attachmentText.setContentType("text/xml");
    attachmentText.setText("PD94bWwgdmV");
    expectedSwid.setAttachmentText(attachmentText);

    tempEntity.newThirdPartyFileCoordinate(file, "CycloneDx", "f1", "n1", "v1", "hash1", "pkg:f1/n1@v1",
        "cpe:/a:acme:application:9.1.1", ThirdPartyComponentDAO.MAPPER.writeValueAsString(expectedSwid));

    ThirdPartyApplicationReportDTO scanData = handler.getScanData(SCAN_ID);

    List<ThirdPartyBillOfMaterialsRowDTO> billOfMaterials = scanData.billOfMaterials;
    assertThat(billOfMaterials).hasSize(1);
    ThirdPartyBillOfMaterialsRowDTO billOfMaterialsRowDTO = billOfMaterials.get(0);
    assertThat(billOfMaterialsRowDTO.cpe).isEqualTo("cpe:/a:acme:application:9.1.1");
    Swid actualSwid = billOfMaterialsRowDTO.swid;
    assertThat(actualSwid.getTagId()).isEqualTo(expectedSwid.getTagId());
    assertThat(actualSwid.getName()).isEqualTo(expectedSwid.getName());
    assertThat(actualSwid.getVersion()).isEqualTo(expectedSwid.getVersion());
    assertThat(actualSwid.getTagVersion()).isEqualTo(expectedSwid.getTagVersion());
    assertThat(actualSwid.isPatch()).isEqualTo(expectedSwid.isPatch());
    AttachmentText expectedAttachmentText = expectedSwid.getAttachmentText();
    assertThat(actualSwid.getAttachmentText().getEncoding()).isEqualTo(expectedAttachmentText.getEncoding());
    assertThat(actualSwid.getAttachmentText().getContentType()).isEqualTo(expectedAttachmentText.getContentType());
    assertThat(actualSwid.getAttachmentText().getText()).isEqualTo(expectedAttachmentText.getText());
  }

  @Test
  public void testGetScanData_mavenCoordinate() {

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", ComponentIdentifier.FORMAT_MAVEN, "n1", "v1", "hash1",
            "pkg:maven/ns1/n1@v1?type=jar");

    final ThirdPartyCoordinateSecurity sec1coord1 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r1", "desc1", "l1", 5f, "Medium", null);

    final ThirdPartyCoordinateLicense lic1coord1 =
        tempEntity.newThirdPartyCoordinateLicense(coord1, "Apache-2.0", "n1", "u1");

    final ThirdPartyApplicationReportDTO scanData = handler.getScanData(SCAN_ID);

    assertThat(scanData.billOfMaterials).hasSize(1);
    assertThat(scanData.securityRows).hasSize(1);

    assertBomContains(scanData.billOfMaterials, coord1, file);
    assertSecurityRowsForComponent(scanData.securityRows, coord1, sec1coord1);
    assertLicenseRowsForComponent(scanData.licenseRows, coord1, 1, lic1coord1);
  }

  @Test
  public void testGetScanData_NoDuplicateComponents_HandlePaths() {
    final ThirdPartyFile file1 = tempEntity.newThirdPartyFile("path1");
    final ThirdPartyFile file2 = tempEntity.newThirdPartyFile("path2");
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file1);
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file2);
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(file1, "f1", "CLAIR", "n1", "v1", "hash1", "pkg:CLAIR/n1@v1");
    ThirdPartyFileCoordinate coord2 =
        tempEntity.newThirdPartyFileCoordinate(file2, "f1", "CLAIR", "n1", "v1", "hash1", "pkg:CLAIR/n1@v1");

    final ThirdPartyCoordinateSecurity sec1coord1 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r1", "desc1", "l1", 5f, "Medium", null);
    tempEntity.newThirdPartyCoordinateSecurity(coord2, "r1", "desc1", "l1", 5f, "Medium", null);

    final ThirdPartyApplicationReportDTO scanData = handler.getScanData(SCAN_ID);

    assertThat(scanData.billOfMaterials).hasSize(1);
    assertThat(scanData.securityRows).hasSize(1);

    assertBomContains(scanData.billOfMaterials, coord1, file1, file2);
    assertSecurityRowsForComponent(scanData.securityRows, coord1, sec1coord1);
  }

  @Test
  public void testGetScanData_NoData() {
    ThirdPartyApplicationReportDTO scanData = handler.getScanData(SCAN_ID);
    assertThat(scanData).isNull();
  }

  @Test
  public void testGetScanData_ScanExists_NoCoordinates() {
    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyApplicationReportDTO scanData = handler.getScanData(SCAN_ID);
    assertThat(scanData).isNotNull();
    assertThat(scanData.billOfMaterials).hasSize(0);
    assertThat(scanData.securityRows).hasSize(0);
  }

  @Test
  public void testDeleteByScanId() throws IOException {
    String scanId = TemporaryEntity.uuid();

    ThirdPartyFile thirdPartyFile1 = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(TemporaryEntity.uuid(), scanId, thirdPartyFile1);
    tempEntity.createSbomMetadata("appId", "1", thirdPartyFile1, PENDING);
    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByScanId(scanId);
    String sbomApplicationPath = tempDir.getRoot().toPath()
        .relativize(insightWork.getSbomDir(sbomMetadata.getApplicationId()).toPath()).normalize().toString();
    File sbomFile = tempDir.newFile(sbomApplicationPath + File.separator + sbomMetadata.getFilename());
    sbomFile.deleteOnExit();
    assertThat(sbomFile).exists();

    handler.deleteByScanId(scanId);

    assertThat(sbomFile).doesNotExist();
    assertThat(handler.getScanData(scanId)).isNull();
  }

  @Test
  public void testGetSecurityVulnerabilitiesForScanId() {
    String scanId = TemporaryEntity.uuid();
    String anotherScanId = TemporaryEntity.uuid();
    ThirdPartyFile thirdPartyFile1 = tempEntity.newThirdPartyFile();
    ThirdPartyFile anotherThirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(TemporaryEntity.uuid(), scanId, thirdPartyFile1);
    tempEntity.newThirdPartyScan(TemporaryEntity.uuid(), anotherScanId, anotherThirdPartyFile);
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile1, "f1", "CLAIR", "n1", "v1", "hash1", "pkg:CLAIR/n1@v1");
    ThirdPartyFileCoordinate coord2 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile1, "f2", "SBOM", "n2", "v2", "hash2", "pkg:SBOM/n1@v1");

    tempEntity.newThirdPartyCoordinateSecurity(coord1, "r1", "desc1", "l1", 5f, "Medium", null);
    tempEntity.newThirdPartyCoordinateSecurity(coord2, "r2", "desc2", "l2", 7f, "High", null);

    //mismatching records, expect not to get filtered
    ThirdPartyFileCoordinate coord3 = tempEntity
        .newThirdPartyFileCoordinate(anotherThirdPartyFile, "f3", "CLAIR", "n3", "v3", "hash3", "pkg:CLAIR/n3@v3");
    tempEntity.newThirdPartyCoordinateSecurity(coord3, "r3", "desc3", "l3", 1f, "Low", null);

    List<ThirdPartyCoordinateSecurity> coordinateSecurities = handler.getSecurityVulnerabilitiesForScanId(scanId);

    assertThat(coordinateSecurities).hasSize(2);
    assertThat(coordinateSecurities.stream().map(ThirdPartyCoordinateSecurity::getRefId))
        .containsExactlyInAnyOrder("r1", "r2");
  }

  @Test
  public void testProcessThirdPartyData_withInfrastructureAsCodeSavesVulnerabilities() throws Exception {
    ReportHelper.saveMockReport(insightWork, tempDir, "/ThirdPartyDataServiceTest/report-with-third-party-iac",
        application.getId(), SCAN_ID);
    ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, application, SCAN_ID);

    ThirdPartyApplicationReportDTO dto = handler.loadThirdPartyInfrastructureAsCodeData(appReport, "app-id");
    assertThat(dto).isNotNull();

    ThirdPartyVulnerability vulnerability = thirdPartyVulnerabilityDAO.getByRefId(dto.securityRows.get(0).reference);
    assertThat(vulnerability).isNotNull();
    assertThat(vulnerability.getAdvisories()).isEqualTo("https://docs.fugue.co/FG_R00229.html");
    assertThat(vulnerability.getUpdateTime()).isCloseTo(new Date(), Duration.ofMinutes(1).toMillis());

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", "082f05c0fe6c7be532ad651cecccde481f9f63d0");
    expectedAttributes.put("real_application_id", "app-id");
    expectedAttributes.put("number_of_components_with_input_type_tf", "2");
    expectedAttributes.put("number_of_components_with_provider_kubernetes", "2");
    expectedAttributes.put("number_of_iac_components", "2");

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.IAC_METRICS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  @Test
  public void testProcessThirdPartyData_withContainerContent_getSecurityVulnerabilitiesForScanId() {
    String scanId = TemporaryEntity.uuid();
    ThirdPartyFile thirdPartyFile1 = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(TemporaryEntity.uuid(), scanId, thirdPartyFile1);
    ThirdPartyFileCoordinate coord1 = tempEntity
        .newThirdPartyFileCoordinate(thirdPartyFile1, "f1", "container", "n1", "v1", "hash1",
            "pkg:generic/n1@v1?qualifier=container");
    ThirdPartyFileCoordinate coord2 = tempEntity
        .newThirdPartyFileCoordinate(thirdPartyFile1, "f2", "container", "n2", "v2", "hash2",
            "pkg:generic/n2@v2?qualifier=container");

    tempEntity.newThirdPartyCoordinateSecurity(coord1, "r1", "desc1", "l1", 5f, "Medium", "v3");
    tempEntity.newThirdPartyCoordinateSecurity(coord2, "r2", "desc2", "l1", 7f, "High", "v4");

    List<ThirdPartyCoordinateSecurity> coordinateSecurities = handler.getSecurityVulnerabilitiesForScanId(scanId);

    assertThat(coordinateSecurities).hasSize(2);
    assertThat(coordinateSecurities.stream().map(ThirdPartyCoordinateSecurity::getRefId))
        .containsExactlyInAnyOrder("r1", "r2");
  }

  @Test
  public void testSendIacMetricsTelemetry() {
    Map<String, Integer> inputTypeCount = new HashMap<>();
    Map<String, Integer> providerCount = new HashMap<>();

    inputTypeCount.put("tf", 1);
    inputTypeCount.put("yaml", 1);

    providerCount.put("aws", 1);
    providerCount.put("kubernetes", 1);

    handler.sendIacMetricsTelemetry("applicationId", inputTypeCount, providerCount, 2);

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("application_id", HdsClientAnalytics.obfuscate("applicationId"));
    expectedAttributes.put("real_application_id", "applicationId");
    expectedAttributes.put("number_of_components_with_provider_aws", "1");
    expectedAttributes.put("number_of_components_with_input_type_tf", "1");
    expectedAttributes.put("number_of_components_with_provider_kubernetes", "1");
    expectedAttributes.put("number_of_components_with_input_type_yaml", "1");
    expectedAttributes.put("number_of_iac_components", "2");

    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.IAC_METRICS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  @Test
  public void testMergeSonatypeDataWithSbomDataWithIndexing_SbomMetadataStatusIsUnchangedIfUnlicensed()
      throws Exception
  {
    productLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    tempEntity.createSbomMetadata("appId", "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report-with-third-party-iac",
        application.getId(), SCAN_ID);
    ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, application, SCAN_ID);

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, appReport, mockCpeResultsTelemetry);

    ThirdPartySbomMetadata updated = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(updated).isNotNull();
    assertThat(updated.getStatus()).isEqualTo(PENDING);
  }

  @Test
  public void testMergeSonatypeDataWithSbomDataWithIndexing_SbomMetadataStatusIsActiveIfLicensed() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    tempEntity.createSbomMetadata("appId", "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report-with-third-party-iac",
        application.getId(), SCAN_ID);
    ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, application, SCAN_ID);

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, appReport, mockCpeResultsTelemetry);

    ThirdPartySbomMetadata updated = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(updated).isNotNull();
  }

  @Test
  public void testMergeSonatypeDataWithSbomDataWithIndexing_SbomMetadataStatusIsUnchangedIfNoScans() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.createSbomMetadata("appId", "1", file, PENDING);

    ReportHelper.saveMockReport(insightWork, tempDir, "/ReportServiceTest/report-with-third-party-iac",
        application.getId(), SCAN_ID);
    ApplicationReport appReport = new ApplicationReport(applicationReportPersistenceService, application, SCAN_ID);

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, appReport, mockCpeResultsTelemetry);

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(PENDING);
  }

  @Test
  public void testGetScanData_ignoreComponentsWithDuplicatedHash() {
    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "f1", "n1", "v1", "hash1", "pkg:f1/n1@v1");
    ThirdPartyFileCoordinate coord2 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "f2", "n2", "v2", "hash2", null);
    ThirdPartyFileCoordinate coord3 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "f1", "n1", "v1", "hash1", "pkg:f1/n1@v1_duplicated");
    ThirdPartyFileCoordinate coord4 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "nuget", "p", "v2", "hash4", "pkg:nuget/p@v2");
    ThirdPartyFileCoordinate coord5 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "npm", "p", "v2", "hash5", "pkg:npm/p@v2");

    final ThirdPartyApplicationReportDTO scanData = handler.getScanData(SCAN_ID);

    assertThat(scanData.billOfMaterials).hasSize(4);

    LinkedHashSet<String> expectedPurlsForHash1 = new LinkedHashSet<>(Arrays
        .asList("pkg:f1/n1@v1", "pkg:f1/n1@v1_duplicated"));
    assertThat(scanData.billOfMaterials.stream().filter(component
        -> component.hash.equals("hash1")).collect(Collectors.toList()))
        .hasSize(1)
        .extracting(thirdPartyBillOfMaterialsRowDTO -> thirdPartyBillOfMaterialsRowDTO.pathnames)
        .containsOnly(expectedPurlsForHash1);

    assertBomContains(scanData.billOfMaterials, coord1, file);
    assertBomContains(scanData.billOfMaterials, coord2, file);
    assertThat(scanData.billOfMaterials.stream().noneMatch(component
        -> component.packageUrl.equals(coord3.getPackageUrl()))).isTrue();
    assertBomContains(scanData.billOfMaterials, coord4, file);
    assertBomContains(scanData.billOfMaterials, coord5, file);
  }

  @Test
  public void testIndexSbomForSearch() {
    Application app = tempEntity.newApplicationWithParent();
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_ENABLED.setEnabled(true);
    ThirdPartySbomMetadata sbomMetadata = tempEntity.newSbomEvaluation(app, "1.2.3", "spdx",
        new PackageUrlIdentifier("pkg:npm/jquery@1.1.1"), "deadbeef", false, PENDING);
    handler.indexSbomForSearch(sbomMetadata);

    List<SearchIndexChange> searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).satisfiesExactly(searchIndexChange -> {
      assertThat(searchIndexChange.getChangeType()).isEqualTo(ChangeType.SBOM);
      assertThat(searchIndexChange.getChangeData()).isEqualTo(app.getId() + ":1.2.3");
    });
  }

  private void assertSecurityRowsForComponent(
      final List<ThirdPartyHealthCheckReportSecurityRowDTO> securityRows,
      final ThirdPartyFileCoordinate coordinate,
      final ThirdPartyCoordinateSecurity... expectedSecRows)
  {
    final List<ThirdPartyHealthCheckReportSecurityRowDTO> found =
        securityRows.stream().filter(sec -> sec.hash.equals(coordinate.getHash())).collect(Collectors.toList());
    assertThat(found).hasSize(expectedSecRows.length);

    for (ThirdPartyCoordinateSecurity expectedSecRow : expectedSecRows) {
      assertThat(found.stream().filter(sec -> sec.reference.equals(expectedSecRow.getRefId())).findFirst())
          .hasValueSatisfying(securityRow -> {
            assertThat(securityRow.componentIdentifier).isEqualTo(handler.getComponentIdentifier(coordinate));
            assertThat(securityRow.matchState).isEqualTo(MatchState.EXACT.toString());
            assertThat(securityRow.description).isEqualTo(expectedSecRow.getDescription());
            assertThat(securityRow.score).isEqualTo(
                BigDecimal.valueOf(expectedSecRow.getSeverity()).setScale(2, RoundingMode.UNNECESSARY).floatValue());
            assertThat(securityRow.url).isEqualTo(expectedSecRow.getLink());
            assertThat(securityRow.fixedVersion).isEqualTo(expectedSecRow.getFixedBy());
            assertThat(securityRow.source).isEqualTo(expectedSecRow.getVulnerabilitySource());
            assertThat(securityRow.severity).isEqualTo(expectedSecRow.getSeverityDescription());
            assertThat(securityRow.cvssVectorString).isEqualTo(expectedSecRow.getAttackVector());
            assertThat(securityRow.ratingMethod).isEqualTo(expectedSecRow.getRatingMethod());
            assertThat(securityRow.recommendations).isEqualTo(expectedSecRow.getRecommendations());
            assertThat(securityRow.advisories).isEqualTo(expectedSecRow.getAdvisories());
            assertThat(securityRow.researchType).isNotNull();
            assertThat(securityRow.detectionType).isNotNull();
            assertThat(securityRow.identificationSource).isEqualTo(expectedSecRow.getIdentificationSources());
          });
    }
  }

  private void assertSecurityRowsForComponentWithVex(
      final List<ThirdPartyHealthCheckReportSecurityRowDTO> securityRows,
      final ThirdPartyFileCoordinate coordinate,
      final ThirdPartyCoordinateSecurity expectedSecRows,
      final ThirdPartyVulnerabilityExploitabilityExchange vex)
  {
    final List<ThirdPartyHealthCheckReportSecurityRowDTO> found =
        securityRows.stream().filter(sec -> sec.hash.equals(coordinate.getHash())).collect(Collectors.toList());

    assertThat(found.stream().filter(sec -> sec.reference.equals(
        expectedSecRows.getRefId())).findFirst())
        .hasValueSatisfying(securityRow -> {
          assertThat(securityRow.analysis.state).isEqualTo(vex.getState());
          assertThat(securityRow.analysis.justification).isEqualTo(vex.getJustification());
          assertThat(securityRow.analysis.response).isEqualTo(vex.getResponse());
          assertThat(securityRow.analysis.detail).isEqualTo(vex.getDetail());
        });
  }

  private void assertLicenseNotProvided(
      final List<ThirdPartyLicenseRowDTO> licenseRows,
      final ThirdPartyFileCoordinate coordinate)
  {
    final List<ThirdPartyLicenseRowDTO> found =
        licenseRows.stream().filter(sec -> Objects.equals(sec.hash, coordinate.getHash())).collect(Collectors.toList());
    assertThat(found).hasSize(1);
    assertThat(found.get(0).declaredLicenses).hasSize(1);
    final ThirdPartyLicenseDTO license = found.get(0).declaredLicenses.first();
    assertThat(license.id).isEqualTo("UNSPECIFIED");
    assertThat(license.name).isEqualTo("Not Provided");
    assertThat(license.url).isNull();
  }

  private void assertLicenseRowsForComponent(
      final List<ThirdPartyLicenseRowDTO> licenseRows,
      final ThirdPartyFileCoordinate coordinate,
      final int expectedLicenseComponents,
      final ThirdPartyCoordinateLicense... expectedLicRows)
  {
    final List<ThirdPartyLicenseRowDTO> found =
        licenseRows.stream().filter(sec -> Objects.equals(sec.hash, coordinate.getHash())).collect(Collectors.toList());
    assertThat(found).hasSize(expectedLicenseComponents);
    for (ThirdPartyCoordinateLicense expectedLicRow : expectedLicRows) {
      assertThat(found.stream().findFirst()).hasValueSatisfying(licenseRow -> {
        assertThat(licenseRow.componentIdentifier).isEqualTo(handler.getComponentIdentifier(coordinate));
        assertThat(licenseRow.declaredLicenses).contains(toLicenseRow(expectedLicRow));
      });
    }
  }

  private ThirdPartyLicenseDTO toLicenseRow(ThirdPartyCoordinateLicense expectedLicRow) {
    ThirdPartyLicenseDTO license = new ThirdPartyLicenseDTO();
    license.id = expectedLicRow.getLicenseId();
    license.name = expectedLicRow.getName();
    license.url = expectedLicRow.getUrl();
    return license;
  }

  private void assertBomContains(
      final List<ThirdPartyBillOfMaterialsRowDTO> bom,
      final ThirdPartyFileCoordinate coordinate,
      final ThirdPartyFile... files)
  {
    assertThat(bom.stream().filter(component -> component.hash.equals(coordinate.getHash())).findFirst())
        .hasValueSatisfying(bomRow -> {
          String expectedPurl = StringUtils.isNotEmpty(coordinate.getPackageUrl()) ?
              coordinate.getPackageUrl() : PackageUrlIdentifier.toPackageUrl(bomRow.componentIdentifier);
          assertThat(bomRow.componentIdentifier).isEqualTo(handler.getComponentIdentifier(coordinate));
          assertThat(bomRow.createTime).isCloseTo(files[0].getCreated().getTime(), withinPercentage(0.001));
          assertThat(bomRow.matchState).isEqualTo(MatchState.EXACT.toString());
          assertThat(bomRow.packageUrl).isEqualTo(expectedPurl);
        });
  }
}
