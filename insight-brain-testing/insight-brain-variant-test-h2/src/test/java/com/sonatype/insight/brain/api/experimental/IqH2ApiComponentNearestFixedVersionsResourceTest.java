/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ComponentNearestFixedVersions;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.experimental.ApiComponentNearestFixedVersionsRequestListDto.ApiComponentNearestFixedVersionsRequestDto;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiComponentNearestFixedVersionsResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(ApiComponentNearestFixedVersionsResource.RESOURCE_PATH);
  }

  @Test
  void testGetNearestFixedVersions_nullComponentsList() throws Exception {
    HttpResponse response = restRequest().post();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("No components provided in the request");
  }

  @Test
  void testGetNearestFixedVersions_emptyComponentsList() throws Exception {
    HttpResponse response = restRequest().body(new ApiComponentNearestFixedVersionsRequestListDto()).post();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("No components provided in the request");
  }

  @Test
  void testGetNearestFixedVersions() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p1", "v1");

    ApiComponentNearestFixedVersionsRequestListDto listDto = new ApiComponentNearestFixedVersionsRequestListDto();
    ApiComponentNearestFixedVersionsRequestDto dto = new ApiComponentNearestFixedVersionsRequestDto();
    dto.setPackageUrl(PackageUrlIdentifier.toPackageUrl(componentIdentifier));
    listDto.getComponents().add(dto);

    ComponentNearestFixedVersions fixedVersion = new ComponentNearestFixedVersions();
    fixedVersion.setComponentIdentifier(componentIdentifier);

    ctx.hdsRespondWith(new ComponentNearestFixedVersions[]{fixedVersion})
        .atUri(ApiComponentNearestFixedVersionsService.HDS_COMPONENT_NEAREST_FIXED_VERSION_PATH);

    HttpResponse response = restRequest().body(listDto).post();
    ctx.assertResponseStatus(200, response);

    List<ComponentNearestFixedVersions> responseDtos = response.getBodyList(ComponentNearestFixedVersions.class);

    assertThat(responseDtos)
        .extracting(ComponentNearestFixedVersions::getComponentIdentifier)
        .containsExactlyInAnyOrder(componentIdentifier);
  }
}
