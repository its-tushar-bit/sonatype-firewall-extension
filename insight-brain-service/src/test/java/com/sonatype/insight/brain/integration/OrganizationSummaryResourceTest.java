/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.clm.dto.model.organization.OrganizationSummary;
import com.sonatype.clm.dto.model.organization.OrganizationSummaryList;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationSummaryResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(OrganizationSummaryResource.RESOURCE_PATH);
  }

  private HttpRequest summaryRequest(Goal goal) {
    return restRequest().query(OrganizationSummaryResource.GOAL_PARAM, goal);
  }

  @Test
  public void testGetOrganizations_EvaluateApplication() throws Exception {
    Organization organization = tempEntity.newOrganization();

    HttpResponse response = summaryRequest(Goal.EVALUATE_APPLICATION).get();
    assertResponseStatus(200, response);

    OrganizationSummaryList organizationListDTO = response.getBody(OrganizationSummaryList.class);
    assertOrganizationSummaryList(organizationListDTO, organization);
  }

  @Test
  public void testGetOrganizations_NoGoalSpecified() throws Exception {
    Organization organization = tempEntity.newOrganization();

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

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
