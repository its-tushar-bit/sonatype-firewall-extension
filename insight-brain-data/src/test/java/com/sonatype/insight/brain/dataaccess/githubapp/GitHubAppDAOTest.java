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
  public void testGetNearestGitHubApps_DirectOwner() {
    var org = tempEntity.newOrganization();
    GitHubApp expectedApp = createGitHubApp(org.getId(), 100L, true);

    var result = gitHubAppDAO.getNearestGitHubApps(org.getId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getOwnerId()).isEqualTo(org.getId());
    assertThat(result.get(0).getId()).isEqualTo(expectedApp.getId());
  }

  @Test
  public void testGetNearestGitHubApps_InheritedFromParent() {
    var rootOrg = tempEntity.newOrganization();
    GitHubApp expectedApp = createGitHubApp(rootOrg.getId(), 100L, true);
    var childOrg = tempEntity.newOrganization(rootOrg);
    var app = tempEntity.newApplication(childOrg.getId());

    var result = gitHubAppDAO.getNearestGitHubApps(app.getId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getOwnerId()).isEqualTo(rootOrg.getId());
    assertThat(result.get(0).getId()).isEqualTo(expectedApp.getId());
  }

  @Test
  public void testGetNearestGitHubApps_ClosestInHierarchy() {
    var rootOrg = tempEntity.newOrganization();
    createGitHubApp(rootOrg.getId(), 100L, true);
    var childOrg = tempEntity.newOrganization(rootOrg);
    GitHubApp expectedApp = createGitHubApp(childOrg.getId(), 200L, true);
    var app = tempEntity.newApplication(childOrg.getId());

    var result = gitHubAppDAO.getNearestGitHubApps(app.getId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getOwnerId()).isEqualTo(childOrg.getId());
    assertThat(result.get(0).getId()).isEqualTo(expectedApp.getId());
  }

  @Test
  public void testGetNearestGitHubApps_NoGitHubAppInHierarchy() {
    var org = tempEntity.newOrganization();
    var app = tempEntity.newApplication(org.getId());

    var result = gitHubAppDAO.getNearestGitHubApps(app.getId());

    assertThat(result).isEmpty();
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
  public void testGetNearestGitHubApps_ReturnsAllActiveAtNearestLevel() {
    var rootOrg = tempEntity.newOrganization();
    var childOrg = tempEntity.newOrganization(rootOrg);
    var app = tempEntity.newApplication(childOrg.getId());

    createGitHubApp(childOrg.getId(), 111L, true);
    createGitHubApp(childOrg.getId(), 222L, true);
    createGitHubApp(rootOrg.getId(), 333L, true);

    var result = gitHubAppDAO.getNearestGitHubApps(app.getId());

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(a -> a.getOwnerId().equals(childOrg.getId()));
  }

  @Test
  public void testGetNearestGitHubApps_ExcludesInactiveApps() {
    var org = tempEntity.newOrganization();
    var app = tempEntity.newApplication(org.getId());

    createGitHubApp(org.getId(), 111L, true);
    createGitHubApp(org.getId(), 222L, false);

    var result = gitHubAppDAO.getNearestGitHubApps(app.getId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getInstallationId()).isEqualTo(111L);
  }

  @Test
  public void testGetNearestGitHubApps_EmptyWhenNoApps() {
    var org = tempEntity.newOrganization();
    var app = tempEntity.newApplication(org.getId());

    var result = gitHubAppDAO.getNearestGitHubApps(app.getId());

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetNearestGitHubApps_OrderedByGitHubAppId() {
    var org = tempEntity.newOrganization();
    var app = tempEntity.newApplication(org.getId());

    createGitHubApp(org.getId(), 111L, true);
    createGitHubApp(org.getId(), 222L, true);

    var result = gitHubAppDAO.getNearestGitHubApps(app.getId());

    var ids = result.stream().map(GitHubApp::getId).toList();
    assertThat(ids).isSorted();
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
    return tempEntity.newGitHubApp(gitHubApp, true);
  }

  @Test
  public void testFindInactive_ReturnsInactiveRecords() {
    var org = tempEntity.newOrganization();
    GitHubApp inactive = createGitHubAppWithLastUpdated(org.getId(), 100L, false, new Date());

    var results = gitHubAppDAO.findInactive();

    assertThat(results).extracting(GitHubApp::getId).contains(inactive.getId());
  }

  @Test
  public void testFindInactive_ExcludesActiveRecords() {
    var org = tempEntity.newOrganization();
    GitHubApp active = createGitHubAppWithLastUpdated(org.getId(), 200L, true, new Date());

    var results = gitHubAppDAO.findInactive();

    assertThat(results).extracting(GitHubApp::getId).doesNotContain(active.getId());
  }

  private GitHubApp createGitHubAppWithLastUpdated(
      String ownerId,
      long installationId,
      boolean isActive,
      Date lastUpdatedAt)
  {
    appIdCounter++;
    GitHubApp gitHubApp = new GitHubApp();
    gitHubApp.setId(UUID.randomUUID().toString());
    gitHubApp.setOwnerId(ownerId);
    gitHubApp.setAppId(appIdCounter);
    gitHubApp.setSlug("test-app");
    gitHubApp.setGithubOrganizationName("test-org");
    gitHubApp.setLastUpdatedAt(lastUpdatedAt);
    gitHubApp.setClientId("Iv1.1234567890abcdef");
    gitHubApp.setClientSecret("client-secret-test");
    gitHubApp.setPrivateKey("test-private-key");
    gitHubApp.setInstallationId(installationId);
    gitHubApp.setActive(isActive);
    return tempEntity.newGitHubApp(gitHubApp, true);
  }
}
