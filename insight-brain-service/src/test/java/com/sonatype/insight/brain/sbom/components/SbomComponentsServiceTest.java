/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import javax.inject.Inject;
import java.util.Arrays;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.BomPageSbomSummaryDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertThrows;

public class SbomComponentsServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SbomComponentsService service;

  private Application app;

  private Organization org;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplicationWithParent(org);
  }

  @Test
  public void testGetSbomComponentDetails() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), "ACTIVE",
            thirdPartyFile.getFilename());
    ThirdPartyFileCoordinate componentA =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1", "h1",
            "pkg:f1/group/n1@v1?type=jar");
    ThirdPartyFileCoordinate componentB =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s2", "f2", "n2", "v2", "h2",
            "pkg:f2/group/n2@v2?type=jar");
    ThirdPartyCoordinateSecurity vulnerabilityA =
        tempEntity.newThirdPartyCoordinateSecurity(componentA, "cve1", "d1", "l1", 9, "f1", "v1", "cvs1", "sd1",
            "cwes1", "m1", "r1", "ad1", "SBOM");
    ThirdPartyCoordinateSecurity vulnerabilityB =
        tempEntity.newThirdPartyCoordinateSecurity(componentA, "cve2", "d2", "l2", 7, "f2", "v2", "cvs2", "sd2",
            "cwes2", "m2", "r2", "ad2", "SBOM,Sonatype");
    ThirdPartyCoordinateSecurity vulnerabilityC =
        tempEntity.newThirdPartyCoordinateSecurity(componentA, "cve3", "d3", "l3", 5, "f3", "v3", "cvs3", "sd3",
            "cwes3", "m3", "r3", "ad3", "Sonatype");
    ThirdPartyCoordinateSecurity vulnerabilityD =
        tempEntity.newThirdPartyCoordinateSecurity(componentB, "cve1", "d1", "l1", 6, "f1", "v1", "cvs1", "sd1",
            "cwes1", "m1", "r1", "ad1", "SBOM");
    ThirdPartyCoordinateSecurity vulnerabilityE =
        tempEntity.newThirdPartyCoordinateSecurity(componentB, "cve2", "d2", "l2", 7, "f2", "v2", "cvs2", "sd2",
            "cwes2", "m2", "r2", "ad2", "SBOM,Sonatype");

    ThirdPartyVulnerabilityExploitabilityExchange vexA =
        tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityA, "cve1", "resolved",
            "code_not_reachable", "response", "details");
    ThirdPartyVulnerabilityExploitabilityExchange vexB =
        tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityB, "cve2", "resolved",
            "code_not_reachable", "response", "details");
    ThirdPartyVulnerabilityExploitabilityExchange vexE =
        tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityE, "cve2", "resolved",
            "code_not_reachable", "response", "details");

    CDPSbomComponentDetailsDTO actualA =
        service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), componentA.getHash());

    CDPSbomComponentDetailsDTO actualB =
        service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), componentB.getHash());

    assertSbomComponentDetailsDTO(actualA, componentA, sbomMetadata);
    assertComponentSummary(actualA, 9, 1, 1, 1);
    assertThat(actualA.getDisclosedVulnerabilities()).hasSize(2);
    assertVulnerabilities(actualA.getDisclosedVulnerabilities().get(0), vulnerabilityA, vexA.getState(),
        vexA.getJustification(), vexA.getDetail());
    assertVulnerabilities(actualA.getDisclosedVulnerabilities().get(1), vulnerabilityB, vexB.getState(),
        vexB.getJustification(), vexB.getDetail());
    assertThat(actualA.getSonatypeIdentifiedVulnerabilities()).hasSize(1);
    assertVulnerabilities(actualA.getSonatypeIdentifiedVulnerabilities().get(0), vulnerabilityC, null, null, null);

    assertSbomComponentDetailsDTO(actualB, componentB, sbomMetadata);
    assertComponentSummary(actualB, 7, 1, 1, 0);
    assertThat(actualB.getDisclosedVulnerabilities()).hasSize(2);
    assertVulnerabilities(actualB.getDisclosedVulnerabilities().get(0), vulnerabilityD, null, null, null);
    assertVulnerabilities(actualB.getDisclosedVulnerabilities().get(1), vulnerabilityE, vexE.getState(),
        vexE.getJustification(), vexE.getDetail());
    assertThat(actualB.getSonatypeIdentifiedVulnerabilities()).isEmpty();
  }

  @Test
  public void testGetSbomComponentDetails_AppNotFound() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), "ACTIVE",
            thirdPartyFile.getFilename());
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");

    assertThrows( "Could not find application with id anyApp" ,
        NotFoundException.class,
        () -> service.getSbomComponentDetails("anyApp", sbomMetadata.getSbomVersion(), component.getHash()));
  }

  @Test
  public void testGetSbomComponentDetails_SbomVersionNotFound() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), "ACTIVE",
            thirdPartyFile.getFilename());
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");

    assertThrows( "Could not find SBOM version anySbomVersion for application " + app.getId(),
        NotFoundException.class,
        () -> service.getSbomComponentDetails(app.getId(), "anySbomVersion", component.getHash()));
  }

  @Test
  public void testGetSbomComponentDetails_ComponentHashNotFound() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), "ACTIVE",
            thirdPartyFile.getFilename());
    tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");

    assertThrows( "Could not find component by hash anyHash",
        NotFoundException.class,
        () -> service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), "anyHash"));
  }

  @Test
  public void testGetSbomComponentDetails_NoVulnerabilities() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), "ACTIVE",
            thirdPartyFile.getFilename());
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1", "h1",
            "pkg:f1/group/n1@v1?type=jar");
    CDPSbomComponentDetailsDTO actual =
        service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), component.getHash());
    assertSbomComponentDetailsDTO(actual, component, sbomMetadata);
    assertThat(actual.getVulnerabilitySummary().getHighestCvssScore()).isZero();
    assertThat(actual.getVulnerabilitySummary().getVerifiedVulnerabilitiesCount()).isZero();
    assertThat(actual.getVulnerabilitySummary().getUnverifiedVulnerabilitiesCount()).isZero();
    assertThat(actual.getDisclosedVulnerabilities()).isEmpty();
    assertThat(actual.getSonatypeIdentifiedVulnerabilities()).isEmpty();
  }

  private void assertSbomComponentDetailsDTO(
      CDPSbomComponentDetailsDTO actual,
      ThirdPartyFileCoordinate component,
      ThirdPartySbomMetadata sbom)
  {
    assertThat(actual).isNotNull();
    assertThat(actual.getName()).isEqualTo(component.getName());
    assertThat(actual.getHash()).isEqualTo(component.getHash());
    assertThat(actual.getVersion()).isEqualTo(component.getVersion());
    assertThat(actual.getPackageUrl()).isEqualTo(component.getPackageUrl());
    assertThat(actual.getComponentIdentifier()).isNotNull();
    assertThat(actual.getComponentIdentifier().getFormat()).isEqualTo(component.getFormat());
    assertThat(actual.getMetadata()).isNotNull();
    assertThat(actual.getMetadata().getApplicationName()).isEqualTo(app.getName());
    assertThat(actual.getMetadata().getOrganizationName()).isEqualTo(org.getName());
    assertThat(actual.getMetadata().getSbomCreationTime()).isEqualTo(sbom.getCreatedAt());
  }

  private void assertComponentSummary(
      CDPSbomComponentDetailsDTO actual,
      double highestCvssScore,
      int verified,
      int unverified,
      int sonatypeIdentified)
  {
    assertThat(actual.getVulnerabilitySummary()).isNotNull();
    assertThat(actual.getVulnerabilitySummary().getHighestCvssScore()).isEqualTo(highestCvssScore);
    assertThat(actual.getVulnerabilitySummary().getVerifiedVulnerabilitiesCount()).isEqualTo(verified);
    assertThat(actual.getVulnerabilitySummary().getUnverifiedVulnerabilitiesCount()).isEqualTo(unverified);
    assertThat(actual.getVulnerabilitySummary().getSonatypeIdentifiedVulnerabilitiesCount()).isEqualTo(
        sonatypeIdentified);
  }

  private void assertVulnerabilities(
      VulnerabilityDetailsDTO actual,
      ThirdPartyCoordinateSecurity vulnerability,
      String vexState,
      String vexJustification,
      String vexDetail)
  {
    assertThat(actual.getCvssScore()).isEqualTo(vulnerability.getSeverity());
    assertThat(actual.getIssue()).isEqualTo(vulnerability.getRefId());
    assertThat(actual.getAnalysisStatus()).isEqualTo(vexState);
    assertThat(actual.getJustification()).isEqualTo(vexJustification);
    assertThat(actual.getDetails()).isEqualTo(vexDetail);
  }

  @Test
  @PostgresTest
  public void testGetSbomMetadataSuccessful() {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    BomPageMetadataDTO resultDto = service.getBomPageMetadata(application.getId(), sbomMetadata.getSbomVersion());
    assertThat(resultDto.fileFormat).isEqualTo(sbomMetadata.getSpecFormat());
    assertThat(resultDto.specification).isEqualTo(sbomMetadata.getSpec());
    assertThat(resultDto.specVersion).isEqualTo(sbomMetadata.getSpecVersion());
    assertThat(resultDto.author).isEqualTo(Arrays.asList("John Doe"));
    assertThat(resultDto.supplier).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(resultDto.manufacturer).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(resultDto.scanId).isEqualTo(thirdPartyScan.getScanId());

    // Test SPDX Format
    ThirdPartySbomMetadata sbomSPDXMetadata = SbomMetadataBuilder.newSbomSPDXMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    BomPageMetadataDTO spdxResultDto = service.getBomPageMetadata(
        application.getId(), sbomSPDXMetadata.getSbomVersion()
    );
    assertThat(spdxResultDto.person).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(spdxResultDto.organization).isEqualTo(Arrays.asList("Example Organization"));
    assertThat(spdxResultDto.fileFormat).isEqualTo(sbomSPDXMetadata.getSpecFormat());
    assertThat(spdxResultDto.specification).isEqualTo(sbomSPDXMetadata.getSpec());
    assertThat(spdxResultDto.specVersion).isEqualTo(sbomSPDXMetadata.getSpecVersion());
  }

  @Test
  public void testGetSbomSummaryForComponents() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h1", "u1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
            "r1", "d1", "l1", 5.5, "sd1", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
            "r2", "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
            "r3", "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity1,
        "r1", "s1", "j1", "r1", "d1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity2,
        "r1", "s1", "j1", "r1", "d1");

    BomPageSbomSummaryDTO resultDto = service.getSbomSummaryForComponents(app.getId(), sbomMetadata.getSbomVersion());
    assertThat(resultDto.getAnnotatedPercentage()).isEqualTo(66.0);
    assertThat(resultDto.getLow()).isEqualTo(1);
    assertThat(resultDto.getHigh()).isEqualTo(1);
    assertThat(resultDto.getMedium()).isEqualTo(1);
    assertThat(resultDto.getCritical()).isEqualTo(0);
    assertThat(resultDto.getDependencyType().getUnspecified()).isEqualTo(1);
  }

  @Test
  public void testGetSbomSummaryForComponents_noSbomFound() {
    Application app = tempEntity.newApplicationWithParent();
    Application app1 = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.getSbomSummaryForComponents(app1.getId(), sbomMetadata.getSbomVersion()))
        .withMessageContaining("Cannot find version " +
            sbomMetadata.getSbomVersion() + " for application with ID " + app1.getId() + ".");
  }

  @Test
  public void testGetSbomSummaryForComponents_noComponentsFound() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();

    BomPageSbomSummaryDTO bomPageSbomSummaryDTO = service.getSbomSummaryForComponents(
        app.getId(), sbomMetadata.getSbomVersion());
    assertThat(bomPageSbomSummaryDTO.getNone()).isEqualTo(null);
    assertThat(bomPageSbomSummaryDTO.getLow()).isEqualTo(null);
    assertThat(bomPageSbomSummaryDTO.getMedium()).isEqualTo(null);
    assertThat(bomPageSbomSummaryDTO.getHigh()).isEqualTo(null);
    assertThat(bomPageSbomSummaryDTO.getCritical()).isEqualTo(null);
    assertThat(bomPageSbomSummaryDTO.getDependencyType()).isEqualTo(null);
    assertThat(bomPageSbomSummaryDTO.getAnnotatedPercentage()).isEqualTo(null);
  }

  @Test
  public void testGetSbomSummaryForComponents_DepedencyType() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h1", "u1", "D");
    ThirdPartyFileCoordinate thirdPartyFileCoordinate2 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h1", "u1", "T");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r1", "d1", "l1", 5.5, "sd1", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r2", "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r3", "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r1", "d1", "l1", 5.5, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r2", "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r3", "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity1,
        "r1", "s1", "j1", "r1", "d1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity2,
        "r1", "s1", "j1", "r1", "d1");

    BomPageSbomSummaryDTO resultDto = service.getSbomSummaryForComponents(app.getId(), sbomMetadata.getSbomVersion());
    assertThat(resultDto.getAnnotatedPercentage()).isEqualTo(33.3);
    assertThat(resultDto.getLow()).isEqualTo(2);
    assertThat(resultDto.getHigh()).isEqualTo(2);
    assertThat(resultDto.getMedium()).isEqualTo(2);
    assertThat(resultDto.getCritical()).isEqualTo(0);
    assertThat(resultDto.getDependencyType().getUnspecified()).isEqualTo(0);
    assertThat(resultDto.getDependencyType().getDirect()).isEqualTo(1);
    assertThat(resultDto.getDependencyType().getTransitive()).isEqualTo(1);
  }
}
