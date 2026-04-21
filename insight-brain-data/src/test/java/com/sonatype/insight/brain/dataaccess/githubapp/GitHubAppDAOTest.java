/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.githubapp;

import java.util.Date;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;

import com.sonatype.insight.dataaccess.TransactionContext;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.201
 */
public class GitHubAppDAOTest
    extends AbstractDbDAOTest
{
  private int appIdCounter = 10000;

  private GitHubAppDAO gitHubAppDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    gitHubAppDAO = daoFactory.createGitHubAppDAO();
  }

  @Test
  public void testCrud() {
    Application app = tempEntity.newApplication(tempEntity.newOrganization().getId());

    // Create
    GitHubApp gitHubApp = createGitHubApp(app.getId(), 100L, false);

    assertThat(gitHubApp.getId()).isNotNull();
    assertThat(gitHubApp.getOwnerId()).isEqualTo(app.getId());
    assertThat(gitHubApp.getAppId()).isNotNull();
    assertThat(gitHubApp.getSlug()).isEqualTo("test-app");
    assertThat(gitHubApp.getClientId()).isEqualTo("Iv1.1234567890abcdef");
    assertThat(gitHubApp.getClientSecret()).isEqualTo("client-secret-test");
    assertThat(gitHubApp.getPrivateKey()).isNotNull();
    assertThat(gitHubApp.getInstallationId()).isNotNull();

    // Read by ID
    GitHubApp retrieved = gitHubAppDAO.getById(gitHubApp.getId());
    assertThat(retrieved).isNotNull();
    assertThat(retrieved.getOwnerId()).isEqualTo(gitHubApp.getOwnerId());
    assertThat(retrieved.getAppId()).isEqualTo(gitHubApp.getAppId());

    // Update
    retrieved.setClientId("Iv1.updated");
    gitHubAppDAO.update(retrieved);

    GitHubApp updated = gitHubAppDAO.getById(gitHubApp.getId());
    assertThat(updated.getClientId()).isEqualTo("Iv1.updated");

    // Delete
    gitHubAppDAO.delete(gitHubApp);
    assertThat(gitHubAppDAO.getById(gitHubApp.getId())).isNull();
  }

  @Test
  public void testGetNearestGitHubApp_DirectOwner() {
    // Setup: Create org with GitHub App
    var org = tempEntity.newOrganization();
    GitHubApp expectedApp = createGitHubApp(org.getId(), 100L, true);

    // Test: Should find GitHub App at org level
    GitHubApp result = gitHubAppDAO.getNearestGitHubApp(org.getId());

    assertThat(result).isNotNull();
    assertThat(result.getOwnerId()).isEqualTo(org.getId());
    assertThat(result.getId()).isEqualTo(expectedApp.getId());
  }

  @Test
  public void testGetNearestGitHubApp_InheritedFromParent() {
    // Setup: Create hierarchy - Root Org (with GitHub App) -> Child Org -> Application
    var rootOrg = tempEntity.newOrganization();
    GitHubApp expectedApp = createGitHubApp(rootOrg.getId(), 100L, true);
    var childOrg = tempEntity.newOrganization(rootOrg);
    var app = tempEntity.newApplication(childOrg.getId());

    // Test: App should inherit GitHub App from root org
    GitHubApp result = gitHubAppDAO.getNearestGitHubApp(app.getId());

    assertThat(result).isNotNull();
    assertThat(result.getOwnerId()).isEqualTo(rootOrg.getId());
    assertThat(result.getId()).isEqualTo(expectedApp.getId());
  }

  @Test
  public void testGetNearestGitHubApp_ClosestInHierarchy() {
    // Setup: Create hierarchy with GitHub Apps at multiple levels
    var rootOrg = tempEntity.newOrganization();
    createGitHubApp(rootOrg.getId(), 100L, true);
    var childOrg = tempEntity.newOrganization(rootOrg);
    GitHubApp expectedApp = createGitHubApp(childOrg.getId(), 200L, true);
    var app = tempEntity.newApplication(childOrg.getId());

    // Test: Should return the closest GitHub App (child org, not root)
    GitHubApp result = gitHubAppDAO.getNearestGitHubApp(app.getId());

    assertThat(result).isNotNull();
    assertThat(result.getOwnerId()).isEqualTo(childOrg.getId());
    assertThat(result.getId()).isEqualTo(expectedApp.getId());
  }

  @Test
  public void testGetNearestGitHubApp_NoGitHubAppInHierarchy() {
    // Setup: Create hierarchy without GitHub App
    var org = tempEntity.newOrganization();
    var app = tempEntity.newApplication(org.getId());

    // Test: Should return null when no GitHub App exists
    GitHubApp result = gitHubAppDAO.getNearestGitHubApp(app.getId());

    assertThat(result).isNull();
  }

  @Test
  public void testGetAllByOwnerId_ReturnsAllGitHubApps() {
    var org = tempEntity.newOrganization();
    GitHubApp app1 = createGitHubApp(org.getId(), 100L, true);
    GitHubApp app2 = createGitHubApp(org.getId(), 200L, false);

    var results = gitHubAppDAO.getAllByOwnerId(org.getId());

    assertThat(results).hasSize(2);
    assertThat(results).extracting(GitHubApp::getId).containsExactlyInAnyOrder(app1.getId(), app2.getId());
  }

  @Test
  public void testGetByGithubAppId_ReturnsCorrectApp() {
    var org = tempEntity.newOrganization();
    GitHubApp gitHubApp = createGitHubApp(org.getId(), 100L, false);

    GitHubApp result = gitHubAppDAO.getByGithubAppId(gitHubApp.getId());

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(gitHubApp.getId());
    assertThat(result.getOwnerId()).isEqualTo(org.getId());
  }

  @Test
  public void testGetByGithubAppId_ReturnsNullWhenNotFound() {
    GitHubApp result = gitHubAppDAO.getByGithubAppId("non-existent-id");

    assertThat(result).isNull();
  }

  @Test
  public void testActivateGitHubApp_DeactivatesOthersAndActivatesOne() {
    var org = tempEntity.newOrganization();
    GitHubApp app1 = createGitHubApp(org.getId(), 100L, true);
    GitHubApp app2 = createGitHubApp(org.getId(), 200L, false);

    gitHubAppDAO.activateGitHubApp(org.getId(), app2.getId());

    GitHubApp updatedApp1 = gitHubAppDAO.getById(app1.getId());
    GitHubApp updatedApp2 = gitHubAppDAO.getById(app2.getId());

    assertThat(updatedApp1.isActive()).isFalse();
    assertThat(updatedApp2.isActive()).isTrue();
  }

  @Test(expected = com.sonatype.insight.error.exception.NotFoundException.class)
  public void testActivateGitHubApp_ThrowsExceptionWhenAppNotFound() {
    var org = tempEntity.newOrganization();

    gitHubAppDAO.activateGitHubApp(org.getId(), "non-existent-id");
  }

  @Test
  public void testDeactivateAllForOwner_DeactivatesAllApps() {
    var org = tempEntity.newOrganization();
    GitHubApp app1 = createGitHubApp(org.getId(), 100L, true);
    GitHubApp app2 = createGitHubApp(org.getId(), 200L, true);

    gitHubAppDAO.deactivateAllForOwner(org.getId());

    GitHubApp updatedApp1 = gitHubAppDAO.getById(app1.getId());
    GitHubApp updatedApp2 = gitHubAppDAO.getById(app2.getId());

    assertThat(updatedApp1.isActive()).isFalse();
    assertThat(updatedApp2.isActive()).isFalse();
  }

  @Test
  public void testDeactivateAllForOwner_NoErrorWhenNoApps() {
    var org = tempEntity.newOrganization();

    gitHubAppDAO.deactivateAllForOwner(org.getId());
  }

  private GitHubApp createGitHubApp(String ownerId, long installationId, boolean isActive) {
    appIdCounter++;

    GitHubApp gitHubApp = new GitHubApp();
    gitHubApp.setId(UUID.randomUUID().toString());
    gitHubApp.setOwnerId(ownerId);
    gitHubApp.setAppId(appIdCounter);
    gitHubApp.setSlug("test-app");
    gitHubApp.setGithubOrganizationName("test-org");
    gitHubApp.setLastUpdatedAt(new Date());
    gitHubApp.setClientId("Iv1.1234567890abcdef");
    gitHubApp.setClientSecret("client-secret-test");
    gitHubApp.setPrivateKey("test-private-key");
    gitHubApp.setInstallationId(installationId);
    gitHubApp.setActive(isActive);
    return tempEntity.newGitHubApp(gitHubApp);
  }

  @Test
  public void testDeactivateAllForOwner_WithTransactionCommit_PersistsChanges() {
    var org = tempEntity.newOrganization();
    GitHubApp app1 = createGitHubApp(org.getId(), 100L, true);
    GitHubApp app2 = createGitHubApp(org.getId(), 200L, true);
    GitHubApp app3 = createGitHubApp(org.getId(), 300L, false);

    assertThat(gitHubAppDAO.getById(app1.getId()).isActive()).isTrue();
    assertThat(gitHubAppDAO.getById(app2.getId()).isActive()).isTrue();
    assertThat(gitHubAppDAO.getById(app3.getId()).isActive()).isFalse();

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      tx.begin();
      gitHubAppDAO.deactivateAllForOwner(tx, org.getId());
      tx.commit();
    }

    assertThat(gitHubAppDAO.getById(app1.getId()).isActive()).isFalse();
    assertThat(gitHubAppDAO.getById(app2.getId()).isActive()).isFalse();
    assertThat(gitHubAppDAO.getById(app3.getId()).isActive()).isFalse();
  }

  @Test
  public void testDeactivateAllForOwner_WithTransactionRollback_RevertsChanges() {
    var org = tempEntity.newOrganization();
    GitHubApp app1 = createGitHubApp(org.getId(), 100L, true);
    GitHubApp app2 = createGitHubApp(org.getId(), 200L, false);
    GitHubApp app3 = createGitHubApp(org.getId(), 300L, false);

    assertThat(gitHubAppDAO.getById(app1.getId()).isActive()).isTrue();
    assertThat(gitHubAppDAO.getById(app2.getId()).isActive()).isFalse();
    assertThat(gitHubAppDAO.getById(app3.getId()).isActive()).isFalse();

    try (TransactionContext tx = gitHubAppDAO.createTransactionContext()) {
      tx.begin();
      gitHubAppDAO.deactivateAllForOwner(tx, org.getId());

      assertThat(gitHubAppDAO.getById(tx, app1.getId()).isActive()).isFalse();
      assertThat(gitHubAppDAO.getById(tx, app2.getId()).isActive()).isFalse();
      assertThat(gitHubAppDAO.getById(tx, app3.getId()).isActive()).isFalse();

      tx.rollback();
    }

    assertThat(gitHubAppDAO.getById(app1.getId()).isActive()).isTrue();
    assertThat(gitHubAppDAO.getById(app2.getId()).isActive()).isFalse();
    assertThat(gitHubAppDAO.getById(app3.getId()).isActive()).isFalse();
  }
}
