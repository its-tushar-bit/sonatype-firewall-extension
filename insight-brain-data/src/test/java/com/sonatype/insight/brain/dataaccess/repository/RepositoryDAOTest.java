/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
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

  @Test
  public void testCascadeDeleteToRepositoryLicenseOverrides() {
    final ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    final LicenseOverride licenseOverride = tempEntity.newLicenseOverride(repository.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "ANTLR-PD");

    dao.delete(repository);

    assertThat(new LicenseOverrideDAO().getById(licenseOverride.getId()), is(nullValue()));
  }

  @Test
  public void testGetByRepositoryManagerInstanceIdAndPublicIdNotNull() throws Exception {
    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String publicId = "publicId";
    try {
      dao.getByRepositoryManagerInstanceIdAndPublicIdNotNull(repositoryManagerInstanceId, publicId);
      fail("Expected NotFoundException");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is(RepositoryDAO.getErrMsgMissingRepo(repositoryManagerInstanceId, publicId)));
    }
  }

  @Test
  public void testDisable_Insert() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity
        .newRepository(repoManager, "SomePublicID", false /* enabled */, true /* quarantineEnabled */);

    repository = dao.getById(repository.getId());
    assertThat(repository.isEnabled(), is(false));
    assertThat(repository.isQuarantineEnabled(), is(false));
  }

  @Test
  public void testDisable_Update() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity
        .newRepository(repoManager, "SomePublicID", true /* enabled */, true /* quarantineEnabled */);
    tempEntity
        .newRepositoryComponent(repository.getId(), "pathname", new Date() /* quarantineTime */, null /* unquarantineTime */);
    RepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId(), "pathname");

    repository.setEnabled(false);
    dao.update(repository);

    repository = dao.getById(repository.getId());
    assertThat(repository.isEnabled(), is(false));
    assertThat(repository.isQuarantineEnabled(), is(false));

    List<RepositoryComponent> repositoryComponents = new RepositoryComponentDAO().getByRepositoryId(repository.getId());
    assertThat(repositoryComponents, hasSize(0));
    policyViolation = new RepositoryPolicyViolationDAO().getById(policyViolation.getId());
    assertThat(policyViolation.isActive(), is(false));
  }

  @Test
  public void testDisableQuarantine_Update() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity
        .newRepository(repoManager, "SomePublicID", true /* enabled */, true /* quarantineEnabled */);
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId(), "pathname",
        new Date() /* quarantineTime */, null /* unquarantineTime */);

    repository.setQuarantineEnabled(false);
    Date before = new Date();
    dao.update(repository);
    Date after = new Date();

    repository = dao.getById(repository.getId());
    assertThat(repository.isQuarantineEnabled(), is(false));

    repositoryComponent = new RepositoryComponentDAO().getById(repositoryComponent.getId());
    assertThat(repositoryComponent.isQuarantined(), is(false));
    assertThat(repositoryComponent.getUnquarantineTime(), is(greaterThanOrEqualTo(before)));
    assertThat(repositoryComponent.getUnquarantineTime(), is(lessThanOrEqualTo(after)));
  }
}
