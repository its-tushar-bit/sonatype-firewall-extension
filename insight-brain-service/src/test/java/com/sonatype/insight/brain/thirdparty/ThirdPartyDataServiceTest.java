/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
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
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
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
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.vulnerability.SecurityVulnerabilityDataService;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.ReferenceLink;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.SecurityVulnerabilitySeverity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Binder;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.AttachmentText;
import org.cyclonedx.model.Swid;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThirdPartyDataServiceTest
    extends AbstractComponentTest
{
  public static final String SCAN_REQUEST_ID = "scan-request-id";

  @Inject
  private ThirdPartyDataService handler;

  @Inject
  private ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO;

  @Inject
  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Inject
  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  private SearchIndexChangeDAO searchIndexChangeDAO;

  @Inject
  private TestProductLicense productLicense;

  @Inject
  private InsightWork insightWork;

  private static final String SCAN_ID = "scanId";

  private TelemetrySender mockTelemetrySender;

  private SecurityVulnerabilityDataService mockSecurityVulnerabilityDataService;

  @Override
  public void configure(Binder binder) {
    mockTelemetrySender = mock(TelemetrySender.class);
    mockSecurityVulnerabilityDataService = mock(SecurityVulnerabilityDataService.class);
    binder.bind(TelemetrySender.class).toInstance(mockTelemetrySender);
    binder.bind(SecurityVulnerabilityDataService.class).toInstance(mockSecurityVulnerabilityDataService);
    super.configure(binder);
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
                "m1", "<dd>r1</dd>", "<dd>a1</dd>");
    final ThirdPartyCoordinateSecurity sec2coord1 =
        tempEntity
            .newThirdPartyCoordinateSecurity(coord1, "r2", "desc2", "l2", 1f, null, "s2", "v:2", "sd2", "<dd>444</dd>",
                "m2", "<dd>r2</dd>", "<dd>a2</dd>");

    final ThirdPartyCoordinateSecurity sec1coord2 =
        tempEntity
            .newThirdPartyCoordinateSecurity(coord2, "r3", "desc3", "l3", 3f, null, "s3", "v:3", "sd3", "<dd>333</dd>",
                "m3", "<dd>r3</dd>", "<dd>a3</dd>");

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

    assertLicenseRowsForComponent(scanData.licenseRows, coord1, 1, lic1coord1,lic2coord1);
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
                "m1", "<dd>r1</dd>", "<dd>a1</dd>");

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
    tempEntity.createSbomMetadata("appId", "1", thirdPartyFile1);
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
    final File reportZip = zipReportDir("/ThirdPartyDataServiceTest/report-with-third-party-iac");

    ThirdPartyApplicationReportDTO dto = handler.loadThirdPartyInfrastructureAsCodeData(reportZip, "app-id");
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
  public void testMergeSonatypeDataWithSbomDataWithIndexing_SbomMetadataStatusIsUnchangedIfUnlicensed()
      throws Exception
  {
    productLicense.setMissingFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    tempEntity.createSbomMetadata("appId", "1", file);

    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ReportServiceTest/report-with-third-party-iac", tempDir).toURI()).toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.PENDING.name());
  }

  @Test
  public void testMergeSonatypeDataWithSbomDataWithIndexing_SbomMetadataStatusIsActiveIfLicensed()
      throws URISyntaxException, IOException
  {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    tempEntity.createSbomMetadata("appId", "1", file);

    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ReportServiceTest/report-with-third-party-iac", tempDir).toURI()).toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.ACTIVE.name());
  }

  @Test
  public void testMergeSonatypeDataWithSbomDataWithIndexing_SbomMetadataStatusIsUnchangedIfNoScans()
      throws URISyntaxException, IOException
  {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.createSbomMetadata("appId", "1", file);

    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ReportServiceTest/report-with-third-party-iac", tempDir).toURI()).toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.PENDING.name());
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_VerifySecurityVulnerabilityUpdatesAndInserts()
      throws URISyntaxException, IOException
  {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    mockSecurityVulnerabilityDataHdsResponse();

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "IaC", "terraform", "aws_s3_bucket.test01", "current",
            "0d8e3bd6ee4e6d50557a", "pkg:terraform/plan.tfplan/aws_s3_bucket.test01@current");

    ThirdPartyCoordinateSecurity tpVuln1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00228", "description", null, 0f, null,
            null);
    tpVuln1.setIdentificationSources("SBOM");
    thirdPartyCoordinateSecurityDAO.update(tpVuln1);

    ThirdPartyCoordinateSecurity tpVuln2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00229", "description1", "link1", 1.0f,
            "fixedBy1", "vulnSource1", "vectorString1", "high1", "cwes1", "deepdive1", "recommendations1",
            "advisories1");
    tpVuln2.setIdentificationSources("SBOM");
    thirdPartyCoordinateSecurityDAO.update(tpVuln2);

    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00230", "description", null, 0f, null,
        null);

    ThirdPartyCoordinateSecurity tpVuln3 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00274", "description2", "link2", 1.0f,
            "fixedBy2", "vulnSource2", "vectorString2", "high2", "cwes2", "deepdive2", "recommendations2",
            "advisories2");
    tpVuln3.setIdentificationSources("SBOM");
    thirdPartyCoordinateSecurityDAO.update(tpVuln3);

    tempEntity.createSbomMetadata("appId", "1", file);

    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ReportServiceTest/report-with-third-party-iac", tempDir).toURI()).toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyCoordinateSecurityList).hasSize(8);

    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00228");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo("description");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM");

    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00229");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00230");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo("description");

    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00274");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.ACTIVE.name());
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_SecurityVulnerabilityDescription()
      throws URISyntaxException, IOException
  {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    SecurityVulnerabilityData securityVulnerabilityData1 = new SecurityVulnerabilityData("CVE-2012-5783");
    securityVulnerabilityData1.description = "";
    securityVulnerabilityData1.explanationMarkdown = "some explanation markdown";
    lenient().when(
        mockSecurityVulnerabilityDataService.getSecurityVulnerabilityDetails(eq(securityVulnerabilityData1.identifier),
            any(), eq(true))).thenReturn(securityVulnerabilityData1);

    SecurityVulnerabilityData securityVulnerabilityData2 = new SecurityVulnerabilityData("CVE-2012-5784");
    securityVulnerabilityData2.description = "";
    securityVulnerabilityData2.explanationMarkdown = "some other explanation markdown";
    lenient().when(
        mockSecurityVulnerabilityDataService.getSecurityVulnerabilityDetails(eq(securityVulnerabilityData2.identifier),
            any(), eq(true))).thenReturn(securityVulnerabilityData2);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        tempEntity.newThirdPartyFileCoordinate(file, "tp", "maven", "commons-httpclient", "3.1",
            "964cd74171f427720480", "pkg:maven/apache-httpclient/commons-httpclient@3.1?type=jar");

    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1, "CVE-2012-5783", "", null, 0f, null,
        null);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate2 =
        tempEntity.newThirdPartyFileCoordinate(file, "tp", "maven", "a", "1",
            "964cd74171f427720481", "pkg:maven/g/a@1?type=jar");

    tempEntity.createSbomMetadata("appId", "1", file);

    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ReportServiceTest/report-with-third-party-license-data", tempDir).toURI())
            .toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateIds(
            Arrays.asList(thirdPartyFileCoordinate1.getId(), thirdPartyFileCoordinate2.getId()));
    assertThat(thirdPartyCoordinateSecurityList).hasSize(2);

    // updated record
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate1.getId(),
            "CVE-2012-5783");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");

    // inserted record
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate2.getId(),
            "CVE-2012-5784");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo("some other explanation markdown");
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_VerifyLicenseUpdatesAndInserts()
      throws URISyntaxException, IOException
  {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "tp", "maven", "commons-httpclient", "3.1",
            "964cd74171f427720480", "pkg:maven/apache-httpclient/commons-httpclient@3.1?type=jar");

    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "Apache-2.0", "Apache-2.0", "link1");

    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "AGPL-2.0", "AGPL-2.0", "link2");

    ThirdPartyCoordinateLicense thirdPartyCoordinateLicense1 =
        tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "AGPL-3.0", "AGPL-3.0", "link3");
    thirdPartyCoordinateLicense1.setIdentificationSources("SBOM");
    thirdPartyCoordinateLicenseDAO.update(thirdPartyCoordinateLicense1);

    tempEntity.createSbomMetadata("appId", "1", file);

    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ReportServiceTest/report-with-third-party-license-data", tempDir).toURI())
            .toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    List<ThirdPartyCoordinateLicense> thirdPartyCoordinateLicenseList = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyCoordinateLicenseList).hasSize(4);

    ThirdPartyCoordinateLicense thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "AGPL-2.0");
    assertThat(thirdPartyCoordinateLicense.getLicenseId()).isEqualTo("AGPL-2.0");
    assertThat(thirdPartyCoordinateLicense.getName()).isEqualTo("AGPL-2.0");
    assertThat(thirdPartyCoordinateLicense.getUrl()).isEqualTo("link2");

    thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "AGPL-3.0");
    assertThat(thirdPartyCoordinateLicense.getLicenseId()).isEqualTo("AGPL-3.0");
    assertThat(thirdPartyCoordinateLicense.getName()).isEqualTo("AGPL-3.0");
    assertThat(thirdPartyCoordinateLicense.getUrl()).isEqualTo("link3");
    assertThat(thirdPartyCoordinateLicense.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "AGPL-1.0");
    assertThat(thirdPartyCoordinateLicense.getLicenseId()).isEqualTo("AGPL-1.0");
    assertThat(thirdPartyCoordinateLicense.getIdentificationSources()).isEqualTo("Sonatype");

    thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "Apache-2.0");
    assertThat(thirdPartyCoordinateLicense.getLicenseId()).isEqualTo("Apache-2.0");
    assertThat(thirdPartyCoordinateLicense.getName()).isEqualTo("Apache-2.0");
    assertThat(thirdPartyCoordinateLicense.getUrl()).isEqualTo("link1");
    assertThat(thirdPartyCoordinateLicense.getIdentificationSources()).isEqualTo("Sonatype");

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.ACTIVE.name());
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
        new PackageUrlIdentifier("pkg:npm/jquery@1.1.1"), "deadbeef", false);
    handler.indexSbomForSearch(sbomMetadata);

    List<SearchIndexChange> searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).satisfiesExactly(searchIndexChange -> {
      assertThat(searchIndexChange.getChangeType()).isEqualTo(ChangeType.SBOM);
      assertThat(searchIndexChange.getChangeData()).isEqualTo(app.getId() + ":1.2.3");
    });
  }

  private void mockSecurityVulnerabilityDataHdsResponse() throws URISyntaxException {
    SecurityVulnerabilityData securityVulnerabilityData1 = new SecurityVulnerabilityData("FG-R00229");
    securityVulnerabilityData1.description = "new description1";
    securityVulnerabilityData1.vulnerabilityLink = new URI("new.link1");
    securityVulnerabilityData1.mainSeverity = new SecurityVulnerabilitySeverity("new source1", "new label1", 2.0f);
    securityVulnerabilityData1.advisories =
        Collections.singletonList(new ReferenceLink("new referenceType1", "new.url1"));

    SecurityVulnerabilityData securityVulnerabilityData2 = new SecurityVulnerabilityData("FG-R00274");
    securityVulnerabilityData2.description = "new description2";
    securityVulnerabilityData2.vulnerabilityLink = new URI("new.link2");
    securityVulnerabilityData2.mainSeverity = new SecurityVulnerabilitySeverity("new source2", "new label2", 2.2f);
    securityVulnerabilityData2.advisories =
        Collections.singletonList(new ReferenceLink("new referenceType2", "new.url2"));

    SecurityVulnerabilityData securityVulnerabilityData3 = new SecurityVulnerabilityData("FG-R00099");
    securityVulnerabilityData3.description = "new description3";
    securityVulnerabilityData3.vulnerabilityLink = new URI("new.link3");
    securityVulnerabilityData3.mainSeverity = new SecurityVulnerabilitySeverity("new source3", "new label3", 3.0f);
    securityVulnerabilityData3.advisories =
        Collections.singletonList(new ReferenceLink("new referenceType3", "new.url3"));
    when(mockSecurityVulnerabilityDataService.getSecurityVulnerabilityDetails(eq(securityVulnerabilityData3.identifier),
        any(), eq(true))).thenReturn(securityVulnerabilityData3);

    SecurityVulnerabilityData securityVulnerabilityData4 = new SecurityVulnerabilityData("FG-R00100");
    securityVulnerabilityData4.description = "new description4";
    securityVulnerabilityData4.vulnerabilityLink = new URI("new.link4");
    securityVulnerabilityData4.mainSeverity = new SecurityVulnerabilitySeverity("new source4", "new label4", 4.0f);
    securityVulnerabilityData4.advisories =
        Collections.singletonList(new ReferenceLink("new referenceType4", "new.url4"));
    when(mockSecurityVulnerabilityDataService.getSecurityVulnerabilityDetails(eq(securityVulnerabilityData4.identifier),
        any(), eq(true))).thenReturn(securityVulnerabilityData4);

    SecurityVulnerabilityData securityVulnerabilityData5 = new SecurityVulnerabilityData("FG-R00275");
    securityVulnerabilityData5.description = "new description5";
    securityVulnerabilityData5.vulnerabilityLink = new URI("new.link5");
    securityVulnerabilityData5.mainSeverity = new SecurityVulnerabilitySeverity("new source5", "new label5", 5.0f);
    securityVulnerabilityData5.advisories =
        Collections.singletonList(new ReferenceLink("new referenceType5", "new.url5"));
    when(mockSecurityVulnerabilityDataService.getSecurityVulnerabilityDetails(eq(securityVulnerabilityData5.identifier),
        any(), eq(true))).thenReturn(securityVulnerabilityData5);

    SecurityVulnerabilityData securityVulnerabilityData6 = new SecurityVulnerabilityData("FG-R00101");
    securityVulnerabilityData6.description = "new description6";
    securityVulnerabilityData6.vulnerabilityLink = new URI("new.link6");
    securityVulnerabilityData6.mainSeverity = new SecurityVulnerabilitySeverity("new source6", "new label6", 6.0f);
    securityVulnerabilityData6.advisories =
        Collections.singletonList(new ReferenceLink("new referenceType6", "new.url6"));
    when(mockSecurityVulnerabilityDataService.getSecurityVulnerabilityDetails(eq(securityVulnerabilityData6.identifier),
        any(), eq(true))).thenReturn(securityVulnerabilityData6);
  }

  private File zipReportDir(String reportResourceName) throws URISyntaxException {
    return Paths.get(ReportHelper.zipReport(reportResourceName, tempDir).toURI()).toFile();
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
            assertThat(securityRow.score).isEqualTo(expectedSecRow.getSeverity());
            assertThat(securityRow.url).isEqualTo(expectedSecRow.getLink());
            assertThat(securityRow.fixedVersion).isEqualTo(expectedSecRow.getFixedBy());
            assertThat(securityRow.source).isEqualTo(expectedSecRow.getVulnerabilitySource());
            assertThat(securityRow.severity).isEqualTo(expectedSecRow.getSeverityDescription());
            assertThat(securityRow.cvssVectorString).isEqualTo(expectedSecRow.getAttackVector());
            assertThat(securityRow.ratingMethod).isEqualTo(expectedSecRow.getRatingMethod());
            assertThat(securityRow.recommendations).isEqualTo(expectedSecRow.getRecommendations());
            assertThat(securityRow.advisories).isEqualTo(expectedSecRow.getAdvisories());
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
