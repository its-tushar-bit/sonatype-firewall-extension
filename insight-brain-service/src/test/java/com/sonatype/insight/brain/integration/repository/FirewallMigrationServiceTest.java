/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import static com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO.getErrMsgMissingRepo;
import static com.sonatype.insight.brain.integration.repository.FirewallMigrationService.PROTOCOL_V1;
import static com.sonatype.insight.brain.model.license.LicenseOverrideStatus.OVERRIDDEN;
import static com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus.CONFIRMED;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.repository.migration.MigrationDetails;
import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryMigrationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.repository.RepositoryMigration;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class FirewallMigrationServiceTest
    extends AbstractComponentTest
{
  private static final String SOURCE_REPOSITORY_MANAGER_INSTANCE_ID = "sourceRepositoryManagerInstance";

  private static final String SOURCE_REPOSITORY_PUBLIC_ID = "source-repository";

  private static final String TARGET_REPOSITORY_MANAGER_INSTANCE_ID = "repositoryManagerInstance";

  private static final String TARGET_REPOSITORY_PUBLIC_ID = "repository";

  @Inject
  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @Inject
  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  @Inject
  private LicenseOverrideDAO licenseOverrideDAO;

  @Inject
  private SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private RepositoryMigrationDAO repositoryMigrationDAO;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private FirewallMigrationService migrationService;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private Policy policy;

  @Test
  public void testFirewallMigrationService_AddsExecutorToShutdownHandler() {
    FirewallMigrationService localService = new FirewallMigrationService(
        lookup(com.sonatype.insight.brain.version.VersionService.class),
        testProductLicense,
        lookup(com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO.class),
        lookup(RepositoryDAO.class),
        lookup(RepositoryMigrationDAO.class),
        lookup(ProxyRepositoryComponentDAO.class),
        lookup(ProxyRepositoryPolicyViolationDAO.class),
        lookup(LicenseOverrideDAO.class),
        lookup(SecurityVulnerabilityOverrideDAO.class),
        lookup(PolicyWaiverDAO.class),
        mockShutdownHandler);

    try {
      verify(mockShutdownHandler).add(localService.getExecutor());
    }
    finally {
      localService.getExecutor().shutdownNow();
    }
  }

  @Before
  public void createPolicy() {
    policy = tempEntity.newPolicy();
  }

  @Test
  public void testVerifyMigrationSupport() {
    migrationService.verifyMigrationSupport(PROTOCOL_V1);
  }

  @Test
  public void testVerifyMigrationSupport_UnsupportedProtocolVersion() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> migrationService.verifyMigrationSupport("v2"))
        .withMessageEndingWith("does not support migration protocol v2, please update your IQ Server.");
  }

  @Test
  public void testVerifyMigrationSupport_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> migrationService.verifyMigrationSupport(PROTOCOL_V1))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testMigrateRepositoryHistory_MissingLicenseFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> migrationService.migrateRepositoryHistory(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID,
            SOURCE_REPOSITORY_PUBLIC_ID, TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID))
        .withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testMigrateRepositoryHistory_UnknownSource() {
    createTargetRepository();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> migrationService.migrateRepositoryHistory(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID,
            SOURCE_REPOSITORY_PUBLIC_ID, TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID))
        .withMessage(getErrMsgMissingRepo(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID, SOURCE_REPOSITORY_PUBLIC_ID));
  }

  @Test
  public void testMigrateRepositoryHistory_ExistingTarget() {
    Repository repository = createTargetRepository();
    Repository sourceRepository = createSourceRepository();
    GeneratedRepositoryData data = generateRepositoryData(sourceRepository);

    testMigrateRepositoryHistory(repository, null, sourceRepository, data);
  }

  @Test
  public void testMigrateRepositoryHistory_AutoCreateTarget() {
    Repository sourceRepository = createSourceRepository();
    GeneratedRepositoryData data = generateRepositoryData(sourceRepository);

    testMigrateRepositoryHistory(null, null, sourceRepository, data);
  }

  @Test
  public void testMigrateRepositoryHistory_Completed() throws Exception {
    testMigrateRepositoryHistoryRerun(MigrationState.COMPLETED);
  }

  @Test
  public void testMigrateRepositoryHistory_Failed() throws Exception {
    testMigrateRepositoryHistoryRerun(MigrationState.FAILED);
  }

  private void testMigrateRepositoryHistoryRerun(MigrationState migrationState) {
    Repository repository = createTargetRepository();
    GeneratedRepositoryData previousRunData = generateRepositoryData(repository);
    Repository sourceRepository = createSourceRepository();
    GeneratedRepositoryData data = generateRepositoryData(sourceRepository);

    RepositoryMigration repositoryMigration = new RepositoryMigration();
    repositoryMigration.setRepositoryId(repository.getId());
    repositoryMigration.setState(migrationState);
    assertThat(repositoryMigrationDAO.tryInsert(repositoryMigration)).isTrue();

    testMigrateRepositoryHistory(repository, previousRunData, sourceRepository, data);
  }

  private void testMigrateRepositoryHistory(
      Repository targetRepository,
      GeneratedRepositoryData previousRunData,
      Repository sourceRepository,
      GeneratedRepositoryData sourceData)
  {
    migrationService.migrateRepositoryHistory(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID, SOURCE_REPOSITORY_PUBLIC_ID,
        TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);

    // Wait for migration to complete
    await().atMost(1, TimeUnit.MINUTES)
        .untilAsserted(() -> assertThat(migrationService
            .getRepositoryMigrationState(TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID)
            .getState())
                .isEqualTo(MigrationState.COMPLETED));

    // Assert source untouched
    assertThat(proxyRepositoryComponentDAO.getByRepositoryId(sourceRepository.getId()))
        .usingElementComparator(componentComparator)
        .containsExactlyInAnyOrderElementsOf(sourceData.components);
    assertThat(proxyRepositoryPolicyViolationDAO.getByRepositoryId(sourceRepository.getId()))
        .usingElementComparator(violationComparator)
        .containsExactlyInAnyOrderElementsOf(sourceData.violations);
    assertThat(licenseOverrideDAO.getByOwnerId(sourceRepository.getId()))
        .usingElementComparator(licenseOverrideComparator)
        .containsExactlyInAnyOrderElementsOf(sourceData.licenseOverrides);
    assertThat(securityVulnerabilityOverrideDAO.getByOwnerId(sourceRepository.getId()))
        .usingElementComparator(vulnerabilityOverrideComparator)
        .containsExactlyInAnyOrderElementsOf(sourceData.vulnerabilityOverrides);
    assertThat(policyWaiverDAO.getByOwnerId(sourceRepository.getId())).usingElementComparator(waiverComparator)
        .containsExactlyInAnyOrderElementsOf(sourceData.policyWaivers);

    if (targetRepository != null) {
      targetRepository = repositoryDAO.getById(targetRepository.getId());
    }
    else {
      targetRepository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(
          TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);
      // Assert the target repository is created automatically
      assertThat(targetRepository).isNotNull();
    }
    // Assert the target repository mirrors the source configuration
    assertThat(targetRepository.getFormat()).isEqualTo(sourceRepository.getFormat());
    assertThat(targetRepository.isAuditEnabled()).isEqualTo(sourceRepository.isAuditEnabled());
    assertThat(targetRepository.isQuarantineEnabled()).isEqualTo(sourceRepository.isQuarantineEnabled());

    // Assert Components are migrated
    List<ProxyRepositoryComponent> migratedComponents = proxyRepositoryComponentDAO
        .getByRepositoryId(targetRepository.getId());
    assertThat(migratedComponents).usingElementComparator(componentComparatorIgnoringIds)
        .containsExactlyInAnyOrderElementsOf(sourceData.components);
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedComponents).usingElementComparator(componentComparator)
          .doesNotContainAnyElementsOf(previousRunData.components);
    }
    // Assert Policy Violations are migrated
    List<ProxyRepositoryPolicyViolation> migratedViolations = proxyRepositoryPolicyViolationDAO
        .getByRepositoryId(targetRepository.getId());
    assertThat(migratedViolations).usingElementComparator(violationComparatorIgnoringIds)
        .containsExactlyInAnyOrderElementsOf(sourceData.violations);
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedViolations).usingElementComparator(violationComparator)
          .doesNotContainAnyElementsOf(previousRunData.violations);
    }
    // Assert License Overrides are migrated
    List<LicenseOverride> migratedLicenseOverrides = licenseOverrideDAO.getByOwnerId(targetRepository.getId());
    assertThat(migratedLicenseOverrides).usingElementComparator(licenseOverrideComparatorIgnoringIds)
        .containsExactlyInAnyOrderElementsOf(sourceData.licenseOverrides);
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedLicenseOverrides).usingElementComparator(licenseOverrideComparator)
          .doesNotContainAnyElementsOf(previousRunData.licenseOverrides);
    }
    // Assert Security Vulnerability Overrides are migrated
    List<SecurityVulnerabilityOverride> migratedVulnerabilityOverrides = securityVulnerabilityOverrideDAO
        .getByOwnerId(targetRepository.getId());
    assertThat(migratedVulnerabilityOverrides).usingElementComparator(vulnerabilityOverrideComparatorIgnoringIds)
        .containsExactlyInAnyOrderElementsOf(sourceData.vulnerabilityOverrides);
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedVulnerabilityOverrides).usingElementComparator(vulnerabilityOverrideComparator)
          .doesNotContainAnyElementsOf(previousRunData.vulnerabilityOverrides);
    }
    // Assert Security Vulnerability Overrides are migrated
    List<PolicyWaiver> migratedPolicyWaivers = policyWaiverDAO.getByOwnerId(targetRepository.getId());
    assertThat(migratedPolicyWaivers).usingElementComparator(waiverComparatorIgnoringIds)
        .containsExactlyInAnyOrderElementsOf(sourceData.policyWaivers);
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedPolicyWaivers).usingElementComparator(waiverComparator)
          .doesNotContainAnyElementsOf(previousRunData.policyWaivers);
    }
  }

  /**
   * Generates 3 {@link ProxyRepositoryComponent components} in the specified {@link Repository} where the first one in
   * the
   * returned list is the one with the latest evaluation timestamp.
   *
   * Each {@link ProxyRepositoryComponent} will get a number of {@link ProxyRepositoryPolicyViolation violations} that
   * is equal
   * to the index under which the component is added in the returned list (first component has no violations, the third
   * has 2 violations).
   */
  private GeneratedRepositoryData generateRepositoryData(Repository repository) {
    GeneratedRepositoryData generatedRepositoryData = new GeneratedRepositoryData();
    DateTime now = DateTime.now();
    while (generatedRepositoryData.components.size() < 3) {
      ProxyRepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(),
          "migrate/history/component-" + (generatedRepositoryData.components.size() + 1), null, null,
          now.minusMinutes(generatedRepositoryData.components.size()).toDate());
      for (int i = 0; i < generatedRepositoryData.components.size(); i++) {
        ProxyRepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(repository.getId(), 5,
            component.getPathname(), true, Action.ID_FAIL, policy.getId(), policy.getName(),
            component.getComponentIdentifier(), component.getLastEvaluationTime());
        generatedRepositoryData.violations.add(violation);
      }
      generatedRepositoryData.components.add(component);
    }

    generatedRepositoryData.policyWaivers.add(tempEntity.newWaiver("hash", policy.getId(), repository.getId()));
    generatedRepositoryData.policyWaivers.add(tempEntity.newWaiver("hash2", policy.getId(), repository.getId(),
        null, "comment", now.toDate(), now.plusHours(1).toDate())); // future expiry
    generatedRepositoryData.policyWaivers.add(tempEntity.newWaiver("hash3", policy.getId(), repository.getId(),
        null, "comment", now.toDate(), now.toDate())); // expired
    generatedRepositoryData.licenseOverrides.add(tempEntity.newLicenseOverride(repository.getId(),
        generatedRepositoryData.components.get(0).getComponentIdentifier(), OVERRIDDEN, "Apache-2.0"));
    generatedRepositoryData.vulnerabilityOverrides.add(
        tempEntity.newSecurityVulnerabilityOverride(repository.getId(), "hash", "source", "referenceId", CONFIRMED));
    generatedRepositoryData.vulnerabilityOverrides.add(
        tempEntity.newSecurityVulnerabilityOverride(repository.getId(), "hash2", "source2", "referenceId2", CONFIRMED));
    return generatedRepositoryData;
  }

  @Test
  public void testMigrateRepositoryHistory_AlreadyRunning() {
    createSourceRepository();
    Repository targetRepository = createTargetRepository();
    RepositoryMigration repositoryMigration = new RepositoryMigration();
    repositoryMigration.setRepositoryId(targetRepository.getId());
    repositoryMigration.setState(MigrationState.RUNNING);
    assertThat(repositoryMigrationDAO.tryInsert(repositoryMigration)).isTrue();

    // The migration request is ignored and the migration continues.
    migrationService.migrateRepositoryHistory(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID, SOURCE_REPOSITORY_PUBLIC_ID,
        TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testGetRepositoryMigrationState_UnknownRepository() {
    tempEntity.newRepositoryManager(TARGET_REPOSITORY_MANAGER_INSTANCE_ID);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> migrationService.getRepositoryMigrationState(TARGET_REPOSITORY_MANAGER_INSTANCE_ID, "unknown"))
        .withMessage(getErrMsgMissingRepo(TARGET_REPOSITORY_MANAGER_INSTANCE_ID, "unknown"));
  }

  @Test
  public void testGetRepositoryMigrationState_MigrationNotStarted() {
    createTargetRepository();

    MigrationDetails migrationDetails = migrationService
        .getRepositoryMigrationState(TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);

    assertThat(migrationDetails.getState()).isEqualTo(MigrationState.FAILED);
  }

  private Repository createTargetRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(TARGET_REPOSITORY_MANAGER_INSTANCE_ID);
    return tempEntity.newRepository(repositoryManager, TARGET_REPOSITORY_PUBLIC_ID);
  }

  private Repository createSourceRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID);
    return tempEntity.newRepository(repositoryManager, SOURCE_REPOSITORY_PUBLIC_ID);
  }

  private static class GeneratedRepositoryData
  {
    final List<ProxyRepositoryComponent> components = new ArrayList<>();

    final List<ProxyRepositoryPolicyViolation> violations = new ArrayList<>();

    final List<LicenseOverride> licenseOverrides = new ArrayList<>();

    final List<SecurityVulnerabilityOverride> vulnerabilityOverrides = new ArrayList<>();

    final List<PolicyWaiver> policyWaivers = new ArrayList<>();
  }

  private <T extends Comparable<? super T>> Comparator<T> nullSafe() {
    return Comparator.nullsFirst(Comparator.naturalOrder());
  }

  private final Comparator<ProxyRepositoryComponent> componentComparatorIgnoringIds = Comparator //
      .comparing(ProxyRepositoryComponent::getPathname) //
      .thenComparing(ProxyRepositoryComponent::getTime) //
      .thenComparing(ProxyRepositoryComponent::getHash) //
      .thenComparing(ProxyRepositoryComponent::getMatchStateId) //
      .thenComparing(ProxyRepositoryComponent::getIdentificationSourceId) //
      .thenComparing(ProxyRepositoryComponent::getLastEvaluationTime) //
      .thenComparing(ProxyRepositoryComponent::getQuarantineTime, nullSafe()) //
      .thenComparing(ProxyRepositoryComponent::getUnquarantineTime, nullSafe()) //
      .thenComparing(ProxyRepositoryComponent::getComponentIdentifier, nullSafe());

  private final Comparator<ProxyRepositoryComponent> componentComparator = Comparator //
      .comparing(ProxyRepositoryComponent::getId) //
      .thenComparing(ProxyRepositoryComponent::getRepositoryId) //
      .thenComparing(componentComparatorIgnoringIds);

  private final Comparator<ProxyRepositoryPolicyViolation> violationComparatorIgnoringIds = Comparator //
      .comparing(ProxyRepositoryPolicyViolation::getPathname) //
      .thenComparing(ProxyRepositoryPolicyViolation::getTime) //
      .thenComparing(ProxyRepositoryPolicyViolation::getPolicyId) //
      .thenComparing(ProxyRepositoryPolicyViolation::getPolicyName) //
      .thenComparing(ProxyRepositoryPolicyViolation::getThreatLevel) //
      .thenComparing(ProxyRepositoryPolicyViolation::getThreatCategory) //
      .thenComparing(ProxyRepositoryPolicyViolation::getHash, nullSafe()) //
      .thenComparing(ProxyRepositoryPolicyViolation::getConstraintFactsId) //
      .thenComparing(ProxyRepositoryPolicyViolation::getActionTypeId, nullSafe()) //
      .thenComparing(ProxyRepositoryPolicyViolation::isWaived) //
      .thenComparing(ProxyRepositoryPolicyViolation::getComponentIdentifier, nullSafe());

  private final Comparator<ProxyRepositoryPolicyViolation> violationComparator = Comparator //
      .comparing(ProxyRepositoryPolicyViolation::getId) //
      .thenComparing(ProxyRepositoryPolicyViolation::getRepositoryId) //
      .thenComparing(violationComparatorIgnoringIds);

  private final Comparator<LicenseOverride> licenseOverrideComparatorIgnoringIds = Comparator //
      .comparing(LicenseOverride::getComponentIdentifier) //
      .thenComparing(LicenseOverride::getStatus) //
      .thenComparing(LicenseOverride::getComment, nullSafe()) //
      .thenComparing(override -> override.getLicenseIds().stream().sorted().collect(joining(",")));

  private final Comparator<LicenseOverride> licenseOverrideComparator = Comparator //
      .comparing(LicenseOverride::getId) //
      .thenComparing(LicenseOverride::getOwnerId) //
      .thenComparing(licenseOverrideComparatorIgnoringIds);

  private final Comparator<SecurityVulnerabilityOverride> vulnerabilityOverrideComparatorIgnoringIds = Comparator //
      .comparing(SecurityVulnerabilityOverride::getHash) //
      .thenComparing(SecurityVulnerabilityOverride::getSource) //
      .thenComparing(SecurityVulnerabilityOverride::getReferenceId) //
      .thenComparing(SecurityVulnerabilityOverride::getStatus) //
      .thenComparing(SecurityVulnerabilityOverride::getComment, nullSafe());

  private final Comparator<SecurityVulnerabilityOverride> vulnerabilityOverrideComparator = Comparator //
      .comparing(SecurityVulnerabilityOverride::getId) //
      .thenComparing(SecurityVulnerabilityOverride::getOwnerId) //
      .thenComparing(vulnerabilityOverrideComparatorIgnoringIds);

  private final Comparator<PolicyWaiver> waiverComparatorIgnoringIds = Comparator //
      .comparing(PolicyWaiver::getPolicyId) //
      .thenComparing(PolicyWaiver::getHash, nullSafe()) //
      .thenComparing(PolicyWaiver::getCreateTime) //
      .thenComparing(PolicyWaiver::getComment, nullSafe()) //
      .thenComparing(PolicyWaiver::getExpiryTime, nullSafe());

  private final Comparator<PolicyWaiver> waiverComparator = Comparator //
      .comparing(PolicyWaiver::getId) //
      .thenComparing(PolicyWaiver::getOwnerId) //
      .thenComparing(waiverComparatorIgnoringIds);
}
