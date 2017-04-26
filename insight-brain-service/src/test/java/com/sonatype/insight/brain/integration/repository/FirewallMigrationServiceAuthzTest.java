/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class FirewallMigrationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String REPOSITORY_MANAGER_INSTANCE_ID = "repositoryManagerInstanceId";

  private static final String REPOSITORY_PUBLIC_ID = "publicId";

  @Inject
  private FirewallMigrationService migrationService;

  private Repository createRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(REPOSITORY_MANAGER_INSTANCE_ID);
    return tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testMigrateRepositoryHistory_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    testMigrateRepositoryHistory();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testMigrateRepositoryHistory_Unauthenticated() {
    testMigrateRepositoryHistory();
  }

  @Test(expected = UnauthorizedException.class)
  public void testMigrateRepositoryHistory_Unauthorized() {
    login();
    testMigrateRepositoryHistory();
  }

  private void testMigrateRepositoryHistory() {
    Repository repository = createRepository();
    RepositoryManager sourceRepositoryManager = tempEntity.newRepositoryManager();
    Repository sourceRepository = tempEntity.newRepository(sourceRepositoryManager, "source-repo");
    migrationService.migrateRepositoryHistory(REPOSITORY_MANAGER_INSTANCE_ID, repository.getPublicId(),
        sourceRepositoryManager.getInstanceId(), sourceRepository.getPublicId(), "some/path");
  }

  @Test
  public void testGetRepositoryMigrationState_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    migrationService.getRepositoryMigrationState(REPOSITORY_MANAGER_INSTANCE_ID, createRepository().getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetRepositoryMigrationState_Unauthenticated() {
    migrationService.getRepositoryMigrationState(REPOSITORY_MANAGER_INSTANCE_ID, createRepository().getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetRepositoryMigrationState_Unauthorized() {
    login();
    migrationService.getRepositoryMigrationState(REPOSITORY_MANAGER_INSTANCE_ID, createRepository().getPublicId());
  }
}
