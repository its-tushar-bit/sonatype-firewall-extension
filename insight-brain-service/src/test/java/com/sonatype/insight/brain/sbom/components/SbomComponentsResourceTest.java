/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.components;

import java.util.Arrays;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SbomComponentsResourceTest
    extends AbstractResourceTest
{
  private static final String METADATA_RESOURCE_PATH =
      SbomComponentsResource.RESOURCE_BASE_PATH + SbomComponentsResource.SBOM_METADATA_PATH;

  @Before
  public void before() throws Exception {
    setFeatures(LicensedFeature.SBOM_MANAGER);
  }

  @Test
  public void testGetSbomMetadataNotFound() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    HttpResponse response = restRequest()
        .path(METADATA_RESOURCE_PATH)
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
        .path(METADATA_RESOURCE_PATH)
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
        .path(METADATA_RESOURCE_PATH)
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
