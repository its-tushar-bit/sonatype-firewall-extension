/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in the {@code com.sonatype.insight.brain.repository} package because it references the package-private
 * {@code RepositoryReportResource.RESOURCE_PATH}/{@code SUMMARY} constants.
 */
@IqH2Test
class IqH2RepositoryReportResourceTest
{
  private static final String REPOSITORY_PUBLIC_ID = "repoPublicId";

  private IqTestContext ctx;

  private RepositoryManager repositoryManager;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(RepositoryReportResource.RESOURCE_PATH);
  }

  private HttpRequest restRequestSummary() {
    return restRequest().path(RepositoryReportResource.SUMMARY);
  }

  @BeforeEach
  void createRepositoryManager() {
    repositoryManager = ctx.tempEntity().newRepositoryManager();
  }

  @Test
  void testGetRepositorySummary() throws Exception {
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true);

    HttpResponse response = restRequestSummary().parameter(repository.getId()).get();

    ctx.assertResponseStatus(200, response);
    RepositorySummary repositorySummary = response.getBody(RepositorySummary.class);
    assertThat(repositorySummary).isNotNull();
  }

  @Test
  void testGetRepositorySummary_NoRepository() throws Exception {
    String repositoryId = "NonExistentRepositoryId";

    HttpResponse response = restRequestSummary().parameter(repositoryId).get();

    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(getErrorMessage(repositoryId));
  }

  @Test
  void testGetRepositorySummary_RepositoryDisabled() throws Exception {
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, false);

    HttpResponse response = restRequestSummary().parameter(repository.getId()).get();

    ctx.assertResponseStatus(200, response);
    RepositorySummary repositorySummary = response.getBody(RepositorySummary.class);
    assertThat(repositorySummary).isNotNull();
  }

  private String getErrorMessage(String repositoryId) {
    return "Repository with ID " + repositoryId + " does not exist.";
  }
}
