/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import com.sonatype.clm.dto.model.repository.migration.MigrationDetails;
import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.integration.repository.FirewallMigrationService.PROTOCOL_V1;
import static org.assertj.core.api.Assertions.assertThat;

@IqPostgresTest
class IqPostgresFirewallMigrationResourceTest
{
  private static final String REPOSITORY_PUBLIC_ID = "publicId";

  private IqTestContext ctx;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(FirewallMigrationResource.RESOURCE_PATH);
  }

  private HttpRequest supportedRequest() {
    return restRequest().path(FirewallMigrationResource.SUPPORTED_PATH);
  }

  @Test
  void testVerifyMigrationSupported() throws Exception {
    HttpResponse response = supportedRequest().parameter(PROTOCOL_V1).post();
    ctx.assertResponseStatus(HttpStatus.SC_NO_CONTENT, response);
  }

  @Test
  void testMigrateRepositoryHistory() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
    RepositoryManager sourceRepositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository sourceRepository = ctx.tempEntity().newRepository(sourceRepositoryManager, "source-central");

    HttpResponse response = restRequest().path(FirewallMigrationResource.HISTORY_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .query("sourceRepositoryManagerInstanceId", sourceRepositoryManager.getInstanceId())
        .query("sourceRepositoryPublicId", sourceRepository.getPublicId())
        .post();
    ctx.assertResponseStatus(HttpStatus.SC_NO_CONTENT, response);
  }

  @Test
  void testGetRepositoryMigrationState() throws Exception {
    RepositoryManager repositoryManager = ctx.tempEntity().newRepositoryManager();
    Repository repository = ctx.tempEntity().newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);

    HttpResponse response = restRequest().path(FirewallMigrationResource.HISTORY_PATH)
        .parameter(repositoryManager.getInstanceId(), repository.getPublicId())
        .get();
    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
    MigrationDetails migrationDetails = response.getBody(MigrationDetails.class);
    assertThat(migrationDetails.getState()).isEqualTo(MigrationState.FAILED);
  }
}
