/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;

import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import jakarta.inject.Inject;
import java.util.List;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class AbstractRepositoryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  protected static final String MANUAL_REPO_MAN_INSTANCE_ID = "manualDeleteRepoManagerInstanceId";

  protected static final String REPOSITORY_PUBLIC_ID = "publicId";

  @Mock
  private RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  protected abstract AbstractRepositoryService getRepositoryService();

  protected abstract ConfigureRepositoriesRequest createConfigureRepositoriesRequest();

  @Mock
  private DbQuarantinedComponentAccessManager quarantinedComponentAccessManager;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Test
  public void testSetAuditEnabled_NewRepositoryManager_NewRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().setAuditEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test
  public void testSetAuditEnabled_NewRepositoryManager_NewRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      getRepositoryService().setAuditEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
    });
  }

  @Test
  public void testSetAuditEnabled_NewRepositoryManager_NewRepository_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      login();
      getRepositoryService().setAuditEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
    });
  }

  @Test
  public void testSetAuditEnabled_ExistingRepositoryManager_NewRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    grantEvaluateComponentPermission(repoManager.getId());
    getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test
  public void testSetAuditEnabled_ExistingRepositoryManager_NewRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), REPOSITORY_PUBLIC_ID, true, null);
    });
  }

  @Test
  public void testSetAuditEnabled_ExistingRepositoryManager_NewRepository_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      login();
      getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), REPOSITORY_PUBLIC_ID, true, null);
    });
  }

  @Test
  public void testSetAuditEnabled_ExistingRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "testPublicId");
    grantEvaluateComponentPermission(repo.getId());
    getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), repo.getPublicId(), true, null);
  }

  @Test
  public void testSetAuditEnabled_ExistingRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      Repository repo = tempEntity.newRepository(repoManager, "testPublicId");
      getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), repo.getPublicId(), true, null);
    });
  }

  @Test
  public void testSetAuditEnabled_ExistingRepository_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      Repository repo = tempEntity.newRepository(repoManager, "testPublicId");
      login();
      getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), repo.getPublicId(), true, null);
    });
  }

  protected Repository createRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(MANUAL_REPO_MAN_INSTANCE_ID);
    return tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testSetQuarantine_Authorized() {
    createRepository();
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().setQuarantine(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test
  public void testSetQuarantine_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      createRepository();
      getRepositoryService().setQuarantine(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
    });
  }

  @Test
  public void testSetQuarantine_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      createRepository();
      grantWritePermission();
      getRepositoryService().setQuarantine(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
    });
  }

  @Test
  public void testGetPolicyEvaluationSummary_Authorized() {
    createRepository();
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, null);
  }

  @Test
  public void testGetPolicyEvaluationSummary_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      createRepository();
      getRepositoryService().getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, null);
    });
  }

  @Test
  public void testGetPolicyEvaluationSummary_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      createRepository();
      grantWritePermission();
      getRepositoryService().getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, null);
    });
  }

  @Test
  public void testGetRepositoryResultsUrl_Authorized() {
    Repository repo = createRepository();
    grantPermission(repo.getId(), Permission.EVALUATE_COMPONENT);
    getRepositoryService().getRepositoryResultsUrl(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, null);
  }

  @Test
  public void testGetRepositoryResultsUrl_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      createRepository();
      getRepositoryService().getRepositoryResultsUrl(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, null);
    });
  }

  @Test
  public void testGetRepositoryResultsUrl_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      createRepository();
      login();
      getRepositoryService().getRepositoryResultsUrl(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, null);
    });
  }

  @Test
  public void testEvaluateComponents_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
        null /* componentEvaluationDataRequestList */, false, null);
  }

  @Test
  public void testEvaluateComponents_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      getRepositoryService().evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
          null /* componentEvaluationDataRequestList */, false, null);
    });
  }

  @Test
  public void testEvaluateComponents_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      grantWritePermission();
      getRepositoryService().evaluateComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
          null /* componentEvaluationDataRequestList */, false, null);
    });
  }

  @Test
  public void testRemoveComponent_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), "somepath",
        null);
  }

  @Test
  public void testRemoveComponent_Authorized_DockerProxyRepository() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    Repository repository = createRepository();
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setFormat("docker");

    String pathname = "containerImage-tag";
    Application application = tempEntity.newApplicationWithParent("test-app-" + tempEntity.uuid(), pathname);
    grantEvaluateComponentPermission(application.getId());

    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    getRepositoryService().removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, repository.getPublicId(), pathname, null);
  }

  @Test
  public void testRemoveComponent_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      getRepositoryService().removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), "somepath",
          null);
    });
  }

  @Test
  public void testRemoveComponent_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      grantWritePermission();
      getRepositoryService().removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), "somepath",
          null);
    });
  }

  @Test
  public void testRemoveComponent_Unauthorized_DockerProxyRepository() {
    assertThrows(UnauthorizedException.class, () -> {
      login();

      Repository repository = createRepository();
      repository.setRepositoryType(RepositoryType.proxy);
      repository.setFormat("docker");

      String pathname = "containerImage-tag";
      Application application = tempEntity.newApplicationWithParent("test-app-" + tempEntity.uuid(), pathname);

      repository.setRelatedOrganizationId(application.getOrganizationId());
      repositoryDAO.update(repository);

      getRepositoryService().removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, repository.getPublicId(), pathname, null);
    });
  }

  @Test
  public void testGetUnquarantinedComponents_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().getUnquarantinedComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), 0,
        null);
  }

  @Test
  public void testGetUnquarantinedComponents_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      getRepositoryService().getUnquarantinedComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
          0,
          null);
    });
  }

  @Test
  public void testGetUnquarantinedComponents_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      login();
      getRepositoryService().getUnquarantinedComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
          0,
          null);
    });
  }

  @Test
  public void testAddProprietaryComponentNames_ExistingRepositoryManager_NewRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(),
          "testRepoPublicId", new ProprietaryComponentNames("npm", "private"));
    });
  }

  @Test
  public void testAddProprietaryComponentNames_ExistingRepositoryManager_NewRepository_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      login();
      getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(),
          "testRepoPublicId", new ProprietaryComponentNames("npm", "private"));
    });
  }

  @Test
  public void testAddProprietaryComponentNames_ExistingRepositoryManager_NewRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    grantEvaluateComponentPermission(repoManager.getId());
    getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(),
        "testRepoPublicId", new ProprietaryComponentNames("npm", "private"));
  }

  @Test
  public void testAddProprietaryComponentNames_ExistingRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
      getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId(),
          new ProprietaryComponentNames("npm", "private"));
    });
  }

  @Test
  public void testAddProprietaryComponentNames_ExistingRepository_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
      login();
      getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId(),
          new ProprietaryComponentNames("npm", "private"));
    });
  }

  @Test
  public void testAddProprietaryComponentNames_ExistingRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    grantEvaluateComponentPermission(repo.getId());
    getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId(),
        new ProprietaryComponentNames("npm", "private"));
  }

  @Test
  public void testAddProprietaryComponentNames_NewRepositoryManager_NewRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      getRepositoryService().addProprietaryComponentNames("testRepoManagerInstanceId", "testRepoPublicId",
          new ProprietaryComponentNames("npm", "private"));
    });
  }

  @Test
  public void testAddProprietaryComponentNames_NewRepositoryManager_NewRepository_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      login();
      getRepositoryService().addProprietaryComponentNames("testRepoManagerInstanceId", "testRepoPublicId",
          new ProprietaryComponentNames("npm", "private"));
    });
  }

  @Test
  public void testAddProprietaryComponentNames_NewRepositoryManager_NewRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().addProprietaryComponentNames("testRepoManagerInstanceId", "testRepoPublicId",
        new ProprietaryComponentNames("npm", "private"));
  }

  // If it's a new repository, then there is nothing to remove, so it's a noop.
  @Test
  public void testRemoveProprietaryComponentNames_ExistingRepositoryManager_NewRepository_Unauthenticated() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    getRepositoryService().removeProprietaryComponentNames(repoManager.getInstanceId(), "testRepoPublicId");
  }

  // If it's a new repository, then there is nothing to remove, so it's a noop.
  @Test
  public void testRemoveProprietaryComponentNames_ExistingRepositoryManager_NewRepository_Unauthorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    login();
    getRepositoryService().removeProprietaryComponentNames(repoManager.getInstanceId(), "testRepoPublicId");
  }

  // If it's a new repository, then there is nothing to remove, so it's a noop.
  @Test
  public void testRemoveProprietaryComponentNames_ExistingRepositoryManager_NewRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    grantEvaluateComponentPermission(repoManager.getId());
    getRepositoryService().removeProprietaryComponentNames(repoManager.getInstanceId(), "testRepoPublicId");
  }

  @Test
  public void testRemoveProprietaryComponentNames_ExistingRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
      getRepositoryService().removeProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId());
    });
  }

  @Test
  public void testRemoveProprietaryComponentNames_ExistingRepository_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
      login();
      getRepositoryService().removeProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId());
    });
  }

  @Test
  public void testRemoveProprietaryComponentNames_ExistingRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    grantEvaluateComponentPermission(repo.getId());
    getRepositoryService().removeProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId());
  }

  // If it's a new repository, then there is nothing to remove, so it's a noop.
  @Test
  public void testRemoveProprietaryComponentNames_NewRepositoryManager_NewRepository_Unauthenticated() {
    getRepositoryService().removeProprietaryComponentNames("testRepoManagerInstanceId", "testRepoPublicId");
  }

  // If it's a new repository, then there is nothing to remove, so it's a noop.
  @Test
  public void testRemoveProprietaryComponentNames_NewRepositoryManager_NewRepository_Unauthorized() {
    login();
    getRepositoryService().removeProprietaryComponentNames("testRepoManagerInstanceId", "testRepoPublicId");
  }

  // If it's a new repository, then there is nothing to remove, so it's a noop.
  @Test
  public void testRemoveProprietaryComponentNames_NewRepositoryManager_NewRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().removeProprietaryComponentNames("testRepoManagerInstanceId", "testRepoPublicId");
  }

  @Test
  public void testGetQuarantinedComponentReportUrl_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
      final Repository repository = tempEntity.newRepository(repositoryManager, "repo");
      getRepositoryService().getQuarantinedComponentReportUrl(repositoryManager.getInstanceId(),
          repository.getPublicId(),
          "", null);
    });
  }

  @Test
  public void testGetQuarantinedComponentReportUrl_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      login();
      final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
      final Repository repository = tempEntity.newRepository(repositoryManager, "repo");
      getRepositoryService().getQuarantinedComponentReportUrl(repositoryManager.getInstanceId(),
          repository.getPublicId(),
          "", null);
    });
  }

  @Test
  public void testGetQuarantinedComponentReportUrl_Authorized() {
    // setup
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repo");
    tempEntity.newRepositoryComponent(repository.getId());
    when(quarantinedComponentAccessManager.createToken(any())).thenReturn("token");

    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().getQuarantinedComponentReportUrl(repositoryManager.getInstanceId(), repository.getPublicId(),
        "path", null);
  }

  @Test
  public void testEvaluateComponentMetadata_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(MANUAL_REPO_MAN_INSTANCE_ID);
    Repository repository = tempEntity.newRepository(repositoryManager, REPOSITORY_PUBLIC_ID, true, true);
    getRepositoryService().evaluateComponentMetadata(MANUAL_REPO_MAN_INSTANCE_ID, repository.getPublicId(),
        null /* componentEvaluationDataRequestList */, null);
  }

  @Test
  public void testEvaluateComponentMetadata_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      getRepositoryService().evaluateComponentMetadata(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
          null /* componentEvaluationDataRequestList */, null);
    });
  }

  @Test
  public void testEvaluateComponentMetadata_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      grantWritePermission();
      getRepositoryService().evaluateComponentMetadata(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
          null /* componentEvaluationDataRequestList */, null);
    });
  }

  @Test
  public void testConfigureRepositories_NewRepositoryManager_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().configureRepositories(MANUAL_REPO_MAN_INSTANCE_ID, createConfigureRepositoriesRequest(),
        null);
  }

  @Test
  public void testConfigureRepositories_NewRepositoryManager_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      getRepositoryService().configureRepositories(MANUAL_REPO_MAN_INSTANCE_ID, null /* configureRepositoriesRequest */,
          null);
    });
  }

  @Test
  public void testConfigureRepositories_NewRepositoryManager_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      login();
      getRepositoryService().configureRepositories(MANUAL_REPO_MAN_INSTANCE_ID, null /* configureRepositoriesRequest */,
          null);
    });
  }

  @Test
  public void testConfigureRepositories_ExistingRepositoryManager_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    grantEvaluateComponentPermission(repoManager.getId());
    getRepositoryService().configureRepositories(repoManager.getInstanceId(), createConfigureRepositoriesRequest(),
        null);
  }

  @Test
  public void testConfigureRepositories_ExistingRepositoryManager_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      getRepositoryService().configureRepositories(repoManager.getInstanceId(), null /* configureRepositoriesRequest */,
          null);
    });
  }

  @Test
  public void testConfigureRepositories_ExistingRepositoryManager_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      login();
      getRepositoryService().configureRepositories(repoManager.getInstanceId(), null /* configureRepositoriesRequest */,
          null);
    });
  }

  @Test
  public void testRemoveRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      Repository repo = tempEntity.newRepository(repoManager, "testPublicId");
      getRepositoryService().removeRepository(repoManager.getInstanceId(), repo.getPublicId());
    });
  }

  @Test
  public void testRemoveRepository_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      Repository repo = tempEntity.newRepository(repoManager, "testPublicId");
      login();
      getRepositoryService().removeRepository(repoManager.getInstanceId(), repo.getPublicId());
    });
  }

  @Test
  public void testRemoveRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "testPublicId");

    grantEvaluateComponentPermission(repo.getId());

    getRepositoryService().removeRepository(repoManager.getInstanceId(), repo.getPublicId());
  }

  @Test
  public void testGetConfiguredRepositories_Authorized() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    grantEvaluateComponentPermission(repositoryManager.getId());

    getRepositoryService().getConfiguredRepositories(repositoryManager.getInstanceId(), 0L, null);
  }

  @Test
  public void testGetConfiguredRepositories_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
      getRepositoryService().getConfiguredRepositories(repositoryManager.getInstanceId(), 0L, null);
    });
  }

  @Test
  public void testGetConfiguredRepositories_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
      login();
      getRepositoryService().getConfiguredRepositories(repositoryManager.getInstanceId(), 0L, null);
    });
  }

  @Test
  public void testAddProprietaryNamespaceNames_ExistingRepository_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
      login();
      getRepositoryService().addProprietaryNamespaceNames(repoManager.getInstanceId(), repo.getPublicId(),
          "npm", List.of("org.sonatype"));
    });
  }

  @Test
  public void testAddProprietaryNamespaceNames_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
      Repository repo = tempEntity.newHostedRepository(repositoryManager, "testRepoPublicId", "npm", true);
      getRepositoryService().addProprietaryNamespaceNames(
          repositoryManager.getInstanceId(), repo.getPublicId(), "npm", List.of("org.sonatype"));
    });
  }

  @Test
  public void testAddProprietaryNamespaceNames_ExistingRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    grantEvaluateComponentPermission(repo.getId());
    getRepositoryService().addProprietaryNamespaceNames(repoManager.getInstanceId(), repo.getPublicId(),
        "npm", List.of("org.sonatype"));
  }

  @Test
  public void testRemoveProprietaryNamespaceNames_ExistingRepository_Unauthorized() {
    assertThrows(UnauthorizedException.class, () -> {
      RepositoryManager repoManager = tempEntity.newRepositoryManager();
      Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
      login();
      getRepositoryService().removeProprietaryNamespaceNames(repoManager.getInstanceId(), repo.getPublicId());
    });
  }

  @Test
  public void testRemoveProprietaryNamespaceNames_ExistingRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    grantEvaluateComponentPermission(repo.getId());
    getRepositoryService().removeProprietaryNamespaceNames(repoManager.getInstanceId(), repo.getPublicId());
  }
}
