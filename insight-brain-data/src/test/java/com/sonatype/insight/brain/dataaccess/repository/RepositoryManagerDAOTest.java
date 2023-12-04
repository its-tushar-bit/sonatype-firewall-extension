/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.NameableDAOTest;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RepositoryManagerDAOTest extends NameableDAOTest<RepositoryManager>
{
  private final RepositoryManagerDAO dao = new RepositoryManagerDAO();

  @Override
  protected RepositoryManager createNameable(String a) {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(System.nanoTime() + "");
    repositoryManager.setName(a);
    dao.update(repositoryManager);
    return repositoryManager;
  }

  @Override
  protected AbstractOperationalSqlDAO<RepositoryManager> getDao() {
    return dao;
  }
  
  @Override
  protected int getMaxNameLength() {
    return NameHelper.MAX_NAME_LENGTH_APP_ORG;
  }

  @Override
  protected RepositoryManager getEntityByName(String name) {
    return dao.getByName(name);
  }

  @Test
  public void testCRUD() {
    // Create
    RepositoryManager repoManager = tempEntity.newRepositoryManager("RepositoryManagerDAOTest");
    String id = repoManager.getId();
    repoManager = dao.getById(id);
    assertThat(repoManager.getInstanceId()).isEqualTo("RepositoryManagerDAOTest");

    // Update
    repoManager.setInstanceId("RepositoryManagerDAOTest updated");
    dao.update(repoManager);
    repoManager = dao.getById(id);
    assertThat(repoManager.getInstanceId()).isEqualTo("RepositoryManagerDAOTest updated");

    // Delete
    dao.delete(repoManager);
    repoManager = dao.getById(id);
    assertThat(repoManager).isNull();
  }

  @Test
  public void testInsert_ValidateNullInstanceId() {
    assertThatThrownBy(() -> tempEntity.newRepositoryManager(null))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testUpdate_ValidateNullInstanceId() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    repoManager.setInstanceId(null);
    assertThatThrownBy(() -> dao.update(repoManager)).isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testInsert_ValidateEmptyInstanceId() {
    assertThatThrownBy(() -> tempEntity.newRepositoryManager(" "))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testUpdate_ValidateEmptyInstanceId() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    repoManager.setInstanceId(" ");
    assertThatThrownBy(() -> dao.update(repoManager))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testInsert_DuplicateInstanceId() {
    tempEntity.newRepositoryManager("MyInstanceId");

    assertThatThrownBy(() -> tempEntity.newRepositoryManager("MyInstanceId"))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("There is already a repository manager with instance ID MyInstanceId.");
  }

  @Test
  public void testUpdate_DuplicateInstanceId() {
    tempEntity.newRepositoryManager("MyInstanceId1");
    RepositoryManager repoManager = tempEntity.newRepositoryManager("MyInstanceId2");
    repoManager.setInstanceId("MyInstanceId1");

    assertThatThrownBy(() -> dao.update(repoManager)).isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("There is already a repository manager with instance ID MyInstanceId1.");
  }

  @Test
  public void testDelete_CascadesToRepositories() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repoManager, "publicId");

    RepositoryDAO repositoryDAO = new RepositoryDAO();
    // sanity check
    assertThat(repositoryDAO.getByRepositoryManagerId(repoManager.getId())).isNotEmpty();

    dao.delete(repoManager);

    assertThat(repositoryDAO.getById(repository.getId())).isNull();
  }

  @Test
  public void testGetByInstanceIdNotNull() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    RepositoryManager resultRepositoryManager = dao.getByInstanceIdNotNull(repositoryManager.getInstanceId());

    assertThat(resultRepositoryManager.getId()).isEqualTo(repositoryManager.getId());
    assertThat(resultRepositoryManager.getInstanceId()).isEqualTo(repositoryManager.getInstanceId());
  }

  @Test
  public void testGetByInstanceIdNotNull_NotFoundException() {
    assertThatThrownBy(() -> dao.getByInstanceIdNotNull("repoManagerInstanceId"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Cannot find a repository manager with instance ID repoManagerInstanceId.");
  }

  @Test
  public void testGetUnconfigured() {
    RepositoryManager configuredRepositoryManager = tempEntity.newRepositoryManager();
    configuredRepositoryManager.setConfigured(true);
    dao.update(configuredRepositoryManager);

    assertThat(dao.getUnconfigured()).extracting(RepositoryManager::getId)
        .containsExactly(repository.getRepositoryManagerId());
  }

  @Test
  public void testUpdate_nameExistsSameEntity() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repositoryManager.setName("name1");
    dao.update(repositoryManager);
    dao.update(repositoryManager);

    RepositoryManager resultRepositoryManager = dao.getById(repositoryManager.getId());

    assertThat(resultRepositoryManager.getName()).isEqualTo("name1");
  }

  @Override
  public void testInsert_ValidateNullName() {
    RepositoryManager repositoryManager = createNameable(null);
    RepositoryManager resultRepositoryManager = dao.getById(repositoryManager.getId());

    assertThat(resultRepositoryManager.getName()).isEqualTo(repositoryManager.getInstanceId());
  }

  @Test
  @Override
  public void testUpdate_ValidateNullName() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repositoryManager.setName(null);

    dao.update(repositoryManager);

    RepositoryManager resultRepositoryManager = dao.getById(repositoryManager.getId());

    assertThat(resultRepositoryManager.getName()).isEqualTo(repositoryManager.getInstanceId());
  }

  @Test
  public void testInsert_InstanceIdWithInvalidNameChars() {
    for (String invalidChar : NameHelperTest.INVALID_CHARACTERS) {
      RepositoryManager repositoryManager = tempEntity.newRepositoryManager("a" + invalidChar + "b");

      RepositoryManager resultRepositoryManager = dao.getById(repositoryManager.getId());
      assertThat(resultRepositoryManager.getName()).isEqualTo(repositoryManager.getInstanceId());
    }
  }

  @Test
  public void testUpdate_InstanceIdWithInvalidNameChars() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    for (String invalidChar : NameHelperTest.INVALID_CHARACTERS) {
      repositoryManager.setInstanceId("a" + invalidChar + "b");
      dao.update(repositoryManager);

      RepositoryManager resultRepositoryManager = dao.getById(repositoryManager.getId());
      assertThat(resultRepositoryManager.getName()).isEqualTo(repositoryManager.getInstanceId());
    }
  }
}
