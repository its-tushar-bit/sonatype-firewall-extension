/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository.onboarding;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.repository.InvalidRepositoryManagerException;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepository;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepositoryManager;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FirewallOnboardingRepositoryManagerDAOTest
    extends AbstractDbDAOTest
{
  private final FirewallOnboardingRepositoryManagerDAO dao = new FirewallOnboardingRepositoryManagerDAO();

  @Test
  public void testCRUD() {
    // Create
    FirewallOnboardingRepositoryManager repoManager =
        tempEntity.newFirewallOnboardingRepositoryManager("testRepoManagerInstanceId");
    String id = repoManager.getId();

    // Read
    repoManager = dao.getById(id);
    assertThat(repoManager.getInstanceId()).isEqualTo("testRepoManagerInstanceId");

    // Update
    repoManager.setConfigureUsername("testUsername");
    dao.update(repoManager);
    repoManager = dao.getById(id);
    assertThat(repoManager.getConfigureUsername()).isEqualTo("testUsername");

    // Delete
    dao.delete(repoManager);
    repoManager = dao.getById(id);
    assertThat(repoManager).isNull();
  }

  @Test
  public void testInsert_NullInstanceId() {
    assertThatThrownBy(() -> {
      tempEntity.newFirewallOnboardingRepositoryManager(null);
    }).isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testInsert_SetsRequestTime() {
    Date beforeInsert = new Date();
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    repoManager = dao.getById(repoManager.getId());
    assertThat(repoManager.getRequestTime()).isAfterOrEqualTo(beforeInsert);
  }

  @Test
  public void testUpdate_NullInstanceId() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    repoManager.setInstanceId(null);
    assertThatThrownBy(() -> {
      dao.update(repoManager);
    }).isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testInsert_EmptyInstanceId() {
    assertThatThrownBy(() -> {
      tempEntity.newFirewallOnboardingRepositoryManager(" ");
    }).isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testUpdate_EmptyInstanceId() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    repoManager.setInstanceId(" ");
    assertThatThrownBy(() -> {
      dao.update(repoManager);
    }).isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testInsert_DuplicateInstanceId() {
    tempEntity.newFirewallOnboardingRepositoryManager("testInstanceId");

    assertThatThrownBy(() -> {
      tempEntity.newFirewallOnboardingRepositoryManager("testInstanceId");
    }).isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("There is already a repository manager with instance ID testInstanceId.");
  }

  @Test
  public void testUpdate_DuplicateInstanceId() {
    tempEntity.newFirewallOnboardingRepositoryManager("testInstanceId1");
    FirewallOnboardingRepositoryManager repoManager =
        tempEntity.newFirewallOnboardingRepositoryManager("testInstanceId2");
    repoManager.setInstanceId("testInstanceId1");

    assertThatThrownBy(() -> {
      dao.update(repoManager);
    }).isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("There is already a repository manager with instance ID testInstanceId1.");
  }

  @Test
  public void testUpdate_RepositoryManagerNotFound() {
    FirewallOnboardingRepositoryManager repoManager =
        new FirewallOnboardingRepositoryManager("testInstanceId", "testUsername", "testUserAgent");
    tempEntity.newFirewallOnboardingRepositoryManager("testInstanceId2");

    assertThatThrownBy(() -> {
      dao.update(repoManager);
    }).isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a repository manager with instance ID 'testInstanceId'.");
  }

  @Test
  public void testUpdate_RequestTimeCannotBeChanged() {
    FirewallOnboardingRepositoryManager repoManager =
        tempEntity.newFirewallOnboardingRepositoryManager("testInstanceId");
    repoManager.setRequestTime(new Date(repoManager.getRequestTime().getTime() + 1));

    assertThatThrownBy(() -> {
      dao.update(repoManager);
    }).isInstanceOf(BadRequestException.class).hasMessage("The request time cannot be changed.");
  }

  @Test
  public void testUpdate_RequestUsernameCannotBeChanged() {
    FirewallOnboardingRepositoryManager repoManager =
        tempEntity.newFirewallOnboardingRepositoryManager("testInstanceId");
    repoManager.setRequestUsername(repoManager.getRequestUsername() + "Updated");

    assertThatThrownBy(() -> {
      dao.update(repoManager);
    }).isInstanceOf(BadRequestException.class).hasMessage("The request user name cannot be changed.");
  }

  @Test
  public void testUpdate_RequestUserAgentCannotBeChanged() {
    FirewallOnboardingRepositoryManager repoManager =
        tempEntity.newFirewallOnboardingRepositoryManager("testInstanceId");
    repoManager.setRequestUserAgent(repoManager.getRequestUserAgent() + "Updated");

    assertThatThrownBy(() -> {
      dao.update(repoManager);
    }).isInstanceOf(BadRequestException.class).hasMessage("The request user agent cannot be changed.");
  }

  @Test
  public void testDelete_CascadesDeleteToRepositories() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    FirewallOnboardingRepository repository = tempEntity.newFirewallOnboardingRepository(repoManager, "testName");

    FirewallOnboardingRepositoryDAO firewallOnboardingRepositoryDAO = new FirewallOnboardingRepositoryDAO();
    // sanity check
    assertThat(firewallOnboardingRepositoryDAO.getByRepositoryManagerId(repoManager.getId())).isNotEmpty();

    dao.delete(repoManager);

    assertThat(firewallOnboardingRepositoryDAO.getById(repository.getId())).isNull();
  }
}
