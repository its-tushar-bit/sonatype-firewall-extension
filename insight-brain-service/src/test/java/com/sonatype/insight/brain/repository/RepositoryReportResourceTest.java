/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
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
    return super.restRequest().path(RepositoryReportResource.SERVICE_PATH, RepositoryReportResource.SUMMARY);
  }

  @Before
  public void createRepositoryManager() {
    repositoryManager = tempEntity.newRepositoryManager();
  }

  @Test
  public void testGetSummary() throws Exception {
    tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true);

    HttpResponse response = restRequest().parameter(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID).get();

    assertResponseStatus(200, response);
    RepositoryReportSummary policyEvaluationSummary = response.getBody(RepositoryReportSummary.class);
    assertThat(policyEvaluationSummary, notNullValue());
  }

  @Test
  public void testGetSummary_NoRepository() throws Exception {
    String repositoryId = "NonExistentRepositoryId";

    HttpResponse response = restRequest().parameter(repositoryManager.getInstanceId(), repositoryId).get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText(),
        is("Unknown repository " + repositoryId + " for repositoryManagerInstanceId " +
            repositoryManager.getInstanceId() + "."));
  }

  @Test
  public void testGetSummary_RepositoryDisabled() throws Exception {
    tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, false);

    HttpResponse response = restRequest().parameter(repositoryManager.getInstanceId(), REPOSITORY_PUBLIC_ID).get();

    assertResponseStatus(200, response);
    RepositoryReportSummary policyEvaluationSummary = response.getBody(RepositoryReportSummary.class);
    assertThat(policyEvaluationSummary, notNullValue());
  }
}
