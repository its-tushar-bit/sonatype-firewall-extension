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
    assertThat(gitHubApp.getAppId()).isEqualTo(12345);
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
}
