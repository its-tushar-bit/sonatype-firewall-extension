/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class RepositoryServiceAuthzTest
    extends AbstractRepositoryServiceAuthzH2Test
{

  @Inject
  TestProductLicenseManager licenseManager;

  @Inject
  private RepositoryService repositoryService;

  @Override
  protected AbstractRepositoryService getRepositoryService() {
    return repositoryService;
  }

  @BeforeEach
  public void setLicenseFeature() {
    licenseManager.setFeatures(LicensedFeature.FIREWALL);
  }

  @Test
  public void testEvaluateComponentsAdhoc_ExistingRepository_Unauthorized() {
    login();

    assertThrows(UnauthorizedException.class, () -> repositoryService.evaluateComponentsAdhoc(
        MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
        null /* componentEvaluationDataRequestList */, null));
  }

  @Test
  public void testEvaluateComponentsAdhoc_ExistingRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> repositoryService.evaluateComponentsAdhoc(
        MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
        null /* componentEvaluationDataRequestList */, null));
  }

  @Test
  public void testEvaluateComponentsAdhoc_ExistingRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    repositoryService.evaluateComponentsAdhoc(MANUAL_REPO_MAN_INSTANCE_ID, createRepository().getPublicId(),
        null /* componentEvaluationDataRequestList */, null);
  }

  @Test
  public void testEvaluateComponentsAdhoc_NewRepository_Unauthorized() {
    login();

    assertThrows(UnauthorizedException.class, () -> repositoryService.evaluateComponentsAdhoc(
        MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID,
        null /* componentEvaluationDataRequestList */, null));
  }

  @Test
  public void testEvaluateComponentsAdhoc_NewRepository_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> repositoryService.evaluateComponentsAdhoc(
        MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID,
        null /* componentEvaluationDataRequestList */, null));
  }

  @Test
  public void testEvaluateComponentsAdhoc_NewRepository_Authorized() {
    grantEvaluateComponentPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    repositoryService.evaluateComponentsAdhoc(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID,
        null /* componentEvaluationDataRequestList */, null);
  }

  @Test
  public void testRemoveExtraComponents_Unauthenticated() {
    tempEntity.newRepository(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);

    assertThrows(UnauthenticatedException.class, () -> repositoryService.removeExtraComponents(
        MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID,
        null /* repositoryComponentPathnames */));
  }

  @Test
  public void testRemoveExtraComponents_Unauthorized() {
    tempEntity.newRepository(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);

    login();

    assertThrows(UnauthorizedException.class, () -> repositoryService.removeExtraComponents(
        MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID,
        null /* repositoryComponentPathnames */));
  }

  @Test
  public void testRemoveExtraComponents_Authorized() {
    Repository repo = tempEntity.newRepository(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID);

    grantEvaluateComponentPermission(repo.getId());

    repositoryService.removeExtraComponents(MANUAL_REPO_MAN_INSTANCE_ID, REPOSITORY_PUBLIC_ID,
        null /* repositoryComponentPathnames */);
  }

  @Test
  public void testGetConfiguredRepositoriesHosted_Authorized() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();
    grantEvaluateComponentPermission(repositoryManager.getId());

    repositoryService.getConfiguredRepositories(repositoryManager.getInstanceId(), null, null, null, null, null, null);
  }

  @Test
  public void testGetConfiguredRepositoriesHosted_Unauthenticated() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();

    assertThrows(UnauthenticatedException.class, () -> repositoryService.getConfiguredRepositories(
        repositoryManager.getInstanceId(), null, null, null, null, null, null));
  }

  @Test
  public void testGetConfiguredRepositoriesHosted_Unauthorized() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();
    login();

    assertThrows(UnauthorizedException.class, () -> repositoryService.getConfiguredRepositories(
        repositoryManager.getInstanceId(), null, null, null, null, null, null));
  }

  @Test
  public void testGetAvailableFormats_Authorized() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();
    grantEvaluateComponentPermission(repositoryManager.getId());

    repositoryService.getAvailableFormats(repositoryManager.getInstanceId());
  }

  @Test
  public void testGetAvailableFormats_Unauthenticated() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();

    assertThrows(UnauthenticatedException.class,
        () -> repositoryService.getAvailableFormats(repositoryManager.getInstanceId()));
  }

  @Test
  public void testGetAvailableFormats_Unauthorized() {
    RepositoryManager repositoryManager =
        tempEntity.newRepositoryManager();
    login();

    assertThrows(UnauthorizedException.class,
        () -> repositoryService.getAvailableFormats(repositoryManager.getInstanceId()));
  }

  @Override
  protected ConfigureRepositoriesRequest createConfigureRepositoriesRequest() {
    return new ConfigureRepositoriesRequest("Nexus", "3.60.0", "http://localhost:8081", null /* repositories */);
  }
}
