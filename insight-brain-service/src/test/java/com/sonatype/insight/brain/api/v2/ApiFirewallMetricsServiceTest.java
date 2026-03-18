/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.roi.RoiConfigurationDefaultValuesDAO;
import com.sonatype.insight.brain.roi.dto.RoiFirewallMetricsDTO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCategory;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryNameConflictConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.policy.facts.TriggerSecurityVulnerabilityWithCategory;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.roi.CurrencyTypes;
import com.sonatype.insight.brain.model.successmetrics.ApiFirewallMetricsResultDTO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.COMPONENTS_AUTO_RELEASED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.COMPONENTS_QUARANTINED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.WAIVED_COMPONENTS;
import static com.sonatype.insight.brain.utils.DateConverter.toDate;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class ApiFirewallMetricsServiceTest
    extends AbstractComponentTest
{
  @Inject
  private FirewallMetricsDAO firewallMetricsDAO;

  @Inject
  private ApiFirewallMetricsService firewallMetricsService;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private RoiConfigurationDefaultValuesDAO dao;

  @Test
  public void testGetFirewallMetrics() {
    LocalDate fiveDaysAgoLocalDate = LocalDate.now().minusDays(5);
    LocalDate oneYearAgoLocalDate = LocalDate.now().minusMonths(12);
    LocalDate twoYearsAgoLocalDate = LocalDate.now().minusMonths(24);
    tempEntity.newFirewallMetrics(WAIVED_COMPONENTS, 20, toDate(fiveDaysAgoLocalDate), fiveDaysAgoLocalDate);
    tempEntity.newFirewallMetrics(COMPONENTS_QUARANTINED, 10, toDate(oneYearAgoLocalDate), oneYearAgoLocalDate);
    tempEntity.newFirewallMetrics(NAMESPACE_ATTACKS_BLOCKED, 30, toDate(twoYearsAgoLocalDate), twoYearsAgoLocalDate);

    Map<FirewallMetricsName, ApiFirewallMetricsResultDTO> firewallMetricsNameValueMap =
        firewallMetricsService.getFirewallMetrics();
    assertThat(firewallMetricsNameValueMap.size()).isEqualTo(6);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO1 = firewallMetricsNameValueMap.get(WAIVED_COMPONENTS);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO2 = firewallMetricsNameValueMap.get(COMPONENTS_QUARANTINED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO3 =
        firewallMetricsNameValueMap.get(NAMESPACE_ATTACKS_BLOCKED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO4 =
        firewallMetricsNameValueMap.get(COMPONENTS_AUTO_RELEASED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO5 =
        firewallMetricsNameValueMap.get(SUPPLY_CHAIN_ATTACKS_BLOCKED);
    ApiFirewallMetricsResultDTO apiFirewallMetricsResultDTO6 =
        firewallMetricsNameValueMap.get(SAFE_VERSIONS_SELECTED_AUTOMATICALLY);
    assertThat(apiFirewallMetricsResultDTO1.getFirewallMetricsValue()).isEqualTo(20);
    assertThat(apiFirewallMetricsResultDTO1.getLatestUpdatedTime()).isEqualTo(toDate(fiveDaysAgoLocalDate));
    assertThat(apiFirewallMetricsResultDTO2.getFirewallMetricsValue()).isEqualTo(10);
    assertThat(apiFirewallMetricsResultDTO2.getLatestUpdatedTime()).isEqualTo(toDate(oneYearAgoLocalDate));
    assertThat(apiFirewallMetricsResultDTO3.getFirewallMetricsValue()).isEqualTo(30);
    assertThat(apiFirewallMetricsResultDTO3.getLatestUpdatedTime()).isEqualTo(toDate(twoYearsAgoLocalDate));
    assertThat(apiFirewallMetricsResultDTO4.getFirewallMetricsValue()).isEqualTo(0);
    assertThat(apiFirewallMetricsResultDTO4.getLatestUpdatedTime()).isNull();
    assertThat(apiFirewallMetricsResultDTO5.getFirewallMetricsValue()).isEqualTo(0);
    assertThat(apiFirewallMetricsResultDTO5.getLatestUpdatedTime()).isNull();
    assertThat(apiFirewallMetricsResultDTO6.getFirewallMetricsValue()).isEqualTo(0);
    assertThat(apiFirewallMetricsResultDTO6.getLatestUpdatedTime()).isNull();
  }

  @Test
  public void testGetFirewallMetrics_NoReleaseIntegrityFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> firewallMetricsService.getFirewallMetrics());
  }

  @Test
  public void testGetFirewallMetrics_NoFirewallAutoUnquarantineFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> firewallMetricsService.getFirewallMetrics());
  }

  @Test
  public void testIncrementFirewallMetrics() {
    Date testDate1 = new GregorianCalendar(2023, Calendar.OCTOBER, 1).getTime();
    Date testDate2 = new GregorianCalendar(2023, Calendar.OCTOBER, 2).getTime();
    LocalDate today = LocalDate.now();

    String id1 = tempEntity.newFirewallMetrics(FirewallMetricsName.WAIVED_COMPONENTS, 20, testDate1, today);
    String id2 = tempEntity.newFirewallMetrics(FirewallMetricsName.COMPONENTS_QUARANTINED, 10, testDate2, today);

    FirewallMetrics firewallMetrics1 = new FirewallMetrics();
    firewallMetrics1.setMetricsName(FirewallMetricsName.WAIVED_COMPONENTS);
    firewallMetrics1.setMetricsValue(50);
    firewallMetrics1.setMetricsLastUpdatedAt(new Date());
    firewallMetrics1.setMetricsDate(today);

    FirewallMetrics firewallMetrics2 = new FirewallMetrics();
    firewallMetrics2.setMetricsName(FirewallMetricsName.COMPONENTS_QUARANTINED);
    firewallMetrics2.setMetricsValue(50);
    firewallMetrics2.setMetricsLastUpdatedAt(new Date());
    firewallMetrics2.setMetricsDate(today);

    firewallMetricsService.incrementFirewallMetrics(firewallMetrics1);
    firewallMetricsService.incrementFirewallMetrics(firewallMetrics2);

    FirewallMetrics firewallMetricsRes1 = firewallMetricsDAO.getById(id1);
    FirewallMetrics firewallMetricsRes2 = firewallMetricsDAO.getById(id2);

    assertThat(firewallMetricsRes1.getMetricsName()).isEqualTo(FirewallMetricsName.WAIVED_COMPONENTS);
    assertThat(firewallMetricsRes2.getMetricsName()).isEqualTo(FirewallMetricsName.COMPONENTS_QUARANTINED);

    assertThat(firewallMetricsRes1.getMetricsValue()).isEqualTo(70);
    assertThat(firewallMetricsRes2.getMetricsValue()).isEqualTo(60);
  }

  @Test
  public void testIsValidProductLicense_NoReleaseIntegrityFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);
    assertThat(firewallMetricsService.isValidProductLicense()).isFalse();
  }

  @Test
  public void testIsValidProductLicense_NoFirewallAutoUnquarantineFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);
    assertThat(firewallMetricsService.isValidProductLicense()).isFalse();
  }

  @Test
  public void testIsValidProductLicense_NoReleaseIntegrityFeatureAndNoFirewallAutoUnquarantineFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);
    assertThat(firewallMetricsService.isValidProductLicense()).isFalse();
  }

  @Test
  public void testIsValidProductLicense_ReleaseIntegrityFeatureAndFirewallAutoUnquarantineFeature() {
    assertThat(firewallMetricsService.isValidProductLicense()).isTrue();
  }

  @Test
  public void testCheckProductLicense_NoReleaseIntegrityFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> firewallMetricsService.checkProductLicense());
  }

  @Test
  public void testCheckProductLicense_NoFirewallAutoUnquarantineFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> firewallMetricsService.checkProductLicense());
  }

  @Test
  public void testCheckProductLicense_NoReleaseIntegrityFeatureAndNoFirewallAutoUnquarantineFeature() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> firewallMetricsService.checkProductLicense());
  }

  @Test
  public void testCheckProductLicense_ReleaseIntegrityFeatureAndFirewallAutoUnquarantineFeature() {
    assertThatNoException().isThrownBy(() -> firewallMetricsService.checkProductLicense());
  }

  @Test
  public void testCheckLicensedFeatures_NoReleaseIntegrityFeature() {
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> firewallMetricsService
        .checkLicensedFeatures(ImmutableSet.of(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE)));
  }

  @Test
  public void testCheckLicensedFeatures_NoFirewallAutoUnquarantineFeature() {
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> firewallMetricsService
            .checkLicensedFeatures(Collections.singleton(LicensedFeature.RELEASE_INTEGRITY)));
  }

  @Test
  public void testCheckLicensedFeatures_NoReleaseIntegrityFeatureAndNoFirewallAutoUnquarantineFeature() {
    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> firewallMetricsService.checkLicensedFeatures(Collections.singleton(LicensedFeature.AUTOMATION)));
  }

  @Test
  public void testCheckLicensedFeatures_ReleaseIntegrityFeatureAndFirewallAutoUnquarantineFeature() {
    assertThatNoException().isThrownBy(() -> firewallMetricsService.checkLicensedFeatures(
        ImmutableSet.of(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, LicensedFeature.RELEASE_INTEGRITY,
            LicensedFeature.AUTOMATION)));
  }

  @Test
  public void testCheckFirewallMetricsInRepositoryPolicyViolation_NamespaceAttacksBlockedMetrics() {
    doTestCheckFirewallMetricsInRepositoryPolicyViolation(
        date -> newProprietaryNameConflictRepositoryPolicyViolation(date), true, false);
  }

  @Test
  public void testCheckFirewallMetricsInRepositoryPolicyViolation_SupplyChainAttacksBlockedMetrics() {
    doTestCheckFirewallMetricsInRepositoryPolicyViolation(date -> newRepositoryPolicyViolationMaliciousCode(date),
        false, true);
  }

  @Test
  public void testCheckFirewallMetricsInRepositoryPolicyViolation_BothMetrics() {
    doTestCheckFirewallMetricsInRepositoryPolicyViolation(
        date -> newProprietaryNameConflictAndMaliciousCodeRepositoryPolicyViolation(date), true, true);
  }

  private void doTestCheckFirewallMetricsInRepositoryPolicyViolation(
      Function<Date, RepositoryPolicyViolation> function,
      boolean isTestingNamespaceAttacksBlockedMetrics,
      boolean isTestingSupplyChainAttacksBlockedMetrics)
  {
    Map<LocalDate, FirewallMetrics> namespaceAttacksBlockedMetrics = new TreeMap<>();
    Map<LocalDate, FirewallMetrics> supplyChainAttacksBlockedMetrics = new TreeMap<>();
    RepositoryPolicyViolation repositoryPolicyViolation = function.apply(new Date());

    firewallMetricsService.checkFirewallMetricsInRepositoryPolicyViolation(repositoryPolicyViolation,
        namespaceAttacksBlockedMetrics, supplyChainAttacksBlockedMetrics);

    // initial today value
    if (isTestingNamespaceAttacksBlockedMetrics) {
      assertThat(namespaceAttacksBlockedMetrics).containsKey(LocalDate.now());
      assertThat(namespaceAttacksBlockedMetrics.values()).extracting(FirewallMetrics::getMetricsName)
          .containsExactly(NAMESPACE_ATTACKS_BLOCKED);
      assertThat(namespaceAttacksBlockedMetrics.values()).extracting(FirewallMetrics::getMetricsValue)
          .containsExactly(1);
    }
    else {
      assertThat(namespaceAttacksBlockedMetrics).isEmpty();
    }

    if (isTestingSupplyChainAttacksBlockedMetrics) {
      assertThat(supplyChainAttacksBlockedMetrics).containsKey(LocalDate.now());
      assertThat(supplyChainAttacksBlockedMetrics.values()).extracting(FirewallMetrics::getMetricsName)
          .containsExactly(SUPPLY_CHAIN_ATTACKS_BLOCKED);
      assertThat(supplyChainAttacksBlockedMetrics.values()).extracting(FirewallMetrics::getMetricsValue)
          .containsExactly(1);
    }
    else {
      assertThat(supplyChainAttacksBlockedMetrics).isEmpty();
    }

    // second value for today
    firewallMetricsService.checkFirewallMetricsInRepositoryPolicyViolation(repositoryPolicyViolation,
        namespaceAttacksBlockedMetrics, supplyChainAttacksBlockedMetrics);

    if (isTestingNamespaceAttacksBlockedMetrics) {
      assertThat(namespaceAttacksBlockedMetrics).containsKey(LocalDate.now());
      assertThat(namespaceAttacksBlockedMetrics.values()).extracting(FirewallMetrics::getMetricsName)
          .containsExactly(NAMESPACE_ATTACKS_BLOCKED);
      assertThat(namespaceAttacksBlockedMetrics.values()).extracting(FirewallMetrics::getMetricsValue)
          .containsExactly(2);
    }
    else {
      assertThat(namespaceAttacksBlockedMetrics).isEmpty();
    }

    if (isTestingSupplyChainAttacksBlockedMetrics) {
      assertThat(supplyChainAttacksBlockedMetrics).containsKey(LocalDate.now());
      assertThat(supplyChainAttacksBlockedMetrics.values()).extracting(FirewallMetrics::getMetricsName)
          .containsExactly(SUPPLY_CHAIN_ATTACKS_BLOCKED);
      assertThat(supplyChainAttacksBlockedMetrics.values()).extracting(FirewallMetrics::getMetricsValue)
          .containsExactly(2);
    }
    else {
      assertThat(supplyChainAttacksBlockedMetrics).isEmpty();
    }

    // third value for yesterday
    repositoryPolicyViolation = function.apply(DateUtils.addDays(new Date(), -1));
    firewallMetricsService.checkFirewallMetricsInRepositoryPolicyViolation(repositoryPolicyViolation,
        namespaceAttacksBlockedMetrics, supplyChainAttacksBlockedMetrics);

    if (isTestingNamespaceAttacksBlockedMetrics) {
      assertThat(namespaceAttacksBlockedMetrics).containsKeys(LocalDate.now().minusDays(1), LocalDate.now());
      assertThat(namespaceAttacksBlockedMetrics.values()).extracting(FirewallMetrics::getMetricsName)
          .containsOnly(NAMESPACE_ATTACKS_BLOCKED);
      assertThat(namespaceAttacksBlockedMetrics.values()).extracting(FirewallMetrics::getMetricsValue)
          .containsExactly(1, 2);
    }
    else {
      assertThat(namespaceAttacksBlockedMetrics).isEmpty();
    }

    if (isTestingSupplyChainAttacksBlockedMetrics) {
      assertThat(supplyChainAttacksBlockedMetrics).containsKeys(LocalDate.now().minusDays(1), LocalDate.now());
      assertThat(supplyChainAttacksBlockedMetrics.values()).extracting(FirewallMetrics::getMetricsName)
          .containsOnly(SUPPLY_CHAIN_ATTACKS_BLOCKED);
      assertThat(supplyChainAttacksBlockedMetrics.values()).extracting(FirewallMetrics::getMetricsValue)
          .containsExactly(1, 2);
    }
    else {
      assertThat(supplyChainAttacksBlockedMetrics).isEmpty();
    }
  }

  private RepositoryPolicyViolation newRepositoryPolicyViolationMaliciousCode(Date time) {
    Component component = new Component(ComponentIdentifier.createNpmCoordinates("p", "v"));

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

    RepositoryPolicyViolation repositoryPolicyViolation = new RepositoryPolicyViolation();
    repositoryPolicyViolation.setConstraintFacts(constraintFactsMaliciousCode);
    repositoryPolicyViolation.setTime(time);
    return repositoryPolicyViolation;
  }

  private RepositoryPolicyViolation newProprietaryNameConflictRepositoryPolicyViolation(Date time) {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository hostedRepository =
        tempEntity.newHostedRepository(repositoryManager, "hostedRepo", ComponentIdentifier.FORMAT_NPM, true);
    Component component = new Component(ComponentIdentifier.createNpmCoordinates("p", "v"));
    component.setConflictingProprietaryName(new ProprietaryComponentName("testPattern", hostedRepository.getId()));

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

    RepositoryPolicyViolation repositoryPolicyViolation = new RepositoryPolicyViolation();
    repositoryPolicyViolation.setConstraintFacts(constraintFactsProprietaryNameConflict);
    repositoryPolicyViolation.setTime(time);
    return repositoryPolicyViolation;
  }

  private RepositoryPolicyViolation newProprietaryNameConflictAndMaliciousCodeRepositoryPolicyViolation(Date date) {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository hostedRepository =
        tempEntity.newHostedRepository(repositoryManager, "hostedRepo", ComponentIdentifier.FORMAT_NPM, true);
    Component component = new Component(ComponentIdentifier.createNpmCoordinates("p", "v"));
    component.setConflictingProprietaryName(new ProprietaryComponentName("testPattern", hostedRepository.getId()));

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

    RepositoryPolicyViolation repositoryPolicyViolation = new RepositoryPolicyViolation();
    repositoryPolicyViolation.setConstraintFacts(constraintFacts);
    repositoryPolicyViolation.setTime(date);
    return repositoryPolicyViolation;
  }

  @Test
  public void testGetRoiFirewallMetrics_RoiConfigurationExist() {
    tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        15,
        BigDecimal.valueOf(400));
    tempEntity.newFirewallMetrics(SUPPLY_CHAIN_ATTACKS_BLOCKED, 10, new Date());
    tempEntity.newFirewallMetrics(NAMESPACE_ATTACKS_BLOCKED, 10, new Date());
    tempEntity.newFirewallMetrics(SAFE_VERSIONS_SELECTED_AUTOMATICALLY, 10, new Date(), LocalDate.now());
    RoiFirewallMetricsDTO roiMetricsDTO = firewallMetricsService.getRoiFirewallMetrics(CurrencyTypes.USD);
    assertThat(roiMetricsDTO).isNotNull();
    assertThat(roiMetricsDTO.getCurrency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiMetricsDTO.getMalwareAttacksPrevented()).isEqualTo(BigDecimal.valueOf(500000));
    assertThat(roiMetricsDTO.getNamespaceAttacksPrevented()).isEqualTo(BigDecimal.valueOf(600000));
    assertThat(roiMetricsDTO.getSafeComponentsAutoSelected()).isEqualTo(BigDecimal.valueOf(700000));
  }

  @Test
  public void testGetRoiFirewallMetrics_NotFirewallMetricsValues() {
    tempEntity.createRoiConfiguration(
        CurrencyTypes.USD,
        BigDecimal.valueOf(50000),
        BigDecimal.valueOf(60000),
        BigDecimal.valueOf(70000),
        15,
        BigDecimal.valueOf(400));
    RoiFirewallMetricsDTO roiMetricsDTO = firewallMetricsService.getRoiFirewallMetrics(CurrencyTypes.USD);
    assertThat(roiMetricsDTO).isNotNull();
    assertThat(roiMetricsDTO.getCurrency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiMetricsDTO.getMalwareAttacksPrevented()).isEqualTo(BigDecimal.valueOf(0));
    assertThat(roiMetricsDTO.getNamespaceAttacksPrevented()).isEqualTo(BigDecimal.valueOf(0));
    assertThat(roiMetricsDTO.getSafeComponentsAutoSelected()).isEqualTo(BigDecimal.valueOf(0));
  }

  @Test
  public void testGetRoiFirewallMetrics_RoiConfigurationNotExist() {
    dao.getAll().forEach(dao::delete);
    tempEntity.createRoiConfigurationDefaultValues(
        CurrencyTypes.USD,
        BigDecimal.valueOf(4350000),
        BigDecimal.valueOf(500000),
        BigDecimal.valueOf(35000),
        BigDecimal.valueOf(10000),
        BigDecimal.valueOf(25000),
        BigDecimal.valueOf(5000),
        30,
        15,
        BigDecimal.valueOf(800),
        BigDecimal.valueOf(400));
    tempEntity.newFirewallMetrics(SUPPLY_CHAIN_ATTACKS_BLOCKED, 10, new Date());
    tempEntity.newFirewallMetrics(NAMESPACE_ATTACKS_BLOCKED, 10, new Date());
    tempEntity.newFirewallMetrics(SAFE_VERSIONS_SELECTED_AUTOMATICALLY, 10, new Date(), LocalDate.now());
    RoiFirewallMetricsDTO roiMetricsDTO = firewallMetricsService.getRoiFirewallMetrics(CurrencyTypes.USD);
    assertThat(roiMetricsDTO).isNotNull();
    assertThat(roiMetricsDTO.getCurrency()).isEqualTo(CurrencyTypes.USD);
    assertThat(roiMetricsDTO.getMalwareAttacksPrevented()).isEqualTo(BigDecimal.valueOf(43500000));
    assertThat(roiMetricsDTO.getNamespaceAttacksPrevented()).isEqualTo(BigDecimal.valueOf(350000));
    assertThat(roiMetricsDTO.getSafeComponentsAutoSelected()).isEqualTo(BigDecimal.valueOf(250000));
  }
}
