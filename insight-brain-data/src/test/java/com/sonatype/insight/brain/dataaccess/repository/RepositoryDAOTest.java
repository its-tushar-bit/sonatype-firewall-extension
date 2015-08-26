/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
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
    Repository repository = tempEntity.newRepository("My Repo Public Id");
    String id = repository.getId();
    repository = dao.getById(id);
    assertThat(repository.getName(), is(repository.getPublicId()));
    assertThat(repository.getPublicId(), is("My Repo Public Id"));
    assertThat(repository.isEnabled(), is(true));
    assertThat(repository.getParentOwnerId(), is(RepositoryContainer.REPOSITORY_CONTAINER_ID));
    assertThat(repository.canHaveChildren(), is(false));
    assertThat(repository.getType(), is(OwnerType.REPOSITORY));

    // Update
    repository.setEnabled(false);
    dao.update(repository);
    repository = dao.getById(id);
    assertThat(repository.isEnabled(), is(false));

    // Delete
    dao.delete(repository);
    repository = dao.getById(id);
    assertThat(repository, is(nullValue()));
  }

  @Test
  public void testValidateNullPublicId_Insert() {
    try {
      tempEntity.newRepository(null /* publicId */);
      fail("Expected InvalidRepositoryException");
    }
    catch (InvalidRepositoryException expected) {
      assertThat(expected.getMessage(), is("The repository public ID cannot be null or empty."));
    }
  }

  @Test
  public void testValidateNullPublicId_Update() {
    Repository repository = tempEntity.newRepository("Some Public ID");
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
      tempEntity.newRepository(" " /* publicId */);
      fail("Expected InvalidRepositoryException");
    }
    catch (InvalidRepositoryException expected) {
      assertThat(expected.getMessage(), is("The repository public ID cannot be null or empty."));
    }
  }

  @Test
  public void testValidateEmptyPublicId_Update() {
    Repository repository = tempEntity.newRepository("Some Public ID");
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
    tempEntity.newRepository(repoManager, "SomePublicID");

    try {
      tempEntity.newRepository(repoManager, "SomePublicID");
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
    tempEntity.newRepository(repoManager, "SomePublicID1");
    Repository repository = tempEntity.newRepository(repoManager, "SomePublicID2");

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
  public void testCascadeDeleteToRepositoryComponents() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());

    dao.delete(repository);

    assertThat(new RepositoryComponentDAO().getById(repositoryComponent.getId()), is(nullValue()));
  }

  @Test
  public void testCascadeDeleteToRepositoryPolicyViolations() {
    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());

    dao.delete(repository);

    assertThat(new RepositoryPolicyViolationDAO().getById(policyViolation.getId()), is(nullValue()));
  }
}
