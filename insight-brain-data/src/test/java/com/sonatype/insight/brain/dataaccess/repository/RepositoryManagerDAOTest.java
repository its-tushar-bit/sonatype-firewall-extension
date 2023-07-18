/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.NameableDAOTest;
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
  public void testValidateNullInstanceId_Insert() {
    assertThatThrownBy(() -> tempEntity.newRepositoryManager(null))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testValidateNullInstanceId_Update() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    repoManager.setInstanceId(null);
    assertThatThrownBy(() -> dao.update(repoManager)).isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testValidateEmptyInstanceId_Insert() {
    assertThatThrownBy(() -> tempEntity.newRepositoryManager(" "))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testValidateEmptyInstanceId_Update() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    repoManager.setInstanceId(" ");
    assertThatThrownBy(() -> dao.update(repoManager))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("The repository manager instance ID cannot be null or empty.");
  }

  @Test
  public void testDuplicateInstanceId_Insert() {
    tempEntity.newRepositoryManager("MyInstanceId");

    assertThatThrownBy(() -> tempEntity.newRepositoryManager("MyInstanceId"))
        .isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("There is already a repository manager with instance ID MyInstanceId.");
  }

  @Test
  public void testDuplicateInstanceId_Update() {
    tempEntity.newRepositoryManager("MyInstanceId1");
    RepositoryManager repoManager = tempEntity.newRepositoryManager("MyInstanceId2");
    repoManager.setInstanceId("MyInstanceId1");

    assertThatThrownBy(() -> dao.update(repoManager)).isInstanceOf(InvalidRepositoryManagerException.class)
        .hasMessage("There is already a repository manager with instance ID MyInstanceId1.");
  }

  @Test
  public void testCascadeDeleteToRepositories() {
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
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    // Sanity check
    assertThat(repositoryManager.getUserAgent()).isNull();

    // Repository without user agent
    assertThat(dao.getUnconfigured()).isEmpty();

    // Repository with Artifactory user agent
    repositoryManager.setUserAgent(
        "Firewall_For_Jfrog_Artifactory/unknown (; Linux; 5.10.109-104.500.amzn2.x86_64; amd64; 17.0.3;"
            + " Jfrog Artifactory unknown)");
    dao.update(repositoryManager);
    assertThat(dao.getUnconfigured()).isEmpty();

    // Repository with NXRM user agent
    repositoryManager.setUserAgent("Nexus/3.56.0-SNAPSHOT (PRO; Windows 10; 10.0; amd64; 1.8.0_352)");
    dao.update(repositoryManager);
    assertThat(dao.getUnconfigured()).extracting(RepositoryManager::getId).containsExactly(repositoryManager.getId());
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

    assertThat(resultRepositoryManager.getName()).isNull();
  }

  @Test
  @Override
  public void testUpdate_ValidateNullName() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repositoryManager.setName(null);

    dao.update(repositoryManager);

    RepositoryManager resultRepositoryManager = dao.getById(repositoryManager.getId());

    assertThat(resultRepositoryManager.getName()).isNull();
  }
}
