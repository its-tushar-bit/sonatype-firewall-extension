/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.After;
import org.junit.Test;

public class RepositoryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String MANUAL_REPO_MAN_INSTANCE_ID = "manualDeleteRepoManagerInstanceId";

  private static final String REPOSITORY_PUBLIC_ID = "publicId";

  @Inject
  private RepositoryService repositoryService;

  private RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  @After
  public void cleanup() {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);
    if (repositoryManager != null) {
      repositoryManagerDAO.delete(repositoryManager);
    }
  }

  @Test
  public void testEnableRepository_NewRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.enableRepository(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEnableRepository_NewRepository_Unauthenticated() {
    repositoryService.enableRepository(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEnableRepository_NewRepository_Unauthorized() {
    grantWritePermission();
    repositoryService.enableRepository(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testEnableRepository_ExistingRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.enableRepository(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEnableRepository_ExistingRepository_Unauthenticated() {
    repositoryService.enableRepository(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testEnableRepository_ExistingRepository_Unauthorized() {
    grantWritePermission();
    repositoryService.enableRepository(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId());
  }

  private Repository createRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(MANUAL_REPO_MAN_INSTANCE_ID);
    return tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testEvaluateComponents_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService
        .evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), null /* componentEvaluationDataRequestList */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateComponents_Unauthenticated() {
    repositoryService
        .evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), null /* componentEvaluationDataRequestList */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateComponents_Unauthorized() {
    grantWritePermission();
    repositoryService
        .evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), null /* componentEvaluationDataRequestList */);
  }

  @Test
  public void testGetPolicyEvaluationSummary_Authorized() {
    createRepository();
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyEvaluationSummary_Unauthenticated() {
    createRepository();
    repositoryService.getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyEvaluationSummary_Unauthorized() {
    createRepository();
    grantWritePermission();
    repositoryService.getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testEvaluateComponents_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService
        .evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), null /* componentEvaluationDataRequestList */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateComponents_Unauthenticated() {
    repositoryService
        .evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), null /* componentEvaluationDataRequestList */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateComponents_Unauthorized() {
    grantWritePermission();
    repositoryService
        .evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), null /* componentEvaluationDataRequestList */);
  }
}
