/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.brain.migration.DisplayNameForFileCoordinateAsyncDbMigration;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.BomPageSbomSummaryDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilityAnalysisForSbomVersion;
import com.sonatype.insight.brain.variant.AbstractComponentPgTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.CPE_MATCH;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.PRIMARY;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.SECONDARY;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.FAST_TRACK;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.PUBLIC_RESEARCH;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.VENDOR_RESEARCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentPgTest
public class SbomComponentsServiceTest
    extends AbstractComponentPgTest
{
  @Inject
  private SbomComponentsService service;

  @Inject
  private InsightWork work;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  private Application app;

  private Organization org;

  @BeforeEach
  public void before() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplicationWithParent(org);
  }

  @Test
  public void testGetSbomComponentDetails() throws IOException {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE,
            thirdPartyFile.getFilename());
    ThirdPartyFileCoordinate componentA =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1", "1249e25aebb15358bedd",
            "pkg:f1/group/n1@v1?type=jar", "componentRef-" + RandomStringUtils.insecure().nextAlphabetic(2));

    ThirdPartyFileCoordinate componentB =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s2", "f2", "n2", "v2", "1249e25aebb15358bedw",
            "pkg:f2/group/n2@v2?type=jar", "componentRef-" + RandomStringUtils.insecure().nextAlphabetic(2));
    ThirdPartyCoordinateSecurity vulnerabilityA =
        tempEntity.newThirdPartyCoordinateSecurity(componentA, "cve1", "d1", "l1", 9, "f1", "v1", "cvs1", "sd1",
            "cwes1", "m1", "r1", "ad1", "SBOM");
    ThirdPartyCoordinateSecurity vulnerabilityB =
        tempEntity.newThirdPartyCoordinateSecurity(componentA, "cve2", "d2", "l2", 7, "f2", "v2", "cvs2", "sd2",
            "cwes2", "m2", "r2", "ad2", "SBOM,Sonatype");
    ThirdPartyCoordinateSecurity vulnerabilityC =
        tempEntity.newThirdPartyCoordinateSecurity(componentA, "cve3", null, "d3", "l3", 5, "f3", "v3", "cvs3", "sd3",
            "cwes3", "m3", "r3", "ad3", "Sonatype", VENDOR_RESEARCH.getId(), PRIMARY.getId());
    ThirdPartyCoordinateSecurity vulnerabilityD =
        tempEntity.newThirdPartyCoordinateSecurity(componentB, "cve1", null, "d1", "l1", 6, "f1", "v1", "cvs1", "sd1",
            "cwes1", "m1", "r1", "ad1", "SBOM", FAST_TRACK.getId(), SECONDARY.getId());
    ThirdPartyCoordinateSecurity vulnerabilityE =
        tempEntity.newThirdPartyCoordinateSecurity(componentB, "cve2", null, "d2", "l2", 7, "f2", "v2", "cvs2", "sd2",
            "cwes2", "m2", "r2", "ad2", "SBOM,Sonatype", PUBLIC_RESEARCH.getId(), CPE_MATCH.getId());

    ThirdPartyVulnerabilityExploitabilityExchange vexA =
        tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityA, "cve1", "resolved",
            "code_not_reachable", "response", "detail");
    ThirdPartyVulnerabilityExploitabilityExchange vexB =
        tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityB, "cve2", "resolved",
            "code_not_reachable", "response", "detail");
    ThirdPartyVulnerabilityExploitabilityExchange vexE =
        tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityE, "cve2", "resolved",
            "code_not_reachable", "response", "detail");
    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsServiceTest", tempDir), reportFile);
    CDPSbomComponentDetailsDTO actualA =
        service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), componentA.getHash());

    CDPSbomComponentDetailsDTO actualB =
        service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), componentB.getHash());

    assertSbomComponentDetailsDTO(actualA, componentA, sbomMetadata);
    assertThat(actualA.getPolicyViolationSummary().getCritical()).isEqualTo(1);
    assertComponentSummary(actualA, 9, 1, 1, 1);
    assertThat(actualA.getDisclosedVulnerabilities()).hasSize(2);
    assertVulnerabilities(actualA.getDisclosedVulnerabilities().get(0), vulnerabilityA, vexA.getState(),
        vexA.getJustification(), vexA.getResponse(), vexA.getDetail(), vexA.getUpdatedAt(),
        vexA.getLastUpdatedByWithoutRealm());
    assertVulnerabilities(actualA.getDisclosedVulnerabilities().get(1), vulnerabilityB, vexB.getState(),
        vexB.getJustification(), vexB.getResponse(), vexB.getDetail(), vexB.getUpdatedAt(),
        vexB.getLastUpdatedByWithoutRealm());
    assertThat(actualA.getSonatypeIdentifiedVulnerabilities()).hasSize(1);
    assertVulnerabilities(actualA.getSonatypeIdentifiedVulnerabilities().get(0), vulnerabilityC, null, null, null, null,
        null, null);

    assertSbomComponentDetailsDTO(actualB, componentB, sbomMetadata);
    assertComponentSummary(actualB, 7, 1, 1, 0);
    assertThat(actualB.getDisclosedVulnerabilities()).hasSize(2);
    assertVulnerabilities(actualB.getDisclosedVulnerabilities().get(0), vulnerabilityD, null, null, null, null, null,
        null);
    assertVulnerabilities(actualB.getDisclosedVulnerabilities().get(1), vulnerabilityE, vexE.getState(),
        vexE.getJustification(), vexE.getResponse(), vexE.getDetail(), vexE.getUpdatedAt(),
        vexE.getLastUpdatedByWithoutRealm());
    assertThat(actualB.getSonatypeIdentifiedVulnerabilities()).isEmpty();
    assertThat(actualB.getPolicyViolationSummary().getCritical()).isEqualTo(1);
    assertThat(actualB.getPolicyViolationSummary().getSevere()).isEqualTo(1);
  }

  @Test
  public void testGetSbomComponentDetails_LatestPreviousAnnotation() throws IOException {
    ThirdPartyFile thirdPartyFilePrevious = tempEntity.newThirdPartyFile();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);

    long now = System.currentTimeMillis();
    ThirdPartySbomMetadata sbomMetadataPrevious =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFilePrevious.getId(), app.getId(), "1.0.0", ACTIVE,
            thirdPartyFilePrevious.getFilename(), "CycloneDx", "XML", "1.5", new Date(now - 1));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), "1.0.1", ACTIVE,
            thirdPartyFile.getFilename(), "CycloneDx", "XML", "1.5", new Date(now + 1));
    ThirdPartyFileCoordinate previousComponentA =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFilePrevious, "s1", "f1", "n1", "v1", "1249e25aebb15358bedd",
            "pkg:f1/group/n1@v1?type=jar", Collections.emptyList(), Collections.emptyList(), null);
    ThirdPartyFileCoordinate componentA =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1", "1249e25aebb15358bedd",
            "pkg:f1/group/n1@v1?type=jar", Collections.emptyList(), Collections.emptyList(), null);
    ThirdPartyFileCoordinate componentB =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s2", "f2", "n2", "v2", "1249e25aebb15358bedw",
            "pkg:f2/group/n2@v2?type=jar");
    ThirdPartyCoordinateSecurity previousVulnerabilityA =
        tempEntity.newThirdPartyCoordinateSecurity(previousComponentA, "cve1", "d1", "l1", 9, "f1", "v1", "cvs1", "sd1",
            "cwes1", "m1", "r1", "ad1", "SBOM");
    ThirdPartyCoordinateSecurity vulnerabilityA =
        tempEntity.newThirdPartyCoordinateSecurity(componentA, "cve1", "d1", "l1", 9, "f1", "v1", "cvs1", "sd1",
            "cwes1", "m1", "r1", "ad1", "SBOM");
    ThirdPartyCoordinateSecurity vulnerabilityB =
        tempEntity.newThirdPartyCoordinateSecurity(componentA, "cve2", "d2", "l2", 7, "f2", "v2", "cvs2", "sd2",
            "cwes2", "m2", "r2", "ad2", "SBOM,Sonatype");
    ThirdPartyCoordinateSecurity vulnerabilityC =
        tempEntity.newThirdPartyCoordinateSecurity(previousComponentA, "cve3", "d3", "l3", 5, "f3", "v3", "cvs3", "sd3",
            "cwes3", "m3", "r3", "ad3", "Sonatype");
    tempEntity.newThirdPartyCoordinateSecurity(componentA, "cve3", "d3", "l3", 5, "f3", "v3", "cvs3", "sd3",
        "cwes3", "m3", "r3", "ad3", "Sonatype");
    tempEntity.newThirdPartyCoordinateSecurity(componentB, "cve1", "d1", "l1", 6, "f1", "v1", "cvs1", "sd1",
        "cwes1", "m1", "r1", "ad1", "SBOM");
    ThirdPartyCoordinateSecurity vulnerabilityE =
        tempEntity.newThirdPartyCoordinateSecurity(componentB, "cve2", "d2", "l2", 7, "f2", "v2", "cvs2", "sd2",
            "cwes2", "m2", "r2", "ad2", "SBOM,Sonatype");

    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(previousVulnerabilityA, "cve1", "resolved1a",
        "code_not_reachable1a", "response1a", "detail1a");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityA, "cve1", "resolved1b",
        "code_not_reachable1b", "response1b", "detail1b");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityB, "cve2", "resolved2a",
        "code_not_reachable2a", "response2a", "detail2a");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityC, "cve3", "resolved3a",
        "code_not_reachable3a", "response3a", "detail3a");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerabilityE, "cve2", "resolved2b",
        "code_not_reachable2b", "response2b", "detail2b");

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsServiceTest", tempDir), reportFile);
    CDPSbomComponentDetailsDTO actualA =
        service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), componentA.getHash());

    CDPSbomComponentDetailsDTO actualB =
        service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), componentB.getHash());

    VulnerabilityAnalysisForSbomVersion previousAnnotation3a =
        new VulnerabilityAnalysisForSbomVersion(sbomMetadataPrevious.getSbomVersion(), "resolved3a",
            "code_not_reachable3a", "response3a", null);

    assertThat(actualA.getDisclosedVulnerabilities()).hasSize(2);
    assertThat(actualA.getDisclosedVulnerabilities().get(0).getLatestPreviousAnnotation()).isNull();
    assertThat(actualA.getDisclosedVulnerabilities().get(1).getLatestPreviousAnnotation()).isNull();

    assertThat(actualA.getSonatypeIdentifiedVulnerabilities()).hasSize(1);
    assertThat(actualA.getSonatypeIdentifiedVulnerabilities().get(0).getLatestPreviousAnnotation()).isNotNull()
        .usingRecursiveComparison()
        .ignoringFields("detail")
        .isEqualTo(previousAnnotation3a);
    assertThat(actualA.getSonatypeIdentifiedVulnerabilities().get(0).getLatestPreviousAnnotation().getDetail())
        .endsWith("'detail3a'");

    assertThat(actualB.getDisclosedVulnerabilities()).hasSize(2);
    assertThat(actualB.getDisclosedVulnerabilities().get(0).getLatestPreviousAnnotation()).isNull();
    assertThat(actualB.getDisclosedVulnerabilities().get(1).getLatestPreviousAnnotation()).isNull();

    assertThat(actualB.getSonatypeIdentifiedVulnerabilities()).isEmpty();
  }

  @Test
  public void testGetSbomComponentDetails_LatestPreviousAnnotation_Components_SameAndDifferent_VersionsMatched() throws IOException {
    ThirdPartyFile thirdPartyFilePrevious = tempEntity.newThirdPartyFile();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);

    long now = System.currentTimeMillis();

    // SBOM versions
    tempEntity.newThirdPartySbomMetadata(thirdPartyFilePrevious.getId(), app.getId(), "1.0.0", ACTIVE,
        thirdPartyFilePrevious.getFilename(), "CycloneDx", "XML", "1.6", new Date(now - 1));
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), "1.0.1", ACTIVE,
            thirdPartyFile.getFilename(), "CycloneDx", "XML", "1.6", new Date(now + 1));

    // Components for previous sbom
    ThirdPartyFileCoordinate componentAv1PreviousSbom =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFilePrevious, "s1", "f1", "componentA", "v1",
            "1249e25aebb15358bedd",
            "pkg:f1/group/componentA@v1?type=jar", Collections.emptyList(), Collections.emptyList(), null);

    ThirdPartyFileCoordinate componentBv1PreviousSbom =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFilePrevious, "s1", "f2", "componentB", "v1",
            "1249e25aebb15358b201",
            "pkg:f1/group/componentB@v1?type=jar", Collections.emptyList(), Collections.emptyList(), null);

    // Components for latest sbom
    ThirdPartyFileCoordinate componentAv1LatestSbom =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "componentA", "v1", "1249e25aebb15358bedd",
            "pkg:f1/group/componentA@v1?type=jar", Collections.emptyList(), Collections.emptyList(), null);

    ThirdPartyFileCoordinate componentAv2LatestSbom =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "componentA",
            "v2", "1249e25aebb15358be02", "pkg:f1/group/componentA@v2?type=jar",
            Collections.emptyList(), Collections.emptyList(), null);

    ThirdPartyFileCoordinate componentBv2LatestSbom =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s2", "f2", "componentB", "v2", "1249e25aebb15358b202",
            "pkg:f2/group/componentB@v2?type=jar", Collections.emptyList(), Collections.emptyList(), null);

    // Vulnerabilities for previous Sbom
    ThirdPartyCoordinateSecurity cve1CompAv1PreviousSbom = tempEntity.newThirdPartyCoordinateSecurity(
        componentAv1PreviousSbom, "CVE-1", "d1", "l1", 9, "f1", "v1",
        "cvs1", "sd1", "cwes1", "m1", "r1", "ad1", "SBOM");

    ThirdPartyCoordinateSecurity cve2CompBv1PreviousSbom = tempEntity.newThirdPartyCoordinateSecurity(
        componentBv1PreviousSbom, "CVE-2", "d2", "l2", 9, "f2", "v2",
        "cvs2", "sd2", "cwes2", "m2", "r2", "ad2", "SBOM");

    // Vulnerabilities for latest Sbom
    tempEntity.newThirdPartyCoordinateSecurity(
        componentAv1LatestSbom, "CVE-1", "d1", "l1", 9, "f1", "v1",
        "cvs1", "sd1", "cwes1", "m1", "r1", "ad1", "SBOM");

    ThirdPartyCoordinateSecurity cve1CompAv2LatestSbom = tempEntity.newThirdPartyCoordinateSecurity(
        componentAv2LatestSbom, "CVE-1", "d2", "l2", 9, "f2", "v2",
        "cvs2", "sd2", "cwes2", "m2", "r2", "ad2", "SBOM");

    tempEntity.newThirdPartyCoordinateSecurity(
        componentBv2LatestSbom, "CVE-2", "d2", "l2", 9, "f2", "v2",
        "cvs2", "sd2", "cwes2", "m2", "r2", "ad2", "SBOM");

    // VEX for component A v1 on previous sbom version
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(cve1CompAv1PreviousSbom, "CVE-1", "resolved1a",
        "code_not_reachable1a", "response1a", "detail1a");

    //// VEX for component A v2 on latest sbom version
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(cve1CompAv2LatestSbom, "CVE-1", "resolved2a",
        "code_not_reachable2a", "response2a", "detail2a");

    // VEX for component B v1 on previous sbom version
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(cve2CompBv1PreviousSbom, "CVE-2", "resolved2b",
        "code_not_reachable2b", "response2b", "detail2b");

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsServiceTest", tempDir), reportFile);
    CDPSbomComponentDetailsDTO actualA =
        service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), componentAv1LatestSbom.getHash());

    CDPSbomComponentDetailsDTO actualAv2 =
        service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), componentAv2LatestSbom.getHash());

    CDPSbomComponentDetailsDTO actualB =
        service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), componentBv2LatestSbom.getHash());

    // Matching component A with same version across 2 different SBOMs. Previous vulnerability found
    assertThat(actualA.getDisclosedVulnerabilities()).hasSize(1);
    VulnerabilityDetailsDTO cve1Found = actualA.getDisclosedVulnerabilities().get(0);
    assertThat(cve1Found.getIssue()).isEqualTo("CVE-1");
    assertVulnerabilityAnalysisForSbomVersion(cve1Found.getLatestPreviousAnnotation(), "1.0.0", "resolved1a",
        "code_not_reachable1a", "response1a", "detail1a");

    // Component A v2 is only defined in the most recent SBOM. No previous annotation found
    VulnerabilityDetailsDTO cve1FoundAgain = actualAv2.getDisclosedVulnerabilities().get(0);
    assertThat(cve1FoundAgain.getIssue()).isEqualTo("CVE-1");
    assertThat(cve1FoundAgain.getLatestPreviousAnnotation()).isNull();

    // Matching component B with different versions across 2 different SBOMs. Previous vulnerability found
    assertThat(actualB.getDisclosedVulnerabilities()).hasSize(1);
    VulnerabilityDetailsDTO cve2Found = actualB.getDisclosedVulnerabilities().get(0);
    assertThat(cve2Found.getIssue()).isEqualTo("CVE-2");
    assertVulnerabilityAnalysisForSbomVersion(cve2Found.getLatestPreviousAnnotation(), "1.0.0", "resolved2b",
        "code_not_reachable2b", "response2b", "detail2b");
  }

  @Test
  public void testGetSbomComponentDetails_AppNotFound() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE,
            thirdPartyFile.getFilename());
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");

    assertThrows(NotFoundException.class,
        () -> service.getSbomComponentDetails("anyApp", sbomMetadata.getSbomVersion(), component.getHash()),
        "Could not find application with id anyApp");
  }

  @Test
  public void testGetSbomComponentDetails_SbomVersionNotFound() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE,
        thirdPartyFile.getFilename());
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");

    assertThrows(NotFoundException.class,
        () -> service.getSbomComponentDetails(app.getId(), "anySbomVersion", component.getHash()),
        "Could not find SBOM version anySbomVersion for application " + app.getId());
  }

  @Test
  public void testGetSbomComponentDetails_ComponentHashNotFound() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE,
            thirdPartyFile.getFilename());
    tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");

    assertThrows(NotFoundException.class,
        () -> service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), "anyHash"),
        "Could not find component by hash anyHash");
  }

  @Test
  public void testGetSbomComponentDetails_NoVulnerabilities_NoOccurrences() throws IOException {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE,
            thirdPartyFile.getFilename());
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1", "1249e25aebb15358bedd",
            "pkg:f1/group/n1@v1?type=jar", "componentRef-" + RandomStringUtils.insecure().nextAlphabetic(2));
    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsServiceTest", tempDir), reportFile);
    CDPSbomComponentDetailsDTO actual =
        service.getSbomComponentDetails(app.getId(), sbomMetadata.getSbomVersion(), component.getHash());
    assertSbomComponentDetailsDTO(actual, component, sbomMetadata);
    assertThat(actual.getVulnerabilitySummary().getHighestCvssScore()).isZero();
    assertThat(actual.getVulnerabilitySummary().getVerifiedVulnerabilitiesCount()).isZero();
    assertThat(actual.getVulnerabilitySummary().getUnverifiedVulnerabilitiesCount()).isZero();
    assertThat(actual.getOccurrences()).isEmpty();
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
    assertThat(actual.getFormat()).isEqualTo(component.getFormat());
    assertThat(actual.getDisplayName()).isEqualTo(component.getDisplayName());
    assertThat(actual.getPackageUrl()).isEqualTo(component.getPackageUrl());
    assertThat(actual.getComponentIdentifier()).isNotNull();
    assertThat(actual.getComponentIdentifier().getFormat()).isEqualTo(component.getFormat());
    assertThat(actual.getFileCoordinateId()).isNull();
    assertThat(actual.getComponentRef()).startsWith("componentRef-");
    assertThat(actual.getMetadata()).isNotNull();
    assertThat(actual.getMetadata().getApplicationName()).isEqualTo(app.getName());
    assertThat(actual.getMetadata().getOrganizationName()).isEqualTo(org.getName());
    assertThat(actual.getMetadata().getSbomCreationTime()).isEqualTo(sbom.getCreatedAt());
    if (CollectionUtils.isEmpty(component.getOccurrencesList())) {
      assertThat(actual.getOccurrences()).isEmpty();
    }
    else {
      assertThat(actual.getOccurrences()).isEqualTo(component.getOccurrencesList());
    }
    assertThat(actual.getMatchState()).isEqualTo(component.getMatchStateId());
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
      String vexResponse,
      String vexDetail,
      Date updatedAt,
      String lastUpdatedBy)
  {
    assertThat(actual.getCvssScore()).isEqualTo(vulnerability.getSeverity());
    assertThat(actual.getIssue()).isEqualTo(vulnerability.getRefId());
    assertThat(actual.getDescription()).isEqualTo(vulnerability.getDescription());
    assertThat(actual.getAnalysisStatus()).isEqualTo(vexState);
    assertThat(actual.getJustification()).isEqualTo(vexJustification);
    assertThat(actual.getResponse()).isEqualTo(vexResponse);
    assertThat(actual.getDetails()).isEqualTo(vexDetail);
    assertThat(actual.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(actual.getLastUpdatedBy()).isEqualTo(lastUpdatedBy);
    assertThat(actual.getIdentificationSources()).isEqualTo(vulnerability.getIdentificationSources());
    assertThat(actual.getResearchType()).isEqualTo(vulnerability.getResearchType());
    assertThat(actual.getDetectionType()).isEqualTo(vulnerability.getDetectionType());
  }

  @Test
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
    assertThat(resultDto.originalFile).isEqualTo(sbomMetadata.getOriginalBinaryFileName());

    // Test SPDX Format
    ThirdPartySbomMetadata sbomSPDXMetadata = SbomMetadataBuilder.newSbomSPDXMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    BomPageMetadataDTO spdxResultDto = service.getBomPageMetadata(
        application.getId(), sbomSPDXMetadata.getSbomVersion());
    assertThat(spdxResultDto.person).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(spdxResultDto.organization).isEqualTo(Arrays.asList("Example Organization"));
    assertThat(spdxResultDto.fileFormat).isEqualTo(sbomSPDXMetadata.getSpecFormat());
    assertThat(spdxResultDto.specification).isEqualTo(sbomSPDXMetadata.getSpec());
    assertThat(spdxResultDto.specVersion).isEqualTo(sbomSPDXMetadata.getSpecVersion());
  }

  @Test
  public void testGetBomPageMetadata_DisplayName_NotMigrated() {
    migrationTrackerDAO.deleteById(DisplayNameForFileCoordinateAsyncDbMigration.class.getSimpleName());
    assertThat(migrationTrackerDAO.isTrackerPresent(DisplayNameForFileCoordinateAsyncDbMigration.class.getSimpleName()))
        .isFalse();
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();

    BomPageMetadataDTO result = service.getBomPageMetadata(application.getId(), sbomMetadata.getSbomVersion());

    assertThat(result).isNotNull();
    assertThat(result.displayNameSortingEnabled).isFalse();
  }

  @Test
  public void testGetBomPageMetadata_DisplayName_Migrated() {
    assertThat(migrationTrackerDAO.isTrackerPresent(DisplayNameForFileCoordinateAsyncDbMigration.class.getSimpleName()))
        .isTrue();
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();

    BomPageMetadataDTO result = service.getBomPageMetadata(application.getId(), sbomMetadata.getSbomVersion());

    assertThat(result).isNotNull();
    assertThat(result.displayNameSortingEnabled).isTrue();
  }

  @Test
  public void testGetSbomSummaryForComponents() throws IOException {
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
            "r1", sbomMetadata.getId(), "d1", "l1", 5.5, "sd1", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
            "r2", sbomMetadata.getId(), "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r3", sbomMetadata.getId(), "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity1,
        "r1", "s1", "j1", "r1", "d1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity2,
        "r2", "s1", "j1", "r1", "d1");

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsServiceTest", tempDir), reportFile);

    BomPageSbomSummaryDTO resultDto = service.getSbomSummaryForComponents(app.getId(), sbomMetadata.getSbomVersion());
    assertThat(resultDto.getLow()).isEqualTo(1);
    assertThat(resultDto.getHigh()).isEqualTo(1);
    assertThat(resultDto.getMedium()).isEqualTo(1);
    assertThat(resultDto.getCritical()).isEqualTo(0);
    assertThat(resultDto.getReleaseStatusPercentage()).isEqualTo(100.0);
    assertThat(resultDto.getDependencyType().getUnspecified()).isEqualTo(1);
  }

  @Test
  public void testGetSbomSummaryForComponents_noCriticalHighVulnerability() throws IOException {
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
            "r1", sbomMetadata.getId(), "d1", "l1", 5.5, "sd1", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
            "r2", sbomMetadata.getId(), "d2", "l2", 2.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
        "r3", sbomMetadata.getId(), "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity1,
        "r1", "s1", "j1", "r1", "d1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity2,
        "r2", "s1", "j1", "r1", "d1");

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsServiceTest", tempDir), reportFile);

    BomPageSbomSummaryDTO resultDto = service.getSbomSummaryForComponents(app.getId(), sbomMetadata.getSbomVersion());
    assertThat(resultDto.getLow()).isEqualTo(2);
    assertThat(resultDto.getHigh()).isEqualTo(0);
    assertThat(resultDto.getMedium()).isEqualTo(1);
    assertThat(resultDto.getCritical()).isEqualTo(0);
    assertThat(resultDto.getReleaseStatusPercentage()).isEqualTo(100.0);
    assertThat(resultDto.getDependencyType().getUnspecified()).isEqualTo(1);
  }

  @Test
  public void testGetSbomSummaryForComponents_noPoliciesViolationsFound() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();

    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(), "s", "SPDX", "n1", "v1", "h1", "u1");

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsServiceTest/noPolicyViolations", tempDir), reportFile);

    BomPageSbomSummaryDTO resultDto = service.getSbomSummaryForComponents(app.getId(), sbomMetadata.getSbomVersion());
    assertThat(resultDto.getPolicyViolationSummary().getCritical()).isZero();
    assertThat(resultDto.getPolicyViolationSummary().getModerate()).isZero();
    assertThat(resultDto.getPolicyViolationSummary().getSevere()).isZero();
    assertThat(resultDto.getPolicyViolationSummary().getLow()).isZero();
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
    assertThat(bomPageSbomSummaryDTO.getReleaseStatusPercentage()).isEqualTo(null);
    assertThat(bomPageSbomSummaryDTO.getPolicyViolationSummary()).isEqualTo(null);
  }

  @Test
  public void testGetSbomSummaryForComponents_DependencyType() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h1", "u1", ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate2 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h1", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity1 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r1", sbomMetadata.getId(), "d1", "l1", 5.5, "sd1", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r2", sbomMetadata.getId(), "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
        "r3", sbomMetadata.getId(), "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
        "r1", sbomMetadata.getId(), "d1", "l1", 5.5, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
        "r2", sbomMetadata.getId(), "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
        "r3", sbomMetadata.getId(), "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity1,
        "r1", "s1", "j1", "r1", "d1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity2,
        "r1", "s1", "j1", "r1", "d1");

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsServiceTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    BomPageSbomSummaryDTO resultDto = service.getSbomSummaryForComponents(app.getId(), sbomMetadata.getSbomVersion());
    assertThat(resultDto.getLow()).isEqualTo(2);
    assertThat(resultDto.getHigh()).isEqualTo(2);
    assertThat(resultDto.getMedium()).isEqualTo(2);
    assertThat(resultDto.getCritical()).isEqualTo(0);
    assertThat(resultDto.getDependencyType().getUnspecified()).isEqualTo(0);
    assertThat(resultDto.getDependencyType().getDirect()).isEqualTo(1);
    assertThat(resultDto.getReleaseStatusPercentage()).isEqualTo(50.0);
    assertThat(resultDto.getDependencyType().getTransitive()).isEqualTo(1);
  }

  @Test
  public void testGetSbomSummaryForComponents_PolicyThreatLevels() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h1", "u1", ThirdPartyDependencyType.DIRECT);

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsServiceTest", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    BomPageSbomSummaryDTO resultDto = service.getSbomSummaryForComponents(app.getId(), sbomMetadata.getSbomVersion());
    assertThat(resultDto.getPolicyViolationSummary().getCritical()).isEqualTo(2);
    assertThat(resultDto.getPolicyViolationSummary().getModerate()).isEqualTo(1);
    assertThat(resultDto.getPolicyViolationSummary().getSevere()).isEqualTo(2);
    assertThat(resultDto.getPolicyViolationSummary().getLow()).isEqualTo(0);
  }

  @Test
  public void testGetSbomSummaryForComponents_NoPolicyViolations() throws IOException {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h1", "u1", ThirdPartyDependencyType.DIRECT);

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsServiceTest/noPolicyViolations", tempDir), reportFile);

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, thirdPartyScan.getScanId());

    BomPageSbomSummaryDTO resultDto = service.getSbomSummaryForComponents(app.getId(), sbomMetadata.getSbomVersion());
    assertThat(resultDto.getPolicyViolationSummary().getCritical()).isEqualTo(0);
    assertThat(resultDto.getPolicyViolationSummary().getModerate()).isEqualTo(0);
    assertThat(resultDto.getPolicyViolationSummary().getSevere()).isEqualTo(0);
    assertThat(resultDto.getPolicyViolationSummary().getLow()).isEqualTo(0);
  }

  private void assertVulnerabilityAnalysisForSbomVersion(
      VulnerabilityAnalysisForSbomVersion retrievedVex,
      String sbomVersion,
      String analysisStatus,
      String justification,
      String response,
      String detail)
  {
    assertThat(retrievedVex).isNotNull()
        .extracting(VulnerabilityAnalysisForSbomVersion::getSbomVersion,
            VulnerabilityAnalysisForSbomVersion::getAnalysisStatus,
            VulnerabilityAnalysisForSbomVersion::getJustification,
            VulnerabilityAnalysisForSbomVersion::getResponse)
        .containsExactly(sbomVersion, analysisStatus, justification, response);
    assertThat(retrievedVex.getDetail()).contains(detail);
  }
}
