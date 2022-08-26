/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.ClusterLock;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
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
import com.sonatype.insight.postgres.PostgresServer;

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

    assertThat(policyWaiverDAO.getActiveByOwnerId(repository.getId())).isEmpty();
  }

  @Test
  public void testCascadeDeleteToRepositoryComponentLocks_H2() {
    testCascadeDeleteToRepositoryComponentLocks();
  }

  @Test
  public void testCascadeDeleteToRepositoryComponentLocks_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testCascadeDeleteToRepositoryComponentLocks();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testCascadeDeleteToRepositoryComponentLocks() {
    Repository repository = tempEntity.newRepository();
    RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    ClusterLock.createForRepositoryComponent(repository.getId(), repositoryComponent.getPathname())
        .close();
    String orphanComponentPathname = "orphanComponentPathname";
    ClusterLock.createForRepositoryComponent(repository.getId(), orphanComponentPathname).close();
    assertThat(ClusterLock.lockExists(ClusterLock
        .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent.getPathname()))).isTrue();
    assertThat(ClusterLock.lockExists(ClusterLock
        .getLockIdForRepositoryComponent(repository.getId(), orphanComponentPathname))).isTrue();

    new RepositoryDAO().delete(repository);

    assertThat(ClusterLock.lockExists(ClusterLock
        .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent.getPathname()))).isFalse();
    assertThat(ClusterLock.lockExists(ClusterLock
        .getLockIdForRepositoryComponent(repository.getId(), orphanComponentPathname))).isFalse();
  }

  @Test
  public void testCascadeDeleteToRepositoryReevaluationLocks_H2() {
    testCascadeDeleteToRepositoryReevaluationLocks();
  }

  @Test
  public void testCascadeDeleteToRepositoryReevaluationLocks_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testCascadeDeleteToRepositoryReevaluationLocks();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testCascadeDeleteToRepositoryReevaluationLocks() {
    Repository repository = tempEntity.newRepository();
    ClusterLock.createForRepositoryReevaluation(repository);
    assertThat(ClusterLock
        .lockExists(ClusterLock.getLockIdForRepositoryReevaluation(repository))).isTrue();

    new RepositoryDAO().delete(repository);

    assertThat(ClusterLock
        .lockExists(ClusterLock.getLockIdForRepositoryReevaluation(repository))).isFalse();
  }

  @Test
  public void testCascadeDeleteToRepositoryMigration() {
    Repository repository = tempEntity.newRepository();
    tempEntity.newRepositoryMigration(repository);
    assertThat(new RepositoryMigrationDAO().getByRepositoryId(repository.getId())).isNotNull();

    new RepositoryDAO().delete(repository);

    assertThat(new RepositoryMigrationDAO().getByRepositoryId(repository.getId())).isNull();
  }

  @Test
  public void testCascadeDeleteToPolicyMonitoring() {
    Repository repository = tempEntity.newRepository("testCascadeDeleteToPolicyMonitoring");
    tempEntity.newPolicyMonitoring(repository.getId(), Stage.ID_PROXY);
    assertThat(new PolicyMonitoringDAO().getByOwnerId(repository.getId())).isNotNull();

    dao.delete(repository);

    assertThat(new PolicyMonitoringDAO().getByOwnerId(repository.getId())).isNull();
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
    assertThat(policyViolation).isNull();
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
    assertThat(repositoryComponent.getUnquarantineTime()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    assertThat(repositoryComponent.getAutoUnquarantined()).isFalse();
  }

  @Test
  public void testGetCount() {
    // Note: First repo is being created in test setup of base class 
    tempEntity.newRepository("repo2");
    assertThat(dao.getCount()).isEqualTo(2);

    tempEntity.newRepository("repo3");
    assertThat(dao.getCount()).isEqualTo(3);
  }

  @Test
  public void testGetQuarantineEnabledCount() {
    tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo1", true, true /* quarantineEnabled */);
    tempEntity.newRepository(tempEntity.newRepositoryManager(), "repo2", true, false /* quarantineEnabled */);

    assertThat(dao.getQuarantineEnabledCount()).isEqualTo(1);
  }

  @Test
  public void testDelete_CascadesToPolicyActionOverrides() {
    Map<String, String> policyActionsOverrides = new HashMap<>();
    policyActionsOverrides.put("build", "warn");
    Policy policyWithOverrides = tempEntity.newPolicy(RepositoryContainer.SINGLETON.getId());
    policyWithOverrides.addPolicyActionsOverride(repository.getId(), policyActionsOverrides);
    policyWithOverrides.addPolicyActionsOverride("fakeOwnerId", policyActionsOverrides);
    new PolicyDAO().update(policyWithOverrides);

    dao.delete(repository);
    Policy policy = new PolicyDAO().getById(policyWithOverrides.getId());
    assertThat(policy.getPolicyActionsOverrides().keySet()).containsExactly("fakeOwnerId");
  }
}
