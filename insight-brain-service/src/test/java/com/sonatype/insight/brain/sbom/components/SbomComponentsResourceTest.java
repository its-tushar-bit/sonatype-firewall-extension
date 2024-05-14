/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.util.Arrays;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SbomComponentsResourceTest extends AbstractResourceTest
{
  private Application app;

  private Organization org;

  @Before
  public void before() throws Exception {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplicationWithParent(org);
    setFeatures(LicensedFeature.SBOM_MANAGER);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(SbomComponentsResource.RESOURCE_BASE_PATH);
  }

  @Test
  public void testGetComponentDetails() throws Exception {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), "ACTIVE",
            thirdPartyFile.getFilename());
    ThirdPartyFileCoordinate component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1", "h1",
            "pkg:f1/group/n1@v1?type=jar");
    tempEntity.newThirdPartyCoordinateSecurity(component, "cve", "d1", "l1", 9, "d1", "f1");

    HttpResponse response = restRequest()
        .parameter(app.getId(), sbomMetadata.getSbomVersion(), component.getHash())
        .path(SbomComponentsResource.COMPONENT_DETAILS_PATH)
        .get();
    assertResponseStatus(200, response);
    CDPSbomComponentDetailsDTO actual = response.getBody(CDPSbomComponentDetailsDTO.class);
    assertThat(actual).isNotNull();
    assertThat(actual.getName()).isEqualTo(component.getName());
    assertThat(actual.getHash()).isEqualTo(component.getHash());
    assertThat(actual.getVersion()).isEqualTo(component.getVersion());
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
    assertThat(actual.getSonatypeIdentifiedVulnerabilities()).isEmpty();
  }

  @Test
  public void testGetComponentDetails_NotFound() throws Exception {
    HttpResponse response = restRequest()
        .parameter(app.getId(), "any", "any")
        .path(SbomComponentsResource.COMPONENT_DETAILS_PATH)
        .get();
    assertResponseStatus(404, response);
  }

  @Test
  public void testGetComponentDetails_Unlicensed() throws Exception {
    setMissingFeature(LicensedFeature.SBOM_MANAGER);
    HttpResponse response = restRequest()
        .parameter(app.getId(), "any", "any")
        .path(SbomComponentsResource.COMPONENT_DETAILS_PATH)
        .get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetSbomMetadataNotFound() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    HttpResponse response = restRequest()
        .path(SbomComponentsResource.SBOM_METADATA_PATH)
        .parameter(app.getId(), "fake-version")
        .get();

    assertResponseStatus(Status.NOT_FOUND.getStatusCode(), response);
    assertThat(response.getBodyText())
        .isEqualTo(String.format("Cannot find version %s for application with ID %s.", "fake-version", app.getId()));
  }

  @Test
  public void testGetSbomMetadataSuccessful() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan = tempEntity.newThirdPartyScan();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    HttpResponse response = restRequest()
        .path(SbomComponentsResource.SBOM_METADATA_PATH)
        .parameter(app.getId(), sbomMetadata.getSbomVersion())
        .get();

    assertResponseStatus(Status.OK.getStatusCode(), response);
    BomPageMetadataDTO resultDto = response.getBody(BomPageMetadataDTO.class);
    assertThat(resultDto.fileFormat).isEqualTo(sbomMetadata.getSpecFormat());
    assertThat(resultDto.specification).isEqualTo(sbomMetadata.getSpec());
    assertThat(resultDto.specVersion).isEqualTo(sbomMetadata.getSpecVersion());
    assertThat(resultDto.author).isEqualTo(Arrays.asList("John Doe"));
    assertThat(resultDto.supplier).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(resultDto.manufacturer).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(resultDto.scanId).isEqualTo(thirdPartyScan.getScanId());

    // Test SPDX Format
    ThirdPartySbomMetadata sbomSPDXMetadata = SbomMetadataBuilder.newSbomSPDXMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSpecVersion("1.5")
        .withThirdPartyFileId(thirdPartyScan.getThirdPartyFileId())
        .build();
    HttpResponse spdxResponse = restRequest()
        .path(SbomComponentsResource.SBOM_METADATA_PATH)
        .parameter(app.getId(), sbomSPDXMetadata.getSbomVersion())
        .get();

    assertResponseStatus(Status.OK.getStatusCode(), spdxResponse);
    BomPageMetadataDTO spdxResultDto = spdxResponse.getBody(BomPageMetadataDTO.class);
    assertThat(spdxResultDto.person).isEqualTo(Arrays.asList("John Doe", "Jane Doe"));
    assertThat(spdxResultDto.organization).isEqualTo(Arrays.asList("Example Organization"));
    assertThat(spdxResultDto.fileFormat).isEqualTo(sbomSPDXMetadata.getSpecFormat());
    assertThat(spdxResultDto.specification).isEqualTo(sbomSPDXMetadata.getSpec());
    assertThat(spdxResultDto.specVersion).isEqualTo(sbomSPDXMetadata.getSpecVersion());
  }
}
