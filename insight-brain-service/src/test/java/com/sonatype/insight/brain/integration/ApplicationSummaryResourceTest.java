/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ApplicationSummaryResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetApplications_EvaluateApplication() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    Response response = AuthedRestAccess.get(getServicePath() + "?" + ApplicationSummaryResource.GOAL_PARAM + "="
        + Goal.EVALUATE_APPLICATION);
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = fromJson(response, ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplications_EvaluateComponent() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    Response response = AuthedRestAccess.get(getServicePath() + "?" + ApplicationSummaryResource.GOAL_PARAM + "="
        + Goal.EVALUATE_COMPONENT);
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = fromJson(response, ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplications_NoGoalSpecified() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    Response response = AuthedRestAccess.get(getServicePath());
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = fromJson(response, ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  // Need to test anonymous access in the resource tests
  @Test
  public void testGetApplications_EvaluateApplication_Anonymous() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    Response response = RestAccess.get(getServicePath() + "?" + ApplicationSummaryResource.GOAL_PARAM + "="
        + Goal.EVALUATE_APPLICATION);
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = fromJson(response, ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  private void assertApplicationSummaryList(ApplicationSummaryList actual, Application expected) {
    assertThat(actual, notNullValue());
    assertThat(actual.getApplicationSummaries(), hasSize(1));
    ApplicationSummary applicationSummary = actual.getApplicationSummaries().get(0);
    assertThat(applicationSummary.getId(), is(expected.getId()));
    assertThat(applicationSummary.getPublicId(), is(expected.getPublicId()));
    assertThat(applicationSummary.getName(), is(expected.getName()));
  }

  private String getServicePath() {
    return getRestBaseUrl() + "/" + ApplicationSummaryResource.SERVICE_PATH;
  }
}
