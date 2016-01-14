/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.integration.repository.RepositoryService.RepositoriesDTO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

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
  public void testGetPolicyThreats_Authorized() {
    Repository repo = createRepository();
    String path = "path";
    tempEntity.newRepositoryComponent(repo.getId(), path, new Date(), null);

    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.getPolicyThreats(repo.getId(), path);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyThreats_Unauthenticated() {
    Repository repo = createRepository();

    repositoryService.getPolicyThreats(repo.getId(), "path");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyThreats_Unauthorized() {
    Repository repo = createRepository();

    login();
    repositoryService.getPolicyThreats(repo.getId(), "path");
  }

  @Test
  public void testSetEnabled_NewRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetEnabled_NewRepository_Unauthenticated() {
    repositoryService.setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetEnabled_NewRepository_Unauthorized() {
    grantWritePermission();
    repositoryService.setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true);
  }

  @Test
  public void testSetEnabled_ExistingRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), true);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetEnabled_ExistingRepository_Unauthenticated() {
    repositoryService.setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), true);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetEnabled_ExistingRepository_Unauthorized() {
    grantWritePermission();
    repositoryService.setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), true);
  }

  private Repository createRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(MANUAL_REPO_MAN_INSTANCE_ID);
    return tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testSetQuarantine_Authorized() {
    createRepository();
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.setQuarantine(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetQuarantine_Unauthenticated() {
    createRepository();
    repositoryService.setQuarantine(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetQuarantine_Unauthorized() {
    createRepository();
    grantWritePermission();
    repositoryService.setQuarantine(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true);
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
        .evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), null /* componentEvaluationDataRequestList */,
            false);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateComponents_Unauthenticated() {
    repositoryService
        .evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), null /* componentEvaluationDataRequestList */,
            false);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateComponents_Unauthorized() {
    grantWritePermission();
    repositoryService
        .evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), null /* componentEvaluationDataRequestList */,
            false);
  }

  @Test
  public void testGetReportSummary_Authorized() {
    Repository repo = createRepository();
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    repositoryService.getReportSummary(repo.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetReportSummary_Unauthenticated() {
    Repository repo = createRepository();
    repositoryService.getReportSummary(repo.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetReportSummary_Unauthorized() {
    grantWritePermission();
    Repository repo = createRepository();

    repositoryService.getReportSummary(repo.getId());
  }

  @Test
  public void testGetReportDetails_Authorized() {
    Repository repo = createRepository();
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.getReportDetails(repo.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetReportDetails_Unauthenticated() {
    Repository repo = createRepository();
    repositoryService.getReportDetails(repo.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetReportDetails_Unauthorized() {
    grantWritePermission();
    Repository repo = createRepository();
    repositoryService.getReportDetails(repo.getId());
  }

  @Test
  public void testRemoveComponent_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), "somepath");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveComponent_Unauthenticated() {
    repositoryService.removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), "somepath");
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveComponent_Unauthorized() {
    grantWritePermission();
    repositoryService.removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), "somepath");
  }

  @Test
  public void testGetRepositoryById_Authorized() {
    Repository repo = createRepository();

    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.getRepositoryById(repo.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetRepositoryById_Unauthenticated() {
    repositoryService.getRepositoryById("repository-id");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetRepositoryById_Unauthorized() {
    Repository repo = createRepository();

    login();
    repositoryService.getRepositoryById(repo.getId());
  }

  @Test
  public void testReevaluateRepository_Authorized() {
    Repository repo = createRepository();

    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.reevaluateRepository(repo.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testReevaluateRepository_Unauthenticated() {
    repositoryService.reevaluateRepository("repository-id");
  }

  @Test(expected = UnauthorizedException.class)
  public void testReevaluateRepository_Unauthorized() {
    Repository repo = createRepository();

    login();
    repositoryService.reevaluateRepository(repo.getId());
  }

  @Test
  public void testDeleteRepository_Authorized() {
    Repository repo = createRepository();
    grantWritePermission(repo.getId());
    repositoryService.deleteRepository(repo.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteRepository_Unauthenticated() {
    repositoryService.deleteRepository("repository-id");
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteRepository_Unauthorized() {
    Repository repo = createRepository();
    login();
    repositoryService.deleteRepository(repo.getId());
  }

  @Test
  public void testGetRepositories_Authorized() {
    Repository repository = createRepository();
    grantReadPermission(repository.getId());
    Repository repository2 = tempEntity.newRepository();
    RepositoriesDTO repositories = repositoryService.getRepositories();
    assertThat(repositories.repositories, hasSize(1));
    assertThat(repositories.repositories.get(0).repository.getId(), equalTo(repository.getId()));
    grantReadPermission(repository2.getId());
    repositories = repositoryService.getRepositories();
    assertThat(repositories.repositories, hasSize(2));
  }

  @Test
  public void testGetRepositories_Unauthenticated() {
    createRepository();
    RepositoriesDTO repositories = repositoryService.getRepositories();
    assertNull(repositories.repositories);
  }

  @Test
  public void testGetRepositories_Unauthorized() {
    createRepository();
    login();
    RepositoriesDTO repositories = repositoryService.getRepositories();
    assertNull(repositories.repositories);
  }
}
