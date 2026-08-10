/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class FirewallMigrationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final String TARGET_REPOSITORY_MANAGER_INSTANCE_ID = "repositoryManagerInstanceId";

  private static final String TARGET_REPOSITORY_PUBLIC_ID = "publicId";

  @Inject
  private FirewallMigrationService migrationService;

  private Repository createTargetRepository() {
    return tempEntity.newRepository(TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testMigrateRepositoryHistory_Authorized() {
    testMigrateRepositoryHistory(true, true);
  }

  @Test
  public void testMigrateRepositoryHistory_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> testMigrateRepositoryHistory(false, false));
  }

  @Test
  public void testMigrateRepositoryHistory_Unauthorized_Target() {
    assertThrows(UnauthorizedException.class, () -> testMigrateRepositoryHistory(false, true));
  }

  @Test
  public void testMigrateRepositoryHistory_Unauthorized_Source() {
    assertThrows(UnauthorizedException.class, () -> testMigrateRepositoryHistory(true, false));
  }

  private void testMigrateRepositoryHistory(boolean grantTargetPermission, boolean grantSourcePermission) {
    Repository targetRepository = createTargetRepository();
    if (grantTargetPermission) {
      grantEvaluateComponentPermission(targetRepository.getId());
    }
    RepositoryManager sourceRepositoryManager = tempEntity.newRepositoryManager();
    Repository sourceRepository = tempEntity.newRepository(sourceRepositoryManager, "source-repo");
    if (grantSourcePermission) {
      grantEvaluateComponentPermission(sourceRepository.getId());
    }
    migrationService.migrateRepositoryHistory(sourceRepositoryManager.getInstanceId(), sourceRepository.getPublicId(),
        TARGET_REPOSITORY_MANAGER_INSTANCE_ID, targetRepository.getPublicId());
  }

  @Test
  public void testGetRepositoryMigrationState_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    migrationService.getRepositoryMigrationState(TARGET_REPOSITORY_MANAGER_INSTANCE_ID,
        createTargetRepository().getPublicId());
  }

  @Test
  public void testGetRepositoryMigrationState_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> migrationService.getRepositoryMigrationState(
        TARGET_REPOSITORY_MANAGER_INSTANCE_ID, createTargetRepository().getPublicId()));
  }

  @Test
  public void testGetRepositoryMigrationState_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> migrationService.getRepositoryMigrationState(
        TARGET_REPOSITORY_MANAGER_INSTANCE_ID, createTargetRepository().getPublicId()));
  }
}
