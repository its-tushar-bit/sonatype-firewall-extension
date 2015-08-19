/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RepositoryDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryDAO dao = new RepositoryDAO();

  @Test
  public void testCRUD() throws Exception {
    // Create
    Repository repository = tempEntity.newRepository("My Repo Name", "My Repo Public Id");
    String id = repository.getId();
    repository = dao.getById(id);
    assertThat(repository.getName(), is("My Repo Name"));
    assertThat(repository.getPublicId(), is("My Repo Public Id"));
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.getParentOwnerId(), is(RepositoryContainer.REPOSITORY_CONTAINER_ID));
    assertThat(repository.canHaveChildren(), is(false));
    assertThat(repository.getType(), is(OwnerType.REPOSITORY));

    // Update
    repository.setName("My Repo Name Updated");
    dao.update(repository);
    repository = dao.getById(id);
    assertThat(repository.getName(), is("My Repo Name Updated"));

    // Delete
    dao.delete(repository);
    repository = dao.getById(id);
    assertThat(repository, is(nullValue()));
  }

  @Test
  public void testValidateNullPublicId_Insert() {
    try {
      tempEntity.newRepository("Some Name", null);
      fail("Expected InvalidRepositoryException");
    }
    catch (InvalidRepositoryException expected) {
      assertThat(expected.getMessage(), is("The repository public ID cannot be null or empty."));
    }
  }

  @Test
  public void testValidateNullPublicId_Update() {
    Repository repository = tempEntity.newRepository("Some Name", "Some Public ID");
    repository.setPublicId(null);
    try {
      dao.update(repository);
      fail("Expected InvalidRepositoryException");
    }
    catch (InvalidRepositoryException expected) {
      assertThat(expected.getMessage(), is("The repository public ID cannot be null or empty."));
    }
  }

  @Test
  public void testValidateEmptyPublicId_Insert() {
    try {
      tempEntity.newRepository("Some Name", " ");
      fail("Expected InvalidRepositoryException");
    }
    catch (InvalidRepositoryException expected) {
      assertThat(expected.getMessage(), is("The repository public ID cannot be null or empty."));
    }
  }

  @Test
  public void testValidateEmptyPublicId_Update() {
    Repository repository = tempEntity.newRepository("Some Name", "Some Public ID");
    repository.setPublicId(" ");
    try {
      dao.update(repository);
      fail("Expected InvalidRepositoryException");
    }
    catch (InvalidRepositoryException expected) {
      assertThat(expected.getMessage(), is("The repository public ID cannot be null or empty."));
    }
  }

  @Test
  public void testDuplicatePublicId_Insert() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    tempEntity.newRepository(repoManager, "Some Name", "SomePublicID");

    try {
      tempEntity.newRepository(repoManager, "Some Other Name", "SomePublicID");
      fail("Expected InvalidRepositoryException");
    }
    catch (InvalidRepositoryException expected) {
      assertEquals("There is already a repository with public ID 'SomePublicID' for the same repository manager.",
          expected.getMessage());
    }
  }

  @Test
  public void testDuplicatePublicId_Update() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    tempEntity.newRepository(repoManager, "Some Name", "SomePublicID1");
    Repository repository = tempEntity.newRepository(repoManager, "Some Other Name", "SomePublicID2");

    try {
      repository.setPublicId("SomePublicID1");
      dao.update(repository);
      fail("Expected InvalidRepositoryException");
    }
    catch (InvalidRepositoryException expected) {
      assertEquals("There is already a repository with public ID 'SomePublicID1' for the same repository manager.",
          expected.getMessage());
    }
  }

  @Test
  public void testValidateNullName_Insert() {
    try {
      tempEntity.newRepository(null, "SomePublicId");
      fail("Expected InvalidRepositoryException");
    }
    catch (InvalidRepositoryException expected) {
      assertThat(expected.getMessage(), is("The repository name cannot be null or empty."));
    }
  }

  @Test
  public void testValidateNullName_Update() {
    Repository repository = tempEntity.newRepository("Some Name", "SomePublicID");
    repository.setName(null);
    try {
      dao.update(repository);
      fail("Expected InvalidRepositoryException");
    }
    catch (InvalidRepositoryException expected) {
      assertThat(expected.getMessage(), is("The repository name cannot be null or empty."));
    }
  }

  @Test
  public void testValidateEmptyName_Insert() {
    try {
      tempEntity.newRepository(" ", "SomePublicID");
      fail("Expected InvalidRepositoryException");
    }
    catch (InvalidRepositoryException expected) {
      assertThat(expected.getMessage(), is("The repository name cannot be null or empty."));
    }
  }

  @Test
  public void testValidateEmptyName_Update() {
    Repository repository = tempEntity.newRepository("Some Name", "SomePublicID");
    repository.setName(" ");
    try {
      dao.update(repository);
      fail("Expected InvalidRepositoryException");
    }
    catch (InvalidRepositoryException expected) {
      assertThat(expected.getMessage(), is("The repository name cannot be null or empty."));
    }
  }
}
