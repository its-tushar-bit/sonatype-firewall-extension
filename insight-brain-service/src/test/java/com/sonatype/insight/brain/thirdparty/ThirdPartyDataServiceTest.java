/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerability;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

public class ThirdPartyDataServiceTest
    extends AbstractComponentTest
{
  public static final String SCAN_REQUEST_ID = "scan-request-id";

  @Inject
  private ThirdPartyDataService handler;

  private static final String SCAN_ID = "scanId";

  @Test
  public void testGetScanData() {
    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "f1", "n1", "v1", "hash1", "pkg:f1/n1@v1");
    ThirdPartyFileCoordinate coord2 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "f2", "n2", "v2", "hash2", null);
    ThirdPartyFileCoordinate coord3 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", "maven", "a", "v2", "hash3", "pkg:maven/a@v2");
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
  public void testGetScanData_mavenCoordinate() {

    final ThirdPartyFile file = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(SCAN_REQUEST_ID, SCAN_ID, file);
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(file, "CLAIR", ComponentIdentifier.FORMAT_MAVEN, "n1", "v1", "hash1",
            "pkg:maven/n1@v1");

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
  public void testDeleteByScanId() {
    String scanId = tempEntity.uuid();

    ThirdPartyFile thirdPartyFile1 = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(tempEntity.uuid(), scanId, thirdPartyFile1);

    handler.deleteByScanId(scanId);
    assertThat(handler.getScanData(scanId)).isNull();
  }

  @Test
  public void testGetSecurityVulnerabilitiesForScanId() {
    String scanId = tempEntity.uuid();
    String anotherScanId = tempEntity.uuid();
    ThirdPartyFile thirdPartyFile1 = tempEntity.newThirdPartyFile();
    ThirdPartyFile anotherThirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(tempEntity.uuid(), scanId, thirdPartyFile1);
    tempEntity.newThirdPartyScan(tempEntity.uuid(), anotherScanId, anotherThirdPartyFile);
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

    ThirdPartyApplicationReportDTO dto = handler.loadThirdPartyInfrastructureAsCodeData(reportZip);
    assertThat(dto).isNotNull();

    ThirdPartyVulnerability vulnerability =
        new ThirdPartyVulnerabilityDAO().getByRefId(dto.securityRows.get(0).reference);
    assertThat(vulnerability).isNotNull();
  }

  @Test
  public void testProcessThirdPartyData_withContainerContent_getSecurityVulnerabilitiesForScanId() throws Exception {
    String scanId = tempEntity.uuid();
    ThirdPartyFile thirdPartyFile1 = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(tempEntity.uuid(), scanId, thirdPartyFile1);
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
            assertThat(securityRow.componentIdentifier).isEqualTo(ComponentIdentifierAdapter
                .toComponentIdentifier(coordinate.getFormat(), coordinate.getName(), coordinate.getVersion()));
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
        assertThat(licenseRow.componentIdentifier).isEqualTo(ComponentIdentifierAdapter
            .toComponentIdentifier(coordinate.getFormat(), coordinate.getName(), coordinate.getVersion()));
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
          assertThat(bomRow.componentIdentifier).isEqualTo(ComponentIdentifierAdapter
              .toComponentIdentifier(coordinate.getFormat(), coordinate.getName(), coordinate.getVersion()));
          assertThat(bomRow.createTime).isCloseTo(files[0].getCreated().getTime(), withinPercentage(0.001));
          assertThat(bomRow.matchState).isEqualTo(MatchState.EXACT.toString());
          assertThat(bomRow.packageUrl).isEqualTo(coordinate.getPackageUrl());
        });
  }
}
