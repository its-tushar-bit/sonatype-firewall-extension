/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  private final RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();
  
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

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test
  public void testGetPolicyThreats_Authorized() {
    Repository repo = createRepository();
    String path = "path";
    tempEntity.newRepositoryComponent(repo.getId(), path, new Date(), null);

    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.getPolicyThreats(repo.getId(), path);
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyThreats_Unauthenticated() {
    Repository repo = createRepository();

    repositoryService.getPolicyThreats(repo.getId(), "path");
  }

  /**
   * @deprecated The tested method is deprecated
   */
  @Deprecated
  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyThreats_Unauthorized() {
    Repository repo = createRepository();

    login();
    repositoryService.getPolicyThreats(repo.getId(), "path");
  }

  @Test
  public void testGetPolicyViolations_Authorized() {
    Repository repo = createRepository();
    String path = "path";
    tempEntity.newRepositoryComponent(repo.getId(), path, new Date(), null);

    grantReadPermission(repo.getId());
    repositoryService.getPolicyViolations(repo.getId(), path);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyViolations_Unauthenticated() {
    Repository repo = createRepository();

    repositoryService.getPolicyViolations(repo.getId(), "path");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyViolations_Unauthorized() {
    Repository repo = createRepository();

    login();
    repositoryService.getPolicyViolations(repo.getId(), "path");
  }

  @Test
  public void testGetPolicyViolation_Authorized() {
    Repository repo = createRepository();
    String path = "path";
    tempEntity.newRepositoryComponent(repo.getId(), path, new Date(), null);

    grantReadPermission(repo.getId());
    repositoryService.getPolicyViolation(repo.getId(), tempEntity.newRepositoryPolicyViolation(repo.getId()).getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyViolation_Unauthenticated() {
    Repository repo = createRepository();

    repositoryService.getPolicyViolation(repo.getId(), "testRepositoryPolicyViolationId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyViolation_Unauthorized() {
    Repository repo = createRepository();

    login();
    repositoryService.getPolicyViolation(repo.getId(), "testRepositoryPolicyViolationId");
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
    login();
    Repository repo = createRepository();

    repositoryService.getReportSummary(repo.getId());
  }

  /**
   * @deprecated The tested method is deprecated. To be removed when the Repository Results View migration to React is
   * completed (Epic: https://issues.sonatype.org/browse/CLM-20597)
   */
  @Test
  @Deprecated
  public void testGetReportDetails_Authorized() {
    Repository repo = createRepository();
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.getReportDetails(repo.getId(), null, null);
  }

  /**
   * @deprecated The tested method is deprecated. To be removed when the Repository Results View migration to React is
   * completed (Epic: https://issues.sonatype.org/browse/CLM-20597)
   */
  @Test(expected = UnauthenticatedException.class)
  @Deprecated
  public void testGetReportDetails_Unauthenticated() {
    Repository repo = createRepository();
    repositoryService.getReportDetails(repo.getId(), null, null);
  }

  /**
   * @deprecated The tested method is deprecated. To be removed when the Repository Results View migration to React is
   * completed (Epic: https://issues.sonatype.org/browse/CLM-20597)
   */
  @Test(expected = UnauthorizedException.class)
  @Deprecated
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
  public void testGetRepositoriesWithReadPermission_Unauthorized() {
    createRepository();
    login();
    List<Repository> repositories = repositoryService.getRepositoriesWithReadPermission();
    assertThat(repositories).isEmpty();
  }

  @Test
  public void testGetRepositoriesWithReadPermission_Unauthenticated() {
    createRepository();
    List<Repository> repositories = repositoryService.getRepositoriesWithReadPermission();
    assertThat(repositories).isEmpty();
  }

  @Test
  public void testGetRepositoriesWithReadPermission() {
    Repository repository = createRepository();
    Repository repository2 = tempEntity.newRepository();

    grantReadPermission(repository.getId());
    List<Repository> repositories = repositoryService.getRepositoriesWithReadPermission();

    assertThat(repositories)
        .as("Read permission given for only one of the repositories.")
        .hasSize(1);
    assertThat(repositories.get(0).getId()).isEqualTo(repository.getId());

    grantReadPermission(repository2.getId());
    repositories = repositoryService.getRepositoriesWithReadPermission();
    assertThat(repositories).hasSize(2);
  }

  @Test
  public void testGetRepositoriesByIds_Unauthorized() {
    Repository repo = createRepository();
    login();
    Set<Repository> repositories = repositoryService.getRepositoriesByIds(Collections.singleton(repo.getId()));
    assertThat(repositories).isEmpty();
  }

  @Test
  public void testGetRepositoriesByIds_Unauthenticated() {
    Repository repo = createRepository();
    Set<Repository> repositories = repositoryService.getRepositoriesByIds(Collections.singleton(repo.getId()));
    assertThat(repositories).isEmpty();
  }

  @Test
  public void testGetRepositoriesByIds() {
    Repository repo = createRepository();
    Repository repo2 = tempEntity.newRepository();

    grantReadPermission(repo.getId());
    Set<Repository> repositories = repositoryService.getRepositoriesByIds(Collections.singleton(repo.getId()));

    assertThat(repositories)
        .as("Read permission given for only one of the repositories.")
        .hasSize(1);
    assertThat(new ArrayList<>(repositories).get(0).getId()).isEqualTo(repo.getId());

    grantReadPermission(repo2.getId());
    repositories = repositoryService.getRepositoriesByIds(new HashSet<>(Arrays.asList(repo.getId(), repo2.getId())));
    assertThat(repositories).hasSize(2);
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

  @Test
  public void testGetProprietaryComponentNamePatterns_Authorized() {
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();
    request.page = 1;
    request.pageSize = 1;
    repositoryService.getProprietaryComponentNamePatterns(request);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetProprietaryComponentNamePatterns_Unauthenticated() {
    repositoryService.getProprietaryComponentNamePatterns(null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetProprietaryComponentNamePatterns_Unauthorized() {
    login();
    repositoryService.getProprietaryComponentNamePatterns(null);
  }
}
