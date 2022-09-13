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
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationSummaryResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(DefaultApplicationSummaryResource.RESOURCE_PATH);
  }

  private HttpRequest summaryRequest(Goal goal) {
    return restRequest().query("goal", goal);
  }

  private HttpRequest underOrgRequest(String organizationId) {
    return restRequest().query("organizationId", organizationId);
  }

  private HttpRequest summaryUnderOrgRequest(Goal goal, String organizationId) {
    return underOrgRequest(organizationId).query("goal", goal);
  }

  @Test
  public void testGetApplications_EvaluateApplication() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = summaryRequest(Goal.EVALUATE_APPLICATION).get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplications_EvaluateComponent() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = summaryRequest(Goal.EVALUATE_COMPONENT).get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplications_NoGoalSpecified() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplicationsByOrganization_EvaluateApplication() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = summaryUnderOrgRequest(Goal.EVALUATE_APPLICATION, application.getOrganizationId()).get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplicationsByOrganization_EvaluateComponent() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = summaryUnderOrgRequest(Goal.EVALUATE_COMPONENT, application.getOrganizationId()).get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  @Test
  public void testGetApplicationsByOrganization_NoGoalSpecified() throws Exception {
    Application application = tempEntity.newApplicationWithParent();

    HttpResponse response = underOrgRequest(application.getOrganizationId()).get();
    assertResponseStatus(200, response);

    ApplicationSummaryList applicationListDTO = response.getBody(ApplicationSummaryList.class);
    assertApplicationSummaryList(applicationListDTO, application);
  }

  private void assertApplicationSummaryList(ApplicationSummaryList actual, Application expected) {
    assertThat(actual).isNotNull();
    assertThat(actual.getApplicationSummaries()).hasSize(1);
    ApplicationSummary applicationSummary = actual.getApplicationSummaries().get(0);
    assertThat(applicationSummary.getId()).isEqualTo(expected.getId());
    assertThat(applicationSummary.getPublicId()).isEqualTo(expected.getPublicId());
    assertThat(applicationSummary.getName()).isEqualTo(expected.getName());
  }

  @Test
  public void testVerifyOrCreateApplication_EvaluateApplication() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    HttpResponse response = restRequest()
        .path(DefaultApplicationSummaryResource.VERIFY_OR_CREATE_APPLICATION_PATH)
        .parameter(app.getPublicId())
        .query("goal", Goal.EVALUATE_APPLICATION)
        .post();
    assertResponseStatus(200, response);

    assertThat(response.getBody(String.class)).isEqualTo("true");
  }
}
