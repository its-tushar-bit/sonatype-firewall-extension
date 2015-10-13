/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryReportResource.RepositoryReportSummary;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class RepositoryReportResourceTest
    extends AbstractResourceTest
{
  private static final String REPOSITORY_PUBLIC_ID = "repoPublicId";

  private RepositoryManager repositoryManager;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RepositoryReportResource.RESOURCE_PATH);
  }

  private HttpRequest restRequestSubpath(final String subPath) {
    return restRequest().path(subPath);
  }

  private HttpRequest restRequestSummary() {
    return restRequest().path(RepositoryReportResource.SUMMARY);
  }

  @Before
  public void createRepositoryManager() {
    repositoryManager = tempEntity.newRepositoryManager();
  }

  private HttpResponse testGet(final String subPath, final String repoInstanceId, final String repositoryId,
      final int expectedStatus) throws Exception
  {

    final HttpResponse response = restRequestSubpath(subPath).parameter(repoInstanceId, repositoryId).get();

    assertResponseStatus(expectedStatus, response);
    return response;
  }

  @Test
  public void testGetSummary() throws Exception {
    tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true);

    HttpResponse response = restRequestSummary().parameter(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID).get();

    assertResponseStatus(200, response);
    RepositoryReportSummary policyEvaluationSummary = response.getBody(RepositoryReportSummary.class);
    assertThat(policyEvaluationSummary, notNullValue());
  }

  @Test
  public void testGetSummary_NoRepository() throws Exception {
    String repositoryId = "NonExistentRepositoryId";

    HttpResponse response = restRequestSummary().parameter(repositoryManager.getInstanceId(), repositoryId).get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText(),
        is("Unknown repository " + repositoryId + " for repositoryManagerInstanceId " +
            repositoryManager.getInstanceId() + "."));
  }

  @Test
  public void testGetSummary_RepositoryDisabled() throws Exception {
    tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, false);

    HttpResponse response = restRequestSummary().parameter(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID).get();

    assertResponseStatus(200, response);
    RepositoryReportSummary policyEvaluationSummary = response.getBody(RepositoryReportSummary.class);
    assertThat(policyEvaluationSummary, notNullValue());
  }

  @Test
  public void testGetReportDetails() throws Exception {
    tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true);

    final HttpResponse response = testGet(RepositoryReportResource.DETAILS_PATH, repositoryManager.getInstanceId(),
        REPOSITORY_PUBLIC_ID, 200);

    final RepositoryReportDetail[] policyEvaluationDetail = response.getBody(RepositoryReportDetail[].class);
    assertThat(policyEvaluationDetail, notNullValue());
  }

  @Test
  public void testGetReportDetails_NoRepository() throws Exception {
    final String repositoryId = "NonExistentRepositoryId";

    final HttpResponse response = testGet(RepositoryReportResource.DETAILS_PATH, repositoryManager.getInstanceId(),
        repositoryId, 404);

    assertThat(response.getBodyText(), is(
        RepositoryDAO.getErrMsgMissingRepo(repositoryManager.getInstanceId(), repositoryId)));
  }

  @Test
  public void testGetReportDetails_RepositoryDisabled() throws Exception {
    tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, false);

    final HttpResponse response = testGet(RepositoryReportResource.DETAILS_PATH, repositoryManager.getInstanceId(),
        REPOSITORY_PUBLIC_ID, 200);

    final RepositoryReportDetail[] policyEvaluationDetail = response.getBody(RepositoryReportDetail[].class);
    assertThat(policyEvaluationDetail, notNullValue());
  }
}
