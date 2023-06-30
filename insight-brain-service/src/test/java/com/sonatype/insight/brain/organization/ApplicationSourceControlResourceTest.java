/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import java.util.stream.IntStream;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dashboard.DashboardResultsDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationSourceControlResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabled() throws Exception {
    final Organization organization = tempEntity.newOrganization();
    final int numAppsWithoutASCF = 5;
    IntStream.range(0, numAppsWithoutASCF)
        .forEach(i -> tempEntity.newApplication(organization.getId()));

    final HttpResponse response = getAppsWithoutAutomatedSourceControlFeedbackRequest().get();
    assertResponseStatus(200, response);

    final DashboardResultsDTO<?> responseData = response.getBody(DashboardResultsDTO.class);

    assertThat(responseData.dashboardResults).hasSize(numAppsWithoutASCF);
    assertThat(responseData.numResults).isEqualTo(numAppsWithoutASCF);
  }

  private HttpRequest getAppsWithoutAutomatedSourceControlFeedbackRequest() {
    return restRequest().path(ApplicationSourceControlResource.RESOURCE_PATH)
        .query("page", 0)
        .query("pageSize", 100);
  }
}
