/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.integration;

import java.io.IOException;
import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.IntegrationStatusDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static com.sonatype.insight.brain.integration.IntegrationResource.DEFAULT_PAGE;
import static com.sonatype.insight.brain.integration.IntegrationResource.DEFAULT_PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetApplicationIntegrationSummaries_UseDefault_WhenNoParametersProvided() throws Exception {
    final Organization org = tempEntity.newOrganization();
    final Application app1 = tempEntity.newApplication(org.getId());
    final Application app2 = tempEntity.newApplication(org.getId());

    final HttpResponse httpResponse =
        restRequest().path(IntegrationResource.RESOURCE_PATH + IntegrationResource.STATUSES_PATH).get();
    assertResponseStatus(200, httpResponse);

    final ApiPageResult<IntegrationStatusDTO> response = getBodyByTypeReference(httpResponse.getBodyBytes(),
        new TypeReference<ApiPageResult<IntegrationStatusDTO>>() { });
    assertThat(response)
        .isNotNull();
    assertThat(response.getPage())
        .isEqualTo(Integer.parseInt(DEFAULT_PAGE));
    assertThat(response.getPageSize())
        .isEqualTo(Integer.parseInt(DEFAULT_PAGE_SIZE));

    final List<IntegrationStatusDTO> results = response.getResults();
    // 2 apps were set up, should be 1 result per app
    assertThat(results)
        .hasSize(2);
    assertThat(results.stream().map(IntegrationStatusDTO::getApplicationId))
        .containsExactlyInAnyOrder(app1.getId(), app2.getId());
    assertThat(results.stream().map(IntegrationStatusDTO::getOrganizationId))
        .containsExactly(org.getId(), org.getId());
  }

  @Test
  public void testGetApplicationIntegrationSummaries_DoNotUseDefault_WhenParametersProvided() throws Exception {
    final Organization org = tempEntity.newOrganization();
    tempEntity.newApplication(org.getId());
    tempEntity.newApplication(org.getId());

    final int page = 2;
    final int pageSize = 2;
    final HttpResponse httpResponse =
        restRequest().path(IntegrationResource.RESOURCE_PATH + IntegrationResource.STATUSES_PATH)
            .query("page", page)
            .query("pageSize", pageSize)
            .get();
    assertResponseStatus(200, httpResponse);

    final ApiPageResult<IntegrationStatusDTO> response = getBodyByTypeReference(httpResponse.getBodyBytes(),
        new TypeReference<ApiPageResult<IntegrationStatusDTO>>() { });
    assertThat(response)
        .isNotNull();
    assertThat(response.getPage())
        .isEqualTo(page);
    assertThat(response.getPageSize())
        .isEqualTo(pageSize);

    final List<IntegrationStatusDTO> results = response.getResults();
    // 2 apps were set up, but asking for 2nd page with a page size of 2, so page 2 should be empty
    assertThat(results)
        .isEmpty();
  }

  private <T> T getBodyByTypeReference(final byte[] bodyBytes, final TypeReference<T> typeRef) {
    try {
      return new ObjectMapper().readValue(bodyBytes, typeRef);
    }
    catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
