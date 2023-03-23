/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.repository.onboarding.FirewallOnboardingRepositoryDTO;
import com.sonatype.clm.dto.model.repository.onboarding.FirewallOnboardingRepositoryManagerDTO;
import com.sonatype.clm.dto.model.repository.onboarding.FirewallOnboardingRepositoryType;
import com.sonatype.clm.dto.model.repository.onboarding.FirewallOnboardingRequest;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.repository.onboarding.FirewallOnboardingRepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepositoryManager;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public abstract class AbstractRepositoryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  protected static final String MANUAL_REPO_MAN_INSTANCE_ID = "manualDeleteRepoManagerInstanceId";

  protected static final String REPOSITORY_PUBLIC_ID = "publicId";

  private final RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();

  @Mock
  private RepositoryPolicyEvaluator repositoryPolicyEvaluator;

  protected abstract AbstractRepositoryService getRepositoryService();

  @Mock
  private DbQuarantinedComponentAccessManager quarantinedComponentAccessManager;

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
    binder.bind(DbQuarantinedComponentAccessManager.class).toInstance(quarantinedComponentAccessManager);
  }

  @Test
  public void testSetEnabled_NewRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetEnabled_NewRepository_Unauthenticated() {
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetEnabled_NewRepository_Unauthorized() {
    grantWritePermission();
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID, true, null);
  }

  @Test
  public void testSetEnabled_ExistingRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), true, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetEnabled_ExistingRepository_Unauthenticated() {
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), true, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetEnabled_ExistingRepository_Unauthorized() {
    grantWritePermission();
    getRepositoryService().setEnabled(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(), true, null);
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
  public void testAddProprietaryComponentNames_Unauthenticated() {
    getRepositoryService().addProprietaryComponentNames(tempEntity.newRepositoryManager().getInstanceId(), "internal",
        new ProprietaryComponentNames("npm", "private"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddProprietaryComponentNames_Unauthorized() {
    login();
    getRepositoryService().addProprietaryComponentNames(tempEntity.newRepositoryManager().getInstanceId(), "internal",
        new ProprietaryComponentNames("npm", "private"));
  }

  @Test
  public void testAddProprietaryComponentNames_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().addProprietaryComponentNames(tempEntity.newRepositoryManager().getInstanceId(), "internal",
        new ProprietaryComponentNames("npm", "private"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRemoveProprietaryComponentNames_Unauthenticated() {
    getRepositoryService().removeProprietaryComponentNames(MANUAL_REPO_MAN_INSTANCE_ID, "internal");
  }

  @Test(expected = UnauthorizedException.class)
  public void testRemoveProprietaryComponentNames_Unauthorized() {
    login();
    getRepositoryService().removeProprietaryComponentNames(MANUAL_REPO_MAN_INSTANCE_ID, "internal");
  }

  @Test
  public void testRemoveProprietaryComponentNames_Authorized() {
    grantManageProprietaryPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    getRepositoryService().removeProprietaryComponentNames(MANUAL_REPO_MAN_INSTANCE_ID, "internal");
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
  public void testFirewallOnboarding_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    FirewallOnboardingRequest firewallOnboardingRequest = new FirewallOnboardingRequest();
    FirewallOnboardingRepositoryManagerDTO firewallOnboardingRepositoryManagerDTO =
        new FirewallOnboardingRepositoryManagerDTO();
    firewallOnboardingRepositoryManagerDTO.instanceId = "testInstanceId";
    firewallOnboardingRequest.repositoryManager = firewallOnboardingRepositoryManagerDTO;
    FirewallOnboardingRepositoryDTO firewallOnboardingRepositoryDTO = new FirewallOnboardingRepositoryDTO();
    firewallOnboardingRepositoryDTO.name = "testName";
    firewallOnboardingRepositoryDTO.format = ComponentIdentifier.FORMAT_MAVEN;
    firewallOnboardingRepositoryDTO.type = FirewallOnboardingRepositoryType.hosted;
    firewallOnboardingRequest.repositories.add(firewallOnboardingRepositoryDTO);

    try {
      getRepositoryService().firewallOnboarding(firewallOnboardingRequest, "clientUserAgent");
    }
    finally {
      FirewallOnboardingRepositoryManagerDAO dao = new FirewallOnboardingRepositoryManagerDAO();
      FirewallOnboardingRepositoryManager repoManager = dao.getByInstanceId("testInstanceId");
      dao.delete(repoManager);
    }
  }

  @Test(expected = UnauthenticatedException.class)
  public void testFirewallOnboarding_Unauthenticated() {
    getRepositoryService().firewallOnboarding(null /* firewallOnboardingRequest */, "clientUserAgent");
  }

  @Test(expected = UnauthorizedException.class)
  public void testFirewallOnboarding_Unauthorized() {
    login();
    getRepositoryService().firewallOnboarding(null /* firewallOnboardingRequest */, "clientUserAgent");
  }
}
