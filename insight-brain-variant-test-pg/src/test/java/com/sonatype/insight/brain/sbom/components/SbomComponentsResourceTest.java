/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.BomPageSbomSummaryDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.sbom.components.SbomComponentsResource.SBOM_SUMMARY_PATH;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType.CPE_MATCH;
import static com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType.PUBLIC_RESEARCH;
import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class SbomComponentsResourceTest
{
  private IqTestContext ctx;

  private InsightWork work;

  private Application app;

  private Organization org;

  @BeforeEach
  void before() throws Exception {
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplicationWithParent(org);
    ctx.setFeatures(LicensedFeature.SBOM_MANAGER);
    work = ctx.lookup(InsightWork.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(SbomComponentsResource.RESOURCE_BASE_PATH);
  }

  @Test
  void testGetComponentDetails() throws Exception {
    ThirdPartyFile thirdPartyFile = ctx.tempEntity().newThirdPartyFile();
    ThirdPartyScan thirdPartyScan = ctx.tempEntity().newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata sbomMetadata =
        ctx.tempEntity()
            .newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE,
                thirdPartyFile.getFilename());
    ThirdPartyFileCoordinate component =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate("86163fcc32524261bfd2bdbedb7eae43", thirdPartyFile, "s1",
                "f1", "n1", "v1", "1249e25aebb15358bedd",
                "pkg:f1/group/n1@v1?type=jar", List.of("dependency:/bom.json/pkg:f1\\n1@v1"), null,
                "similar");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(component, "cve", null, "d1", "l1", 9, "f1", "v1", "cvs1", "d1",
            "cwes1", "m1", "r1", "ad1", "SBOM", PUBLIC_RESEARCH.getId(), CPE_MATCH.getId());
    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsResourceTest", ctx.tempFolder()), reportFile);

    HttpResponse response = restRequest()
        .parameter(app.getId(), sbomMetadata.getSbomVersion(), component.getHash())
        .path(SbomComponentsResource.COMPONENT_DETAILS_PATH)
        .get();
    ctx.assertResponseStatus(200, response);
    CDPSbomComponentDetailsDTO actual = response.getBody(CDPSbomComponentDetailsDTO.class);
    assertThat(actual).isNotNull();
    assertThat(actual.getName()).isEqualTo(component.getName());
    assertThat(actual.getHash()).isEqualTo(component.getHash());
    assertThat(actual.getVersion()).isEqualTo(component.getVersion());
    assertThat(actual.getFormat()).isEqualTo(component.getFormat());
    assertThat(actual.getDisplayName()).isEqualTo(component.getDisplayName());
    assertThat(actual.getComponentIdentifier()).isNotNull();
    assertThat(actual.getComponentIdentifier().getFormat()).isEqualTo(component.getFormat());
    assertThat(actual.getPackageUrl()).isEqualTo(component.getPackageUrl());
    assertThat(actual.getMetadata()).isNotNull();
    assertThat(actual.getMetadata().getApplicationName()).isEqualTo(app.getName());
    assertThat(actual.getMetadata().getOrganizationName()).isEqualTo(org.getName());
    assertThat(actual.getMetadata().getSbomCreationTime()).isEqualTo(sbomMetadata.getCreatedAt());
    assertThat(actual.getVulnerabilitySummary()).isNotNull();
    assertThat(actual.getVulnerabilitySummary().getHighestCvssScore()).isEqualTo(9);
    assertThat(actual.getVulnerabilitySummary().getVerifiedVulnerabilitiesCount()).isZero();
    assertThat(actual.getVulnerabilitySummary().getUnverifiedVulnerabilitiesCount()).isEqualTo(1);
    assertThat(actual.getDisclosedVulnerabilities().size()).isOne();
    assertThat(actual.getDisclosedVulnerabilities().get(0).getResearchType()).isEqualTo(PUBLIC_RESEARCH.getId());
    assertThat(actual.getDisclosedVulnerabilities().get(0).getDetectionType()).isEqualTo(CPE_MATCH.getId());
    assertThat(actual.getSonatypeIdentifiedVulnerabilities()).isEmpty();
    assertThat(actual.getOccurrences()).isNotEmpty().hasSize(1);
    assertThat(actual.getOccurrences()).isEqualTo(component.getOccurrencesList());
    assertThat(actual.getMatchState()).isEqualTo(component.getMatchStateId());
    assertThat(actual.getPolicyViolationSummary().getCritical()).isEqualTo(1);
  }

  @Test
  void testGetComponentDetails_NotFound() throws Exception {
    HttpResponse response = restRequest()
        .parameter(app.getId(), "any", "any")
        .path(SbomComponentsResource.COMPONENT_DETAILS_PATH)
        .get();
    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testGetComponentDetails_Unlicensed() throws Exception {
    ctx.setMissingFeature(LicensedFeature.SBOM_MANAGER);
    HttpResponse response = restRequest()
        .parameter(app.getId(), "any", "any")
        .path(SbomComponentsResource.COMPONENT_DETAILS_PATH)
        .get();
    ctx.assertResponseStatus(402, response);
  }

  @Test
  void testGetSbomMetadataNotFound() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    HttpResponse response = restRequest()
        .path(SbomComponentsResource.SBOM_METADATA_PATH)
        .parameter(app.getId(), "fake-version")
        .get();

    ctx.assertResponseStatus(Status.NOT_FOUND.getStatusCode(), response);
    assertThat(response.getBodyText())
        .isEqualTo(String.format("Cannot find version %s for application with ID %s.", "fake-version", app.getId()));
  }

  @Test
  void testGetSbomMetadataSuccessful() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = ctx.tempEntity().newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(ctx.daoFactory())
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    HttpResponse response = restRequest()
        .path(SbomComponentsResource.SBOM_METADATA_PATH)
        .parameter(app.getId(), sbomMetadata.getSbomVersion())
        .get();

    ctx.assertResponseStatus(Status.OK.getStatusCode(), response);
    BomPageMetadataDTO resultDto = response.getBody(BomPageMetadataDTO.class);
    assertThat(resultDto.fileFormat).isEqualTo(sbomMetadata.getSpecFormat());
    assertThat(resultDto.specification).isEqualTo(sbomMetadata.getSpec());
    assertThat(resultDto.specVersion).isEqualTo(sbomMetadata.getSpecVersion());
    assertThat(resultDto.author).isEqualTo(Arrays.asList("John Doe"));
    assertThat(resultDto.supplier).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(resultDto.manufacturer).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(resultDto.scanId).isEqualTo(thirdPartyScan.getScanId());
    assertThat(resultDto.isValid).isTrue();

    // Test SPDX Format
    ThirdPartySbomMetadata sbomSPDXMetadata = SbomMetadataBuilder.newSbomSPDXMetadataBuilder(ctx.daoFactory())
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    HttpResponse spdxResponse = restRequest()
        .path(SbomComponentsResource.SBOM_METADATA_PATH)
        .parameter(app.getId(), sbomSPDXMetadata.getSbomVersion())
        .get();

    ctx.assertResponseStatus(Status.OK.getStatusCode(), spdxResponse);
    BomPageMetadataDTO spdxResultDto = spdxResponse.getBody(BomPageMetadataDTO.class);
    assertThat(spdxResultDto.person).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(spdxResultDto.organization).isEqualTo(Arrays.asList("Example Organization"));
    assertThat(spdxResultDto.fileFormat).isEqualTo(sbomSPDXMetadata.getSpecFormat());
    assertThat(spdxResultDto.specification).isEqualTo(sbomSPDXMetadata.getSpec());
    assertThat(spdxResultDto.specVersion).isEqualTo(sbomSPDXMetadata.getSpecVersion());
    assertThat(spdxResultDto.isValid).isTrue();
  }

  @Test
  void testGetSbomMetadata_isValid_Null() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = ctx.tempEntity().newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(ctx.daoFactory())
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .withIsValid(null)
        .build();

    HttpResponse response = restRequest()
        .path(SbomComponentsResource.SBOM_METADATA_PATH)
        .parameter(app.getId(), sbomMetadata.getSbomVersion())
        .get();

    ctx.assertResponseStatus(Status.OK.getStatusCode(), response);
    BomPageMetadataDTO resultDto = response.getBody(BomPageMetadataDTO.class);
    assertThat(resultDto.isValid).isTrue();
  }

  @Test
  void testGetSbomSummaryForComponentsNotFound() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    HttpResponse response = restRequest()
        .path(SBOM_SUMMARY_PATH)
        .parameter(app.getId(), "fake-version")
        .get();

    ctx.assertResponseStatus(Status.NOT_FOUND.getStatusCode(), response);
    assertThat(response.getBodyText())
        .isEqualTo(String.format("Cannot find version %s for application with ID %s.", "fake-version", app.getId()));
  }

  @Test
  void testGetSbomSummaryForComponents() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = ctx.tempEntity().newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(ctx.daoFactory())
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h1", "u1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity1 =
        ctx.tempEntity()
            .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
                "r1", sbomMetadata.getId(), "d1", "l1", 5.5, "sd1", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 =
        ctx.tempEntity()
            .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
                "r2", sbomMetadata.getId(), "d2", "l2", 7.5, "sd2", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate,
            "r3", sbomMetadata.getId(), "d3", "l3", 3.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity1,
            "r1", "s1", "j1", "r1", "d1");
    ctx.tempEntity()
        .newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity2,
            "r1", "s1", "j1", "r1", "d1");

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsResourceTest", ctx.tempFolder()), reportFile);

    HttpResponse response = restRequest()
        .path(SBOM_SUMMARY_PATH)
        .parameter(app.getId(), sbomMetadata.getSbomVersion())
        .get();

    ctx.assertResponseStatus(Status.OK.getStatusCode(), response);
    BomPageSbomSummaryDTO resultDto = response.getBody(BomPageSbomSummaryDTO.class);
    assertThat(resultDto.getLow()).isEqualTo(1);
    assertThat(resultDto.getHigh()).isEqualTo(1);
    assertThat(resultDto.getMedium()).isEqualTo(1);
    assertThat(resultDto.getCritical()).isEqualTo(0);
    assertThat(resultDto.getDependencyType().getUnspecified()).isEqualTo(1);
  }

  @Test
  void testGetSbomSummaryForComponents_DependencyType() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = ctx.tempEntity().newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(ctx.daoFactory())
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h1", "u1", ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate2 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h1", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity1 =
        ctx.tempEntity()
            .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
                "r1", sbomMetadata.getId(), "d1", "l1", 5.5, "sd1", "f1");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 =
        ctx.tempEntity()
            .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
                "r2", sbomMetadata.getId(), "d2", "l2", 7.5, "sd2", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r3", sbomMetadata.getId(), "d3", "l3", 3.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r1", sbomMetadata.getId(), "d1", "l1", 5.5, "sd1", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r2", sbomMetadata.getId(), "d2", "l2", 7.5, "sd2", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r3", sbomMetadata.getId(), "d3", "l3", 3.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity1,
            "r1", "s1", "j1", "r1", "d1");
    ctx.tempEntity()
        .newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity2,
            "r1", "s1", "j1", "r1", "d1");

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsResourceTest", ctx.tempFolder()), reportFile);

    HttpResponse response = restRequest()
        .path(SBOM_SUMMARY_PATH)
        .parameter(app.getId(), sbomMetadata.getSbomVersion())
        .get();

    ctx.assertResponseStatus(Status.OK.getStatusCode(), response);
    BomPageSbomSummaryDTO resultDto = response.getBody(BomPageSbomSummaryDTO.class);
    assertThat(resultDto.getLow()).isEqualTo(2);
    assertThat(resultDto.getHigh()).isEqualTo(2);
    assertThat(resultDto.getMedium()).isEqualTo(2);
    assertThat(resultDto.getCritical()).isEqualTo(0);
    assertThat(resultDto.getDependencyType().getUnspecified()).isEqualTo(0);
    assertThat(resultDto.getDependencyType().getDirect()).isEqualTo(1);
    assertThat(resultDto.getDependencyType().getTransitive()).isEqualTo(1);
  }

  @Test
  void testGetSbomSummaryForComponents_PolicyThreatLevel() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = ctx.tempEntity().newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(ctx.daoFactory())
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h1", "u1", ThirdPartyDependencyType.DIRECT);

    File reportFile = work.getReportFile(app.getId(), thirdPartyScan.getScanId());
    FileUtils.copyURLToFile(ReportHelper
        .zipReport("/SbomComponentsResourceTest", ctx.tempFolder()), reportFile);

    HttpResponse response = restRequest()
        .path(SBOM_SUMMARY_PATH)
        .parameter(app.getId(), sbomMetadata.getSbomVersion())
        .get();

    ctx.assertResponseStatus(Status.OK.getStatusCode(), response);
    BomPageSbomSummaryDTO resultDto = response.getBody(BomPageSbomSummaryDTO.class);
    assertThat(resultDto.getPolicyViolationSummary().getCritical()).isEqualTo(2);
    assertThat(resultDto.getPolicyViolationSummary().getModerate()).isEqualTo(1);
    assertThat(resultDto.getPolicyViolationSummary().getSevere()).isEqualTo(1);
    assertThat(resultDto.getPolicyViolationSummary().getLow()).isEqualTo(0);
  }
}
