/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.repository.migration.MigrationDetails;
import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO.getErrMsgMissingRepo;
import static com.sonatype.insight.brain.integration.repository.FirewallMigrationService.PROTOCOL_V1;
import static com.sonatype.insight.brain.model.license.LicenseOverrideStatus.OVERRIDDEN;
import static com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus.CONFIRMED;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;

public class FirewallMigrationServiceTest
    extends AbstractComponentTest
{
  private static final String SOURCE_REPOSITORY_MANAGER_INSTANCE_ID = "sourceRepositoryManagerInstance";

  private static final String SOURCE_REPOSITORY_PUBLIC_ID = "source-repository";

  private static final String TARGET_REPOSITORY_MANAGER_INSTANCE_ID = "repositoryManagerInstance";

  private static final String TARGET_REPOSITORY_PUBLIC_ID = "repository";

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private FirewallMigrationService migrationService;

  private Policy policy;

  @Before
  public void createPolicy() throws Exception {
    policy = tempEntity.newPolicy();
  }

  @After
  public void deleteAutoCreatedRepositoryManagers() throws Exception {
    RepositoryManagerDAO repositoryManagerDAO = new RepositoryManagerDAO();
    RepositoryManager manager = repositoryManagerDAO.getByInstanceId(TARGET_REPOSITORY_MANAGER_INSTANCE_ID);
    if (manager != null) {
      repositoryManagerDAO.delete(manager);
    }
  }

  @Test
  public void testVerifyMigrationSupport() throws Exception {
    migrationService.verifyMigrationSupport(PROTOCOL_V1);
  }

  @Test
  public void testVerifyMigrationSupport_UnsupportedProtocolVersion() throws Exception {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      migrationService.verifyMigrationSupport("v2");
    }).withMessageEndingWith("does not support migration protocol v2, please update your IQ Server.");
  }

  @Test
  public void testVerifyMigrationSupport_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      migrationService.verifyMigrationSupport(PROTOCOL_V1);
    }).withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testMigrateRepositoryHistory_MissingLicenseFeature() throws Exception {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      migrationService.migrateRepositoryHistory(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID, SOURCE_REPOSITORY_PUBLIC_ID,
          TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);
    }).withMessage(InvalidLicenseException.INVALID_LICENSE_MSG);
  }

  @Test
  public void testMigrateRepositoryHistory_UnknownSource() throws Exception {
    createTargetRepository();
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      migrationService.migrateRepositoryHistory(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID, SOURCE_REPOSITORY_PUBLIC_ID,
          TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);
    }).withMessage(getErrMsgMissingRepo(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID, SOURCE_REPOSITORY_PUBLIC_ID));
  }

  @Test
  public void testMigrateRepositoryHistory_ExistingTarget() throws Exception {
    Repository repository = createTargetRepository();
    Repository sourceRepository = createSourceRepository();
    GeneratedRepositoryData data = generateRepositoryData(sourceRepository);

    testMigrateRepositoryHistory(repository, null, sourceRepository, data);
  }

  @Test
  public void testMigrateRepositoryHistory_AutoCreateTarget() throws Exception {
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

  private void testMigrateRepositoryHistoryRerun(MigrationState migrationState) throws Exception {
    Repository repository = createTargetRepository();
    GeneratedRepositoryData previousRunData = generateRepositoryData(repository);
    Repository sourceRepository = createSourceRepository();
    GeneratedRepositoryData data = generateRepositoryData(sourceRepository);

    MigrationDetails migrationDetails = new MigrationDetails(migrationState);
    assertThat(migrationService.putIfAbsent(repository.getId(), migrationDetails)).isTrue();

    testMigrateRepositoryHistory(repository, previousRunData, sourceRepository, data);
  }

  private void testMigrateRepositoryHistory(Repository targetRepository,
                                            GeneratedRepositoryData previousRunData,
                                            Repository sourceRepository,
                                            GeneratedRepositoryData sourceData)
  {
    migrationService.migrateRepositoryHistory(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID, SOURCE_REPOSITORY_PUBLIC_ID,
        TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);

    // Wait for migration to complete
    await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
      assertThat(migrationService
          .getRepositoryMigrationState(TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID).getState())
              .isEqualTo(MigrationState.COMPLETED);
    });

    // Assert source untouched
    assertThat(new RepositoryComponentDAO().getByRepositoryId(sourceRepository.getId()))
        .usingElementComparator(componentComparator).containsExactlyInAnyOrderElementsOf(sourceData.components);
    assertThat(new RepositoryPolicyViolationDAO().getByRepositoryId(sourceRepository.getId()))
        .usingElementComparator(violationComparator).containsExactlyInAnyOrderElementsOf(sourceData.violations);
    assertThat(new LicenseOverrideDAO().getByOwnerId(sourceRepository.getId()))
        .usingElementComparator(licenseOverrideComparator)
        .containsExactlyInAnyOrderElementsOf(sourceData.licenseOverrides);
    assertThat(new SecurityVulnerabilityOverrideDAO().getByOwnerId(sourceRepository.getId()))
        .usingElementComparator(vulnerabilityOverrideComparator)
        .containsExactlyInAnyOrderElementsOf(sourceData.vulnerabilityOverrides);
    assertThat(new PolicyWaiverDAO().getByOwnerId(sourceRepository.getId())).usingElementComparator(waiverComparator)
        .containsExactlyInAnyOrderElementsOf(sourceData.policyWaivers);

    if (targetRepository != null) {
      targetRepository = new RepositoryDAO().getById(targetRepository.getId());
    }
    else {
      targetRepository = new RepositoryDAO().getByRepositoryManagerInstanceIdAndPublicId(
          TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);
      // Assert the target repository is created automatically
      assertThat(targetRepository).isNotNull();
    }
    // Assert the target repository mirrors the source configuration
    assertThat(targetRepository.getFormat()).isEqualTo(sourceRepository.getFormat());
    assertThat(targetRepository.isEnabled()).isEqualTo(sourceRepository.isEnabled());
    assertThat(targetRepository.isQuarantineEnabled()).isEqualTo(sourceRepository.isQuarantineEnabled());

    // Assert Components are migrated
    List<RepositoryComponent> migratedComponents = new RepositoryComponentDAO()
        .getByRepositoryId(targetRepository.getId());
    assertThat(migratedComponents).usingElementComparator(componentComparatorIgnoringIds)
        .containsExactlyInAnyOrderElementsOf(sourceData.components);
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedComponents).usingElementComparator(componentComparator)
          .doesNotContainAnyElementsOf(previousRunData.components);
    }
    // Assert Policy Violations are migrated
    List<RepositoryPolicyViolation> migratedViolations = new RepositoryPolicyViolationDAO()
        .getByRepositoryId(targetRepository.getId());
    assertThat(migratedViolations).usingElementComparator(violationComparatorIgnoringIds)
        .containsExactlyInAnyOrderElementsOf(sourceData.violations);
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedViolations).usingElementComparator(violationComparator)
          .doesNotContainAnyElementsOf(previousRunData.violations);
    }
    // Assert License Overrides are migrated
    List<LicenseOverride> migratedLicenseOverrides = new LicenseOverrideDAO().getByOwnerId(targetRepository.getId());
    assertThat(migratedLicenseOverrides).usingElementComparator(licenseOverrideComparatorIgnoringIds)
        .containsExactlyInAnyOrderElementsOf(sourceData.licenseOverrides);
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedLicenseOverrides).usingElementComparator(licenseOverrideComparator)
          .doesNotContainAnyElementsOf(previousRunData.licenseOverrides);
    }
    // Assert Security Vulnerability Overrides are migrated
    List<SecurityVulnerabilityOverride> migratedVulnerabilityOverrides = new SecurityVulnerabilityOverrideDAO()
        .getByOwnerId(targetRepository.getId());
    assertThat(migratedVulnerabilityOverrides).usingElementComparator(vulnerabilityOverrideComparatorIgnoringIds)
        .containsExactlyInAnyOrderElementsOf(sourceData.vulnerabilityOverrides);
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedVulnerabilityOverrides).usingElementComparator(vulnerabilityOverrideComparator)
          .doesNotContainAnyElementsOf(previousRunData.vulnerabilityOverrides);
    }
    // Assert Security Vulnerability Overrides are migrated
    List<PolicyWaiver> migratedPolicyWaivers = new PolicyWaiverDAO().getByOwnerId(targetRepository.getId());
    assertThat(migratedPolicyWaivers).usingElementComparator(waiverComparatorIgnoringIds)
        .containsExactlyInAnyOrderElementsOf(sourceData.policyWaivers);
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedPolicyWaivers).usingElementComparator(waiverComparator)
          .doesNotContainAnyElementsOf(previousRunData.policyWaivers);
    }
  }

  /**
   * Generates 3 {@link RepositoryComponent components} in the specified {@link Repository} where the first one in the
   * returned list is the one with the latest evaluation timestamp.
   *
   * Each {@link RepositoryComponent} will get a number of {@link RepositoryPolicyViolation violations} that is equal
   * to the index under which the component is added in the returned list (first component has no violations, the third
   * has 2 violations).
   */
  private GeneratedRepositoryData generateRepositoryData(Repository repository) {
    GeneratedRepositoryData generatedRepositoryData = new GeneratedRepositoryData();
    DateTime now = DateTime.now();
    while (generatedRepositoryData.components.size() < 3) {
      RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(),
          "migrate/history/component-" + (generatedRepositoryData.components.size() + 1), null, null,
          now.minusMinutes(generatedRepositoryData.components.size()).toDate());
      for (int i = 0; i < generatedRepositoryData.components.size(); i++) {
        RepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(repository.getId(), 5,
            component.getPathname(), true, true, Action.ID_FAIL, policy.getId(), policy.getName(),
            component.getComponentIdentifier(), component.getLastEvaluationTime());
        generatedRepositoryData.violations.add(violation);
      }
      generatedRepositoryData.components.add(component);
    }

    generatedRepositoryData.violations.add(tempEntity.newRepositoryPolicyViolation(repository.getId(), 5,
        "migrated/deleted/component", false, false, Action.ID_FAIL, policy.getId(), policy.getName(),
        createMavenCoordinates("migration", "deleted", "1"), now.minusHours(1).toDate()));
    generatedRepositoryData.policyWaivers.add(tempEntity.newWaiver("hash", policy.getId(), repository.getId()));
    generatedRepositoryData.licenseOverrides.add(tempEntity.newLicenseOverride(repository.getId(),
        generatedRepositoryData.components.get(0).getComponentIdentifier(), OVERRIDDEN, "Apache-2.0"));
    generatedRepositoryData.vulnerabilityOverrides.add(
        tempEntity.newSecurityVulnerabilityOverride(repository.getId(), "hash", "source", "referenceId", CONFIRMED));
    generatedRepositoryData.vulnerabilityOverrides.add(
        tempEntity.newSecurityVulnerabilityOverride(repository.getId(), "hash2", "source2", "referenceId2", CONFIRMED));
    return generatedRepositoryData;
  }

  @Test
  public void testMigrateRepositoryHistory_AlreadyRunning() throws Exception {
    createSourceRepository();
    Repository targetRepository = createTargetRepository();
    MigrationDetails migrationDetails = new MigrationDetails();
    assertThat(migrationService.putIfAbsent(targetRepository.getId(), migrationDetails)).isTrue();

    // The migration request is ignored and the migration continues.
    migrationService.migrateRepositoryHistory(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID, SOURCE_REPOSITORY_PUBLIC_ID,
        TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testGetRepositoryMigrationState_UnknownRepository() throws Exception {
    tempEntity.newRepositoryManager(TARGET_REPOSITORY_MANAGER_INSTANCE_ID);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      migrationService.getRepositoryMigrationState(TARGET_REPOSITORY_MANAGER_INSTANCE_ID, "unknown");
    }).withMessage(getErrMsgMissingRepo(TARGET_REPOSITORY_MANAGER_INSTANCE_ID, "unknown"));
  }

  @Test
  public void testGetRepositoryMigrationState_MigrationNotStarted() throws Exception {
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
    final List<RepositoryComponent> components = new ArrayList<>();

    final List<RepositoryPolicyViolation> violations = new ArrayList<>();

    final List<LicenseOverride> licenseOverrides = new ArrayList<>();

    final List<SecurityVulnerabilityOverride> vulnerabilityOverrides = new ArrayList<>();

    final List<PolicyWaiver> policyWaivers = new ArrayList<>();
  }

  private <T extends Comparable<? super T>> Comparator<T> nullSafe() {
    return Comparator.nullsFirst(Comparator.naturalOrder());
  }

  private final Comparator<RepositoryComponent> componentComparatorIgnoringIds = Comparator //
      .comparing(RepositoryComponent::getPathname) //
      .thenComparing(RepositoryComponent::getTime) //
      .thenComparing(RepositoryComponent::getHash) //
      .thenComparing(RepositoryComponent::getMatchStateId) //
      .thenComparing(RepositoryComponent::getIdentificationSourceId) //
      .thenComparing(RepositoryComponent::getLastEvaluationTime) //
      .thenComparing(RepositoryComponent::getQuarantineTime, nullSafe()) //
      .thenComparing(RepositoryComponent::getUnquarantineTime, nullSafe()) //
      .thenComparing(RepositoryComponent::getComponentIdentifier, nullSafe());

  private final Comparator<RepositoryComponent> componentComparator = Comparator //
      .comparing(RepositoryComponent::getId) //
      .thenComparing(RepositoryComponent::getRepositoryId) //
      .thenComparing(componentComparatorIgnoringIds);

  private final Comparator<RepositoryPolicyViolation> violationComparatorIgnoringIds = Comparator //
      .comparing(RepositoryPolicyViolation::getPathname) //
      .thenComparing(RepositoryPolicyViolation::isActive) //
      .thenComparing(RepositoryPolicyViolation::getTime) //
      .thenComparing(RepositoryPolicyViolation::getPolicyId) //
      .thenComparing(RepositoryPolicyViolation::getPolicyName) //
      .thenComparing(RepositoryPolicyViolation::getThreatLevel) //
      .thenComparing(RepositoryPolicyViolation::getThreatCategory) //
      .thenComparing(RepositoryPolicyViolation::getHash, nullSafe()) //
      .thenComparing(RepositoryPolicyViolation::getConstraintFactsJson) //
      .thenComparing(RepositoryPolicyViolation::getActionTypeId, nullSafe()) //
      .thenComparing(RepositoryPolicyViolation::isWaived) //
      .thenComparing(RepositoryPolicyViolation::getComponentIdentifier, nullSafe());

  private final Comparator<RepositoryPolicyViolation> violationComparator = Comparator //
      .comparing(RepositoryPolicyViolation::getId) //
      .thenComparing(RepositoryPolicyViolation::getRepositoryId) //
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
      .thenComparing(PolicyWaiver::getComment, nullSafe());

  private final Comparator<PolicyWaiver> waiverComparator = Comparator //
      .comparing(PolicyWaiver::getId) //
      .thenComparing(PolicyWaiver::getOwnerId) //
      .thenComparing(waiverComparatorIgnoringIds);
}
