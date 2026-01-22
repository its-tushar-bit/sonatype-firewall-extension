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

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

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

  @Override
  protected ConfigureRepositoriesRequest createConfigureRepositoriesRequest() {
    return new ConfigureRepositoriesRequest("Nexus", "3.60.0", null /* repositories */);
  }
}
