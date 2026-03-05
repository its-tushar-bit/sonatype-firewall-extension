/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.githubapp;

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
    GitHubApp gitHubApp = tempEntity.newGitHubApp(app.getId());

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
    GitHubApp expectedApp = tempEntity.newGitHubApp(org.getId());

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
    GitHubApp expectedApp = tempEntity.newGitHubApp(rootOrg.getId());
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
    tempEntity.newGitHubApp(rootOrg.getId());
    var childOrg = tempEntity.newOrganization(rootOrg);
    GitHubApp expectedApp = tempEntity.newGitHubApp(childOrg.getId());
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
}
