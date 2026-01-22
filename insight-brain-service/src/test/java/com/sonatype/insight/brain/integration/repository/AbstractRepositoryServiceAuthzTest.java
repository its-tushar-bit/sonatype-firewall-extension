/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

  @Override
  public void configure(Binder binder) {
    binder.bind(RepositoryPolicyEvaluator.class).toInstance(repositoryPolicyEvaluator);
    binder.bind(DbQuarantinedComponentAccessManager.class).toInstance(quarantinedComponentAccessManager);
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    super.configure(binder);
  }

  @Test
  public void testSetAuditEnabled_NewRepositoryManager_NewRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().setAuditEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetAuditEnabled_NewRepositoryManager_NewRepository_Unauthenticated() {
    getRepositoryService().setAuditEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetAuditEnabled_NewRepositoryManager_NewRepository_Unauthorized() {
    login();
    getRepositoryService().setAuditEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test
  public void testSetAuditEnabled_ExistingRepositoryManager_NewRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    grantEvaluateComponentPermission(repoManager.getId());
    getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetAuditEnabled_ExistingRepositoryManager_NewRepository_Unauthenticated() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetAuditEnabled_ExistingRepositoryManager_NewRepository_Unauthorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    login();
    getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test
  public void testSetAuditEnabled_ExistingRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "testPublicId");
    grantEvaluateComponentPermission(repo.getId());
    getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), repo.getPublicId(), true, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetAuditEnabled_ExistingRepository_Unauthenticated() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "testPublicId");
    getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), repo.getPublicId(), true, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetAuditEnabled_ExistingRepository_Unauthorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "testPublicId");
    login();
    getRepositoryService().setAuditEnabled(repoManager.getInstanceId(), repo.getPublicId(), true, null);
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

  @Test(expected = UnauthenticatedException.class)
  public void testSetQuarantine_Unauthenticated() {
    createRepository();
    getRepositoryService().setQuarantine(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetQuarantine_Unauthorized() {
    createRepository();
    grantWritePermission();
    getRepositoryService().setQuarantine(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test
  public void testGetPolicyEvaluationSummary_Authorized() {
    createRepository();
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyEvaluationSummary_Unauthenticated() {
    createRepository();
    getRepositoryService().getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyEvaluationSummary_Unauthorized() {
    createRepository();
    grantWritePermission();
    getRepositoryService().getPolicyEvaluationSummary(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, null);
  }

  @Test
  public void testGetRepositoryResultsUrl_Authorized() {
    Repository repo = createRepository();
    grantPermission(repo.getId(), Permission.EVALUATE_COMPONENT);
    getRepositoryService().getRepositoryResultsUrl(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetRepositoryResultsUrl_Unauthenticated() {
    createRepository();
    getRepositoryService().getRepositoryResultsUrl(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetRepositoryResultsUrl_Unauthorized() {
    createRepository();
    login();
    getRepositoryService().getRepositoryResultsUrl(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, null);
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
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplicationWithParent(organization.getId(), pathname);
    grantEvaluateComponentPermission(application.getId());

    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    getRepositoryService().removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, repository.getPublicId(), pathname, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveComponent_Unauthenticated() {
    getRepositoryService().removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), "somepath",
        null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveComponent_Unauthorized() {
    grantWritePermission();
    getRepositoryService().removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), "somepath",
        null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveComponent_Unauthorized_DockerProxyRepository() {
    login();

    Repository repository = createRepository();
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setFormat("docker");

    String pathname = "containerImage-tag";
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplicationWithParent(organization.getId(), pathname);

    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    getRepositoryService().removeComponent(MANUAL_REPO_MAN_INSTANCE_ID, repository.getPublicId(), pathname, null);
  }

  @Test
  public void testGetUnquarantinedComponents_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().getUnquarantinedComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), 0,
        null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetUnquarantinedComponents_Unauthenticated() {
    getRepositoryService().getUnquarantinedComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), 0,
        null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetUnquarantinedComponents_Unauthorized() {
    login();
    getRepositoryService().getUnquarantinedComponents(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), 0,
        null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddProprietaryComponentNames_ExistingRepositoryManager_NewRepository_Unauthenticated() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(),
        "testRepoPublicId", new ProprietaryComponentNames("npm", "private"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddProprietaryComponentNames_ExistingRepositoryManager_NewRepository_Unauthorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    login();
    getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(),
        "testRepoPublicId", new ProprietaryComponentNames("npm", "private"));
  }

  @Test
  public void testAddProprietaryComponentNames_ExistingRepositoryManager_NewRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    grantEvaluateComponentPermission(repoManager.getId());
    getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(),
        "testRepoPublicId", new ProprietaryComponentNames("npm", "private"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddProprietaryComponentNames_ExistingRepository_Unauthenticated() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId(),
        new ProprietaryComponentNames("npm", "private"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddProprietaryComponentNames_ExistingRepository_Unauthorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    login();
    getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId(),
        new ProprietaryComponentNames("npm", "private"));
  }

  @Test
  public void testAddProprietaryComponentNames_ExistingRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    grantEvaluateComponentPermission(repo.getId());
    getRepositoryService().addProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId(),
        new ProprietaryComponentNames("npm", "private"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddProprietaryComponentNames_NewRepositoryManager_NewRepository_Unauthenticated() {
    getRepositoryService().addProprietaryComponentNames("testRepoManagerInstanceId", "testRepoPublicId",
        new ProprietaryComponentNames("npm", "private"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddProprietaryComponentNames_NewRepositoryManager_NewRepository_Unauthorized() {
    login();
    getRepositoryService().addProprietaryComponentNames("testRepoManagerInstanceId", "testRepoPublicId",
        new ProprietaryComponentNames("npm", "private"));
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

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveProprietaryComponentNames_ExistingRepository_Unauthenticated() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    getRepositoryService().removeProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveProprietaryComponentNames_ExistingRepository_Unauthorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    login();
    getRepositoryService().removeProprietaryComponentNames(repoManager.getInstanceId(), repo.getPublicId());
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

  @Test(expected = UnauthenticatedException.class)
  public void testGetQuarantinedComponentReportUrl_Unauthenticated() {
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repo");
    getRepositoryService().getQuarantinedComponentReportUrl(repositoryManager.getInstanceId(), repository.getPublicId(),
        "", null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetQuarantinedComponentReportUrl_Unauthorized() {
    login();
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repo");
    getRepositoryService().getQuarantinedComponentReportUrl(repositoryManager.getInstanceId(), repository.getPublicId(),
        "", null);
  }

  @Test
  public void testGetQuarantinedComponentReportUrl_Authorized() {
    //setup
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

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateComponentMetadata_Unauthenticated() {
    getRepositoryService().evaluateComponentMetadata(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
        null /* componentEvaluationDataRequestList */, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateComponentMetadata_Unauthorized() {
    grantWritePermission();
    getRepositoryService().evaluateComponentMetadata(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
        null /* componentEvaluationDataRequestList */, null);
  }

  @Test
  public void testConfigureRepositories_NewRepositoryManager_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().configureRepositories(MANUAL_REPO_MAN_INSTANCE_ID, createConfigureRepositoriesRequest(),
        null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testConfigureRepositories_NewRepositoryManager_Unauthenticated() {
    getRepositoryService().configureRepositories(MANUAL_REPO_MAN_INSTANCE_ID, null /* configureRepositoriesRequest */,
        null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testConfigureRepositories_NewRepositoryManager_Unauthorized() {
    login();
    getRepositoryService().configureRepositories(MANUAL_REPO_MAN_INSTANCE_ID, null /* configureRepositoriesRequest */,
        null);
  }

  @Test
  public void testConfigureRepositories_ExistingRepositoryManager_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    grantEvaluateComponentPermission(repoManager.getId());
    getRepositoryService().configureRepositories(repoManager.getInstanceId(), createConfigureRepositoriesRequest(),
        null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testConfigureRepositories_ExistingRepositoryManager_Unauthenticated() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    getRepositoryService().configureRepositories(repoManager.getInstanceId(), null /* configureRepositoriesRequest */,
        null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testConfigureRepositories_ExistingRepositoryManager_Unauthorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    login();
    getRepositoryService().configureRepositories(repoManager.getInstanceId(), null /* configureRepositoriesRequest */,
        null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveRepository_Unauthenticated() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "testPublicId");
    getRepositoryService().removeRepository(repoManager.getInstanceId(), repo.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveRepository_Unauthorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "testPublicId");
    login();
    getRepositoryService().removeRepository(repoManager.getInstanceId(), repo.getPublicId());
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

  @Test(expected = UnauthenticatedException.class)
  public void testGetConfiguredRepositories_Unauthenticated() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    getRepositoryService().getConfiguredRepositories(repositoryManager.getInstanceId(), 0L, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetConfiguredRepositories_Unauthorized() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    login();
    getRepositoryService().getConfiguredRepositories(repositoryManager.getInstanceId(), 0L, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddProprietaryNamespaceNames_ExistingRepository_Unauthorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    login();
    getRepositoryService().addProprietaryNamespaceNames(repoManager.getInstanceId(), repo.getPublicId(),
        "npm", List.of("org.sonatype"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddProprietaryNamespaceNames_Unauthenticated() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repositoryManager, "testRepoPublicId", "npm", true);
    getRepositoryService().addProprietaryNamespaceNames(
        repositoryManager.getInstanceId(), repo.getPublicId(), "npm", List.of("org.sonatype"));
  }

  @Test
  public void testAddProprietaryNamespaceNames_ExistingRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    grantEvaluateComponentPermission(repo.getId());
    getRepositoryService().addProprietaryNamespaceNames(repoManager.getInstanceId(), repo.getPublicId(),
        "npm", List.of("org.sonatype"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveProprietaryNamespaceNames_ExistingRepository_Unauthorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    login();
    getRepositoryService().removeProprietaryNamespaceNames(repoManager.getInstanceId(), repo.getPublicId());
  }

  @Test
  public void testRemoveProprietaryNamespaceNames_ExistingRepository_Authorized() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testRepoPublicId", "npm", true);
    grantEvaluateComponentPermission(repo.getId());
    getRepositoryService().removeProprietaryNamespaceNames(repoManager.getInstanceId(), repo.getPublicId());
  }
}
