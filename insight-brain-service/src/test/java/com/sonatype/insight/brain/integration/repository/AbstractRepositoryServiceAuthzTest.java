/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.integration.repository.AbstractRepositoryService.RepositoriesDTO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractRepositoryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String MANUAL_REPO_MAN_INSTANCE_ID = "manualDeleteRepoManagerInstanceId";

  private static final String REPOSITORY_PUBLIC_ID = "publicId";

  private RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  @Mock
  private RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  protected abstract AbstractRepositoryService getRepositoryService();

  @After
  public void cleanup() {
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(MANUAL_REPO_MAN_INSTANCE_ID);
    if (repositoryManager != null) {
      repositoryManagerDAO.delete(repositoryManager);
    }
  }

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(RepositoryPolicyEvaluator.class).toInstance(repositoryPolicyEvaluator);
  }

  @Test
  public void testUnquarantineComponent_Authorized() {
    String path = "path";
    Repository repository = createRepository();

    grantWritePermission(repository.getId());
    try {
      getRepositoryService().unquarantineComponent(repository.getId(), path, null);
    }
    catch (NotFoundException e) {
      // We are testing access permissions and we don't care if the component exists
      // This avoids the need to mock the HDS client for the permissions test
      assertThat(e).hasMessage(
          "Cannot find a component with path " + path + " in repository with ID " + repository.getId() + ".");
    }
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUnquarantineComponent_Unauthenticated() {
    Repository repository = createRepository();

    getRepositoryService().unquarantineComponent(repository.getId(), "path", null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUnquarantineComponent_Unauthorized() {
    Repository repository = createRepository();

    login();
    getRepositoryService().unquarantineComponent(repository.getId(), "path", null);
  }

  @Test
  public void testGetPolicyThreats_Authorized() {
    Repository repo = createRepository();
    String path = "path";
    tempEntity.newRepositoryComponent(repo.getId(), path, new Date(), null);

    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().getPolicyThreats(repo.getId(), path);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyThreats_Unauthenticated() {
    Repository repo = createRepository();

    getRepositoryService().getPolicyThreats(repo.getId(), "path");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyThreats_Unauthorized() {
    Repository repo = createRepository();

    login();
    getRepositoryService().getPolicyThreats(repo.getId(), "path");
  }

  @Test
  public void testSetEnabled_NewRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetEnabled_NewRepository_Unauthenticated() {
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetEnabled_NewRepository_Unauthorized() {
    grantWritePermission();
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true);
  }

  @Test
  public void testSetEnabled_ExistingRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), true);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetEnabled_ExistingRepository_Unauthenticated() {
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), true);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetEnabled_ExistingRepository_Unauthorized() {
    grantWritePermission();
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), true);
  }

  private Repository createRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(MANUAL_REPO_MAN_INSTANCE_ID);
    return tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testSetQuarantine_Authorized() {
    createRepository();
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().setQuarantine(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetQuarantine_Unauthenticated() {
    createRepository();
    getRepositoryService().setQuarantine(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetQuarantine_Unauthorized() {
    createRepository();
    grantWritePermission();
    getRepositoryService().setQuarantine(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true);
  }

  @Test
  public void testGetPolicyEvaluationSummary_Authorized() {
    createRepository();
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyEvaluationSummary_Unauthenticated() {
    createRepository();
    getRepositoryService().getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyEvaluationSummary_Unauthorized() {
    createRepository();
    grantWritePermission();
    getRepositoryService().getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testEvaluateComponents_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
        null /* componentEvaluationDataRequestList */, false, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateComponents_Unauthenticated() {
    getRepositoryService().evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
        null /* componentEvaluationDataRequestList */, false, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateComponents_Unauthorized() {
    grantWritePermission();
    getRepositoryService().evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
        null /* componentEvaluationDataRequestList */, false, null);
  }

  @Test
  public void testGetReportSummary_Authorized() {
    Repository repo = createRepository();
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    getRepositoryService().getReportSummary(repo.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetReportSummary_Unauthenticated() {
    Repository repo = createRepository();
    getRepositoryService().getReportSummary(repo.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetReportSummary_Unauthorized() {
    grantWritePermission();
    Repository repo = createRepository();

    getRepositoryService().getReportSummary(repo.getId());
  }

  @Test
  public void testGetReportDetails_Authorized() {
    Repository repo = createRepository();
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().getReportDetails(repo.getId(), null, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetReportDetails_Unauthenticated() {
    Repository repo = createRepository();
    getRepositoryService().getReportDetails(repo.getId(), null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetReportDetails_Unauthorized() {
    grantWritePermission();
    Repository repo = createRepository();
    getRepositoryService().getReportDetails(repo.getId(), null, null);
  }

  @Test
  public void testRemoveComponent_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), "somepath");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveComponent_Unauthenticated() {
    getRepositoryService().removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), "somepath");
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveComponent_Unauthorized() {
    grantWritePermission();
    getRepositoryService().removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), "somepath");
  }

  @Test
  public void testGetRepositoryById_Authorized() {
    Repository repo = createRepository();

    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().getRepositoryById(repo.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetRepositoryById_Unauthenticated() {
    getRepositoryService().getRepositoryById("repository-id");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetRepositoryById_Unauthorized() {
    Repository repo = createRepository();

    login();
    getRepositoryService().getRepositoryById(repo.getId());
  }

  @Test
  public void testReevaluateRepository_Authorized() {
    Repository repo = createRepository();

    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().reevaluateRepository(repo.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testReevaluateRepository_Unauthenticated() {
    getRepositoryService().reevaluateRepository("repository-id");
  }

  @Test(expected = UnauthorizedException.class)
  public void testReevaluateRepository_Unauthorized() {
    Repository repo = createRepository();

    login();
    getRepositoryService().reevaluateRepository(repo.getId());
  }

  @Test
  public void testDeleteRepository_Authorized() {
    Repository repo = createRepository();
    grantWritePermission(repo.getId());
    getRepositoryService().deleteRepository(repo.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteRepository_Unauthenticated() {
    getRepositoryService().deleteRepository("repository-id");
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteRepository_Unauthorized() {
    Repository repo = createRepository();
    login();
    getRepositoryService().deleteRepository(repo.getId());
  }

  @Test
  public void testGetRepositories_Authorized() {
    Repository repository = createRepository();
    grantReadPermission(repository.getId());
    Repository repository2 = tempEntity.newRepository();
    RepositoriesDTO repositories = getRepositoryService().getRepositories();
    assertThat(repositories.repositories).hasSize(1);
    assertThat(repositories.repositories.get(0).repository.getId()).isEqualTo(repository.getId());
    grantReadPermission(repository2.getId());
    repositories = getRepositoryService().getRepositories();
    assertThat(repositories.repositories).hasSize(2);
  }

  @Test
  public void testGetRepositories_Unauthenticated() {
    createRepository();
    RepositoriesDTO repositories = getRepositoryService().getRepositories();
    assertThat(repositories.repositories).isNull();
  }

  @Test
  public void testGetRepositories_Unauthorized() {
    createRepository();
    login();
    RepositoriesDTO repositories = getRepositoryService().getRepositories();
    assertThat(repositories.repositories).isNull();
  }

  @Test
  public void testReevaluateComponent_Authorized() {
    Repository repo = createRepository();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().reevaluateComponent(repo.getId(), component.getHash(), null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testReevaluateComponent_Unauthenticated() {
    getRepositoryService().reevaluateComponent("repository-id", "component-hash", null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testReevaluateComponent_Unauthorized() {
    Repository repo = createRepository();

    login();
    getRepositoryService().reevaluateComponent(repo.getId(), "some-hash", null);
  }

  @Test
  public void testGetUnquarantinedComponents_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().getUnquarantinedComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), 0);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetUnquarantinedComponents_Unauthenticated() {
    getRepositoryService().getUnquarantinedComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), 0);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetUnquarantinedComponents_Unauthorized() {
    login();
    getRepositoryService().getUnquarantinedComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), 0);
  }
}
