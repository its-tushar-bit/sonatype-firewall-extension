/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.clm.dto.model.organization.OrganizationSummary;
import com.sonatype.clm.dto.model.organization.OrganizationSummaryList;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.integration.Goal;
import com.sonatype.insight.brain.integration.OrganizationSummaryResource;
import com.sonatype.insight.brain.model.Organization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2OrganizationSummaryResourceTest
{
  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(OrganizationSummaryResource.RESOURCE_PATH);
  }

  private HttpRequest summaryRequest(Goal goal) {
    return restRequest().query(OrganizationSummaryResource.GOAL_PARAM, goal);
  }

  @Test
  void testGetOrganizations_EvaluateApplication() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();

    HttpResponse response = summaryRequest(Goal.EVALUATE_APPLICATION).get();
    ctx.assertResponseStatus(200, response);

    OrganizationSummaryList organizationListDTO = response.getBody(OrganizationSummaryList.class);
    assertOrganizationSummaryList(organizationListDTO, organization);
  }

  @Test
  void testGetOrganizations_NoGoalSpecified() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    OrganizationSummaryList organizationListDTO = response.getBody(OrganizationSummaryList.class);
    assertOrganizationSummaryList(organizationListDTO, organization);
  }

  private void assertOrganizationSummaryList(OrganizationSummaryList actual, Organization expected) {
    assertThat(actual).isNotNull();
    assertThat(actual.getOrganizationSummaries()).hasSize(1);
    OrganizationSummary organizationSummary = actual.getOrganizationSummaries().get(0);
    assertThat(organizationSummary.getId()).isEqualTo(expected.getId());
    assertThat(organizationSummary.getName()).isEqualTo(expected.getName());
  }
}
