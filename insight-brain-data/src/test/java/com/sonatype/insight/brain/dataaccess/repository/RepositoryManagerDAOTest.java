/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RepositoryManagerDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryManagerDAO dao = new RepositoryManagerDAO();

  @Test
  public void testCRUD() throws Exception {
    // Create
    RepositoryManager repoManager = tempEntity.newRepositoryManager("RepositoryManagerDAOTest");
    String id = repoManager.getId();
    repoManager = dao.getById(id);
    assertThat(repoManager.getInstanceId(), is("RepositoryManagerDAOTest"));

    // Update
    repoManager.setInstanceId("RepositoryManagerDAOTest updated");
    dao.update(repoManager);
    repoManager = dao.getById(id);
    assertThat(repoManager.getInstanceId(), is("RepositoryManagerDAOTest updated"));

    // Delete
    dao.delete(repoManager);
    repoManager = dao.getById(id);
    assertThat(repoManager, is(nullValue()));
  }

  @Test
  public void testValidateNullInstanceId_Insert() {
    try {
      tempEntity.newRepositoryManager(null);
      fail("Expected DataAccessException");
    }
    catch (DataAccessException expected) {
      assertThat(expected.getMessage(), is("The repository manager instance ID cannot be null or empty."));
    }
  }

  @Test
  public void testValidateNullInstanceId_Update() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    repoManager.setInstanceId(null);
    try {
      dao.update(repoManager);
      fail("Expected DataAccessException");
    }
    catch (DataAccessException expected) {
      assertThat(expected.getMessage(), is("The repository manager instance ID cannot be null or empty."));
    }
  }

  @Test
  public void testValidateEmptyInstanceId_Insert() {
    try {
      tempEntity.newRepositoryManager(" ");
      fail("Expected DataAccessException");
    }
    catch (DataAccessException expected) {
      assertThat(expected.getMessage(), is("The repository manager instance ID cannot be null or empty."));
    }
  }

  @Test
  public void testValidateEmptyInstanceId_Update() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    repoManager.setInstanceId(" ");
    try {
      dao.update(repoManager);
      fail("Expected DataAccessException");
    }
    catch (DataAccessException expected) {
      assertThat(expected.getMessage(), is("The repository manager instance ID cannot be null or empty."));
    }
  }

  @Test
  public void testDuplicateInstanceId_Insert() {
    tempEntity.newRepositoryManager("MyInstanceId");

    try {
      tempEntity.newRepositoryManager("MyInstanceId");
      fail("Expected DataAccessException");
    }
    catch (DataAccessException expected) {
      assertEquals("There is already a repository manager with instance ID MyInstanceId.", expected.getMessage());
    }
  }

  @Test
  public void testDuplicateInstanceId_Update() {
    tempEntity.newRepositoryManager("MyInstanceId1");
    RepositoryManager repoManager = tempEntity.newRepositoryManager("MyInstanceId2");
    repoManager.setInstanceId("MyInstanceId1");

    try {
      dao.update(repoManager);
      fail("Expected DataAccessException");
    }
    catch (DataAccessException expected) {
      assertEquals("There is already a repository manager with instance ID MyInstanceId1.", expected.getMessage());
    }
  }

  @Test
  public void testCascadeDeleteToRepositories() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repoManager, "name", "publicId");

    RepositoryDAO repositoryDAO = new RepositoryDAO();
    // sanity check
    assertThat(repositoryDAO.getByRepositoryManagerId(repoManager.getId()), is(not(empty())));

    dao.delete(repoManager);

    assertThat(repositoryDAO.getById(repository.getId()), is(nullValue()));
  }
}
