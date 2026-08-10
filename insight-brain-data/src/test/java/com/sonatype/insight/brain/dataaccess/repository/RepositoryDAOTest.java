/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.ProprietaryComponentNamePattern;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RepositoryDAOTest
    extends AbstractDbDAOTest
{
  private PolicyWaiverDAO policyWaiverDAO;

  private RepositoryMigrationDAO repositoryMigrationDAO;

  private PolicyMonitoringDAO policyMonitoringDAO;

  private ProprietaryComponentNamePatternDAO proprietaryComponentNamePatternDAO;

  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private PolicyDAO policyDAO;

  private SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private LicenseOverrideDAO licenseOverrideDAO;

  private OrganizationDAO organizationDAO;

  private RepositoryDAO dao;

  private HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryDAO();
    policyWaiverDAO = daoFactory.createPolicyWaiverDAO();
    repositoryMigrationDAO = daoFactory.createRepositoryMigrationDAO();
    policyMonitoringDAO = daoFactory.createPolicyMonitoringDAO();
    proprietaryComponentNamePatternDAO = daoFactory.createProprietaryComponentNamePatternDAO();
    proxyRepositoryComponentDAO = daoFactory.createRepositoryComponentDAO();
    proxyRepositoryPolicyViolationDAO = daoFactory.createRepositoryPolicyViolationDAO();
    policyDAO = daoFactory.createPolicyDAO();
    securityVulnerabilityOverrideDAO = daoFactory.createSecurityVulnerabilityOverrideDAO();
    licenseOverrideDAO = daoFactory.createLicenseOverrideDAO();
    organizationDAO = daoFactory.createOrganizationDAO();
    hostedRepositoryComponentDAO = daoFactory.createHostedRepositoryComponentDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    Repository repository = tempEntity.newRepository("My Repo Public Id");
    String repositorymanagerId = repository.getRepositoryManagerId();
    String id = repository.getId();
    repository = dao.getById(id);
    assertThat(repository.getName()).isEqualTo(repository.getPublicId());
    assertThat(repository.getPublicId()).isEqualTo("My Repo Public Id");
    assertThat(repository.isAuditEnabled()).isTrue();
    assertThat(repository.getParentOwnerId()).isEqualTo(repositorymanagerId);
    assertThat(repository.canHaveChildren()).isTrue();
    assertThat(repository.getType()).isEqualTo(OwnerType.REPOSITORY);
    assertThat(repository.getRepositoryType()).isEqualTo(RepositoryType.proxy);
    assertThat(repository.getLastManualConfigureTime()).isNull();

    // Update
    Date now = new Date();
    repository.setAuditEnabled(false);
    repository.setLastManualConfigureTime(now);
    dao.update(repository);
    repository = dao.getById(id);
    assertThat(repository.isAuditEnabled()).isFalse();
    assertThat(repository.getLastManualConfigureTime()).isEqualTo(now);

    // Delete
    dao.delete(repository);
    repository = dao.getById(id);
    assertThat(repository).isNull();
  }

  @Test
  public void testInsert_ValidateNullPublicId() {
    assertThatThrownBy(() -> tempEntity.newRepository((String) null /* publicId */))
        .isInstanceOf(InvalidRepositoryException.class)
        .hasMessage("The repository public ID cannot be null or empty.");
  }

  @Test
  public void testUpdate_ValidateNullPublicId() {
    Repository repository = tempEntity.newRepository("Some Public ID");
    repository.setPublicId(null);
    assertThatThrownBy(() -> dao.update(repository))
        .isInstanceOf(InvalidRepositoryException.class)
        .hasMessage("The repository public ID cannot be null or empty.");
  }

  @Test
  public void testInsert_ValidateEmptyPublicId() {
    assertThatThrownBy(() -> tempEntity.newRepository(" " /* publicId */))
        .isInstanceOf(InvalidRepositoryException.class)
        .hasMessage("The repository public ID cannot be null or empty.");
  }

  @Test
  public void testUpdate_ValidateEmptyPublicId() {
    Repository repository = tempEntity.newRepository("Some Public ID");
    repository.setPublicId(" ");
    assertThatThrownBy(() -> dao.update(repository))
        .isInstanceOf(InvalidRepositoryException.class)
        .hasMessage("The repository public ID cannot be null or empty.");
  }

  @Test
  public void testInsert_DuplicatePublicId() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    tempEntity.newRepository(repoManager, "SomePublicID");

    assertThatThrownBy(() -> tempEntity.newRepository(repoManager, "SomePublicID"))
        .isInstanceOf(InvalidRepositoryException.class)
        .hasMessage("There is already a repository with public ID 'SomePublicID' for the same repository manager.");
  }

  @Test
  public void testUpdate_DuplicatePublicId() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    tempEntity.newRepository(repoManager, "SomePublicID1");
    Repository repository = tempEntity.newRepository(repoManager, "SomePublicID2");

    repository.setPublicId("SomePublicID1");
    assertThatThrownBy(() -> dao.update(repository)).isInstanceOf(InvalidRepositoryException.class)
        .hasMessage("There is already a repository with public ID 'SomePublicID1' for the same repository manager.");
  }

  @Test
  public void testDelete_CascadesToRepositoryComponents() {
    Repository repository = tempEntity.newRepository();
    ProxyRepositoryComponent proxyRepositoryComponent = tempEntity.newRepositoryComponent(repository.getId());

    dao.delete(repository);

    assertThat(proxyRepositoryComponentDAO.getById(proxyRepositoryComponent.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToProxyRepositoryPolicyViolations() {
    Repository repository = tempEntity.newRepository();
    ProxyRepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());

    dao.delete(repository);

    assertThat(proxyRepositoryPolicyViolationDAO.getById(policyViolation.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToHostedRepositoryComponents() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository hostedRepo = tempEntity.newRepository(repoManager, "hostedRepoWithHrc",
        RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(hostedRepo);

    dao.delete(hostedRepo);

    assertThat(hostedRepositoryComponentDAO.getById(hrc.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToHostedRepositoryComponents_atChunkBoundary() {
    // HostedRepositoryComponentDAO.deleteByRepositoryId pages HRCs in chunks of 500; exercise both an
    // exact-multiple count and a count one over the boundary to catch off-by-one errors in the chunking loop.
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository hostedRepo = tempEntity.newRepository(repoManager, "hostedRepoWithManyHrcs",
        RepositoryType.hosted, ComponentIdentifier.FORMAT_MAVEN);

    List<HostedRepositoryComponent> hrcs = new ArrayList<>();
    for (int i = 0; i < 501; i++) {
      hrcs.add(new HostedRepositoryComponent(hostedRepo.getId(), "path/boundary-" + i + ".jar", "hash-" + i));
    }
    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      tx.begin();
      hostedRepositoryComponentDAO.insertBatch(tx, hrcs, false);
      tx.commit();
    }

    dao.delete(hostedRepo);

    try (TransactionContext tx = hostedRepositoryComponentDAO.createTransactionContext()) {
      assertThat(hostedRepositoryComponentDAO.getByRepositoryId(tx, hostedRepo.getId())).isEmpty();
    }
  }

  @Test
  public void testDelete_HostedRepo_CascadesToProxyRepositoryPolicyViolations() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repoManager, "hostedRepoPublicId",
        RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    ProxyRepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());

    dao.delete(repository);

    assertThat(proxyRepositoryPolicyViolationDAO.getById(policyViolation.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToRepositoryLicenseOverrides() {
    final ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    final LicenseOverride licenseOverride = tempEntity.newLicenseOverride(repository.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "ANTLR-PD");

    dao.delete(repository);

    assertThat(licenseOverrideDAO.getById(licenseOverride.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToSecurityVulnerabilityOverrides() {
    SecurityVulnerabilityOverride securityVulnerabilityOverride = tempEntity.newSecurityVulnerabilityOverride(
        repository.getId(), "hash", "source", "referenceId", SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);

    dao.delete(repository);

    assertThat(securityVulnerabilityOverrideDAO.getById(securityVulnerabilityOverride.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToPolicyWaivers() {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    PolicyWaiver policyWaiver = new PolicyWaiver(policy.getId(), repository.getId(), "Comment");
    policyWaiverDAO.insert(policyWaiver);

    // sanity check
    assertThat(policyWaiverDAO.getByOwnerId(repository.getId())).hasSize(1);

    dao.delete(repository);

    assertThat(policyWaiverDAO.getByOwnerId(repository.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadesToPolicyWaiverRequests() {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);
    ProxyRepositoryPolicyViolation policyViolation = tempEntity.newRepositoryPolicyViolation(repository.getId());
    tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest().setOwnerId(repository.getId())
        .setPolicyId(policy.getId())
        .setPolicyViolationId(policyViolation.getId()));

    // sanity check
    PolicyWaiverRequestDAO policyWaiverRequestDAO = daoFactory.createPolicyWaiverRequestDAO();
    assertThat(policyWaiverRequestDAO.getByOwnerId(repository.getId())).hasSize(1);

    dao.delete(repository);

    assertThat(policyWaiverRequestDAO.getByOwnerId(repository.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadesToRepositoryMigration() {
    Repository repository = tempEntity.newRepository();
    tempEntity.newRepositoryMigration(repository);
    assertThat(repositoryMigrationDAO.getByRepositoryId(repository.getId())).isNotNull();

    dao.delete(repository);

    assertThat(repositoryMigrationDAO.getByRepositoryId(repository.getId())).isNull();
  }

  @Test
  public void testDelete_CascadesToPolicyMonitoring() {
    Repository repository = tempEntity.newRepository("testCascadeDeleteToPolicyMonitoring");
    tempEntity.newPolicyMonitoring(repository.getId(), Stage.ID_PROXY);
    List<String> repos = new ArrayList<>();
    repos.add(repository.getId());
    assertThat(policyMonitoringDAO.getByOwnerId(repository.getId()))
        .isNotEmpty()
        .hasSize(1)
        .extracting("ownerId")
        .isEqualTo(repos);

    dao.delete(repository);

    assertThat(policyMonitoringDAO.getByOwnerId(repository.getId())).isEmpty();
  }

  @Test
  public void testDelete_CascadesToProprietaryComponentNamePatterns() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "testPublicId", RepositoryType.hosted,
        ComponentIdentifier.FORMAT_NPM);

    tempEntity.newProprietaryComponentNamePattern(repository, "namespacePattern", null);
    assertThat(proprietaryComponentNamePatternDAO.getByRepositoryId(repository.getId())).isNotEmpty();

    dao.delete(repository);

    assertThat(proprietaryComponentNamePatternDAO.getByRepositoryId(repository.getId())).isEmpty();
  }

  @Test
  public void testGetByRepositoryManagerInstanceIdAndPublicIdNotNull() {
    final String repositoryManagerInstanceId = "repositoryManagerInstanceId";
    final String publicId = "publicId";
    assertThatThrownBy(
        () -> dao.getByRepositoryManagerInstanceIdAndPublicIdNotNull(repositoryManagerInstanceId, publicId))
            .isInstanceOf(NotFoundException.class)
            .hasMessage(RepositoryDAO.getErrMsgMissingRepo(repositoryManagerInstanceId, publicId));
  }

  @Test
  public void tesInsert_ValidateEnabledFeatures_ProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository = new Repository(repoManager.getId(), "SomePublicID1");
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(false);
    repository.setPolicyCompliantComponentSelectionEnabled(false);
    repository.setNamespaceConfusionProtectionEnabled(false);

    dao.insert(repository);
    Repository retrievedRepository = dao.getById(repository.getId());
    assertThat(retrievedRepository.isAuditEnabled()).isFalse();
    assertThat(retrievedRepository.isQuarantineEnabled()).isFalse();
    assertThat(retrievedRepository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(retrievedRepository.isNamespaceConfusionProtectionEnabled()).isFalse();

    repository = new Repository(repoManager.getId(), "SomePublicID2");
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setAuditEnabled(true);
    repository.setQuarantineEnabled(false);
    repository.setPolicyCompliantComponentSelectionEnabled(false);
    repository.setNamespaceConfusionProtectionEnabled(false);
    dao.insert(repository);
    retrievedRepository = dao.getById(repository.getId());
    assertThat(retrievedRepository.isAuditEnabled()).isTrue();
    assertThat(retrievedRepository.isQuarantineEnabled()).isFalse();
    assertThat(retrievedRepository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(retrievedRepository.isNamespaceConfusionProtectionEnabled()).isFalse();

    repository = new Repository(repoManager.getId(), "SomePublicID3");
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setAuditEnabled(true);
    repository.setQuarantineEnabled(true);
    repository.setPolicyCompliantComponentSelectionEnabled(false);
    repository.setNamespaceConfusionProtectionEnabled(false);
    repository.setQuarantineEnabled(true);
    dao.insert(repository);
    retrievedRepository = dao.getById(repository.getId());
    assertThat(retrievedRepository.isAuditEnabled()).isTrue();
    assertThat(retrievedRepository.isQuarantineEnabled()).isTrue();
    assertThat(retrievedRepository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(retrievedRepository.isNamespaceConfusionProtectionEnabled()).isFalse();

    repository = new Repository(repoManager.getId(), "SomePublicID4");
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setAuditEnabled(true);
    repository.setQuarantineEnabled(true);
    repository.setPolicyCompliantComponentSelectionEnabled(true);
    repository.setNamespaceConfusionProtectionEnabled(false);
    dao.insert(repository);
    retrievedRepository = dao.getById(repository.getId());
    assertThat(retrievedRepository.isAuditEnabled()).isTrue();
    assertThat(retrievedRepository.isQuarantineEnabled()).isTrue();
    assertThat(retrievedRepository.isPolicyCompliantComponentSelectionEnabled()).isTrue();
    assertThat(retrievedRepository.isNamespaceConfusionProtectionEnabled()).isFalse();

    repository = new Repository(repoManager.getId(), "SomePublicID5");
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(true);
    repository.setPolicyCompliantComponentSelectionEnabled(false);
    repository.setNamespaceConfusionProtectionEnabled(false);
    dao.insert(repository);
    repository = dao.getById(repository.getId());
    assertThat(repository.isAuditEnabled()).isFalse();
    assertThat(repository.isQuarantineEnabled()).isFalse();
    assertThat(repository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(repository.isNamespaceConfusionProtectionEnabled()).isFalse();

    Repository invalidRepository = new Repository(repoManager.getId(), "SomePublicIDInvalid");
    invalidRepository.setRepositoryType(RepositoryType.proxy);
    invalidRepository.setAuditEnabled(false);
    invalidRepository.setQuarantineEnabled(false);
    invalidRepository.setPolicyCompliantComponentSelectionEnabled(false);
    invalidRepository.setNamespaceConfusionProtectionEnabled(true);
    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.insert(invalidRepository);
    }).withMessage("Namespace Confusion Protection can be enabled only for hosted repositories.");
    repository.setNamespaceConfusionProtectionEnabled(false);

    invalidRepository.setAuditEnabled(true);
    invalidRepository.setQuarantineEnabled(false);
    invalidRepository.setPolicyCompliantComponentSelectionEnabled(true);
    invalidRepository.setNamespaceConfusionProtectionEnabled(false);
    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.insert(invalidRepository);
    }).withMessage("Policy Compliant Component Selection requires Audit and Quarantine to be enabled.");
  }

  @Test
  public void testInsert_ValidateEnabledFeatures_HostedRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    final Repository repository = new Repository(repoManager.getId(), "SomePublicID");
    repository.setRepositoryType(RepositoryType.hosted);
    repository.setQuarantineEnabled(false);
    repository.setPolicyCompliantComponentSelectionEnabled(false);
    repository.setNamespaceConfusionProtectionEnabled(false);

    repository.setAuditEnabled(true);
    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.insert(repository);
    }).withMessage("Audit can be enabled only for proxy repositories.");

    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(true);
    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.insert(repository);
    }).withMessage("Quarantine can be enabled only for proxy repositories.");

    repository.setQuarantineEnabled(false);
    repository.setPolicyCompliantComponentSelectionEnabled(true);
    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.insert(repository);
    }).withMessage("Policy Compliant Component Selection can be enabled only for proxy repositories.");

    repository.setPolicyCompliantComponentSelectionEnabled(false);
    repository.setNamespaceConfusionProtectionEnabled(true);
    dao.insert(repository);

    Repository retrievedRepository = dao.getById(repository.getId());
    assertThat(retrievedRepository.isAuditEnabled()).isFalse();
    assertThat(retrievedRepository.isQuarantineEnabled()).isFalse();
    assertThat(retrievedRepository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(retrievedRepository.isNamespaceConfusionProtectionEnabled()).isTrue();
  }

  @Test
  public void testUpdate_ValidateEnabledFeatures_HostedRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    final Repository repository = new Repository(repoManager.getId(), "SomePublicID");
    repository.setRepositoryType(RepositoryType.hosted);
    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(false);
    repository.setPolicyCompliantComponentSelectionEnabled(false);
    repository.setNamespaceConfusionProtectionEnabled(false);
    dao.insert(repository);

    repository.setAuditEnabled(true);
    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.update(repository);
    }).withMessage("Audit can be enabled only for proxy repositories.");

    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(true);
    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.update(repository);
    }).withMessage("Quarantine can be enabled only for proxy repositories.");

    repository.setQuarantineEnabled(false);
    repository.setPolicyCompliantComponentSelectionEnabled(true);
    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.update(repository);
    }).withMessage("Policy Compliant Component Selection can be enabled only for proxy repositories.");

    repository.setPolicyCompliantComponentSelectionEnabled(false);
    repository.setNamespaceConfusionProtectionEnabled(true);
    dao.update(repository);

    Repository retrievedRepository = dao.getById(repository.getId());
    assertThat(retrievedRepository.isAuditEnabled()).isFalse();
    assertThat(retrievedRepository.isQuarantineEnabled()).isFalse();
    assertThat(retrievedRepository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(retrievedRepository.isNamespaceConfusionProtectionEnabled()).isTrue();
  }

  @Test
  public void testUpdate_ValidateEnabledFeatures_ProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    final Repository repository = new Repository(repoManager.getId(), "SomePublicID");
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(false);
    repository.setPolicyCompliantComponentSelectionEnabled(false);
    repository.setNamespaceConfusionProtectionEnabled(false);
    dao.insert(repository);

    repository.setAuditEnabled(true);
    dao.update(repository);
    Repository retrievedRepository = dao.getById(repository.getId());
    assertThat(retrievedRepository.isAuditEnabled()).isTrue();
    assertThat(retrievedRepository.isQuarantineEnabled()).isFalse();
    assertThat(retrievedRepository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(retrievedRepository.isNamespaceConfusionProtectionEnabled()).isFalse();

    repository.setQuarantineEnabled(true);
    dao.update(repository);
    retrievedRepository = dao.getById(repository.getId());
    assertThat(retrievedRepository.isAuditEnabled()).isTrue();
    assertThat(retrievedRepository.isQuarantineEnabled()).isTrue();
    assertThat(retrievedRepository.isPolicyCompliantComponentSelectionEnabled()).isFalse();
    assertThat(retrievedRepository.isNamespaceConfusionProtectionEnabled()).isFalse();

    repository.setPolicyCompliantComponentSelectionEnabled(true);
    dao.update(repository);
    retrievedRepository = dao.getById(repository.getId());
    assertThat(retrievedRepository.isAuditEnabled()).isTrue();
    assertThat(retrievedRepository.isQuarantineEnabled()).isTrue();
    assertThat(retrievedRepository.isPolicyCompliantComponentSelectionEnabled()).isTrue();

    repository.setNamespaceConfusionProtectionEnabled(true);
    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.update(repository);
    }).withMessage("Namespace Confusion Protection can be enabled only for hosted repositories.");
    repository.setNamespaceConfusionProtectionEnabled(false);

    repository.setAuditEnabled(false);
    repository.setQuarantineEnabled(true);
    repository.setNamespaceConfusionProtectionEnabled(false);
    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.update(repository);
    }).withMessage("Policy Compliant Component Selection requires Audit and Quarantine to be enabled.");

    repository.setAuditEnabled(true);
    repository.setQuarantineEnabled(false);
    repository.setNamespaceConfusionProtectionEnabled(false);
    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.update(repository);
    }).withMessage("Policy Compliant Component Selection requires Audit and Quarantine to be enabled.");
  }

  @Test
  public void testUpdate_DisableQuarantine() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity
        .newRepository(repoManager, "SomePublicID", true /* enabled */, true /* quarantineEnabled */);
    ProxyRepositoryComponent proxyRepositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "pathname",
            new Date() /* quarantineTime */, null /* unquarantineTime */);

    repository.setQuarantineEnabled(false);
    Date before = new Date();
    dao.update(repository);
    Date after = new Date();

    repository = dao.getById(repository.getId());
    assertThat(repository.isQuarantineEnabled()).isFalse();

    proxyRepositoryComponent = proxyRepositoryComponentDAO.getById(proxyRepositoryComponent.getId());
    assertThat(proxyRepositoryComponent.isQuarantined()).isFalse();
    assertThat(proxyRepositoryComponent.getUnquarantineTime()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    assertThat(proxyRepositoryComponent.getAutoUnquarantined()).isFalse();
  }

  @Test
  public void testUpdate_DisableAudit() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository =
        tempEntity.newRepository(repoManager, "SomePublicID", true /* enabled */, true /* quarantineEnabled */);
    ProxyRepositoryComponent proxyRepositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), "pathname",
            new Date() /* quarantineTime */, null /* unquarantineTime */);
    ProxyRepositoryPolicyViolation proxyRepositoryPolicyViolation =
        tempEntity.newRepositoryPolicyViolation(proxyRepositoryComponent, "testPolicyId");

    repository.setAuditEnabled(false);

    dao.update(repository);

    repository = dao.getById(repository.getId());
    assertThat(repository.isQuarantineEnabled()).isFalse();

    assertThat(proxyRepositoryComponentDAO.getById(proxyRepositoryComponent.getId())).isNull();
    assertThat(proxyRepositoryPolicyViolationDAO.getById(proxyRepositoryPolicyViolation.getId())).isNull();
  }

  @Test
  public void testUpdate_DisableNamespaceConfusionProtection() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repository =
        tempEntity.newRepository(repoManager, "SomePublicID", RepositoryType.hosted, ComponentIdentifier.FORMAT_NPM);
    repository.setNamespaceConfusionProtectionEnabled(true);
    dao.update(repository);
    ProprietaryComponentNamePattern proprietaryComponentNamePattern =
        tempEntity.newProprietaryComponentNamePattern(repository, "foo", null);

    repository.setNamespaceConfusionProtectionEnabled(false);

    dao.update(repository);

    repository = dao.getById(repository.getId());

    assertThat(proprietaryComponentNamePatternDAO.getById(proprietaryComponentNamePattern.getId())).isNull();
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
  public void testDelete_CascadesToPolicyOverrides() {
    Map<String, String> policyActionsOverrides = new HashMap<>();
    policyActionsOverrides.put("build", "warn");
    Policy policyWithOverrides = tempEntity.newPolicy(RepositoryContainer.SINGLETON.getId());
    policyWithOverrides.addPolicyActionsOverride(repository.getId(), policyActionsOverrides);
    policyWithOverrides.addPolicyActionsOverride("fakeOwnerId", policyActionsOverrides);
    Notifications policyNotificationsOverride = new Notifications();
    policyNotificationsOverride.add(new UserNotification("user@domain", BuildStageType.ID));
    policyWithOverrides.addPolicyNotificationsOverride(repository.getId(), policyNotificationsOverride);
    policyWithOverrides.addPolicyNotificationsOverride("fakeOwnerId", policyNotificationsOverride);
    policyDAO.update(policyWithOverrides);

    dao.delete(repository);
    Policy policy = policyDAO.getById(policyWithOverrides.getId());
    assertThat(policy.getPolicyActionsOverrides().keySet()).containsExactly("fakeOwnerId");
    assertThat(policy.getPolicyNotificationsOverrides().keySet()).containsExactly("fakeOwnerId");
  }

  @Test
  public void testUpdate_CannotChangeRepositoryType() {
    repository.setRepositoryType(RepositoryType.hosted);

    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.update(repository);
    }).withMessage("Cannot change the repository type.");
  }

  @Test
  public void testUpdate_CannotChangeFormat() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repository = tempEntity.newRepository(repositoryManager, "testPublicId", ComponentIdentifier.FORMAT_MAVEN);

    repository.setFormat(ComponentIdentifier.FORMAT_NPM);

    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.update(repository);
    }).withMessage("Cannot change the repository format.");
  }

  @Test
  public void testUpdate_NullFormat() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    repository = tempEntity.newRepository(repositoryManager, "testPublicId", null /* format */);

    repository.setFormat(ComponentIdentifier.FORMAT_NPM);

    dao.update(repository);

    repository = dao.getById(repository.getId());
    assertThat(repository.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_NPM);

    repository.setFormat(null);

    assertThatExceptionOfType(InvalidRepositoryException.class).isThrownBy(() -> {
      dao.update(repository);
    }).withMessage("Cannot change the repository format.");
  }

  @Test
  public void testGetByRepositoryManagerIdAndLastManualConfigureTime() {
    Date may5th20239AM = Date.from(LocalDateTime.of(2023, 5, 1, 9, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    Date may5th202310AM = Date.from(LocalDateTime.of(2023, 5, 1, 10, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
    Date may5th202311AM = Date.from(LocalDateTime.of(2023, 5, 1, 11, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    tempEntity.newRepository(repositoryManager, "testRepoNpm", RepositoryType.proxy, "npm",
        may5th20239AM);
    Repository repository =
        tempEntity.newRepository(repositoryManager, "testRepoMaven", RepositoryType.proxy, "maven", may5th202311AM);

    List<Repository> repositories =
        dao.getByRepositoryManagerIdAndLastManualConfigureTime(repositoryManager.getId(), may5th202310AM);

    assertThat(repositories).hasSize(1);

    Repository resultRepo = repositories.get(0);
    assertThat(resultRepo.getId()).isEqualTo(repository.getId());
    assertThat(resultRepo.getName()).isEqualTo(repository.getName());
    assertThat(resultRepo.getFormat()).isEqualTo(repository.getFormat());
    assertThat(resultRepo.getType()).isEqualTo(repository.getType());
    assertThat(resultRepo.isAuditEnabled()).isEqualTo(repository.isAuditEnabled());
    assertThat(resultRepo.isQuarantineEnabled()).isEqualTo(repository.isQuarantineEnabled());
    assertThat(resultRepo.isPolicyCompliantComponentSelectionEnabled()).isEqualTo(
        repository.isPolicyCompliantComponentSelectionEnabled());
    assertThat(resultRepo.isNamespaceConfusionProtectionEnabled()).isEqualTo(
        repository.isNamespaceConfusionProtectionEnabled());
  }

  @Test
  public void testGetByRepositoryType() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository hostedRepo = tempEntity.newHostedRepository(repoManager, "testHostedRepo", "npm", true);

    List<Repository> proxyRepos = dao.getByRepositoryType(RepositoryType.proxy);
    assertThat(proxyRepos).hasSize(1);
    assertThat(proxyRepos.get(0)).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(repository);
    List<Repository> hostedRepos = dao.getByRepositoryType(RepositoryType.hosted);
    assertThat(hostedRepos).hasSize(1);
    assertThat(hostedRepos.get(0)).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(hostedRepo);
  }

  @Test
  public void getByRepositoryManagerIdAndRepositoryType() {
    RepositoryManager repoManager1 = tempEntity.newRepositoryManager();
    Repository hostedRepo = tempEntity.newHostedRepository(repoManager1, "testHostedRepo", "npm", true);
    Repository proxyRepo = tempEntity.newProxyRepository(repoManager1, "testProxyRepo", "maven", true, false);

    RepositoryManager repoManager2 = tempEntity.newRepositoryManager();
    tempEntity.newHostedRepository(repoManager2, "testHostedRepo", "npm", true);
    tempEntity.newProxyRepository(repoManager2, "testProxyRepo", "maven", true, false);

    // result must include only hostedRepo of repoManager1
    List<Repository> hostedRepos =
        dao.getByRepositoryManagerIdAndRepositoryType(repoManager1.getId(), RepositoryType.hosted);
    assertThat(hostedRepos).hasSize(1);
    assertThat(hostedRepos.get(0)).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(hostedRepo);

    // result must include only proxyRepo of repoManager1
    List<Repository> proxyRepos =
        dao.getByRepositoryManagerIdAndRepositoryType(repoManager1.getId(), RepositoryType.proxy);
    assertThat(proxyRepos).hasSize(1);
    assertThat(proxyRepos.get(0)).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(proxyRepo);
  }

  @Test
  public void testGetCountByRepositoryType() {
    tempEntity.newProxyRepository(tempEntity.newRepositoryManager(), "proxyRepo1", "maven", true, true);
    tempEntity.newProxyRepository(tempEntity.newRepositoryManager(), "proxyRepo2", "npm", true, false);
    tempEntity.newHostedRepository(tempEntity.newRepositoryManager(), "hostedRepo2", "maven", false);
    tempEntity.newHostedRepository(tempEntity.newRepositoryManager(), "hostedRepo1", "npm", true);

    // AbstractDbDAOTest.setup() creates one proxy repo, so count should be 3
    assertThat(dao.getCountByRepositoryType(RepositoryType.proxy)).isEqualTo(3);
    assertThat(dao.getCountByRepositoryType(RepositoryType.hosted)).isEqualTo(2);
  }

  @Test
  public void testGetByRepositoryIdAndManagerId() {
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager();
    Repository repository1 = tempEntity.newRepository(repositoryManager1, "repo1");
    tempEntity.newRepository(repositoryManager1, "repo2");

    assertThat(dao.getByRepositoryIdAndManagerId(repositoryManager1.getId(), repository1.getId()).getId()).isEqualTo(
        repository1.getId());
    assertThat(dao.getByRepositoryIdAndManagerId(repositoryManager2.getId(), repository1.getId())).isNull();
  }

  @Test
  public void testGetByAncestorId() {
    assertThat(dao.getByAncestorId(Organization.ROOT_ORGANIZATION_ID))
        .extracting(Repository::getId)
        .containsExactlyInAnyOrder(repository.getId());

    assertThat(dao.getByAncestorId(tempEntity.newOrganization().getId())).isEmpty();

    RepositoryManager repoMan1 = tempEntity.newRepositoryManager();
    Repository repo11 = tempEntity.newRepository(repoMan1);
    Repository repo12 = tempEntity.newRepository(repoMan1);

    RepositoryManager repoMan2 = tempEntity.newRepositoryManager();
    Repository repo21 = tempEntity.newRepository(repoMan2);
    Repository repo22 = tempEntity.newRepository(repoMan2);

    assertThat(dao.getByAncestorId(Organization.ROOT_ORGANIZATION_ID))
        .extracting(Repository::getId)
        .containsExactlyInAnyOrder(repository.getId(), repo11.getId(), repo12.getId(), repo21.getId(), repo22.getId());

    assertThat(dao.getByAncestorId(repoMan1.getId()))
        .extracting(Repository::getId)
        .containsExactlyInAnyOrder(repo11.getId(), repo12.getId());

    assertThat(dao.getByAncestorId(repoMan2.getId()))
        .extracting(Repository::getId)
        .containsExactlyInAnyOrder(repo21.getId(), repo22.getId());

    // querying directly by repo id should not return that repo nor anything else
    assertThat(dao.getByAncestorId(repo11.getId())).isEmpty();
  }

  @Test
  public void testGetByContainerImageId() {
    Repository repository1 =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.proxy, "docker");

    organization.setRelatedRepositoryId(repository1.getId());
    organizationDAO.update(organization);

    // entities that should be found
    Repository repository2 =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo2", RepositoryType.proxy, "docker");
    Organization organization2 = tempEntity.newOrganization();
    organization2.setRelatedRepositoryId(repository2.getId());
    tempEntity.newApplicationWithParent(organization2);

    tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo3", RepositoryType.proxy, "docker");
    tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo4", RepositoryType.hosted, "docker");

    Repository result = dao.getByContainerImageId(application.getId());
    assertThat(result).isNotNull();
    JPA.assertEntityEquals(result, repository1);

    result = dao.getByContainerImageId(application.getPublicId());
    assertThat(result).isNotNull();
    JPA.assertEntityEquals(result, repository1);
  }

  @Test
  public void testGetByIds() {
    Repository repository1 = tempEntity.newRepository();
    Repository repository2 = tempEntity.newRepository();
    // not querying for this one
    tempEntity.newRepository();

    assertThat(dao.getByIds(Set.of(repository1.getId(), repository2.getId())))
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(JPA.IGNORE_FIELDS)
        .containsExactlyInAnyOrder(repository1, repository2);
  }
}
