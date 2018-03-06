/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ApplicationSummaryResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(ApplicationSummaryResource.RESOURCE_PATH);
  }

  private HttpRequest summaryRequest(Goal goal) {
    return restRequest().query(ApplicationSummaryResource.GOAL_PARAM, goal);
  }

  @Test
  public void testGetApplications_EvaluateApplication() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    HttpResponse response = summaryRequest(Goal.EVALUATE_APPLICATION).get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplications_EvaluateComponent() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    HttpResponse response = summaryRequest(Goal.EVALUATE_COMPONENT).get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplications_NoGoalSpecified() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  // Need to test anonymous access in the resource tests
  @Test
  public void testGetApplications_EvaluateApplication_Anonymous() throws Exception {
    HttpResponse response = summaryRequest(Goal.EVALUATE_APPLICATION).anon().get();
    assertResponseStatus(401, response);
  }

  @Test
  @ManualServerInit
  public void testGetApplications_EvaluateApplication_Anonymous_AnonymousClientAccessAllowed() throws Exception {
    initServer(new Configurator() {
      @Override
      public void configure(final InsightConfig config) {
        config.setAnonymousClientAccessAllowed(true);
      }
    });
    Application application = tempEntity.newApplicationWithParent("testPublicId");
    HttpResponse response = summaryRequest(Goal.EVALUATE_APPLICATION).anon().get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplications_EvaluateComponent_Anonymous() throws Exception {
    HttpResponse response = summaryRequest(Goal.EVALUATE_COMPONENT).anon().get();
    assertResponseStatus(401, response);
  }

  @Test
  @ManualServerInit
  public void testGetApplications_EvaluateComponent_Anonymous_AnonymousClientAccessAllowed() throws Exception {
    initServer(new Configurator() {
      @Override
      public void configure(final InsightConfig config) {
        config.setAnonymousClientAccessAllowed(true);
      }
    });
    tempEntity.newApplicationWithParent("testPublicId");
    HttpResponse response = summaryRequest(Goal.EVALUATE_COMPONENT).anon().get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertThat(applicationListDTO.getApplicationSummaries().size(), is(0));
  }

  @Test
  public void testGetApplications_NoGoal_Anonymous() throws Exception {
    HttpResponse response = restRequest().anon().get();
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetApplications_NoGoal_Anonymous_AnonymousClientAccessAllowed() throws Exception {
    initServer(new Configurator() {
      @Override
      public void configure(final InsightConfig config) {
        config.setAnonymousClientAccessAllowed(true);
      }
    });
    Application application = tempEntity.newApplicationWithParent("testPublicId");
    HttpResponse response = restRequest().anon().get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
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

  @Test
  public void testVerifyOrCreateApplication_EvaluateApplication() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    HttpResponse response = restRequest().path(ApplicationSummaryResource.VERIFY_OR_CREATE_APPLICATION_PATH)
        .parameter(app.getPublicId()).query(ApplicationSummaryResource.GOAL_PARAM, Goal.EVALUATE_APPLICATION).post();
    assertResponseStatus(200, response);

    assertThat(response.getBody(String.class), is("true"));
  }
}
