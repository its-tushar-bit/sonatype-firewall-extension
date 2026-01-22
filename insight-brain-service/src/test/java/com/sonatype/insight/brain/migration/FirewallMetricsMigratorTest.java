/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCategory;
import com.sonatype.insight.brain.model.configuration.ProductLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.policy.facts.TriggerSecurityVulnerabilityWithCategory;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.jaxrs.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;
import com.sonatype.insight.test.LogOutput;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.COMPONENTS_AUTO_RELEASED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.COMPONENTS_QUARANTINED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.WAIVED_COMPONENTS;
import static com.sonatype.insight.brain.utils.DateConverter.toDate;
import static com.sonatype.insight.brain.utils.DateConverter.toLocalDate;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

public class FirewallMetricsMigratorTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(FirewallMetricsMigrator.class);

  @Inject
  private FirewallMetricsMigrator firewallMetricsMigrator;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private FirewallMetricsDAO firewallMetricsDAO;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private ProductLicenseDAO productLicenseDaoMock;

  private Repository hostedRepository;

  private Repository repository1;

  private Repository repository2;

  @Override
  public void configure(Binder binder) {
    binder.bind(ProductLicenseDAO.class).toInstance(productLicenseDaoMock);
    super.configure(binder);
  }

  @Before
  public void before() {
    migrationTrackerDAO.deleteById(FirewallMetricsMigrator.MIGRATION_ID);

    lenient().when(productLicenseDaoMock.get()).thenAnswer(invocation -> {
      SignedProductLicenseDetailsDTO licenseDetailsDTO = new SignedProductLicenseDetailsDTO();
      licenseDetailsDTO.features = testProductLicense.getFeatures().stream()
          .map(Enum::name)
          .collect(toCollection(TreeSet::new));
      // Value that is not present in the enum LicensedFeature but can be found in the installed license file
      licenseDetailsDTO.features.add("ADVANCED_DEVELOPMENT_PACK");
      ProductLicense productLicense = new ProductLicense();
      productLicense.setLicenseDetails(JsonUtils.toJson(licenseDetailsDTO));
      return productLicense;
    });
  }

  @Test
  public void testMigrate_AlreadyMigrated() {
    migrationTrackerDAO.insertTracker(FirewallMetricsMigrator.MIGRATION_ID);
    firewallMetricsMigrator.migrate();

    assertThat(logOutput).contains("Initial Firewall Metrics already calculated.");
    assertThat(firewallMetricsDAO.getAll()).isEmpty();
  }

  @Test
  public void testMigrate_InvalidLicense_MissingFirewallAutoUnquarantine() {
    doTestMigrate_InvalidLicense(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);
  }

  @Test
  public void testMigrate_InvalidLicense_MissingReleaseIntegrity() {
    doTestMigrate_InvalidLicense(LicensedFeature.RELEASE_INTEGRITY);
  }

  private void doTestMigrate_InvalidLicense(LicensedFeature licensedFeature) {
    testProductLicense.setMissingFeatures(licensedFeature);
    firewallMetricsMigrator.migrate();

    assertThat(logOutput).contains("Invalid license to calculate Firewall Metrics.");
    assertThat(migrationTrackerDAO.getById(FirewallMetricsMigrator.MIGRATION_ID)).isNull();
    assertThat(firewallMetricsDAO.getAll()).isEmpty();
  }

  @Test
  public void testMigrate_NothingToMigrate_NoRepositories() {
    firewallMetricsMigrator.migrate();

    assertThat(logOutput)
        .contains("Calculating Firewall Metrics from 0 repositories.")
        .contains("Firewall Metrics calculated for 0 repositories");
    assertThat(firewallMetricsDAO.getAll()).isEmpty();
    assertThat(migrationTrackerDAO.getById(FirewallMetricsMigrator.MIGRATION_ID)).isNotNull();
  }

  @Test
  public void testMigrate_NothingToMigrate_NoDataForRepositories() {
    createRepositories();
    firewallMetricsMigrator.migrate();

    assertThat(logOutput)
        .contains("Calculating Firewall Metrics from 3 repositories.")
        .contains("Firewall Metrics calculated for 3 repositories")
        .doesNotContain("Saving");
    assertThat(firewallMetricsDAO.getAll()).isEmpty();
    assertThat(migrationTrackerDAO.getById(FirewallMetricsMigrator.MIGRATION_ID)).isNotNull();
  }

  @Test
  public void testMigrate_NamespaceAttacksBlockedAndSupplyChainAttacksBlockedMetrics_WithoutPreviousMetrics() {
    doTestMigrate_NamespaceAttacksBlockedAndSupplyChainAttacksBlockedMetrics(null, null);
  }

  @Test
  public void testMigrate_NamespaceAttacksBlockedAndSupplyChainAttacksBlockedMetrics_WithPreviousMetrics() {
    LocalDate date1 = LocalDate.now();
    LocalDate date2 = date1.minusDays(1);

    FirewallMetrics metric1 = new FirewallMetrics(date1, NAMESPACE_ATTACKS_BLOCKED, 2);
    firewallMetricsDAO.insert(metric1);
    FirewallMetrics metric2 = new FirewallMetrics(date2, SUPPLY_CHAIN_ATTACKS_BLOCKED, 5);
    firewallMetricsDAO.insert(metric2);

    doTestMigrate_NamespaceAttacksBlockedAndSupplyChainAttacksBlockedMetrics(metric1, metric2);
  }

  private void doTestMigrate_NamespaceAttacksBlockedAndSupplyChainAttacksBlockedMetrics(
      FirewallMetrics previousNamespaceAttacksBlockedMetrics,
      FirewallMetrics previousSupplyChainAttacksBlockedMetrics)
  {
    firewallMetricsMigrator.setRepositoryPolicyViolationsBatchSize(5);
    createRepositories();

    Component component = new Component(ComponentIdentifier.createNpmCoordinates("p", "v"));
    component.setConflictingProprietaryName(new ProprietaryComponentName("testPattern", hostedRepository.getId()));

    LocalDate date1 = LocalDate.now().minusDays(5);
    LocalDate date2 = date1.minusMonths(3);
    LocalDate date3 = date2.minusDays(1);

    int expectedNamespaceAttacksBlockedMetricsAtDate1 = 10;
    int expectedNamespaceAttacksBlockedMetricsAtDate2 = 50;
    int expectedSupplyChainAttacksBlockedMetricsAtDate1 = 4;
    int expectedSupplyChainAttacksBlockedMetricsAtDate2 = 15;

    createProprietaryNameConflictRepositoryPolicyViolations(component, date1, date2,
        expectedNamespaceAttacksBlockedMetricsAtDate1, expectedNamespaceAttacksBlockedMetricsAtDate2);

    createMaliciousCodeRepositoryPolicyViolations(component, date1, date2,
        expectedSupplyChainAttacksBlockedMetricsAtDate1, expectedSupplyChainAttacksBlockedMetricsAtDate2);

    createProprietaryNameConflictAndMaliciousCodeRepositoryPolicyViolation(component, date3);

    firewallMetricsMigrator.migrate();

    assertThat(logOutput)
        .contains("Calculating Firewall Metrics from 3 repositories.")
        .contains("Firewall Metrics calculated for 3 repositories");

    List<FirewallMetrics> metrics = firewallMetricsDAO.getAll();

    int expectedMetricsCount = 6;
    int namespaceAttacksBlockedMetricsCount = 3;
    int supplyChainAttacksBlockedMetricsCount = 3;
    if (previousNamespaceAttacksBlockedMetrics != null) {
      expectedMetricsCount++;
      namespaceAttacksBlockedMetricsCount++;
    }
    if (previousSupplyChainAttacksBlockedMetrics != null) {
      expectedMetricsCount++;
      supplyChainAttacksBlockedMetricsCount++;
    }
    assertThat(metrics).hasSize(expectedMetricsCount);

    List<FirewallMetrics> namespaceAttacksBlockedMetrics = metrics.stream()
        .filter(metric -> metric.getMetricsName() == NAMESPACE_ATTACKS_BLOCKED)
        .sorted(Comparator.comparing(FirewallMetrics::getMetricsDate))
        .collect(toList());

    assertThat(namespaceAttacksBlockedMetrics).hasSize(namespaceAttacksBlockedMetricsCount);

    List<LocalDate> dates = Lists.newArrayList(date3, date2, date1);
    if (previousNamespaceAttacksBlockedMetrics != null) {
      dates.add(previousNamespaceAttacksBlockedMetrics.getMetricsDate());
    }
    assertThat(namespaceAttacksBlockedMetrics)
        .extracting(FirewallMetrics::getMetricsDate)
        .containsExactlyElementsOf(dates);

    List<Integer> metricValues = Lists.newArrayList(1, expectedNamespaceAttacksBlockedMetricsAtDate2,
        expectedNamespaceAttacksBlockedMetricsAtDate1);
    if (previousNamespaceAttacksBlockedMetrics != null) {
      metricValues.add(previousNamespaceAttacksBlockedMetrics.getMetricsValue());
    }
    assertThat(namespaceAttacksBlockedMetrics)
        .extracting(FirewallMetrics::getMetricsValue)
        .containsExactlyElementsOf(metricValues);

    List<FirewallMetrics> supplyChainAttacksBlockedMetrics = metrics.stream()
        .filter(metric -> metric.getMetricsName() == SUPPLY_CHAIN_ATTACKS_BLOCKED)
        .sorted(Comparator.comparing(FirewallMetrics::getMetricsDate))
        .collect(toList());

    assertThat(supplyChainAttacksBlockedMetrics).hasSize(supplyChainAttacksBlockedMetricsCount);

    dates = Lists.newArrayList(date3, date2, date1);
    if (previousSupplyChainAttacksBlockedMetrics != null) {
      dates.add(previousSupplyChainAttacksBlockedMetrics.getMetricsDate());
    }
    assertThat(supplyChainAttacksBlockedMetrics)
        .extracting(FirewallMetrics::getMetricsDate)
        .containsExactlyElementsOf(dates);

    metricValues = Lists.newArrayList(1, expectedSupplyChainAttacksBlockedMetricsAtDate2,
        expectedSupplyChainAttacksBlockedMetricsAtDate1);
    if (previousSupplyChainAttacksBlockedMetrics != null) {
      metricValues.add(previousSupplyChainAttacksBlockedMetrics.getMetricsValue());
    }
    assertThat(supplyChainAttacksBlockedMetrics)
        .extracting(FirewallMetrics::getMetricsValue)
        .containsExactlyElementsOf(metricValues);

    assertThat(migrationTrackerDAO.getById(FirewallMetricsMigrator.MIGRATION_ID)).isNotNull();
  }

  @Test
  public void testMigrate_ComponentsQuarantinedMetrics_WithoutPreviousMetrics() {
    createRepositories();

    Date now = new Date();
    Date oneYearAgo = DateUtils.addHours(DateUtils.addYears(now, -1), 1);
    Date moreThanOneYearAgo = DateUtils.addDays(oneYearAgo, -1);

    tempEntity.newRepositoryComponent(repository1.getId(), MatchState.EXACT, "now-quarantined-path", "hash1",
        ComponentIdentifier.createNpmCoordinates("p1", "v1"), now, now);
    tempEntity.newRepositoryComponent(repository1.getId(), MatchState.EXACT, "not-quarantined-path", "hash2",
        ComponentIdentifier.createNpmCoordinates("p2", "v2"), now, null /* quarantine time */);
    tempEntity.newRepositoryComponent(repository2.getId(), MatchState.EXACT, "1-year-ago-quarantined-path-1", "hash3",
        ComponentIdentifier.createNpmCoordinates("p3", "v3"), oneYearAgo, oneYearAgo);
    tempEntity.newRepositoryComponent(repository1.getId(), MatchState.EXACT, "1-year-ago-quarantined-path-2", "hash4",
        ComponentIdentifier.createNpmCoordinates("p4", "v4"), oneYearAgo, oneYearAgo);
    tempEntity.newRepositoryComponent(repository2.getId(), MatchState.EXACT, "+1-year-ago-quarantined-path", "hash5",
        ComponentIdentifier.createNpmCoordinates("p5", "v5"), moreThanOneYearAgo, moreThanOneYearAgo);

    firewallMetricsMigrator.migrate();

    assertThat(logOutput)
        .contains("Calculating Firewall Metrics from 3 repositories.")
        .contains("Firewall Metrics calculated for 3 repositories");

    List<FirewallMetrics> metrics = new ArrayList<>(firewallMetricsDAO.getAll());

    assertThat(metrics).hasSize(2);
    metrics.sort(Comparator.comparing(FirewallMetrics::getMetricsDate));

    assertThat(metrics)
        .extracting(FirewallMetrics::getMetricsName)
        .containsOnly(COMPONENTS_QUARANTINED);

    assertThat(metrics)
        .extracting(FirewallMetrics::getMetricsDate)
        .containsExactly(toLocalDate(oneYearAgo), toLocalDate(now));

    assertThat(metrics)
        .extracting(FirewallMetrics::getMetricsValue)
        .containsExactly(2, 1);

    assertThat(migrationTrackerDAO.getById(FirewallMetricsMigrator.MIGRATION_ID)).isNotNull();
  }

  @Test
  public void testMigrate_ComponentsQuarantinedMetrics_WithPreviousMetrics() {
    doTestMigrate_WithPreviousMetrics(
        COMPONENTS_QUARANTINED,
        "Components quarantined Firewall Metrics already calculated",
        date -> tempEntity.newRepositoryComponent(repository1.getId(), MatchState.EXACT, "quarantined-path", "hash",
            ComponentIdentifier.createNpmCoordinates("p", "v"), date, date));
  }

  @Test
  public void testMigrate_ComponentsAutoReleasedMetrics_WithoutPreviousMetrics() {
    createRepositories();

    Date now = new Date();
    Date oneYearAgo = DateUtils.addHours(DateUtils.addYears(now, -1), 1);
    Date moreThanOneYearAgo = DateUtils.addDays(oneYearAgo, -1);

    tempEntity.newRepositoryComponent(repository1.getId(), "now-auto-released-path-1", now, now, true);
    tempEntity.newRepositoryComponent(repository2.getId(), "now-auto-released-path-2", now, now, true);
    tempEntity.newRepositoryComponent(repository1.getId(), "not-auto-released-path", now, now, false);
    tempEntity.newRepositoryComponent(repository2.getId(), "1-year-ago-auto-released-path-2", oneYearAgo, oneYearAgo,
        true);
    tempEntity.newRepositoryComponent(repository1.getId(), "+1-year-ago-auto-released-path-2", moreThanOneYearAgo,
        moreThanOneYearAgo, true);

    firewallMetricsMigrator.migrate();

    assertThat(logOutput)
        .contains("Calculating Firewall Metrics from 3 repositories.")
        .contains("Firewall Metrics calculated for 3 repositories");

    List<FirewallMetrics> metrics = firewallMetricsDAO.getAll().stream()
        .filter(metric -> metric.getMetricsName() == COMPONENTS_AUTO_RELEASED)
        .collect(toList());

    assertThat(metrics).hasSize(2);
    metrics.sort(Comparator.comparing(FirewallMetrics::getMetricsDate));

    assertThat(metrics)
        .extracting(FirewallMetrics::getMetricsDate)
        .containsExactly(toLocalDate(oneYearAgo), toLocalDate(now));

    assertThat(metrics)
        .extracting(FirewallMetrics::getMetricsValue)
        .containsExactly(1, 2);

    assertThat(migrationTrackerDAO.getById(FirewallMetricsMigrator.MIGRATION_ID)).isNotNull();
  }

  @Test
  public void testMigrate_ComponentsAutoReleasedMetrics_WithPreviousMetrics() {
    doTestMigrate_WithPreviousMetrics(
        COMPONENTS_AUTO_RELEASED,
        "Components auto-released Firewall Metrics already calculated",
        date -> tempEntity.newRepositoryComponent(repository1.getId(), "auto-released-path", date, date, true));
  }

  @Test
  public void testMigrate_WaivedComponentsMetrics_WithoutPreviousMetrics() {
    createRepositories();

    Date now = new Date();
    Date oneYearAgo = DateUtils.addHours(DateUtils.addYears(now, -1), 1);
    Date moreThanOneYearAgo = DateUtils.addDays(oneYearAgo, -1);

    Policy policy1 = tempEntity.newPolicy(repository1);
    Policy policy2 = tempEntity.newPolicy(repository2);

    tempEntity.newWaiver("hash1", policy1.getId(), repository1.getId(), emptyList(), "now-waived-1", now);
    tempEntity.newWaiver("hash2", policy2.getId(), repository2.getId(), emptyList(), "now-waived-2", now);
    tempEntity.newWaiver("hash3", policy2.getId(), repository2.getId(), emptyList(), "1-year-ago-waived", oneYearAgo);
    tempEntity.newWaiver("hash4", policy1.getId(), repository1.getId(), emptyList(), "+1-year-ago-waived",
        moreThanOneYearAgo);

    firewallMetricsMigrator.migrate();

    assertThat(logOutput)
        .contains("Calculating Firewall Metrics from 3 repositories.")
        .contains("Firewall Metrics calculated for 3 repositories");

    List<FirewallMetrics> metrics = new ArrayList<>(firewallMetricsDAO.getAll());

    assertThat(metrics).hasSize(2);
    metrics.sort(Comparator.comparing(FirewallMetrics::getMetricsDate));

    assertThat(metrics)
        .extracting(FirewallMetrics::getMetricsName)
        .containsOnly(WAIVED_COMPONENTS);

    assertThat(metrics)
        .extracting(FirewallMetrics::getMetricsDate)
        .containsExactly(toLocalDate(oneYearAgo), toLocalDate(now));

    assertThat(metrics)
        .extracting(FirewallMetrics::getMetricsValue)
        .containsExactly(1, 2);

    assertThat(migrationTrackerDAO.getById(FirewallMetricsMigrator.MIGRATION_ID)).isNotNull();
  }

  @Test
  public void testMigrate_WaivedComponentsMetrics_WithPreviousMetrics() {
    doTestMigrate_WithPreviousMetrics(
        WAIVED_COMPONENTS,
        "Waived components Firewall Metrics already calculated",
        date -> {
          Policy policy = tempEntity.newPolicy(repository1);
          tempEntity.newWaiver("hash1", policy.getId(), repository1.getId(), emptyList(), "waived", date);
        });
  }

  private void createProprietaryNameConflictRepositoryPolicyViolations(
      Component component,
      LocalDate localDate1,
      LocalDate localDate2,
      int expectedNamespaceAttacksBlockedMetricsAtDate1,
      int expectedNamespaceAttacksBlockedMetricsAtDate2)
  {
    Date date1 = toDate(localDate1);
    Date date2 = toDate(localDate2);
    MatchFact matchFactProprietaryNameConflict =
        new MatchFact(component, "policyProprietaryNameConflict", null, 0, emptyList());

    Condition conditionProprietaryNameConflict =
        new Condition(ProprietaryNameConflictConditionType.ID, ProprietaryNameConflictConditionType.OP_IS_PRESENT);

    ConditionFact conditionFactProprietaryNameConflict = ComponentPolicyEvaluator
        .createConditionFact(conditionProprietaryNameConflict, matchFactProprietaryNameConflict);

    ConstraintFact constraintFactProprietaryNameConflict =
        new ConstraintFact("proprietaryNameConflict", "proprietaryNameConflict", LogicalOperator.OR.name());
    constraintFactProprietaryNameConflict.addConditionFact(conditionFactProprietaryNameConflict);

    List<ConstraintFact> constraintFactsProprietaryNameConflict = singletonList(constraintFactProprietaryNameConflict);

    for (int i = 0; i < expectedNamespaceAttacksBlockedMetricsAtDate1; i++) {
      tempEntity.newRepositoryPolicyViolation(repository1.getId(), 10, "path", "hash",
          constraintFactsProprietaryNameConflict, false, FailActionType.ID,
          matchFactProprietaryNameConflict.getPolicyId(), matchFactProprietaryNameConflict.getPolicyId(),
          component.getComponentIdentifier(), DateUtils.addMinutes(date1, i * 10), null, null, null);
    }

    for (int i = 0; i < expectedNamespaceAttacksBlockedMetricsAtDate2; i++) {
      tempEntity.newRepositoryPolicyViolation(repository2.getId(), 10, "path", "hash",
          constraintFactsProprietaryNameConflict, false, FailActionType.ID,
          matchFactProprietaryNameConflict.getPolicyId(), matchFactProprietaryNameConflict.getPolicyId(),
          component.getComponentIdentifier(), DateUtils.addMinutes(date2, i * 10), null, null, null);
    }

    // Should not be counted in the metrics
    MatchFact matchFactProprietaryNameConflictNotPresent =
        new MatchFact(component, "ProprietaryNameConflictNotPresent", null, 0, emptyList());

    Condition conditionProprietaryNameConflictNotPresent =
        new Condition(ProprietaryNameConflictConditionType.ID, ProprietaryNameConflictConditionType.OP_IS_NOT_PRESENT);

    ConditionFact conditionFactProprietaryNameConflictNotPresent = ComponentPolicyEvaluator
        .createConditionFact(conditionProprietaryNameConflictNotPresent, matchFactProprietaryNameConflictNotPresent);

    ConstraintFact constraintFactProprietaryNameConflictNotPresent = new ConstraintFact(
        "proprietaryNameConflictNotPresent", "proprietaryNameConflictNotPresent", LogicalOperator.OR.name());
    constraintFactProprietaryNameConflictNotPresent.addConditionFact(conditionFactProprietaryNameConflictNotPresent);

    List<ConstraintFact> constraintFactsProprietaryNameConflictNotPresent =
        singletonList(constraintFactProprietaryNameConflictNotPresent);

    tempEntity.newRepositoryPolicyViolation(repository1.getId(), 10, "path", "hash",
        constraintFactsProprietaryNameConflictNotPresent, false, FailActionType.ID,
        matchFactProprietaryNameConflictNotPresent.getPolicyId(),
        matchFactProprietaryNameConflictNotPresent.getPolicyId(), component.getComponentIdentifier(), date1, null, null,
        null);
  }

  private void createMaliciousCodeRepositoryPolicyViolations(
      Component component,
      LocalDate localDate1,
      LocalDate localDate2,
      int expectedSupplyChainAttacksBlockedMetricsAtDate1,
      int expectedSupplyChainAttacksBlockedMetricsAtDate2)
  {
    Date date1 = toDate(localDate1);
    Date date2 = toDate(localDate2);
    Condition conditionMaliciousCode = new Condition(SecurityVulnerabilityCategoryConditionType.ID,
        ConditionTypes.SecurityVulnerabilityCategoryConditionType.getSupportedOperators().get(0),
        SecurityVulnerabilityCategory.MALICIOUS_CODE.getId());

    TriggerSecurityVulnerabilityWithCategory triggerSecurityVulnerabilityWithCategory =
        new TriggerSecurityVulnerabilityWithCategory();
    ConditionTrigger conditionTrigger = new ConditionTrigger(0, triggerSecurityVulnerabilityWithCategory);
    MatchFact matchFactMaliciousCode =
        new MatchFact(component, "policyMaliciousCode", null, 0, singletonList(conditionTrigger));

    ConditionFact conditionFactMaliciousCode =
        ComponentPolicyEvaluator.createConditionFact(conditionMaliciousCode, matchFactMaliciousCode);

    ConstraintFact constraintFactMaliciousCode =
        new ConstraintFact("constraintMaliciousCode", "constraintMaliciousCode", LogicalOperator.AND.name());
    constraintFactMaliciousCode.addConditionFact(conditionFactMaliciousCode);

    List<ConstraintFact> constraintFactsMaliciousCode = singletonList(constraintFactMaliciousCode);

    for (int i = 0; i < expectedSupplyChainAttacksBlockedMetricsAtDate1; i++) {
      tempEntity.newRepositoryPolicyViolation(repository1.getId(), 10, "path", "hash", constraintFactsMaliciousCode,
          false, FailActionType.ID, matchFactMaliciousCode.getPolicyId(), matchFactMaliciousCode.getPolicyId(),
          component.getComponentIdentifier(), DateUtils.addMinutes(date1, i * 10), null, null, null);
    }

    for (int i = 0; i < expectedSupplyChainAttacksBlockedMetricsAtDate2; i++) {
      tempEntity.newRepositoryPolicyViolation(repository2.getId(), 10, "path", "hash", constraintFactsMaliciousCode,
          false, FailActionType.ID, matchFactMaliciousCode.getPolicyId(), matchFactMaliciousCode.getPolicyId(),
          component.getComponentIdentifier(), DateUtils.addMinutes(date2, i * 10), null, null, null);
    }

    // Should not be counted in the metrics
    Condition conditionIsNotMaliciousCode = new Condition(SecurityVulnerabilityCategoryConditionType.ID,
        ConditionTypes.SecurityVulnerabilityCategoryConditionType.getSupportedOperators().get(1),
        SecurityVulnerabilityCategory.MALICIOUS_CODE.getId());

    TriggerSecurityVulnerabilityWithCategory triggerIsNotMaliciousCode = new TriggerSecurityVulnerabilityWithCategory();
    ConditionTrigger conditionTriggerIsNotMaliciousCode = new ConditionTrigger(0, triggerIsNotMaliciousCode);
    MatchFact matchFactIsNotMaliciousCode = new MatchFact(component, "policyIsNotMaliciousCode", null, 0,
        singletonList(conditionTriggerIsNotMaliciousCode));

    ConditionFact conditionFactIsNotMaliciousCode =
        ComponentPolicyEvaluator.createConditionFact(conditionIsNotMaliciousCode, matchFactIsNotMaliciousCode);

    ConstraintFact constraintFactIsNotMaliciousCode =
        new ConstraintFact("constraintIsNotMaliciousCode", "constraintIsNotMaliciousCode", LogicalOperator.AND.name());
    constraintFactIsNotMaliciousCode.addConditionFact(conditionFactIsNotMaliciousCode);

    List<ConstraintFact> constraintFactsIsNotMaliciousCode = singletonList(constraintFactIsNotMaliciousCode);

    tempEntity.newRepositoryPolicyViolation(repository2.getId(), 10, "path", "hash", constraintFactsIsNotMaliciousCode,
        false, FailActionType.ID, matchFactIsNotMaliciousCode.getPolicyId(), matchFactIsNotMaliciousCode.getPolicyId(),
        component.getComponentIdentifier(), date2, null, null, null);
  }

  private void createProprietaryNameConflictAndMaliciousCodeRepositoryPolicyViolation(
      Component component,
      LocalDate date)
  {
    TriggerSecurityVulnerabilityWithCategory triggerSecurityVulnerabilityWithCategory =
        new TriggerSecurityVulnerabilityWithCategory();
    ConditionTrigger conditionTrigger = new ConditionTrigger(0, triggerSecurityVulnerabilityWithCategory);

    MatchFact matchFact = new MatchFact(component, "policyProprietaryNameConflictAndMaliciousCode", null, 0,
        singletonList(conditionTrigger));

    Condition conditionProprietaryNameConflict =
        new Condition(ProprietaryNameConflictConditionType.ID, ProprietaryNameConflictConditionType.OP_IS_PRESENT);

    ConditionFact conditionFactProprietaryNameConflict =
        ComponentPolicyEvaluator.createConditionFact(conditionProprietaryNameConflict, matchFact);

    Condition conditionMaliciousCode = new Condition(SecurityVulnerabilityCategoryConditionType.ID,
        ConditionTypes.SecurityVulnerabilityCategoryConditionType.getSupportedOperators().get(0),
        SecurityVulnerabilityCategory.MALICIOUS_CODE.getId());

    ConditionFact conditionFactMaliciousCode =
        ComponentPolicyEvaluator.createConditionFact(conditionMaliciousCode, matchFact);

    ConstraintFact constraintFact = new ConstraintFact("constraintProprietaryNameConflictAndMalicious",
        "constraintProprietaryNameConflictAndMalicious", LogicalOperator.AND.name());
    constraintFact.addConditionFact(conditionFactProprietaryNameConflict);
    constraintFact.addConditionFact(conditionFactMaliciousCode);

    List<ConstraintFact> constraintFacts = singletonList(constraintFact);

    tempEntity.newRepositoryPolicyViolation(repository2.getId(), 10, "path", "hash", constraintFacts, false,
        FailActionType.ID, matchFact.getPolicyId(), matchFact.getPolicyId(), component.getComponentIdentifier(),
        toDate(date), null, null, null);
  }

  private void createRepositories() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    hostedRepository =
        tempEntity.newHostedRepository(repositoryManager, "hostedRepo", ComponentIdentifier.FORMAT_NPM, true);
    repository1 = tempEntity.newRepository();
    repository2 = tempEntity.newRepository();
  }

  private void doTestMigrate_WithPreviousMetrics(
      FirewallMetricsName firewallMetricsName,
      String expectedLog,
      Consumer<Date> creatorForMetricData)
  {
    createRepositories();

    LocalDate now = LocalDate.now();
    LocalDate yesterday = now.minusDays(1);

    FirewallMetrics metric = new FirewallMetrics(now, firewallMetricsName, 2);
    firewallMetricsDAO.insert(metric);

    creatorForMetricData.accept(toDate(yesterday));

    firewallMetricsMigrator.migrate();

    assertThat(logOutput).contains("Calculating Firewall Metrics from 3 repositories.")
        .contains(expectedLog)
        .contains("Firewall Metrics calculated for 3 repositories");

    List<FirewallMetrics> metrics = firewallMetricsDAO.getAll().stream()
        .filter(firewallMetric -> firewallMetric.getMetricsName() == firewallMetricsName).collect(toList());

    assertThat(metrics).hasSize(1);
    assertThat(metrics.get(0)).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(metric);

    assertThat(migrationTrackerDAO.getById(FirewallMetricsMigrator.MIGRATION_ID)).isNotNull();
  }
}
