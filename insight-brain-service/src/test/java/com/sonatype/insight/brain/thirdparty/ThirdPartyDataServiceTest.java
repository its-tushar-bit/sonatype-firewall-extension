/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
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
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerability;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.sbom.SbomPostImportMetricsTelemetry;
import com.sonatype.insight.brain.sbom.SbomResultsMatcherTelemetry;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.ReferenceLink;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.SecurityVulnerabilityCustomData;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.SecurityVulnerabilitySeverity;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.SecurityVulnerabilityWeakness;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.SecurityVulnerabilityWeakness.CweId;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData.VulnerabilitySource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Binder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.AttachmentText;
import org.cyclonedx.model.Swid;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.withinPercentage;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ThirdPartyDataServiceTest
    extends AbstractComponentTest
{
  public static final String SCAN_REQUEST_ID = "scan-request-id";

  @Inject
  private ThirdPartyDataService handler;

  @Inject
  private ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO;

  @Inject
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Inject
  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Inject
  private ThirdPartyScanDAO thirdPartyScanDAO;

  @Inject
  private SearchIndexChangeDAO searchIndexChangeDAO;

  @Inject
  private ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  @Inject
  private TestProductLicense productLicense;

  @Inject
  private InsightWork insightWork;

  private static final String SCAN_ID = "scanId";

  private TelemetrySender mockTelemetrySender;

  @Override
  public void configure(Binder binder) {
    mockTelemetrySender = mock(TelemetrySender.class);
    binder.bind(TelemetrySender.class).toInstance(mockTelemetrySender);
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
    tempEntity.createSbomMetadata("appId", "1", thirdPartyFile1, "PENDING");
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
    tempEntity.createSbomMetadata("appId", "1", file, "PENDING");

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
    tempEntity.createSbomMetadata("appId", "1", file, "PENDING");

    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ReportServiceTest/report-with-third-party-iac", tempDir).toURI()).toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.ACTIVE.name());
  }

  @Test
  public void testMergeSonatypeDataWithSbomDataWithIndexing_BinaryScan_NoThirdPartyContent()
      throws URISyntaxException, IOException
  {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    tempEntity.createSbomMetadataForBinaryScan(null, "1", file, "PENDING");

    final File reportZip = Paths.get(ReportHelper.zipReport(
        "/ThirdPartyDataServiceTest/report-for-binary-scan", tempDir).toURI()).toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    List<ThirdPartyFileCoordinate> fileCoordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(sbomMetadata.getThirdPartyFileId());
    Map<String, ThirdPartyFileCoordinate> coords = fileCoordinates.stream()
        .collect(Collectors.toMap(ThirdPartyFileCoordinate::getPackageUrl, Function.identity()));
    File actualSbomFile =
        new File(insightWork.getSbomDir(sbomMetadata.getApplicationId()), sbomMetadata.getFilename());

    try (InputStream actualInputStream = new GZIPInputStream(new FileInputStream(actualSbomFile));
         InputStream expectedInputStream =
             ThirdPartyDataServiceTest.class
                 .getResourceAsStream("/ThirdPartyDataServiceTest/binaryScanOriginalSboms/original-bom.json"))
    {
      String actualSbomAsString = IOUtils.toString(actualInputStream, Charset.defaultCharset());
      String expectedSbomAsString = IOUtils.toString(expectedInputStream, Charset.defaultCharset());
      assertThatJson(actualSbomAsString)
          .whenIgnoringPaths("metadata.timestamp", "components[*].bom-ref", "components[*].properties[0].value")
          .isEqualTo(expectedSbomAsString);
    }

    List<PackageUrlIdentifier> expectedUrls = Stream.of(
        "pkg:pypi/orange@1.0.1?qualifier=py2.py3-none-any&extension=whl",
        "pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0",
        "pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1",
        "pkg:maven/com.sun.istack/istack-commons-runtime@4.1.2?type=jar").map(PackageUrlIdentifier::new).toList();

    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.ACTIVE.name());
    assertThat(fileCoordinates).hasSize(4);
    assertThat(coords.keySet()).containsExactlyInAnyOrderElementsOf(expectedUrls.stream().map(PackageUrlIdentifier::
        getPackageUrl).collect(Collectors.toList()));

    ThirdPartyFileCoordinate tpfc1 = coords.get(new PackageUrlIdentifier(
        "pkg:pypi/orange@1.0.1?qualifier=py2.py3-none-any&extension=whl").getPackageUrl());
    assertThat(tpfc1.getId()).isNotEmpty();
    assertThat(tpfc1.getThirdPartyFileId()).isEqualTo(sbomMetadata.getThirdPartyFileId());
    assertThat(tpfc1.getPackageUrl()).isEqualTo(new PackageUrlIdentifier(
        "pkg:pypi/orange@1.0.1?qualifier=py2.py3-none-any&extension=whl").getPackageUrl());
    assertThat(tpfc1.getName()).isEqualTo("orange");
    assertThat(tpfc1.getVersion()).isEqualTo("1.0.1");
    assertThat(tpfc1.getHash()).isEqualTo("093080a1a4bbd2750541");
    assertThat(tpfc1.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(tpfc1.getFormat()).isEqualTo("pypi");
    assertThat(tpfc1.getSource()).isEqualTo("Sonatype");
    assertThat(tpfc1.getDependencyType()).isNull();
    assertThat(tpfc1.getCpe()).isNull();
    assertThat(tpfc1.getSwid()).isNull();

    List<ThirdPartyCoordinateSecurity> tpvListC1 = thirdPartyCoordinateSecurityDAO
        .getByFileCoordinateId(tpfc1.getId());

    assertThat(tpvListC1.size()).isEqualTo(2);
    ThirdPartyCoordinateSecurity fgR00229 = tpvListC1.get(0);
    assertThat(fgR00229.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(fgR00229.getRefId()).isEqualTo("FG-R00229");
    assertThat(fgR00229.getAdvisories()).isNull();
    assertThat(fgR00229.getAttackVector()).isEqualTo("1.vectorString");
    assertThat(fgR00229.getCwes()).isEqualTo("cwe-1,2.cwe");
    assertThat(fgR00229.getDescription()).isBlank();
    assertThat(fgR00229.getLink()).isEqualTo("1.url");
    assertThat(fgR00229.getRecommendations()).isNull();
    assertThat(fgR00229.getSeverity()).isEqualTo(9.0d);
    assertThat(fgR00229.getSeverityDescription()).isEqualTo("CRITICAL");
    assertThat(fgR00229.getVulnerabilitySource()).isEqualTo("IAC");
    assertThat(fgR00229.getRatingMethod()).isNull();

    ThirdPartyCoordinateSecurity fgr00274 = tpvListC1.get(1);
    assertThat(fgr00274.getRefId()).isEqualTo("FG-R00274");
    assertThat(fgr00274.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(fgr00274.getAdvisories()).isNull();
    assertThat(fgr00274.getAttackVector()).isNull();
    assertThat(fgr00274.getCwes()).isNull();
    assertThat(fgr00274.getDescription()).isBlank();
    assertThat(fgr00274.getLink()).isNull();
    assertThat(fgr00274.getRecommendations()).isNull();
    assertThat(fgr00274.getSeverity()).isEqualTo(7.0d);
    assertThat(fgr00274.getSeverityDescription()).isEqualTo("HIGH");
    assertThat(fgr00274.getVulnerabilitySource()).isEqualTo("IAC");
    assertThat(fgr00274.getRatingMethod()).isNull();

    List<ThirdPartyCoordinateLicense> tclListC1 = thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(tpfc1.getId());
    assertThat(tclListC1.size()).isEqualTo(2);
    ThirdPartyCoordinateLicense  component1License1 = tclListC1.get(0);
    assertThat(component1License1.getLicenseId()).isEqualTo("Apache-2.0");
    ThirdPartyCoordinateLicense  component1License2 = tclListC1.get(1);
    assertThat(component1License2.getLicenseId()).isEqualTo("MIT");

    ThirdPartyFileCoordinate tpfc2 = coords.get("pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0");
    assertThat(tpfc2.getId()).isNotEmpty();
    assertThat(tpfc2.getThirdPartyFileId()).isEqualTo(sbomMetadata.getThirdPartyFileId());
    assertThat(tpfc2.getPackageUrl()).isEqualTo("pkg:nuget/Microsoft.Identity.Client.Extensions.Msal@2.23.0");
    assertThat(tpfc2.getName()).isEqualTo("Microsoft.Identity.Client.Extensions.Msal");
    assertThat(tpfc2.getVersion()).isEqualTo("2.23.0");
    assertThat(tpfc2.getHash()).isEqualTo("00603c85922bf35d8edd");
    assertThat(tpfc2.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(tpfc2.getFormat()).isEqualTo("nuget");
    assertThat(tpfc2.getSource()).isEqualTo("Sonatype");
    assertThat(tpfc2.getDependencyType()).isEqualTo("T");
    assertThat(tpfc2.getCpe()).isNull();
    assertThat(tpfc2.getSwid()).isNull();

    ThirdPartyFileCoordinate tpfc3 = coords.get("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1");
    assertThat(tpfc3.getId()).isNotEmpty();
    assertThat(tpfc3.getThirdPartyFileId()).isEqualTo(sbomMetadata.getThirdPartyFileId());
    assertThat(tpfc3.getPackageUrl()).isEqualTo("pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1");
    assertThat(tpfc3.getName()).isEqualTo("Microsoft.IdentityModel.Protocols");
    assertThat(tpfc3.getVersion()).isEqualTo("6.25.1");
    assertThat(tpfc3.getHash()).isEqualTo("c795e78734c2860bb627");
    assertThat(tpfc3.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(tpfc3.getFormat()).isEqualTo("nuget");
    assertThat(tpfc3.getSource()).isEqualTo("Sonatype");
    assertThat(tpfc3.getDependencyType()).isEqualTo("D");
    assertThat(tpfc3.getCpe()).isNull();
    assertThat(tpfc3.getSwid()).isNull();

    ThirdPartyFileCoordinate tpfc4 = coords.get("pkg:maven/com.sun.istack/istack-commons-runtime@4.1.2?type=jar");
    assertThat(tpfc4.getId()).isNotEmpty();
    assertThat(tpfc4.getThirdPartyFileId()).isEqualTo(sbomMetadata.getThirdPartyFileId());
    assertThat(tpfc4.getPackageUrl()).isEqualTo("pkg:maven/com.sun.istack/istack-commons-runtime@4.1.2?type=jar");
    assertThat(tpfc4.getName()).isEqualTo("com.sun.istack:istack-commons-runtime");
    assertThat(tpfc4.getVersion()).isEqualTo("4.1.2");
    assertThat(tpfc4.getHash()).isEqualTo("18ec117c85f3ba0ac654");
    assertThat(tpfc4.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(tpfc4.getFormat()).isEqualTo("maven");
    assertThat(tpfc4.getSource()).isEqualTo("Sonatype");
    assertThat(tpfc4.getDependencyType()).isEqualTo("T");
    assertThat(tpfc4.getCpe()).isNull();
    assertThat(tpfc4.getSwid()).isNull();
  }

  @Test
  public void testMergeSonatypeDataWithSbomDataWithIndexing_SbomMetadataStatusIsUnchangedIfNoScans()
      throws URISyntaxException, IOException
  {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.createSbomMetadata("appId", "1", file, "PENDING");

    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ReportServiceTest/report-with-third-party-iac", tempDir).toURI()).toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.PENDING.name());
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_BestMatchWithSonatypeIdentifier() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    tempEntity.createSbomMetadata(app.getId(), "1", file, "PENDING");

    ThirdPartyFileCoordinate sbomComponent = null;
    try {
      sbomComponent =
          new ThirdPartyFileCoordinate("093080a1a4bbd2750540", "SBOM", "pypi", "orange", "1.0.1", file.getId());
      sbomComponent.setId("123456789"); // the same as in the bom.json results
      sbomComponent.setPackageUrl("pkg:pypi/citrus/orange@1.0.1?extension=whl&qualifier=py2.py3-none-any");
      sbomComponent.setIdentificationSources("SBOM");
      thirdPartyFileCoordinateDAO.insert(sbomComponent);
      final File reportZip =
          Paths.get(ReportHelper.zipReport("/ThirdPartyDataServiceTest/report-with-multiple-results", tempDir).toURI())
              .toFile();

      handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);
      ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);

      sbomComponent = thirdPartyFileCoordinateDAO.getById(sbomComponent.getId());
      assertThat(sbomComponent.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
      //updated purl from the best match result
      assertThat(sbomComponent.getPackageUrl()).isEqualTo(
          "pkg:pypi/citrus/orange@1.0.1?extension=whl&qualifier=py2.py3-none-any&arch=x86_64");

      ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
      assertThat(sbomMetadata).isNotNull();
      assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.ACTIVE.name());

      List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(sbomComponent.getId());
      assertThat(thirdPartyCoordinateSecurityList).hasSize(2);

      List<ThirdPartyCoordinateLicense> licenses =
          thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(sbomComponent.getId());
      assertThat(licenses).hasSize(1);
      verify(mockTelemetrySender, times(2)).send(telemetryDataArgumentCaptor.capture());
      List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getAllValues();
      TelemetryData telemetryData = telemetryDataList.get(0);

      assertThat(telemetryData).isNotNull();
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_RESULT_BEST_MATCH_METRICS);
      assertThat(telemetryData.getAttributes()).hasSize(1).containsKey("sbom_results_matcher_stats");
      SbomResultsMatcherTelemetry resultsMatcherTelemetry =
          (SbomResultsMatcherTelemetry) telemetryData.getAttributes().get("sbom_results_matcher_stats");
      assertThat(resultsMatcherTelemetry.getWinnerStat())
          .extracting(s -> s.purlMatchScore, s -> s.hashMatchScore, s -> s.coordMatchScore)
          .containsExactly(20.0f, 0.0f, 15.0f);
      assertThat(resultsMatcherTelemetry.getMatchStats()).hasSize(4)
          .extracting(s -> s.purlMatchScore, s -> s.hashMatchScore, s -> s.coordMatchScore)
          .containsExactly(tuple(17.5f, 0.0f, 15.0f), tuple(16.25f, 0.0f, 15.0f),
              tuple(18.75f, 0.0f, 15.0f), tuple(20.0f, 0.0f, 15.0f));

      telemetryData = telemetryDataList.get(1);
      assertThat(telemetryData).isNotNull();
      assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_POST_IMPORT_METRICS);
      assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
      SbomPostImportMetricsTelemetry importMetricsTelemetry =
          (SbomPostImportMetricsTelemetry) telemetryData.getAttributes().get("sbom_post_import_metrics");
      assertThat(importMetricsTelemetry.getVerifiedVulnerabilityCount()).isEqualTo(0);
      assertThat(importMetricsTelemetry.getUnverifiedVulnerabilityCount()).isEqualTo(0);
      assertThat(importMetricsTelemetry.getAdditionalVulnerabilitiesCount()).isEqualTo(2);
      assertThat(importMetricsTelemetry.getTotalVulnerabilitiesCount()).isEqualTo(0);
    }
    finally {
      if (sbomComponent != null) {
        thirdPartyFileCoordinateDAO.delete(sbomComponent);
      }
    }
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_VerifySecurityVulnerabilityUpdatesAndInsertsAndTelemetry()
      throws URISyntaxException, IOException
  {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "IaC", "terraform", "aws_s3_bucket.test01", "current",
            "0d8e3bd6ee4e6d50557a", "pkg:terraform/plan.tfplan/aws_s3_bucket.test01@current");

    //Update Scenario 1: existing third party security in db is not modified if not present in report zip or in sonatype
    //FG-R00228 not in report zip but in db with minimal third party vulnerability data
    ThirdPartyCoordinateSecurity tpVuln1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00228", "", null, 0f, null, null);
    tpVuln1.setIdentificationSources("SBOM");
    thirdPartyCoordinateSecurityDAO.update(tpVuln1);

    //Update Scenario 2: existing third party coordinate security record in db is modified with sonatype data
    //FG-R00229 with complete third party vulnerability data
    ThirdPartyCoordinateSecurity tpVuln2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00229", "description1", "link1", 1.0f,
            "deepdive1", "fixedby1");
    tpVuln2.setIdentificationSources("SBOM");
    thirdPartyCoordinateSecurityDAO.update(tpVuln2);

    //Insert Scenario 1: new third party coordinate security record is inserted in db with the minimal sonatype data
    //FG-R00274 with no third party vulnerability data in report zip or db

    //Insert Scenario 2: new third party coordinate security record is inserted in db with complete sonatype data
    //FG-R00275 with no third party vulnerability data in report zip or db

    tempEntity.createSbomMetadata("appId", "1", file, "PENDING");

    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ReportServiceTest/report-with-third-party-security-data", tempDir).toURI())
            .toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    thirdPartyFileCoordinate = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyFileCoordinate.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyCoordinateSecurityList).hasSize(7);

    //Update Scenario 1
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00228");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isEqualTo("<dd>a1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("v:1");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("<dd>1234</dd>");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isBlank();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isEqualTo("<dd>r1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isZero();
    assertThat(thirdPartyCoordinateSecurity.getSeverityDescription()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getVulnerabilitySource()).isEqualTo("source");
    assertThat(thirdPartyCoordinateSecurity.getRatingMethod()).isEqualTo("m1");

    //Update Scenario 2
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00229");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isEqualTo("<dd>a1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString1");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("234");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo("description1");
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new.link1");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isEqualTo("<dd>r1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(9.0d);
    assertThat(thirdPartyCoordinateSecurity.getSeverityDescription()).isEqualTo("CRITICAL");
    assertThat(thirdPartyCoordinateSecurity.getVulnerabilitySource()).isEqualTo("IAC");
    assertThat(thirdPartyCoordinateSecurity.getRatingMethod()).isEqualTo("m1");

    //Insert Scenario 1
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00274");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isBlank();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(7.0d);
    assertThat(thirdPartyCoordinateSecurity.getSeverityDescription()).isEqualTo("HIGH");
    assertThat(thirdPartyCoordinateSecurity.getVulnerabilitySource()).isEqualTo("IAC");
    assertThat(thirdPartyCoordinateSecurity.getRatingMethod()).isNull();

    //Insert Scenario 2
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00275");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString5");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("789");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new.link5");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(7.0d);
    assertThat(thirdPartyCoordinateSecurity.getSeverityDescription()).isEqualTo("HIGH");
    assertThat(thirdPartyCoordinateSecurity.getVulnerabilitySource()).isEqualTo("NVD");
    assertThat(thirdPartyCoordinateSecurity.getRatingMethod()).isEqualTo("CVSSV3");

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.ACTIVE.name());

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_POST_IMPORT_METRICS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    SbomPostImportMetricsTelemetry telemetry = (SbomPostImportMetricsTelemetry) telemetryData.getAttributes()
        .get("sbom_post_import_metrics");
    assertThat(telemetry.getVerifiedVulnerabilityCount()).isEqualTo(1);
    assertThat(telemetry.getUnverifiedVulnerabilityCount()).isEqualTo(0);
    assertThat(telemetry.getAdditionalVulnerabilitiesCount()).isEqualTo(5);
    assertThat(telemetry.getTotalVulnerabilitiesCount()).isEqualTo(2);
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_vulnerabilities_mergeLogicForCMAndTelemetry()
      throws URISyntaxException, IOException
  {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "IaC", "terraform", "aws_s3_bucket.test01", "current",
            "0d8e3bd6ee4e6d50557a", "pkg:terraform/plan.tfplan/aws_s3_bucket.test01@current");

    // Vulnerability in DB - FG-R00230 - Only SBOM source identifier, Sonatype should be added.
    ThirdPartyCoordinateSecurity tpVuln1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00230", "description2", "link2", 1.0f,
            "deepdive2", "fixedby2");
    tpVuln1.setIdentificationSources("SBOM");
    thirdPartyCoordinateSecurityDAO.update(tpVuln1);

    // Vulnerability in DB - FG-R00231 - With both SBOM and Sonatype source identifiers. Nothing added.
    ThirdPartyCoordinateSecurity tpVuln2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00231", "description3", "link3", 1.0f,
            "deepdive3", "fixedby3");
    tpVuln2.setIdentificationSources("SBOM,Sonatype");
    tpVuln2.setCwes("");
    thirdPartyCoordinateSecurityDAO.update(tpVuln2);

    // Vulnerability in DB - FG-R00232 - With both SBOM and Sonatype source identifiers. No HDS results. Sonatype
    // source should be removed.
    ThirdPartyCoordinateSecurity tpVuln3 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00232", "description4", "link4", 1.0f,
            "deepdive4", "fixedby4");
    tpVuln3.setIdentificationSources("SBOM,Sonatype");
    thirdPartyCoordinateSecurityDAO.update(tpVuln3);

    // Vulnerability in DB - FG-R00233 - With only Sonatype source identifiers. No HDS results. Record should be
    // deleted from DB along with VEX annotations if any.
    ThirdPartyCoordinateSecurity tpVuln4 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00233", "description5", "link5", 1.0f,
            "deepdive5", "fixedby5");
    tpVuln4.setIdentificationSources("Sonatype");
    thirdPartyCoordinateSecurityDAO.update(tpVuln4);
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(tpVuln4, "FG-R00233", "resolved",
        "code_not_reachable", "will_not_fix,update", null);

    // Extra vulnerability in DB not in the file. It should be deleted.
    ThirdPartyCoordinateSecurity tpVuln5 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00234", "description6", "link6", 1.0f,
            "deepdive6", "fixedby6");
    tpVuln5.setIdentificationSources("Sonatype");
    thirdPartyCoordinateSecurityDAO.update(tpVuln5);

    // Extra vulnerability in DB not in the file. It should be deleted.
    ThirdPartyCoordinateSecurity tpVuln6 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate, "FG-R00235", "description7", "link7", 1.0f,
            "deepdive7", "fixedby7");
    tpVuln6.setIdentificationSources("Sonatype");
    thirdPartyCoordinateSecurityDAO.update(tpVuln6);
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(tpVuln4, "FG-R00235", "resolved",
        "code_not_reachable", "will_not_fix,update", null);

    tempEntity.createSbomMetadata("appId", "1", file, "PENDING");

    final File reportZip =
        Paths.get(ReportHelper
            .zipReport("/ThirdPartyDataServiceTest/report-with-third-party-security-data",
                tempDir).toURI()).toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    thirdPartyFileCoordinate = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyFileCoordinate.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyCoordinateSecurityList).hasSize(4);

    // Vulnerability not in DB - FG-R00229 - It should have Source Identifier = Sonatype
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00229");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString1");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new link1");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isNull();

    // Vulnerability not in DB - FG-R00230 - It should have Source Identifier = SBOM,Sonatype
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00230");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isEqualTo("<dd>a1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString2");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo("description2");
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new link2");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isEqualTo("<dd>r1<dd/>");

    // Vulnerability not in DB - FG-R00231 - It should have Source Identifier = SBOM,Sonatype
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00231");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM,Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isEqualTo("<dd>a1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString3");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo("description3");
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new link3");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isEqualTo("<dd>r1<dd/>");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEmpty();

    // Vulnerability not in DB - FG-R00232 - It should have Source Identifier = SBOM
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00232");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isEqualTo(tpVuln3.getAdvisories());
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo(tpVuln3.getAttackVector());
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo(tpVuln3.getCwes());
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo(tpVuln3.getDescription());
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("link4");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isEqualTo(tpVuln3.getRecommendations());
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(1.0d);

    // Vulnerability not in DB - FG-R00233 - It should have been deleted from DB.
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00233");
    assertThat(thirdPartyCoordinateSecurity).isNull();
    ThirdPartyVulnerabilityExploitabilityExchange vexFromDB =
        thirdPartyVulnerabilityExploitabilityExchangeDAO.getByCoordinateSecurityIdAndRefId(tpVuln5.getId(),
            "FG-R00233");
    assertThat(vexFromDB).isNull();

    // Vulnerability in DB not in file - FG-R00234 - It should have been deleted from DB.
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00234");
    assertThat(thirdPartyCoordinateSecurity).isNull();

    // Vulnerability in DB not in file - FG-R00234 - It should have been deleted from DB along with its VEX annotation.
    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00235");
    assertThat(thirdPartyCoordinateSecurity).isNull();
    vexFromDB = thirdPartyVulnerabilityExploitabilityExchangeDAO.getByCoordinateSecurityIdAndRefId(tpVuln6.getId(),
        "FG-R00235");
    assertThat(vexFromDB).isNull();

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.ACTIVE.name());

    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(mockTelemetrySender).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SBOM_POST_IMPORT_METRICS);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    SbomPostImportMetricsTelemetry telemetry = (SbomPostImportMetricsTelemetry) telemetryData.getAttributes()
        .get("sbom_post_import_metrics");
    assertThat(telemetry.getVerifiedVulnerabilityCount()).isEqualTo(2);
    assertThat(telemetry.getUnverifiedVulnerabilityCount()).isEqualTo(1);
    assertThat(telemetry.getAdditionalVulnerabilitiesCount()).isEqualTo(1);
    assertThat(telemetry.getTotalVulnerabilitiesCount()).isEqualTo(6);
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_invalidPurlScenario() throws Exception {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate = tempEntity.newThirdPartyFileCoordinate(file, "SBOM", "maven",
        "commons-httpclient", "3.1", "964cd74171f427720480", null);
    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "Apache", "Apache-2.0", "link1", "SBOM");

    tempEntity.createSbomMetadata("appId", "1", file, "PENDING");

    final File reportZip =
        Paths.get(ReportHelper
            .zipReport("/ThirdPartyDataServiceTest/report-with-invalid-purl",
                tempDir).toURI()).toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.ACTIVE.name());
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_licenses_mergeLogicForCM()
      throws URISyntaxException, IOException
  {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "tp", "maven", "commons-httpclient", "3.1",
            "964cd74171f427720480", "pkg:maven/apache-httpclient/commons-httpclient@3.1?type=jar");

    // License from the json file, only with SBOM identification sources, so it should get Sonatype added to it.
    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "Apache", "Apache-2.0", "link1", "SBOM");
    // License only in DB with both SBOM and Sonatype identification sources, so Sonatype should be removed.
    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "AGPL-2.0", "AGPL-2.0", "link2",
        "SBOM,Sonatype");
    // License only in DB with both SBOM and Sonatype identification sources, so it should be deleted from the DB.
    tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "AGPL-3.0", "AGPL-3.0", "link3", "Sonatype");

    tempEntity.createSbomMetadata("appId", "1", file, "PENDING");

    final File reportZip =
        Paths.get(ReportHelper
            .zipReport("/ThirdPartyDataServiceTest/report-with-third-party-license-data",
                tempDir).toURI()).toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    thirdPartyFileCoordinate = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyFileCoordinate.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    List<ThirdPartyCoordinateLicense> thirdPartyCoordinateLicenseList = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyCoordinateLicenseList).hasSize(3);

    ThirdPartyCoordinateLicense thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "Apache");
    assertThat(thirdPartyCoordinateLicense.getLicenseId()).isEqualTo("Apache");
    assertThat(thirdPartyCoordinateLicense.getName()).isEqualTo("Apache-2.0");
    assertThat(thirdPartyCoordinateLicense.getUrl()).isEqualTo("link1");
    assertThat(thirdPartyCoordinateLicense.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "AGPL-2.0");
    assertThat(thirdPartyCoordinateLicense.getLicenseId()).isEqualTo("AGPL-2.0");
    assertThat(thirdPartyCoordinateLicense.getName()).isEqualTo("AGPL-2.0");
    assertThat(thirdPartyCoordinateLicense.getUrl()).isEqualTo("link2");
    assertThat(thirdPartyCoordinateLicense.getIdentificationSources()).isEqualTo("SBOM");

    thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "AGPL-3.0");
    assertThat(thirdPartyCoordinateLicense).isNull();

    thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "AGPL-1.0");
    assertThat(thirdPartyCoordinateLicense.getLicenseId()).isEqualTo("AGPL-1.0");
    assertThat(thirdPartyCoordinateLicense.getName()).isEqualTo("AGPL-1.0");
    assertThat(thirdPartyCoordinateLicense.getUrl()).isEqualTo("url.1");
    assertThat(thirdPartyCoordinateLicense.getIdentificationSources()).isEqualTo("Sonatype");

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.ACTIVE.name());
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_VerifyPythonSecurityAndVulnerabilityUpdates()
      throws URISyntaxException, IOException
  {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "SBOM", "pypi", "pip", "24.0",
            "964cd74171f427720480", "pkg:pypi/pip@24.0");

    ThirdPartyCoordinateLicense license =
        tempEntity.newThirdPartyCoordinateLicense(thirdPartyFileCoordinate, "GPL-2.0", "GPL-2.0", null);
    license.setIdentificationSources("Sonatype");

    thirdPartyCoordinateLicenseDAO.update(license);

    tempEntity.createSbomMetadata("appId", "1", file, "PENDING");

    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ThirdPartyDataServiceTest/report-with-python-components", tempDir).toURI())
            .toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    List<ThirdPartyCoordinateLicense> thirdPartyCoordinateLicenseList = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateId(thirdPartyFileCoordinate.getId());

    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList = thirdPartyCoordinateSecurityDAO
        .getByFileCoordinateId(thirdPartyFileCoordinate.getId());

    assertThat(thirdPartyCoordinateLicenseList).hasSize(1);
    assertThat(thirdPartyCoordinateSecurityList).hasSize(3);

    ThirdPartyCoordinateLicense thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "GPL-2.0");
    assertThat(thirdPartyCoordinateLicense).isNull();

    thirdPartyCoordinateLicense = thirdPartyCoordinateLicenseDAO
        .getByFileCoordinateIdAndLicenseId(thirdPartyFileCoordinate.getId(), "MIT");
    assertThat(thirdPartyCoordinateLicense.getLicenseId()).isEqualTo("MIT");
    assertThat(thirdPartyCoordinateLicense.getName()).isNull();
    assertThat(thirdPartyCoordinateLicense.getUrl()).isNull();
    assertThat(thirdPartyCoordinateLicense.getIdentificationSources()).isEqualTo("Sonatype");

    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity = thirdPartyCoordinateSecurityDAO
        .getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "CVE-2018-20225");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString1");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("348");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new.link1");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(7.8d);

    thirdPartyCoordinateSecurity = thirdPartyCoordinateSecurityDAO
        .getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "CVE-2023-45803");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString2");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("200");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new.link2");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(4.2d);

    thirdPartyCoordinateSecurity = thirdPartyCoordinateSecurityDAO
        .getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "CVE-2024-3651");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("Sonatype");
    assertThat(thirdPartyCoordinateSecurity.getAdvisories()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getAttackVector()).isEqualTo("new vectorString3");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("400");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getLink()).isEqualTo("new.link3");
    assertThat(thirdPartyCoordinateSecurity.getRecommendations()).isNull();
    assertThat(thirdPartyCoordinateSecurity.getSeverity()).isEqualTo(6.2d);

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.ACTIVE.name());
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_VerifyComponentIdentificationSourceAndDependencyType()
      throws URISyntaxException, IOException
  {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        tempEntity.newThirdPartyFileCoordinate(file, "src", "nuget", "Microsoft.IdentityModel.JsonWebTokens", "6.25.1",
            "0e3da21fd80b9853692d", "pkg:nuget/Microsoft.IdentityModel.JsonWebTokens@6.25.1");

    ThirdPartyFileCoordinate thirdPartyFileCoordinate2 =
        tempEntity.newThirdPartyFileCoordinate(file, "src", "nuget", "Microsoft.IdentityModel.Protocols", "6.25.1",
            "c795e78734c2860bb627", "pkg:nuget/Microsoft.IdentityModel.Protocols@6.25.1");

    ThirdPartyFileCoordinate thirdPartyFileCoordinate3 =
        tempEntity.newThirdPartyFileCoordinate(file, "src", "nuget", "Microsoft.Extensions.Options", "5.0.0",
            "d98bcd35050378773586", "pkg:nuget/Microsoft.Extensions.Options@5.0.0");

    ThirdPartyCoordinateSecurity tpVuln1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1, "CVE-2024-21319", "description", null, 0f,
            null, null);
    tpVuln1.setIdentificationSources("SBOM");
    thirdPartyCoordinateSecurityDAO.update(tpVuln1);

    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2, "CVE-2022-38013", "description1", "link1",
        1.0f, "fixedBy1", "vulnSource1", "vectorString1", "high1", "cwes1", "deepdive1", "recommendations1",
        "advisories1", "SBOM");

    tempEntity.createSbomMetadata("appId", "1", file, "PENDING");

    final File reportZip =
        Paths.get(ReportHelper.zipReport("/ReportServiceTest/report-with-dependencies", tempDir).toURI()).toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    thirdPartyFileCoordinate1 = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate1.getId());
    assertThat(thirdPartyFileCoordinate1.getDependencyType()).isEqualTo("T");
    assertThat(thirdPartyFileCoordinate1.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    thirdPartyFileCoordinate2 = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate2.getId());
    assertThat(thirdPartyFileCoordinate2.getDependencyType()).isEqualTo("D");
    assertThat(thirdPartyFileCoordinate2.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    thirdPartyFileCoordinate3 = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate3.getId());
    assertThat(thirdPartyFileCoordinate3.getDependencyType()).isNull();
    assertThat(thirdPartyFileCoordinate3.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate1.getId());
    assertThat(thirdPartyCoordinateSecurityList).hasSize(1);

    thirdPartyCoordinateSecurityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate2.getId());
    assertThat(thirdPartyCoordinateSecurityList).hasSize(1);

    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate1.getId(),
            "CVE-2024-21319");
    assertThat(thirdPartyCoordinateSecurity.getDescription()).isEqualTo("description");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM,Sonatype");

    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate2.getId(),
            "CVE-2022-38013");
    assertThat(thirdPartyCoordinateSecurity.getIdentificationSources()).isEqualTo("SBOM");

    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByThirdPartyFileId(file.getId());
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getStatus()).isEqualTo(SbomStatus.ACTIVE.name());
  }

  @Test
  public void testMergeSonatypeDataWithSbomData_CweIds() throws URISyntaxException, IOException {
    productLicense.setFeatures(LicensedFeature.SBOM_MANAGER);

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);

    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(file, "IaC", "terraform", "aws_s3_bucket.test01", "current",
            "0d8e3bd6ee4e6d50557a", "pkg:terraform/plan.tfplan/aws_s3_bucket.test01@current");

    tempEntity.createSbomMetadata("appId", "1", file, SbomStatus.ACTIVE.name());

    final File reportZip =
        Paths.get(ReportHelper
            .zipReport("/ThirdPartyDataServiceTest/report-with-third-party-security-data",
                tempDir).toURI()).toFile();

    handler.mergeSonatypeDataWithSbomDataWithIndexing(SCAN_ID, reportZip);

    thirdPartyFileCoordinate = thirdPartyFileCoordinateDAO.getById(thirdPartyFileCoordinate.getId());
    List<ThirdPartyCoordinateSecurity> thirdPartyCoordinateSecurityList =
        thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(thirdPartyFileCoordinate.getId());
    assertThat(thirdPartyCoordinateSecurityList).hasSize(3);

    // Vulnerability not in DB - FG-R00229 - It should have Source Identifier = Sonatype
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00229");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isEqualTo("508");

    thirdPartyCoordinateSecurity =
        thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(thirdPartyFileCoordinate.getId(), "FG-R00230");
    assertThat(thirdPartyCoordinateSecurity.getCwes()).isNull();
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
        new PackageUrlIdentifier("pkg:npm/jquery@1.1.1"), "deadbeef", false, "PENDING");
    handler.indexSbomForSearch(sbomMetadata);

    List<SearchIndexChange> searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).satisfiesExactly(searchIndexChange -> {
      assertThat(searchIndexChange.getChangeType()).isEqualTo(ChangeType.SBOM);
      assertThat(searchIndexChange.getChangeData()).isEqualTo(app.getId() + ":1.2.3");
    });
  }

  @Test
  public void testCleanUpPreviousReport() throws IOException {
    executeCleanUpPreviousReportTest(true, false);
  }

  @Test
  public void testCleanUpPreviousReport_FeatureDisabled() throws IOException {
    executeCleanUpPreviousReportTest(false, true);
  }

  private void executeCleanUpPreviousReportTest(
      boolean featureEnabled,
      boolean expectedPreviousReportDirExists) throws IOException
  {
    String appId = "appId";
    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan("scanRequestId", "scanId", file);
    thirdPartyScan.setPreviousScanId("previousScanId");
    thirdPartyScanDAO.update(thirdPartyScan);

    String applicationReportPath = tempDir.getRoot().toPath()
        .relativize(insightWork.getReportDir(appId).toPath()).normalize().toString().concat("/");
    tempDir.newFolder(applicationReportPath + thirdPartyScan.getPreviousScanId());
    tempDir.newFolder(applicationReportPath + thirdPartyScan.getScanId());

    if (!featureEnabled) {
      SystemConfigurationPropertyFeature.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT.setEnabled(false);
    }
    handler.cleanUpPreviousReport(appId, file.getId(), thirdPartyScan.getScanId());

    ThirdPartyScan updatedThirdPartyScan = thirdPartyScanDAO.getById(thirdPartyScan.getId());

    assertThat(insightWork.getReportDir(appId, "previousScanId").exists()).isEqualTo(expectedPreviousReportDirExists);
    assertThat(insightWork.getReportDir(appId, thirdPartyScan.getScanId()).exists()).isTrue();
    assertThat(updatedThirdPartyScan.getPreviousScanId()).isNull();
    assertThat(thirdPartyScanDAO.getById(thirdPartyScan.getId())).isNotNull();
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

  public static class SecurityVulnerabilityTestDataBuilder
  {
    private SecurityVulnerabilityData securityVulnerabilityData;

    private String index;

    private String description;

    private URI vulnerabilityLink;

    private SecurityVulnerabilitySeverity mainSeverity;

    private List<ReferenceLink> advisories;

    private SecurityVulnerabilityCustomData customData;

    private String recommendationMarkdown;

    private SecurityVulnerabilityWeakness weakness;

    private VulnerabilitySource source;

    private SecurityVulnerabilityTestDataBuilder() {
      // noop
    }

    public static SecurityVulnerabilityTestDataBuilder getBuilder() {
      return new SecurityVulnerabilityTestDataBuilder();
    }

    public SecurityVulnerabilityTestDataBuilder withDefaultValues(String index) throws URISyntaxException {
      this.index = index;
      this.description = "new description" + index;
      this.vulnerabilityLink = new URI("new.link" + index);
      this.mainSeverity = new SecurityVulnerabilitySeverity("new source" + index, "new label" + index, 1.0f);
      this.advisories =
          Collections.singletonList(new ReferenceLink("new referenceType" + index, "new.url" + index));
      this.customData = new SecurityVulnerabilityData.SecurityVulnerabilityCustomData();
      this.customData.cvssVector = "new vectorString" + index;
      this.recommendationMarkdown = "new recommendations" + index;
      return this;
    }

    public SecurityVulnerabilityTestDataBuilder withMainSeverityScore(float score) {
      this.mainSeverity.score = score;
      return this;
    }

    public SecurityVulnerabilityTestDataBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public SecurityVulnerabilityTestDataBuilder withVulnerabilitySource(String vulnerabilitySource) {
      this.source = new VulnerabilitySource(vulnerabilitySource, vulnerabilitySource);
      return this;
    }

    public SecurityVulnerabilityTestDataBuilder withMainSeverity(String source, String label, float score) {
      this.mainSeverity = new SecurityVulnerabilitySeverity(source, label, score);
      return this;
    }

    public SecurityVulnerabilityTestDataBuilder withCweIds(List<String> cweIds) throws URISyntaxException {
      this.weakness = new SecurityVulnerabilityWeakness();
      this.weakness.cweSource = "cweSource" + this.index;
      this.weakness.cweIds = new ArrayList<>();
      for (String cweId : cweIds) {
        this.weakness.cweIds.add(new CweId(cweId, new URI("cweUri" + this.index)));
      }
      return this;
    }

    public SecurityVulnerabilityData build(String identifier) {
      SecurityVulnerabilityData securityVulnerabilityData = new SecurityVulnerabilityData(identifier);
      securityVulnerabilityData.description = this.description;
      securityVulnerabilityData.vulnerabilityLink = this.vulnerabilityLink;
      securityVulnerabilityData.mainSeverity = this.mainSeverity;
      securityVulnerabilityData.advisories = this.advisories;
      securityVulnerabilityData.customData = this.customData;
      securityVulnerabilityData.recommendationMarkdown = this.recommendationMarkdown;

      if (this.weakness != null) {
        securityVulnerabilityData.weakness = this.weakness;
      }

      if (this.source != null) {
        securityVulnerabilityData.source = this.source;
      }

      return securityVulnerabilityData;
    }
  }
}
