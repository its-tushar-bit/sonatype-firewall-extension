/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository.onboarding;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.repository.onboarding.FirewallOnboardingRepositoryType;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.repository.InvalidRepositoryException;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepository;
import com.sonatype.insight.brain.model.repository.onboarding.FirewallOnboardingRepositoryManager;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FirewallOnboardingRepositoryDAOTest
    extends AbstractDbDAOTest
{
  private final FirewallOnboardingRepositoryDAO dao = new FirewallOnboardingRepositoryDAO();

  @Test
  public void testCRUD() {
    // Create
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    FirewallOnboardingRepository repository = tempEntity.newFirewallOnboardingRepository(repoManager, "testName");
    String id = repository.getId();

    // Read
    repository = dao.getById(id);
    assertThat(repository.getRepositoryManagerId()).isEqualTo(repoManager.getId());
    assertThat(repository.getName()).isEqualTo("testName");
    assertThat(repository.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_NPM);
    assertThat(repository.getType()).isEqualTo(FirewallOnboardingRepositoryType.proxy);
    assertThat(repository.isAuditEnabled()).isFalse();
    assertThat(repository.isQuarantineEnabled()).isFalse();
    assertThat(repository.isNamespaceConfusionProtectionEnabled()).isFalse();

    // Update
    repository.setAuditEnabled(true);
    dao.update(repository);
    repository = dao.getById(id);
    assertThat(repository.isAuditEnabled()).isTrue();

    // Delete
    dao.delete(repository);
    repository = dao.getById(id);
    assertThat(repository).isNull();
  }

  @Test
  public void testInsert_NullName() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    assertThatThrownBy(() -> {
      tempEntity.newFirewallOnboardingRepository(repoManager, null /* name */);
    }).isInstanceOf(InvalidRepositoryException.class).hasMessage("The repository name cannot be null or empty.");
  }

  @Test
  public void testInsert_NullFormat() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    assertThatThrownBy(() -> {
      dao.insert(new FirewallOnboardingRepository(repoManager.getId(), "testName", null /* format */,
          FirewallOnboardingRepositoryType.hosted));
    }).isInstanceOf(InvalidRepositoryException.class).hasMessage("The repository format cannot be null or empty.");
  }

  @Test
  public void testInsert_EmptyFormat() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    assertThatThrownBy(() -> {
      dao.insert(new FirewallOnboardingRepository(repoManager.getId(), "testName", " " /* format */,
          FirewallOnboardingRepositoryType.hosted));
    }).isInstanceOf(InvalidRepositoryException.class).hasMessage("The repository format cannot be null or empty.");
  }

  @Test
  public void testInsert_NullType() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    assertThatThrownBy(() -> {
      dao.insert(new FirewallOnboardingRepository(repoManager.getId(), "testName", ComponentIdentifier.FORMAT_MAVEN,
          null /* type */));
    }).isInstanceOf(InvalidRepositoryException.class).hasMessage("The repository type cannot be null.");
  }

  @Test
  public void testUpdate_NullName() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    FirewallOnboardingRepository repository = tempEntity.newFirewallOnboardingRepository(repoManager, "testName");
    repository.setName(null);
    assertThatThrownBy(() -> {
      dao.update(repository);
    }).isInstanceOf(InvalidRepositoryException.class).hasMessage("The repository name cannot be null or empty.");
  }

  @Test
  public void testInsert_EmptyName() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    assertThatThrownBy(() -> {
      tempEntity.newFirewallOnboardingRepository(repoManager, " " /* name */);
    }).isInstanceOf(InvalidRepositoryException.class).hasMessage("The repository name cannot be null or empty.");
  }

  @Test
  public void testUpdate_EmptyName() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    FirewallOnboardingRepository repository = tempEntity.newFirewallOnboardingRepository(repoManager, "testName");
    repository.setName(" ");
    assertThatThrownBy(() -> {
      dao.update(repository);
    }).isInstanceOf(InvalidRepositoryException.class).hasMessage("The repository name cannot be null or empty.");
  }

  @Test
  public void testInsert_DuplicateName() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    tempEntity.newFirewallOnboardingRepository(repoManager, "testName");

    assertThatThrownBy(() -> {
      tempEntity.newFirewallOnboardingRepository(repoManager, "testName");
    }).isInstanceOf(InvalidRepositoryException.class)
        .hasMessage("There is already a repository with name 'testName' for the same repository manager.");
  }

  @Test
  public void testUpdate_DuplicateName() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    tempEntity.newFirewallOnboardingRepository(repoManager, "testName1");
    FirewallOnboardingRepository repository = tempEntity.newFirewallOnboardingRepository(repoManager, "testName2");

    repository.setName("testName1");
    assertThatThrownBy(() -> {
      dao.update(repository);
    }).isInstanceOf(InvalidRepositoryException.class)
        .hasMessage("There is already a repository with name 'testName1' for the same repository manager.");
  }

  @Test
  public void testUpdate_RepositoryNotFound() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    FirewallOnboardingRepository repository = new FirewallOnboardingRepository(repoManager.getId(), "testName",
        ComponentIdentifier.FORMAT_NPM, FirewallOnboardingRepositoryType.hosted);

    assertThatThrownBy(() -> {
      dao.update(repository);
    }).isInstanceOf(NotFoundException.class).hasMessage("Cannot find a repository with name 'testName'.");
  }

  @Test
  public void testUpdate_CannotChangeRepositoryFormat() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    FirewallOnboardingRepository repository = tempEntity.newFirewallOnboardingRepository(repoManager, "testName");

    repository.setFormat(ComponentIdentifier.FORMAT_MAVEN);
    assertThatThrownBy(() -> {
      dao.update(repository);
    }).isInstanceOf(BadRequestException.class).hasMessage("The repository format cannot be changed.");
  }

  @Test
  public void testUpdate_CannotChangeRepositoryType() {
    FirewallOnboardingRepositoryManager repoManager = tempEntity.newFirewallOnboardingRepositoryManager();
    FirewallOnboardingRepository repository = tempEntity.newFirewallOnboardingRepository(repoManager, "testName");

    repository.setType(FirewallOnboardingRepositoryType.hosted);
    assertThatThrownBy(() -> {
      dao.update(repository);
    }).isInstanceOf(BadRequestException.class).hasMessage("The repository type cannot be changed.");
  }
}
