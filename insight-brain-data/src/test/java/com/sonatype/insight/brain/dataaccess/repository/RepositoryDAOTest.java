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
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    assertThat(repository.getName()).isEqualTo(repository.getPublicId());
    assertThat(repository.getPublicId()).isEqualTo("My Repo Public Id");
    assertThat(repository.isEnabled()).isTrue();
    assertThat(repository.getParentOwnerId()).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(repository.canHaveChildren()).isFalse();
    assertThat(repository.getType()).isEqualTo(OwnerType.REPOSITORY);

    // Update
    repository.setEnabled(false);
    dao.update(repository);
    repository = dao.getById(id);
    assertThat(repository.isEnabled()).isFalse();

    // Delete
    dao.delete(repository);
    repository = dao.getById(id);
    assertThat(repository).isNull();
  }

  @Test
  public void testValidateNullPublicId_Insert() {
    assertThatThrownBy(() -> {
      tempEntity.newRepository(null /* publicId */);
    }).isInstanceOf(InvalidRepositoryException.class).hasMessage("The repository public ID cannot be null or empty.");
  }

  @Test
  public void testValidateNullPublicId_Update() {
    Repository repository = tempEntity.newRepository("Some Public ID");
    repository.setPublicId(null);
    assertThatThrownBy(() -> {
      dao.update(repository);
    }).isInstanceOf(InvalidRepositoryException.class).hasMessage("The repository public ID cannot be null or empty.");
  }

  @Test
  public void testValidateEmptyPublicId_Insert() {
    assertThatThrownBy(() -> {
      tempEntity.newRepository(" " /* publicId */);
    }).isInstanceOf(InvalidRepositoryException.class).hasMessage("The repository public ID cannot be null or empty.");
  }

  @Test
  public void testValidateEmptyPublicId_Update() {
    Repository repository = tempEntity.newRepository("Some Public ID");
    repository.setPublicId(" ");
    assertThatThrownBy(() -> {
      dao.update(repository);
    }).isInstanceOf(InvalidRepositoryException.class).hasMessage("The repository public ID cannot be null or empty.");
  }

  @Test
  public void testDuplicatePublicId_Insert() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    tempEntity.newRepository(repoManager, "SomePublicID");

    assertThatThrownBy(() -> {
      tempEntity.newRepository(repoManager, "SomePublicID");
    }).isInstanceOf(InvalidRepositoryException.class)
        .hasMessage("There is already a repository with public ID 'SomePublicID' for the same repository manager.");
  }

  @Test
  public void testDuplicatePublicId_Update() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    tempEntity.newRepository(repoManager, "SomePublicID1");
    Repository repository = tempEntity.newRepository(repoManager, "SomePublicID2");

    repository.setPublicId("SomePublicID1");
    assertThatThrownBy(() -> {
      dao.update(repository);
    }).isInstanceOf(InvalidRepositoryException.class)
        .hasMessage("There is already a repository with public ID 'SomePublicID1' for the same repository manager.");
  }

  @Test
  public void testCascadeDeleteToRepositoryComponents() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());

    dao.delete(repository);

    assertThat(new RepositoryComponentDAO().getById(repositoryComponent.getId())).isNull();
  }

  @Test
  public void testCascadeDeleteToRepositoryPolicyViolations() {
    Repository repository = tempEntity.newRepository();
    RepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());

    dao.delete(repository);

    assertThat(new RepositoryPolicyViolationDAO().getById(policyViolation.getId())).isNull();
  }

  @Test
  public void testCascadeDeleteToRepositoryLicenseOverrides() {
    final ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    final LicenseOverride licenseOverride = tempEntity.newLicenseOverride(repository.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "ANTLR-PD");

    dao.delete(repository);

    assertThat(new LicenseOverrideDAO().getById(licenseOverride.getId())).isNull();
  }

  @Test
  public void testCascadeDeleteToSecurityVulnerabilityOverrides() {
    SecurityVulnerabilityOverride securityVulnerabilityOverride = tempEntity.newSecurityVulnerabilityOverride(
        repository.getId(), "hash", "source", "refrenceId", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);

    dao.delete(repository);

    assertThat(new SecurityVulnerabilityOverrideDAO().getById(securityVulnerabilityOverride.getId())).isNull();
  }

  @Test
  public void testCascadeDeleteToPolicyWaivers() {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiver policyWaiver = new PolicyWaiver(policy.getId(), repository.getId(), "Comment");
    PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();
    policyWaiverDAO.insert(policyWaiver);

    dao.delete(repository);

    assertThat(policyWaiverDAO.getByOwnerId(repository.getId())).isEmpty();
  }

  @Test
  public void testGetByRepositoryManagerInstanceIdAndPublicIdNotNull() throws Exception {
    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String publicId = "publicId";
    assertThatThrownBy(() -> {
      dao.getByRepositoryManagerInstanceIdAndPublicIdNotNull(repositoryManagerInstanceId, publicId);
    }).isInstanceOf(NotFoundException.class)
        .hasMessage(RepositoryDAO.getErrMsgMissingRepo(repositoryManagerInstanceId, publicId));
  }

  @Test
  public void testDisable_Insert() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity
        .newRepository(repoManager, "SomePublicID", false /* enabled */, true /* quarantineEnabled */);

    repository = dao.getById(repository.getId());
    assertThat(repository.isEnabled()).isFalse();
    assertThat(repository.isQuarantineEnabled()).isFalse();
  }

  @Test
  public void testDisable_Update() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity
        .newRepository(repoManager, "SomePublicID", true /* enabled */, true /* quarantineEnabled */);
    tempEntity.newRepositoryComponent(repository.getId(), "pathname", new Date() /* quarantineTime */,
        null /* unquarantineTime */);
    RepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId(), "pathname");

    repository.setEnabled(false);
    dao.update(repository);

    repository = dao.getById(repository.getId());
    assertThat(repository.isEnabled()).isFalse();
    assertThat(repository.isQuarantineEnabled()).isFalse();

    List<RepositoryComponent> repositoryComponents = new RepositoryComponentDAO().getByRepositoryId(repository.getId());
    assertThat(repositoryComponents).isEmpty();
    policyViolation = new RepositoryPolicyViolationDAO().getById(policyViolation.getId());
    assertThat(policyViolation.isActive()).isFalse();
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
    assertThat(repository.isQuarantineEnabled()).isFalse();

    repositoryComponent = new RepositoryComponentDAO().getById(repositoryComponent.getId());
    assertThat(repositoryComponent.isQuarantined()).isFalse();
    assertThat(repositoryComponent.getUnquarantineTime()).isAfterOrEqualsTo(before).isBeforeOrEqualsTo(after);
  }
}
