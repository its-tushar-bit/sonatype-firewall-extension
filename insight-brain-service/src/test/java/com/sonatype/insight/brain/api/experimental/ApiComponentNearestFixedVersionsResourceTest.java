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
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiComponentNearestFixedVersionsResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ApiComponentNearestFixedVersionsResource.RESOURCE_PATH);
  }

  @Test
  public void testGetNearestFixedVersions_nullComponentsList() throws Exception {
    HttpResponse response = restRequest().post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("No components provided in the request");
  }

  @Test
  public void testGetNearestFixedVersions_emptyComponentsList() throws Exception {
    HttpResponse response = restRequest().body(new ApiComponentNearestFixedVersionsRequestListDto()).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("No components provided in the request");
  }

  @Test
  public void testGetNearestFixedVersions() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p1", "v1");

    ApiComponentNearestFixedVersionsRequestListDto listDto = new ApiComponentNearestFixedVersionsRequestListDto();
    ApiComponentNearestFixedVersionsRequestDto dto = new ApiComponentNearestFixedVersionsRequestDto();
    dto.setPackageUrl(PackageUrlIdentifier.toPackageUrl(componentIdentifier));
    listDto.getComponents().add(dto);

    ComponentNearestFixedVersions fixedVersion = new ComponentNearestFixedVersions();
    fixedVersion.setComponentIdentifier(componentIdentifier);

    hdsRespondWith(new ComponentNearestFixedVersions[]{fixedVersion})
        .atUri(ApiComponentNearestFixedVersionsService.HDS_COMPONENT_NEAREST_FIXED_VERSION_PATH);

    HttpResponse response = restRequest().body(listDto).post();
    assertResponseStatus(200, response);

    List<ComponentNearestFixedVersions> responseDtos = response.getBodyList(ComponentNearestFixedVersions.class);

    assertThat(responseDtos)
        .extracting(ComponentNearestFixedVersions::getComponentIdentifier)
        .containsExactlyInAnyOrder(componentIdentifier);
  }
}
