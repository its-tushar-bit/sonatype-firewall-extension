/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.IOException;
import java.util.List;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiIntegrationsCiCdStatIncrementDto;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.developer.integrationdashboard.api.IntegrationStatusDTO;
import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiUsageIncrementDto;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static com.sonatype.insight.brain.developer.integrationdashboard.IntegrationResource.DEFAULT_PAGE;
import static com.sonatype.insight.brain.developer.integrationdashboard.IntegrationResource.DEFAULT_PAGE_SIZE;
import static com.sonatype.insight.brain.developer.integrationdashboard.IntegrationResource.FIVE_YEARS_IN_MS;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class IntegrationResourceTest
    extends AbstractAuditTest
{
  static final String GET_CI_CD_USAGE_PATH = IntegrationResource.RESOURCE_PATH + "/stats/cicd/usage-over-time";

  static final String GET_APPLICATION_COUNT_HISTORY_OVER_TIME_PATH = IntegrationResource.RESOURCE_PATH +
      "/stats/usage-over-time";

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

  @Test
  public void testGetCiCdUsageStatIncrementsOverTime_UseDefault_WhenNoParametersProvided() throws Exception {
    final HttpResponse httpResponse =
        restRequest().path(GET_CI_CD_USAGE_PATH).get();
    assertResponseStatus(200, httpResponse);

    final List<ApiIntegrationsCiCdStatIncrementDto> response = getBodyByTypeReference(httpResponse.getBodyBytes(),
        new TypeReference<List<ApiIntegrationsCiCdStatIncrementDto>>() { });

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(12);
  }

  @Test
  public void testGetCiCdUsageStatIncrementsOverTime_UseDefault_AcceptsValidQueryParameters() throws Exception {
    final HttpResponse httpResponse =
        restRequest().path(GET_CI_CD_USAGE_PATH)
            .query("incrementSizeMillis", 1)
            .query("numberOfIncrements", 24)
            .get();
    assertResponseStatus(200, httpResponse);

    final List<ApiIntegrationsCiCdStatIncrementDto> response = getBodyByTypeReference(httpResponse.getBodyBytes(),
        new TypeReference<List<ApiIntegrationsCiCdStatIncrementDto>>() { });

    assertThat(response).isNotNull();
    assertThat(response.size()).isEqualTo(24);

    // assert each entry is incremented by 1
    for (int i = 1; i < response.size(); i++) {
      long lastTimeStamp = response.get(i - 1).getDateTimeMillis();
      long currentTimeStamp = response.get(i).getDateTimeMillis();

      assertThat(currentTimeStamp - lastTimeStamp).isEqualTo(1);
    }
  }

  @Test
  public void testGetCiCdUsageStatIncrementsOverTime_ReturnsBadRequest_WhenParametersAreOutsideBounds()
      throws Exception
  {
    final HttpResponse givenQueryAskTooSmallOfIncrementSize =
        restRequest().path(GET_CI_CD_USAGE_PATH)
            .query("incrementSizeMillis", 0)
            .get();
    assertResponseStatus(400, givenQueryAskTooSmallOfIncrementSize);

    final HttpResponse givenQueryAskTooLargeOfIncrementSize =
        restRequest().path(GET_CI_CD_USAGE_PATH)
            .query("incrementSizeMillis", FIVE_YEARS_IN_MS + 1)
            .get();
    assertResponseStatus(400, givenQueryAskTooLargeOfIncrementSize);

    final HttpResponse givenQueryAsksForTooFewIncrements =
        restRequest().path(GET_CI_CD_USAGE_PATH)
            .query("numberOfIncrements", 0)
            .get();
    assertResponseStatus(400, givenQueryAsksForTooFewIncrements);

    final HttpResponse givenQueryAsksForTooManyIncrements =
        restRequest().path(GET_CI_CD_USAGE_PATH)
            .query("numberOfIncrements", 53)
            .get();
    assertResponseStatus(400, givenQueryAsksForTooManyIncrements);
  }

  @Test
  public void testGetApplicationCountHistoryOverTime_UseDefault_WhenNoParametersProvided() throws Exception {
    final HttpResponse httpResponse =
        restRequest().path(GET_APPLICATION_COUNT_HISTORY_OVER_TIME_PATH).get();
    assertResponseStatus(200, httpResponse);

    final List<ApiUsageIncrementDto> response = getBodyByTypeReference(httpResponse.getBodyBytes(),
        new TypeReference<List<ApiUsageIncrementDto>>() { });

    assertThat(response)
        .isNotNull()
        .hasSize(12);
  }

  @Test
  public void testGetApplicationCountHistoryOverTime_UseDefault_AcceptsValidQueryParameters() throws Exception {
    final HttpResponse httpResponse =
        restRequest().path(GET_APPLICATION_COUNT_HISTORY_OVER_TIME_PATH)
            .query("incrementSizeMillis", 1)
            .query("numberOfIncrements", 24)
            .get();
    assertResponseStatus(200, httpResponse);

    final List<ApiUsageIncrementDto> response = getBodyByTypeReference(httpResponse.getBodyBytes(),
        new TypeReference<List<ApiUsageIncrementDto>>() { });

    assertThat(response)
        .isNotNull()
        .hasSize(24);

    // assert each entry is incremented by 1
    for (int i = 1; i < response.size(); i++) {
      long lastTimeStamp = response.get(i - 1).getDateTimeMillis();
      long currentTimeStamp = response.get(i).getDateTimeMillis();

      assertThat(currentTimeStamp - lastTimeStamp).isEqualTo(1);
    }
  }

  @Test
  public void testGetApplicationCountHistoryOverTime_ReturnsBadRequest_WhenParametersAreOutsideBounds()
      throws Exception
  {
    final HttpResponse givenQueryAskTooSmallOfIncrementSize =
        restRequest().path(GET_APPLICATION_COUNT_HISTORY_OVER_TIME_PATH)
            .query("incrementSizeMillis", 0)
            .get();
    assertResponseStatus(400, givenQueryAskTooSmallOfIncrementSize);

    final HttpResponse givenQueryAskTooLargeOfIncrementSize =
        restRequest().path(GET_CI_CD_USAGE_PATH)
            .query("incrementSizeMillis", FIVE_YEARS_IN_MS + 1)
            .get();
    assertResponseStatus(400, givenQueryAskTooLargeOfIncrementSize);

    final HttpResponse givenQueryAsksForTooFewIncrements =
        restRequest().path(GET_CI_CD_USAGE_PATH)
            .query("numberOfIncrements", 0)
            .get();
    assertResponseStatus(400, givenQueryAsksForTooFewIncrements);

    final HttpResponse givenQueryAsksForTooManyIncrements =
        restRequest().path(GET_CI_CD_USAGE_PATH)
            .query("numberOfIncrements", 53)
            .get();
    assertResponseStatus(400, givenQueryAsksForTooManyIncrements);
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
