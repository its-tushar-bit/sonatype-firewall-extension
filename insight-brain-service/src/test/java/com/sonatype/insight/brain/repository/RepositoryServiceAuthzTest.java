/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dto.repository.RepositoriesDTO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String MANUAL_REPO_MAN_INSTANCE_ID = "manualDeleteRepoManagerInstanceId";

  private static final String REPOSITORY_PUBLIC_ID = "publicId";

  private RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();
  
  @Inject
  private RepositoryService repositoryService;

  @Mock
  private RepositoryPolicyEvaluator repositoryPolicyEvaluator;

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
      repositoryService.unquarantineComponent(repository.getId(), path, null);
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

    repositoryService.unquarantineComponent(repository.getId(), "path", null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUnquarantineComponent_Unauthorized() {
    Repository repository = createRepository();

    login();
    repositoryService.unquarantineComponent(repository.getId(), "path", null);
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

  private Repository createRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(MANUAL_REPO_MAN_INSTANCE_ID);
    return tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
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
    repositoryService.getReportDetails(repo.getId(), null, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetReportDetails_Unauthenticated() {
    Repository repo = createRepository();
    repositoryService.getReportDetails(repo.getId(), null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetReportDetails_Unauthorized() {
    grantWritePermission();
    Repository repo = createRepository();
    repositoryService.getReportDetails(repo.getId(), null, null);
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
    assertThat(repositories.repositories).hasSize(1);
    assertThat(repositories.repositories.get(0).repository.getId()).isEqualTo(repository.getId());
    grantReadPermission(repository2.getId());
    repositories = repositoryService.getRepositories();
    assertThat(repositories.repositories).hasSize(2);
  }

  @Test
  public void testGetRepositories_Unauthenticated() {
    createRepository();
    RepositoriesDTO repositories = repositoryService.getRepositories();
    assertThat(repositories.repositories).isNull();
  }

  @Test
  public void testGetRepositories_Unauthorized() {
    createRepository();
    login();
    RepositoriesDTO repositories = repositoryService.getRepositories();
    assertThat(repositories.repositories).isNull();
  }

  @Test
  public void testReevaluateComponent_Authorized() {
    Repository repo = createRepository();
    RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.reevaluateComponent(repo.getId(), component.getHash(), null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testReevaluateComponent_Unauthenticated() {
    repositoryService.reevaluateComponent("repository-id", "component-hash", null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testReevaluateComponent_Unauthorized() {
    Repository repo = createRepository();

    login();
    repositoryService.reevaluateComponent(repo.getId(), "some-hash", null);
  }

  @Test
  public void testGetPolicyEvaluationTimestamps_Authorized() {
    Repository repo = createRepository();

    grantReadPermission(repo.getId());
    repositoryService.getPolicyEvaluationTimestamps(repo.getId(),
        ComponentIdentifier.createNpmCoordinates("packageId", "version"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyEvaluationTimestamps_Unauthenticated() {
    repositoryService.getPolicyEvaluationTimestamps("repository-id",
        ComponentIdentifier.createNpmCoordinates("packageId", "version"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyEvaluationTimestamps_Unauthorized() {
    Repository repo = createRepository();

    login();
    repositoryService.getPolicyEvaluationTimestamps(repo.getId(),
        ComponentIdentifier.createNpmCoordinates("packageId", "version"));
  }
}
