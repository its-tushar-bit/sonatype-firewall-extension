/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class RepositoryServiceAuthzTest
    extends AbstractRepositoryServiceAuthzTest
{
  @Inject
  private RepositoryService repositoryService;

  @Override
  protected AbstractRepositoryService getRepositoryService() {
    return repositoryService;
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateComponentsAdhoc_ExistingRepository_Unauthorized() {
    login();

    repositoryService.evaluateComponentsAdhoc(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
        null /* componentEvaluationDataRequestList */, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateComponentsAdhoc_ExistingRepository_Unauthenticated() {
    repositoryService.evaluateComponentsAdhoc(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
        null /* componentEvaluationDataRequestList */, null);
  }

  @Test
  public void testEvaluateComponentsAdhoc_ExistingRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    repositoryService.evaluateComponentsAdhoc(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
        null /* componentEvaluationDataRequestList */, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateComponentsAdhoc_NewRepository_Unauthorized() {
    login();

    repositoryService.evaluateComponentsAdhoc(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID,
        null /* componentEvaluationDataRequestList */, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateComponentsAdhoc_NewRepository_Unauthenticated() {
    repositoryService.evaluateComponentsAdhoc(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID,
        null /* componentEvaluationDataRequestList */, null);
  }

  @Test
  public void testEvaluateComponentsAdhoc_NewRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    repositoryService.evaluateComponentsAdhoc(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID,
        null /* componentEvaluationDataRequestList */, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveExtraComponents_Unauthenticated() {
    tempEntity.newRepository(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    repositoryService.removeExtraComponents(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID,
        null /* repositoryComponentPathnames */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveExtraComponents_Unauthorized() {
    tempEntity.newRepository(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);

    login();

    repositoryService.removeExtraComponents(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID,
        null /* repositoryComponentPathnames */);
  }

  @Test
  public void testRemoveExtraComponents_Authorized() {
    Repository repo = tempEntity.newRepository(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);

    grantEvaluateComponentPermission(repo.getId());

    repositoryService.removeExtraComponents(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID,
        null /* repositoryComponentPathnames */);
  }

  @Test
  public void testGetConfiguredRepositoriesHosted_Authorized() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();
    grantEvaluateComponentPermission(repositoryManager.getId());

    repositoryService.getConfiguredRepositories(repositoryManager.getInstanceId(), null, null, null, null, null, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetConfiguredRepositoriesHosted_Unauthenticated() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();

    repositoryService.getConfiguredRepositories(repositoryManager.getInstanceId(), null, null, null, null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetConfiguredRepositoriesHosted_Unauthorized() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();
    login();

    repositoryService.getConfiguredRepositories(repositoryManager.getInstanceId(), null, null, null, null, null, null);
  }

  @Test
  public void testGetAvailableFormats_Authorized() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();
    grantEvaluateComponentPermission(repositoryManager.getId());

    repositoryService.getAvailableFormats(repositoryManager.getInstanceId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAvailableFormats_Unauthenticated() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();

    repositoryService.getAvailableFormats(repositoryManager.getInstanceId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAvailableFormats_Unauthorized() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();
    login();

    repositoryService.getAvailableFormats(repositoryManager.getInstanceId());
  }

  @Override
  protected ConfigureRepositoriesRequest createConfigureRepositoriesRequest() {
    return new ConfigureRepositoriesRequest("Nexus", "3.60.0", "http://localhost:8081", null /* repositories */);
  }
}
