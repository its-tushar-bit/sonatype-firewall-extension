/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.repository.ProprietaryComponentNamePatternDTO;
import com.sonatype.insight.brain.dto.repository.RepositoriesDTO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class RepositoryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private RepositoryService repositoryService;

  @Mock
  private RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  @Mock
  private HdsClient hdsClientMock;

  @Mock
  private TaskScheduler mockTaskScheduler;

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
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    return tempEntity.newRepository(repositoryManager, "testPublicId");
  }

  @Test
  public void testGetRepositorySummary_Authorized() {
    Repository repo = createRepository();
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    repositoryService.getRepositorySummary(repo.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetRepositorySummary_Unauthenticated() {
    Repository repo = createRepository();
    repositoryService.getRepositorySummary(repo.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetRepositorySummary_Unauthorized() {
    login();
    Repository repo = createRepository();

    repositoryService.getRepositorySummary(repo.getId());
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
  public void testGetRepositoriesWithReadPermissionByIds_Unauthorized() {
    Repository repo = createRepository();
    login();
    List<Repository> repositories =
        repositoryService.getRepositoriesWithReadPermissionByIds(Collections.singleton(repo.getId()));
    assertThat(repositories).isEmpty();
  }

  @Test
  public void testGetRepositoriesWithReadPermissionByIds_Unauthenticated() {
    Repository repo = createRepository();
    List<Repository> repositories =
        repositoryService.getRepositoriesWithReadPermissionByIds(Collections.singleton(repo.getId()));
    assertThat(repositories).isEmpty();
  }

  @Test
  public void testGetRepositoriesWithReadPermissionByIds() {
    Repository repo = createRepository();
    Repository repo2 = tempEntity.newRepository();

    grantReadPermission(repo.getId());
    List<Repository> repositories =
        repositoryService.getRepositoriesWithReadPermissionByIds(Collections.singleton(repo.getId()));

    assertThat(repositories)
        .as("Read permission given for only one of the repositories.")
        .hasSize(1);
    assertThat(repositories.get(0).getId()).isEqualTo(repo.getId());

    grantReadPermission(repo2.getId());
    repositories = repositoryService.getRepositoriesWithReadPermissionByIds(Set.of(repo.getId(), repo2.getId()));
    assertThat(repositories).hasSize(2);
  }

  @Test
  public void testReevaluateComponent_Authorized() {
    Repository repo = createRepository();
    ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId());
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
  public void testUpdateProprietaryComponentNamePattern_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    ProprietaryComponentNamePattern proprietaryComponentNamePattern = tempEntity.newProprietaryComponentNamePattern(
        repo, "namespacePattern", null);

    grantWritePermission(repo.getId());

    ProprietaryComponentNamePatternDTO request = new ProprietaryComponentNamePatternDTO(
        proprietaryComponentNamePattern.getId(), proprietaryComponentNamePattern.getFormat(),
        proprietaryComponentNamePattern.getNamespacePattern(), proprietaryComponentNamePattern.getNamePattern(),
        repoManager.getInstanceId(), repoManager.getName(), repo.getPublicId(), false /* enabled */);
    repositoryService.updateProprietaryComponentNamePattern(request);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateProprietaryComponentNamePattern_Unauthenticated() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    ProprietaryComponentNamePattern proprietaryComponentNamePattern =
        tempEntity.newProprietaryComponentNamePattern(repo, "namespacePattern", null);

    ProprietaryComponentNamePatternDTO request = new ProprietaryComponentNamePatternDTO(
        proprietaryComponentNamePattern.getId(), proprietaryComponentNamePattern.getFormat(),
        proprietaryComponentNamePattern.getNamespacePattern(), proprietaryComponentNamePattern.getNamePattern(),
        repoManager.getInstanceId(), repoManager.getName(), repo.getPublicId(), false /* enabled */);
    repositoryService.updateProprietaryComponentNamePattern(request);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateProprietaryComponentNamePattern_Unauthorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo =
        tempEntity.newRepository(repoManager, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    ProprietaryComponentNamePattern proprietaryComponentNamePattern =
        tempEntity.newProprietaryComponentNamePattern(repo, "namespacePattern", null);

    login();

    ProprietaryComponentNamePatternDTO request = new ProprietaryComponentNamePatternDTO(
        proprietaryComponentNamePattern.getId(), proprietaryComponentNamePattern.getFormat(),
        proprietaryComponentNamePattern.getNamespacePattern(), proprietaryComponentNamePattern.getNamePattern(),
        repoManager.getInstanceId(), repoManager.getName(), repo.getPublicId(), false /* enabled */);
    repositoryService.updateProprietaryComponentNamePattern(request);
  }

  @Test
  public void testCheckReadPermissionRepositoryContainer_Unauthenticated() {
    assertThat(repositoryService.checkReadPermissionRepositoryContainer()).isFalse();
  }

  @Test
  public void testCheckReadPermissionRepositoryContainer_Unauthorized() {
    login();
    assertThat(repositoryService.checkReadPermissionRepositoryContainer()).isFalse();
  }

  @Test
  public void testCheckReadPermissionRepositoryContainer_Authorized() {
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(repositoryService.checkReadPermissionRepositoryContainer()).isTrue();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetUnconfiguredRepositoryManagers_Unauthenticated() {
    repositoryService.getUnconfiguredRepositoryManagers();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetUnconfiguredRepositoryManagers_Unauthorized() {
    login();
    repositoryService.getUnconfiguredRepositoryManagers();
  }

  @Test
  public void testGetUnconfiguredRepositoryManagers_Authorized() {
    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.getUnconfiguredRepositoryManagers();
  }

  @Test
  public void testGetRepositoriesByRepositoryManagerId_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo1 =
        tempEntity.newRepository(repoManager, "testPublicId1", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    Repository repo2 =
        tempEntity.newRepository(repoManager, "testPublicId2", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);

    grantReadPermission(repo1.getId());
    RepositoriesDTO result = repositoryService.getRepositoriesByRepositoryManagerId(repoManager.getId());
    assertThat(result.repositories).hasSize(1);
    assertThat(result.repositories.get(0).repository.getId()).isEqualTo(repo1.getId());

    grantReadPermission(repo2.getId());
    result = repositoryService.getRepositoriesByRepositoryManagerId(repoManager.getId());
    assertThat(result.repositories).hasSize(2);
    List<String> repositoryIds =
        result.repositories.stream().map(dto -> dto.repository.getId()).collect(Collectors.toList());
    assertThat(repositoryIds).containsExactlyInAnyOrder(repo1.getId(), repo2.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testConfigureRepositories_Unauthenticated() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repositoryService.configureRepositories(repositoryManager.getId(), null /* repositoryDTOs */);
  }

  @Test(expected = UnauthorizedException.class)
  public void testConfigureRepositories_Unauthorized() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    login();
    repositoryService.configureRepositories(repositoryManager.getId(), null /* repositoryDTOs */);
  }

  @Test
  public void testConfigureRepositories_Authorized() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    grantWritePermission(repositoryManager.getId());
    repositoryService.configureRepositories(repositoryManager.getId(), null /* repositoryDTOs */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testConfigureFirewallOnboarding_Unauthenticated() {
    repositoryService.configureFirewallOnboarding(null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testConfigureFirewallOnboarding_Unauthorized() {
    login();
    repositoryService.configureFirewallOnboarding(null);
  }

  @Test
  public void testConfigureFirewallOnboarding_Authorized() {
    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    repositoryService.configureFirewallOnboarding(new FirewallOnboardingOptionsDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateName_Unauthenticated() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repositoryService.updateName(repositoryManager.getId(), "newName");
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateName_Unauthorized() {
    login();
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repositoryService.updateName(repositoryManager.getId(), "newName");
  }

  @Test
  public void testUpdateName_Authorized() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    grantWritePermission(repositoryManager.getId());
    repositoryService.updateName(repositoryManager.getId(), "newName");
  }

  @Test
  public void testGetRepositoryManagers_Unauthenticated() {
    tempEntity.newRepositoryManager();
    assertThat(repositoryService.getRepositoryManagers()).isEmpty();
  }

  @Test
  public void testGetRepositoryManagers_Unauthorized() {
    tempEntity.newRepositoryManager();
    login();
    List<RepositoryManager> allRepositoryManagers = repositoryService.getRepositoryManagers();
    assertThat(allRepositoryManagers).isEmpty();
  }

  @Test
  public void testGetRepositoryManagers_Authorized() {
    RepositoryManager repositoryManagerOne = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManagerTwo = tempEntity.newRepositoryManager();
    tempEntity.newRepositoryManager();

    grantReadPermission(repositoryManagerOne.getId());

    List<RepositoryManager> allRepositoryManagers = repositoryService.getRepositoryManagers();
    assertThat(allRepositoryManagers).hasSize(1);
    assertThat(allRepositoryManagers.get(0).getId()).isEqualTo(repositoryManagerOne.getId());

    grantReadPermission(repositoryManagerTwo.getId());

    List<String> allRepositoryManagersIds = repositoryService.getRepositoryManagers()
        .stream()
        .map(RepositoryManager::getId)
        .collect(toList());
    assertThat(allRepositoryManagersIds).containsExactlyInAnyOrder(
        repositoryManagerOne.getId(),
        repositoryManagerTwo.getId());
  }

  @Test
  public void testGetProprietaryComponentNamePatternsByOwner_Authorized() {
    RepositoryManager repoManager1 = tempEntity.newRepositoryManager();
    Repository repo1 =
        tempEntity.newRepository(repoManager1, "testPublicId", RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern1 =
        tempEntity.newProprietaryComponentNamePattern(repo1, "testNamespacePattern1", "testNamePattern1");
    Repository repo2 =
        tempEntity.newRepository(repoManager1, "testPublicId1", RepositoryType.hosted,
            ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern2 =
        tempEntity.newProprietaryComponentNamePattern(repo2, "testNamespacePattern3", "testNamePattern3");

    RepositoryManager repoManager2 = tempEntity.newRepositoryManager();
    Repository repo3 =
        tempEntity.newRepository(repoManager2, "testPublicId2", RepositoryType.hosted,
            ComponentIdentifier.FORMAT_MAVEN);
    ProprietaryComponentNamePattern pattern3 =
        tempEntity.newProprietaryComponentNamePattern(repo3, "testNamespacePattern4", "testNamePattern4");

    ProprietaryComponentNamePatternRequest request = new ProprietaryComponentNamePatternRequest();
    request.page = 1;
    request.pageSize = 3;

    grantReadPermission(repo1.getId());
    // Repository Level - result must include only patterns of repo1
    ProprietaryComponentNamePatternsPage result =
        repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY, repo1.getId(), request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(1);
    assertThat(result.proprietaryComponentNamePatterns.get(0).id).isEqualTo(pattern1.getId());

    result = repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY, repo2.getId(), request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(0);

    result = repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER,
        repoManager1.getId(), request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(1);
    assertThat(result.proprietaryComponentNamePatterns.get(0).id).isEqualTo(pattern1.getId());
    result = repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER,
        repoManager2.getId(), request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(0);
    result = repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(1);
    assertThat(result.proprietaryComponentNamePatterns.get(0).id).isEqualTo(pattern1.getId());

    grantReadPermission(repoManager1.getId());
    // Repository Manager Level - result must include only patterns of repos in repoManager1
    result = repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY, repo2.getId(), request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(1);
    assertThat(result.proprietaryComponentNamePatterns.get(0).id).isEqualTo(pattern2.getId());

    result =
        repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER, repoManager1.getId(),
            request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(2);
    assertThat(result.proprietaryComponentNamePatterns.get(0).id).isEqualTo(pattern1.getId());
    assertThat(result.proprietaryComponentNamePatterns.get(1).id).isEqualTo(pattern2.getId());

    result =
        repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER, repoManager2.getId(),
            request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(0);

    result = repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(2);
    assertThat(result.proprietaryComponentNamePatterns.get(0).id).isEqualTo(pattern1.getId());
    assertThat(result.proprietaryComponentNamePatterns.get(1).id).isEqualTo(pattern2.getId());

    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    // Repository Container Level - result must include patterns of all repos
    result = repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY, repo1.getId(), request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(1);
    assertThat(result.proprietaryComponentNamePatterns.get(0).id).isEqualTo(pattern1.getId());
    result = repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY, repo2.getId(), request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(1);
    assertThat(result.proprietaryComponentNamePatterns.get(0).id).isEqualTo(pattern2.getId());

    result = repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER,
        repoManager1.getId(), request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(2);
    assertThat(result.proprietaryComponentNamePatterns.get(0).id).isEqualTo(pattern1.getId());
    assertThat(result.proprietaryComponentNamePatterns.get(1).id).isEqualTo(pattern2.getId());
    result = repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_MANAGER,
        repoManager2.getId(), request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(1);
    assertThat(result.proprietaryComponentNamePatterns.get(0).id).isEqualTo(pattern3.getId());

    result = repositoryService.getProprietaryComponentNamePatternsByOwner(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, request);
    assertThat(result.proprietaryComponentNamePatterns).hasSize(3);
    assertThat(result.proprietaryComponentNamePatterns.get(0).id).isEqualTo(pattern1.getId());
    assertThat(result.proprietaryComponentNamePatterns.get(1).id).isEqualTo(pattern2.getId());
    assertThat(result.proprietaryComponentNamePatterns.get(2).id).isEqualTo(pattern3.getId());
  }
}
