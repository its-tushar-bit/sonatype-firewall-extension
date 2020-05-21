/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryReportResource.RepositoryReportSummary;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryReportResourceTest
    extends AbstractResourceTest
{
  private static final String REPOSITORY_PUBLIC_ID = "repoPublicId";

  private RepositoryManager repositoryManager;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RepositoryReportResource.RESOURCE_PATH);
  }

  private HttpRequest restRequestSummary() {
    return restRequest().path(RepositoryReportResource.SUMMARY);
  }

  private HttpRequest restPolicyThreatRequest(final String repositoryId, final String pathname) {
    return restRequest().path(RepositoryReportResource.POLICY_THREAT_PATH).parameter(repositoryId, pathname);
  }

  @Before
  public void createRepositoryManager() {
    repositoryManager = tempEntity.newRepositoryManager();
  }

  private HttpResponse testGet(final String subPath, final String repositoryId, final int expectedStatus)
      throws Exception
  {
    final HttpResponse response = restRequest().path(subPath).parameter(repositoryId).get();

    assertResponseStatus(expectedStatus, response);
    return response;
  }

  @Test
  public void testGetSummary() throws Exception {
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true);

    HttpResponse response = restRequestSummary().parameter(repository.getId()).get();

    assertResponseStatus(200, response);
    RepositoryReportSummary policyEvaluationSummary = response.getBody(RepositoryReportSummary.class);
    assertThat(policyEvaluationSummary).isNotNull();
  }

  @Test
  public void testGetSummary_NoRepository() throws Exception {
    String repositoryId = "NonExistentRepositoryId";

    HttpResponse response = restRequestSummary().parameter(repositoryId).get();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(getErrorMessage(repositoryId));
  }

  @Test
  public void testGetSummary_RepositoryDisabled() throws Exception {
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, false);

    HttpResponse response = restRequestSummary().parameter(repository.getId()).get();

    assertResponseStatus(200, response);
    RepositoryReportSummary policyEvaluationSummary = response.getBody(RepositoryReportSummary.class);
    assertThat(policyEvaluationSummary).isNotNull();
  }

  @Test
  public void testGetReportDetails() throws Exception {
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true);

    final HttpResponse response = testGet(RepositoryReportResource.DETAILS_PATH, repository.getId(), 200);

    final RepositoryReportDetail[] policyEvaluationDetail = response.getBody(RepositoryReportDetail[].class);
    assertThat(policyEvaluationDetail).isNotNull();
  }

  @Test
  public void testGetReportDetails_NoRepository() throws Exception {
    final String repositoryId = "NonExistentRepositoryId";

    final HttpResponse response = testGet(RepositoryReportResource.DETAILS_PATH, repositoryId, 404);

    assertThat(response.getBodyText()).isEqualTo(getErrorMessage(repositoryId));
  }

  @Test
  public void testGetReportDetails_RepositoryDisabled() throws Exception {
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, false);

    final HttpResponse response = testGet(RepositoryReportResource.DETAILS_PATH, repository.getId(), 200);

    final RepositoryReportDetail[] policyEvaluationDetail = response.getBody(RepositoryReportDetail[].class);
    assertThat(policyEvaluationDetail).isNotNull();
  }

  @Test
  public void testGetPolicyThreats() throws Exception {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), "dir/path");
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 8, repositoryComponent.getPathname(), false,
        "policyId1", "policyName1", repositoryComponent.getComponentIdentifier());

    HttpResponse response = restPolicyThreatRequest(repository.getId(), repositoryComponent.getPathname()).get();
    assertResponseStatus(200, response);
    RepositoryPolicyThreatDTO repositoryPolicyThreatDTO = response.getBody(RepositoryPolicyThreatDTO.class);
    assertThat(repositoryPolicyThreatDTO.activePolicyViolations).hasSize(1);
  }

  private String getErrorMessage(String repositoryId) {
    return "Cannot find a repository with ID " + repositoryId + ".";
  }
}
