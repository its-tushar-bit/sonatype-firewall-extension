/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static com.sonatype.insight.brain.integration.repository.FirewallMigrationService.PROTOCOL_V1;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FirewallMigrationResourceTest
    extends AbstractResourceTest
{
  private static final String REPOSITORY_PUBLIC_ID = "publicId";

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(FirewallMigrationResource.RESOURCE_PATH);
  }

  private HttpRequest supportedRequest() {
    return restRequest().path(FirewallMigrationResource.SUPPORTED_PATH);
  }

  @Test
  public void testVerifyMigrationSupported() throws Exception {
    HttpResponse response = supportedRequest().parameter(PROTOCOL_V1).post();
    assertResponseStatus(HttpStatus.SC_NO_CONTENT, response);
  }

  @Test
  public void testMigrateRepositoryHistory() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    RepositoryManager sourceRepositoryManager = tempEntity.newRepositoryManager();
    Repository sourceRepository = tempEntity.newRepository(sourceRepositoryManager, "source-central");

    HttpResponse response = restRequest().path(FirewallMigrationResource.HISTORY_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .query("sourceRepositoryManagerInstanceId", sourceRepositoryManager.getInstanceId())
        .query("sourceRepositoryPublicId", sourceRepository.getPublicId()).post();
    assertResponseStatus(HttpStatus.SC_NO_CONTENT, response);
  }

  @Test
  public void testGetRepositoryMigrationState() throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    HttpResponse response = restRequest().path(FirewallMigrationResource.HISTORY_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId()).get();
    assertResponseStatus(HttpStatus.SC_OK, response);
    MigrationState migrationState = response.getBody(MigrationState.class);
    assertThat(migrationState, is(MigrationState.FAILED));
  }
}
