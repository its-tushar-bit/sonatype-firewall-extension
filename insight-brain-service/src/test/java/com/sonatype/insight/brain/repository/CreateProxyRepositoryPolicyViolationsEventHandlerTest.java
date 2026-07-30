/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.ProprietaryComponentName;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCategory;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
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
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.utils.DateConverter.toDate;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateProxyRepositoryPolicyViolationsEventHandlerTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(CreateProxyRepositoryPolicyViolationsEventHandler.class);

  @Inject
  private CreateProxyRepositoryPolicyViolationsEventHandler handler;

  @Inject
  private FirewallMetricsDAO firewallMetricsDAO;

  @Inject
  private TestProductLicense testProductLicense;

  private Repository repository;

  private Component component;

  @Before
  public void before() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository hostedRepository =
        tempEntity.newHostedRepository(repositoryManager, "hostedRepo", ComponentIdentifier.FORMAT_NPM, true);
    repository = tempEntity.newRepository(repositoryManager);

    component = new Component(ComponentIdentifier.createNpmCoordinates("p", "v"));
    component.setConflictingProprietaryName(new ProprietaryComponentName("testPattern", hostedRepository.getId()));
  }

  @Test
  public void testOnRepositoryPolicyViolationsCreated_InvalidProductLicense() {
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE);

    handler.onProxyRepositoryPolicyViolationsCreated(null);

    assertThat(logOutput).contains("Invalid product license to create Firewall Metrics");
    assertThat(firewallMetricsDAO.getAll()).isEmpty();
  }

  @Test
  public void testOnProxyRepositoryPolicyViolationsCreated_EmptyProxyRepositoryPolicyViolations() {
    handler.onProxyRepositoryPolicyViolationsCreated(new CreateRepositoryPolicyViolationsEvent());

    assertThat(logOutput).contains("No repository policy violations to process");
    assertThat(firewallMetricsDAO.getAll()).isEmpty();
  }

  @Test
  public void testOnRepositoryPolicyViolationsCreated_NamespaceAttacksBlocked() {
    int expectedMetrics = 14;
    doTestOnRepositoryPolicyViolationsCreated(expectedMetrics, NAMESPACE_ATTACKS_BLOCKED,
        createProprietaryNameConflictProxyRepositoryPolicyViolations(expectedMetrics));
  }

  @Test
  public void testOnRepositoryPolicyViolationsCreated_SupplyChainAttacksBlocked() {
    int expectedMetrics = 30;
    doTestOnRepositoryPolicyViolationsCreated(expectedMetrics, SUPPLY_CHAIN_ATTACKS_BLOCKED,
        createMaliciousCodeRepositoryPolicyViolations(expectedMetrics));
  }

  @Test
  public void testOnRepositoryPolicyViolationsCreated_NamespaceAttacksBlockedAndSupplyChainAttacksBlocked() {
    int expectedNamespaceAttacksBlockedMetrics = 29;
    int expectedSupplyChainAttacksBlockedMetrics = 3;

    CreateRepositoryPolicyViolationsEvent event = new CreateRepositoryPolicyViolationsEvent();
    event.proxyRepositoryPolicyViolations
        .addAll(createProprietaryNameConflictProxyRepositoryPolicyViolations(expectedNamespaceAttacksBlockedMetrics));
    event.proxyRepositoryPolicyViolations
        .addAll(createMaliciousCodeRepositoryPolicyViolations(expectedSupplyChainAttacksBlockedMetrics));

    handler.onProxyRepositoryPolicyViolationsCreated(event);

    List<FirewallMetrics> result = firewallMetricsDAO.getAll();

    assertThat(result)
        .extracting(FirewallMetrics::getMetricsName)
        .containsExactlyInAnyOrder(NAMESPACE_ATTACKS_BLOCKED, SUPPLY_CHAIN_ATTACKS_BLOCKED);

    FirewallMetrics namespaceAttacksBlockedMetrics = result.stream()
        .filter(metric -> metric.getMetricsName() == NAMESPACE_ATTACKS_BLOCKED)
        .findFirst()
        .get();

    assertThat(namespaceAttacksBlockedMetrics.getMetricsValue()).isEqualTo(expectedNamespaceAttacksBlockedMetrics);

    FirewallMetrics supplyChainAttacksBlockedMetrics = result.stream()
        .filter(metric -> metric.getMetricsName() == SUPPLY_CHAIN_ATTACKS_BLOCKED)
        .findFirst()
        .get();

    assertThat(supplyChainAttacksBlockedMetrics.getMetricsValue()).isEqualTo(expectedSupplyChainAttacksBlockedMetrics);
  }

  private void doTestOnRepositoryPolicyViolationsCreated(
      int expectedMetrics,
      FirewallMetricsName firewallMetricsName,
      List<ProxyRepositoryPolicyViolation> proxyRepositoryPolicyViolations)
  {
    CreateRepositoryPolicyViolationsEvent event = new CreateRepositoryPolicyViolationsEvent();
    event.proxyRepositoryPolicyViolations = proxyRepositoryPolicyViolations;

    handler.onProxyRepositoryPolicyViolationsCreated(event);

    assertThat(logOutput).contains("Start processing repository policy violations for Firewall Metrics");
    assertThat(logOutput).contains("Finished processing repository policy violations for Firewall Metrics");

    List<FirewallMetrics> result = firewallMetricsDAO.getAll();

    assertThat(result).hasSize(1);

    FirewallMetrics firewallMetrics = result.get(0);
    assertThat(firewallMetrics.getMetricsName()).isEqualTo(firewallMetricsName);
    assertThat(firewallMetrics.getMetricsValue()).isEqualTo(expectedMetrics);
  }

  private List<ProxyRepositoryPolicyViolation> createProprietaryNameConflictProxyRepositoryPolicyViolations(
      int expectedMetrics)
  {
    List<ProxyRepositoryPolicyViolation> proxyRepositoryPolicyViolations = new ArrayList<>();

    Date date = toDate(LocalDate.now().minusDays(expectedMetrics));
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

    for (int i = 0; i < expectedMetrics; i++) {
      proxyRepositoryPolicyViolations
          .add(tempEntity.newRepositoryPolicyViolation(repository.getId(), 10, "path", "hash",
              constraintFactsProprietaryNameConflict, false, FailActionType.ID,
              matchFactProprietaryNameConflict.getPolicyId(), matchFactProprietaryNameConflict.getPolicyId(),
              component.getComponentIdentifier(), DateUtils.addSeconds(date, i * 10), null, null, null));
    }

    return proxyRepositoryPolicyViolations;
  }

  private List<ProxyRepositoryPolicyViolation> createMaliciousCodeRepositoryPolicyViolations(int expectedMetrics) {
    List<ProxyRepositoryPolicyViolation> proxyRepositoryPolicyViolations = new ArrayList<>();

    Date date = toDate(LocalDate.now().minusDays(expectedMetrics));

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

    for (int i = 0; i < expectedMetrics; i++) {
      proxyRepositoryPolicyViolations.add(
          tempEntity.newRepositoryPolicyViolation(repository.getId(), 10, "path", "hash", constraintFactsMaliciousCode,
              false, FailActionType.ID, matchFactMaliciousCode.getPolicyId(), matchFactMaliciousCode.getPolicyId(),
              component.getComponentIdentifier(), DateUtils.addSeconds(date, i * 10), null, null, null));
    }

    return proxyRepositoryPolicyViolations;
  }
}
