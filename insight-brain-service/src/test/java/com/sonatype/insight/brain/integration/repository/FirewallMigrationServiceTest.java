/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.repository.migration.MigrationDetails;
import com.sonatype.clm.dto.model.repository.migration.MigrationState;
import com.sonatype.insight.brain.TestProductLicenseManager;
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
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.awaitility.core.ThrowingRunnable;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO.getErrMsgMissingRepo;
import static com.sonatype.insight.brain.integration.repository.FirewallMigrationService.PROTOCOL_V1;
import static com.sonatype.insight.brain.model.license.LicenseOverrideStatus.OVERRIDDEN;
import static com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus.CONFIRMED;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FirewallMigrationServiceTest
    extends AbstractComponentTest
{
  private static final String SOURCE_REPOSITORY_MANAGER_INSTANCE_ID = "sourceRepositoryManagerInstance";

  private static final String SOURCE_REPOSITORY_PUBLIC_ID = "source-repository";

  private static final String TARGET_REPOSITORY_MANAGER_INSTANCE_ID = "repositoryManagerInstance";

  private static final String TARGET_REPOSITORY_PUBLIC_ID = "repository";

  @Inject
  private CLMLicenseManager clmLicenseManager;

  @Inject
  private TestProductLicenseManager productLicenseManager;

  @Inject
  private FirewallMigrationService migrationService;

  private Policy policy;

  @Before
  public void createPolicy() throws Exception {
    policy = tempEntity.newPolicy("FirewallMigrationServiceTest");
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
    try {
      migrationService.verifyMigrationSupport("v2");
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), endsWith("does not support migration protocol v2, please update your IQ Server."));
    }
  }

  @Test
  public void testVerifyMigrationSupport_MissingLicenseFeature() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    try {
      migrationService.verifyMigrationSupport(PROTOCOL_V1);
      fail("Expected exception");
    }
    catch (InvalidLicenseException expected) {
      assertThat(expected.getMessage(), is(InvalidLicenseException.INVALID_LICENSE_MSG));
    }
  }

  @Test
  public void testMigrateRepositoryHistory_MissingLicenseFeature() throws Exception {
    productLicenseManager.setProducts(ProductLicenseDetails.PRODUCT_RISK);
    clmLicenseManager.installLicense(null);
    try {
      migrationService.migrateRepositoryHistory(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID, SOURCE_REPOSITORY_PUBLIC_ID,
          TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);
      fail("Expected exception");
    }
    catch (InvalidLicenseException e) {
      assertThat(e.getMessage(), is(InvalidLicenseException.INVALID_LICENSE_MSG));
    }
  }

  @Test
  public void testMigrateRepositoryHistory_UnknownSource() throws Exception {
    createTargetRepository();
    try {
      migrationService.migrateRepositoryHistory(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID, SOURCE_REPOSITORY_PUBLIC_ID,
          TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);
      fail("Expected exception");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(),
          is(getErrMsgMissingRepo(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID, SOURCE_REPOSITORY_PUBLIC_ID)));
    }
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
    assertThat(migrationService.putIfAbsent(repository.getId(), migrationDetails), is(true));

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
    await().atMost(1, TimeUnit.MINUTES).untilAsserted(new ThrowingRunnable()
    {
      @Override
      public void run() {
        assertThat(migrationService
            .getRepositoryMigrationState(TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID).getState(),
            is(MigrationState.COMPLETED));
      }
    });

    // Assert source untouched
    assertThat(new RepositoryComponentDAO().getByRepositoryId(sourceRepository.getId()),
        containsComponents(sourceData.components, false));
    assertThat(new RepositoryPolicyViolationDAO().getByRepositoryId(sourceRepository.getId()),
        containsViolations(sourceData.violations, false));
    assertThat(new LicenseOverrideDAO().getByOwnerId(sourceRepository.getId()),
        containsLicenseOverrides(sourceData.licenseOverrides, false));
    assertThat(new SecurityVulnerabilityOverrideDAO().getByOwnerId(sourceRepository.getId()),
        containsVulnerabilityOverrides(sourceData.vulnerabilityOverrides, false));
    assertThat(new PolicyWaiverDAO().getByOwnerId(sourceRepository.getId()),
        containsPolicyWaivers(sourceData.policyWaivers, false));

    if (targetRepository != null) {
      targetRepository = new RepositoryDAO().getById(targetRepository.getId());
    }
    else {
      targetRepository = new RepositoryDAO().getByRepositoryManagerInstanceIdAndPublicId(
          TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);
      // Assert the target repository is created automatically
      assertThat(targetRepository, is(notNullValue()));
    }
    // Assert the target repository mirrors the source configuration
    assertThat(targetRepository.getFormat(), is(sourceRepository.getFormat()));
    assertThat(targetRepository.isEnabled(), is(sourceRepository.isEnabled()));
    assertThat(targetRepository.isQuarantineEnabled(), is(sourceRepository.isQuarantineEnabled()));

    // Assert Components are migrated
    List<RepositoryComponent> migratedComponents = new RepositoryComponentDAO()
        .getByRepositoryId(targetRepository.getId());
    assertThat(migratedComponents, containsComponents(sourceData.components, true));
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedComponents, not(containsComponents(previousRunData.components, false)));
    }
    // Assert Policy Violations are migrated
    List<RepositoryPolicyViolation> migratedViolations = new RepositoryPolicyViolationDAO()
        .getByRepositoryId(targetRepository.getId());
    assertThat(migratedViolations, containsViolations(sourceData.violations, true));
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedViolations, not(containsViolations(previousRunData.violations, false)));
    }
    // Assert License Overrides are migrated
    List<LicenseOverride> migratedLicenseOverrides = new LicenseOverrideDAO().getByOwnerId(targetRepository.getId());
    assertThat(migratedLicenseOverrides, containsLicenseOverrides(sourceData.licenseOverrides, true));
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedLicenseOverrides, not(containsLicenseOverrides(previousRunData.licenseOverrides, false)));
    }
    // Assert Security Vulnerability Overrides are migrated
    List<SecurityVulnerabilityOverride> migratedVulnerabilityOverrides = new SecurityVulnerabilityOverrideDAO()
        .getByOwnerId(targetRepository.getId());
    assertThat(migratedVulnerabilityOverrides, containsVulnerabilityOverrides(sourceData.vulnerabilityOverrides, true));
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedVulnerabilityOverrides,
          not(containsVulnerabilityOverrides(previousRunData.vulnerabilityOverrides, false)));
    }
    // Assert Security Vulnerability Overrides are migrated
    List<PolicyWaiver> migratedPolicyWaivers = new PolicyWaiverDAO().getByOwnerId(targetRepository.getId());
    assertThat(migratedPolicyWaivers, containsPolicyWaivers(sourceData.policyWaivers, true));
    if (previousRunData != null) {
      // Previous run data should be gone by now
      assertThat(migratedPolicyWaivers, not(containsPolicyWaivers(previousRunData.policyWaivers, false)));
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
    assertThat(migrationService.putIfAbsent(targetRepository.getId(), migrationDetails), is(true));

    // The migration request is ignored and the migration continues.
    migrationService.migrateRepositoryHistory(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID, SOURCE_REPOSITORY_PUBLIC_ID,
        TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testGetRepositoryMigrationState_UnknownRepository() throws Exception {
    tempEntity.newRepositoryManager(TARGET_REPOSITORY_MANAGER_INSTANCE_ID);

    try {
      migrationService.getRepositoryMigrationState(TARGET_REPOSITORY_MANAGER_INSTANCE_ID, "unknown");
      fail("Expected exception");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is(getErrMsgMissingRepo(TARGET_REPOSITORY_MANAGER_INSTANCE_ID, "unknown")));
    }
  }

  @Test
  public void testGetRepositoryMigrationState_MigrationNotStarted() throws Exception {
    createTargetRepository();

    MigrationDetails migrationDetails = migrationService
        .getRepositoryMigrationState(TARGET_REPOSITORY_MANAGER_INSTANCE_ID, TARGET_REPOSITORY_PUBLIC_ID);

    assertThat(migrationDetails.getState(), is(MigrationState.FAILED));
  }

  private Repository createTargetRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(TARGET_REPOSITORY_MANAGER_INSTANCE_ID);
    return tempEntity.newRepository(repositoryManager, TARGET_REPOSITORY_PUBLIC_ID);
  }

  private Repository createSourceRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager(SOURCE_REPOSITORY_MANAGER_INSTANCE_ID);
    return tempEntity.newRepository(repositoryManager, SOURCE_REPOSITORY_PUBLIC_ID);
  }

  private Matcher<Iterable<? extends RepositoryComponent>> containsComponents(List<RepositoryComponent> components,
                                                                              final boolean ignoreIds)
  {
    return new IsIterableContainingInAnyOrder<>(
        components.stream().map(repositoryComponent -> new RepositoryComponentMatcher(repositoryComponent, ignoreIds))
            .collect(Collectors.toList()));
  }

  private Matcher<Iterable<? extends RepositoryPolicyViolation>> containsViolations(List<RepositoryPolicyViolation> violations,
                                                                                    final boolean ignoreIds)
  {
    return new IsIterableContainingInAnyOrder<>(violations.stream()
        .map(repositoryPolicyViolation -> new RepositoryPolicyViolationMatcher(repositoryPolicyViolation, ignoreIds))
        .collect(Collectors.toList()));
  }

  private Matcher<Iterable<? extends LicenseOverride>> containsLicenseOverrides(List<LicenseOverride> licenseOverrides,
                                                                                final boolean ignoreIds)
  {
    return new IsIterableContainingInAnyOrder<>(licenseOverrides.stream()
        .map(licenseOverride -> new LicenseOverrideMatcher(licenseOverride, ignoreIds)).collect(Collectors.toList()));
  }

  private Matcher<Iterable<? extends SecurityVulnerabilityOverride>> containsVulnerabilityOverrides(List<SecurityVulnerabilityOverride> overrides,
                                                                                                    final boolean ignoreIds)
  {
    return new IsIterableContainingInAnyOrder<>(overrides.stream()
        .map(securityVulnerabilityOverride -> new SecurityVulnerabilityOverrideMatcher(securityVulnerabilityOverride,
            ignoreIds))
        .collect(Collectors.toList()));
  }

  private Matcher<Iterable<? extends PolicyWaiver>> containsPolicyWaivers(List<PolicyWaiver> policyWaivers,
                                                                          final boolean ignoreIds)
  {
    return new IsIterableContainingInAnyOrder<>(policyWaivers.stream()
        .map(policyWaiver -> new PolicyWaiverMatcher(policyWaiver, ignoreIds)).collect(Collectors.toList()));
  }

  private static class GeneratedRepositoryData
  {
    final List<RepositoryComponent> components = new ArrayList<>();

    final List<RepositoryPolicyViolation> violations = new ArrayList<>();

    final List<LicenseOverride> licenseOverrides = new ArrayList<>();

    final List<SecurityVulnerabilityOverride> vulnerabilityOverrides = new ArrayList<>();

    final List<PolicyWaiver> policyWaivers = new ArrayList<>();
  }

  private class RepositoryComponentMatcher
      extends TypeSafeDiagnosingMatcher<RepositoryComponent>
  {
    private final RepositoryComponent component;

    private final boolean ignoreIds;

    RepositoryComponentMatcher(final RepositoryComponent component, final boolean ignoreIds) {
      this.component = component;
      this.ignoreIds = ignoreIds;
    }

    @Override
    protected boolean matchesSafely(final RepositoryComponent item, final Description mismatchDescription) {
      if (!ignoreIds && !Objects.equals(component.getId(), item.getId())) {
        mismatchDescription.appendText("has id ").appendValue(item.getId());
        return false;
      }
      else if (!ignoreIds && !Objects.equals(component.getRepositoryId(), item.getRepositoryId())) {
        mismatchDescription.appendText("has repositoryId ").appendValue(item.getRepositoryId());
        return false;
      }
      else if (!Objects.equals(component.getPathname(), item.getPathname())) {
        mismatchDescription.appendText("has pathname ").appendValue(item.getPathname());
        return false;
      }
      else if (!Objects.equals(component.getTime(), item.getTime())) {
        mismatchDescription.appendText("has time ").appendValue(item.getTime());
        return false;
      }
      else if (!Objects.equals(component.getHash(), item.getHash())) {
        mismatchDescription.appendText("has hash ").appendValue(item.getHash());
        return false;
      }
      else if (!Objects.equals(component.getMatchStateId(), item.getMatchStateId())) {
        mismatchDescription.appendText("has matchStateId ").appendValue(item.getMatchStateId());
        return false;
      }
      else if (!Objects.equals(component.getIdentificationSourceId(), item.getIdentificationSourceId())) {
        mismatchDescription.appendText("has identificationSourceId ").appendValue(item.getIdentificationSourceId());
        return false;
      }
      else if (!Objects.equals(component.getLastEvaluationTime(), item.getLastEvaluationTime())) {
        mismatchDescription.appendText("has lastEvaluationTime ").appendValue(item.getLastEvaluationTime());
        return false;
      }
      else if (!Objects.equals(component.getQuarantineTime(), item.getQuarantineTime())) {
        mismatchDescription.appendText("has quarantineTime ").appendValue(item.getQuarantineTime());
        return false;
      }
      else if (!Objects.equals(component.getUnquarantineTime(), item.getUnquarantineTime())) {
        mismatchDescription.appendText("has unquarantineTime ").appendValue(item.getUnquarantineTime());
        return false;
      }
      else if (!Objects.equals(component.getComponentIdentifier(), item.getComponentIdentifier())) {
        mismatchDescription.appendText("has componentIdentifier ").appendValue(item.getComponentIdentifier());
        return false;
      }
      return true;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("matching repository component ").appendValue(component);
      if (ignoreIds) {
        description.appendText(" ignoring id and repositoryId fields");
      }
    }
  }

  private class RepositoryPolicyViolationMatcher
      extends TypeSafeDiagnosingMatcher<RepositoryPolicyViolation>
  {
    private final RepositoryPolicyViolation violation;

    private final boolean ignoreIds;

    RepositoryPolicyViolationMatcher(final RepositoryPolicyViolation violation, final boolean ignoreIds) {
      this.violation = violation;
      this.ignoreIds = ignoreIds;
    }

    @Override
    protected boolean matchesSafely(final RepositoryPolicyViolation item, final Description mismatchDescription) {
      if (!ignoreIds && !Objects.equals(violation.getId(), item.getId())) {
        mismatchDescription.appendText("has id ").appendValue(item.getId());
        return false;
      }
      else if (!ignoreIds && !Objects.equals(violation.getRepositoryId(), item.getRepositoryId())) {
        mismatchDescription.appendText("has repositoryId ").appendValue(item.getRepositoryId());
        return false;
      }
      else if (!Objects.equals(violation.getPathname(), item.getPathname())) {
        mismatchDescription.appendText("has pathname ").appendValue(item.getPathname());
        return false;
      }
      else if (violation.isActive() != item.isActive()) {
        mismatchDescription.appendText("has active ").appendValue(item.isActive());
        return false;
      }
      else if (!Objects.equals(violation.getTime(), item.getTime())) {
        mismatchDescription.appendText("has time ").appendValue(item.getTime());
        return false;
      }
      else if (!Objects.equals(violation.getPolicyId(), item.getPolicyId())) {
        mismatchDescription.appendText("has policyId ").appendValue(item.getPolicyId());
        return false;
      }
      else if (!Objects.equals(violation.getPolicyName(), item.getPolicyName())) {
        mismatchDescription.appendText("has policyName ").appendValue(item.getPolicyName());
        return false;
      }
      else if (violation.getThreatLevel() != item.getThreatLevel()) {
        mismatchDescription.appendText("has threadLevel ").appendValue(item.getThreatLevel());
        return false;
      }
      else if (!Objects.equals(violation.getThreatCategory(), item.getThreatCategory())) {
        mismatchDescription.appendText("has threadCategory ").appendValue(item.getThreatCategory());
        return false;
      }
      else if (!Objects.equals(violation.getHash(), item.getHash())) {
        mismatchDescription.appendText("has hash ").appendValue(item.getHash());
        return false;
      }
      else if (!Objects.equals(violation.getConstraintFactsJson(), item.getConstraintFactsJson())) {
        mismatchDescription.appendText("has constraintFactsJson ").appendValue(item.getConstraintFactsJson());
        return false;
      }
      else if (!Objects.equals(violation.getActionTypeId(), item.getActionTypeId())) {
        mismatchDescription.appendText("has actionTypeId ").appendValue(item.getActionTypeId());
        return false;
      }
      else if (violation.isWaived() != item.isWaived()) {
        mismatchDescription.appendText("has waived ").appendValue(item.isWaived());
        return false;
      }
      else if (!Objects.equals(violation.getComponentIdentifier(), item.getComponentIdentifier())) {
        mismatchDescription.appendText("has componentIdentifier ").appendValue(item.getComponentIdentifier());
        return false;
      }
      return true;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("matching repository policy violation ").appendValue(violation);
      if (ignoreIds) {
        description.appendText(" ignoring id and repositoryId fields");
      }
    }
  }

  private class LicenseOverrideMatcher
      extends TypeSafeDiagnosingMatcher<LicenseOverride>
  {
    private final LicenseOverride override;

    private final Matcher<Iterable<? extends String>> licenseMatcher;

    private final boolean ignoreIds;

    private LicenseOverrideMatcher(final LicenseOverride override, final boolean ignoreIds) {
      this.override = override;
      licenseMatcher = containsInAnyOrder(
          this.override.getLicenseIds().toArray(new String[this.override.getLicenseIds().size()]));
      this.ignoreIds = ignoreIds;
    }

    @Override
    protected boolean matchesSafely(final LicenseOverride item, final Description mismatchDescription) {
      if (!ignoreIds && !Objects.equals(override.getId(), item.getId())) {
        mismatchDescription.appendText("has id ").appendValue(item.getId());
        return false;
      }
      else if (!ignoreIds && !Objects.equals(override.getOwnerId(), item.getOwnerId())) {
        mismatchDescription.appendText("has ownerId ").appendValue(item.getOwnerId());
        return false;
      }
      else if (!Objects.equals(override.getComponentIdentifier(), item.getComponentIdentifier())) {
        mismatchDescription.appendText("has componentIdentifier ").appendValue(item.getComponentIdentifier());
        return false;
      }
      else if (!Objects.equals(override.getStatus(), item.getStatus())) {
        mismatchDescription.appendText("has status ").appendValue(item.getStatus());
        return false;
      }
      else if (!Objects.equals(override.getComment(), item.getComment())) {
        mismatchDescription.appendText("has comment ").appendValue(item.getComment());
        return false;
      }
      else if (!licenseMatcher.matches(item.getLicenseIds())) {
        licenseMatcher.describeMismatch(item.getLicenseIds(), mismatchDescription);
        return false;
      }
      return true;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("matching license override ").appendValue(override);
      if (ignoreIds) {
        description.appendText(" ignoring id and ownerId");
      }
    }
  }

  private class SecurityVulnerabilityOverrideMatcher
      extends TypeSafeDiagnosingMatcher<SecurityVulnerabilityOverride>
  {
    private final SecurityVulnerabilityOverride override;

    private final boolean ignoreIds;

    private SecurityVulnerabilityOverrideMatcher(final SecurityVulnerabilityOverride override,
                                                 final boolean ignoreIds)
    {
      this.override = override;
      this.ignoreIds = ignoreIds;
    }

    @Override
    protected boolean matchesSafely(final SecurityVulnerabilityOverride item, final Description mismatchDescription) {
      if (!ignoreIds && !Objects.equals(override.getId(), item.getId())) {
        mismatchDescription.appendText("has id ").appendValue(item.getId());
        return false;
      }
      else if (!ignoreIds && !Objects.equals(override.getOwnerId(), item.getOwnerId())) {
        mismatchDescription.appendText("has ownerId ").appendValue(item.getOwnerId());
        return false;
      }
      else if (!Objects.equals(override.getHash(), item.getHash())) {
        mismatchDescription.appendText("has hash ").appendValue(item.getHash());
        return false;
      }
      else if (!Objects.equals(override.getSource(), item.getSource())) {
        mismatchDescription.appendText("has source ").appendValue(item.getSource());
        return false;
      }
      else if (!Objects.equals(override.getReferenceId(), item.getReferenceId())) {
        mismatchDescription.appendText("has referenceId ").appendValue(item.getReferenceId());
        return false;
      }
      else if (!Objects.equals(override.getStatus(), item.getStatus())) {
        mismatchDescription.appendText("has status ").appendValue(item.getStatus());
        return false;
      }
      else if (!Objects.equals(override.getComment(), item.getComment())) {
        mismatchDescription.appendText("has comment ").appendValue(item.getComment());
        return false;
      }
      return true;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("matching security vulnerability override ").appendValue(override);
      if (ignoreIds) {
        description.appendText(" ignoring id and ownerId");
      }
    }
  }

  private class PolicyWaiverMatcher
      extends TypeSafeDiagnosingMatcher<PolicyWaiver>
  {
    private final PolicyWaiver policyWaiver;

    private final boolean ignoreIds;

    private PolicyWaiverMatcher(PolicyWaiver policyWaiver, boolean ignoreIds) {
      this.policyWaiver = policyWaiver;
      this.ignoreIds = ignoreIds;
    }

    @Override
    protected boolean matchesSafely(final PolicyWaiver item, final Description mismatchDescription) {
      if (!ignoreIds && !Objects.equals(policyWaiver.getId(), item.getId())) {
        mismatchDescription.appendText("has id ").appendValue(item.getId());
        return false;
      }
      else if (!ignoreIds && !Objects.equals(policyWaiver.getOwnerId(), item.getOwnerId())) {
        mismatchDescription.appendText("has ownerId ").appendValue(item.getOwnerId());
        return false;
      }
      else if (!Objects.equals(policyWaiver.getPolicyId(), item.getPolicyId())) {
        mismatchDescription.appendText("has policyId ").appendValue(item.getPolicyId());
        return false;
      }
      else if (!Objects.equals(policyWaiver.getHash(), item.getHash())) {
        mismatchDescription.appendText("has hash ").appendValue(item.getHash());
        return false;
      }
      else if (!Objects.equals(policyWaiver.getCreateTime(), item.getCreateTime())) {
        mismatchDescription.appendText("has createTime ").appendValue(item.getCreateTime());
        return false;
      }
      else if (!Objects.equals(policyWaiver.getComment(), item.getComment())) {
        mismatchDescription.appendText("has comment ").appendValue(item.getComment());
        return false;
      }
      return true;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("matching policy waiver ").appendValue(policyWaiver);
      if (ignoreIds) {
        description.appendText(" ignoring id and ownerId");
      }
    }
  }
}
