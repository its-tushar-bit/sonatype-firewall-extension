/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.ApiFirewallMetricsService;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.hds.FirewallAuditHdsClient;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternService;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.scan.matcher.firewall.RepositoryPathnameParser;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.telemetry.ProxyRepositoryComponentTelemetryCreator;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.webhook.FirewallPolicyAlertEventService;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class RepositoryReevaluationTaskTest
    extends AbstractComponentH2Test
{
  private RepositoryReevaluationTask task;

  private Repository repository;

  @Inject
  private ComponentPolicyEvaluator componentPolicyEvaluator;

  private Policy policy;

  @Inject
  private ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @Inject
  private ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyViolationLoggerFactory policyViolationLoggerFactory;

  @Inject
  private FirewallIgnorePatternService firewallIgnorePatternService;

  @Inject
  private ProxyRepositoryComponentDeleteService proxyRepositoryComponentDeleteService;

  @Inject
  private RepositoryPolicyAlertEmailer repositoryPolicyAlertEmailer;

  @Inject
  private ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  @Inject
  private ProxyRepositoryComponentTelemetryCreator proxyRepositoryComponentTelemetryCreator;

  @Inject
  private ClusterLockManager clusterLockManager;

  @Inject
  private ApiFirewallMetricsService firewallMetricsService;

  @Inject
  private RepositoryPathnameParser repositoryPathnameParser;

  @Inject
  private FirewallPolicyAlertEventService firewallPolicyAlertEventService;

  @Mock
  private FirewallAuditHdsClient auditHdsClient;

  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private AsyncEventBus mockEventBus;

  private ProxyRepositoryComponent unknownComponent;

  private ProxyRepositoryComponent component;

  private final ComponentIdentifier claimedIdentifier =
      ComponentIdentifier.createMavenCoordinates("com", "claimed", "3.0");

  private final ComponentIdentifier newIdentifier =
      ComponentIdentifier.createMavenCoordinates("com", "new-component", "2.0");

  private final ExecutorService spyExecutorService = spy(Executors.newFixedThreadPool(1));

  /*
   * Setup:
   * - Existing repository, one unknown component no violations, one component with a violation & quarantined
   * - New policy that both components will violate when reevaluated
   * - New claim for the unknown component
   */
  @BeforeEach
  public void setup() throws Exception {
    repository = tempEntity.newRepository();

    component = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("org", "known", "1.0.0"));
    component.setQuarantineTime(new Date());
    proxyRepositoryComponentDAO.update(component);
    unknownComponent = tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);

    tempEntity.newRepositoryPolicyViolation(component, 1, true, "old", null);

    // policy things should violate
    policy = createPolicy();

    tempEntity.newClaimedComponent(component.getHash(), claimedIdentifier);
    tempEntity.newWaiver(unknownComponent.getHash(), policy.getId(), Organization.ROOT_ORGANIZATION_ID);

    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    firewallIgnorePatterns.regexpsByRepositoryFormat = new HashMap<>();
    lenient().when(mockHdsClient.get(eq(FirewallIgnorePatterns.class),
        eq(FirewallIgnorePatternUpdater.HDS_IGNORE_PATTERNS_PATH))).thenReturn(firewallIgnorePatterns);

    task = new RepositoryReevaluationTask(repository,
        new RepositoryPolicyEvaluator(componentPolicyEvaluator, proxyRepositoryComponentDAO,
            proxyRepositoryPolicyViolationDAO,
            policyDAO, auditHdsClient, null, policyViolationLoggerFactory, firewallIgnorePatternService,
            componentDetailsLoaderFactory, proxyRepositoryComponentDeleteService, repositoryPolicyAlertEmailer,
            proxyRepositoryComponentTelemetryCreator, clusterLockManager, mockEventBus, firewallMetricsService,
            repositoryPathnameParser, firewallPolicyAlertEventService),
        spyExecutorService, 10, proxyRepositoryComponentDAO, clusterLockManager);
    createHdsResponse();
  }

  /*
   * Both components should be known, one unquarantined, old policy violation gone, 2 new policy violations
   */
  @Test
  public void testTask() throws Exception {
    Date timeBeforeReevaluation = new Date();
    task.run();
    verify(spyExecutorService).shutdown();
    spyExecutorService.awaitTermination(1, TimeUnit.MINUTES);

    List<ProxyRepositoryComponent> components = proxyRepositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(components).hasSize(2);
    assertHasComponent(components, component.getPathname(), MatchState.EXACT, IdentificationSource.MANUAL.getId(),
        claimedIdentifier, false /* quarantined */, timeBeforeReevaluation);
    assertHasComponent(components, unknownComponent.getPathname(), MatchState.EXACT,
        IdentificationSource.SONATYPE.getId(), newIdentifier, false /* quarantined */, timeBeforeReevaluation);

    List<ProxyRepositoryPolicyViolation> violations =
        proxyRepositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(violations).hasSize(2);
    assertHasViolation(violations, component.getPathname(), policy.getName(), policy.getThreatLevel(),
        claimedIdentifier, false);
    assertHasViolation(violations, unknownComponent.getPathname(), policy.getName(), policy.getThreatLevel(),
        newIdentifier, true);
  }

  private static void assertHasViolation(
      List<ProxyRepositoryPolicyViolation> violations,
      String pathname,
      String policyName,
      int threatLevel,
      ComponentIdentifier componentIdentifier,
      boolean waived)
  {
    for (ProxyRepositoryPolicyViolation violation : violations) {
      if (violation.getPathname().equals(pathname) && violation.getPolicyName().equals(policyName)) {
        assertThat(violation.getThreatLevel()).isEqualTo(threatLevel);
        assertThat(violation.getComponentIdentifier()).isEqualTo(componentIdentifier);
        assertThat(violation.isWaived()).isEqualTo(waived);
        return;
      }
    }
    fail("Failed to locate component " + pathname);
  }

  private static void assertHasComponent(
      List<ProxyRepositoryComponent> components,
      String pathname,
      MatchState matchState,
      String identificationSource,
      ComponentIdentifier componentIdentifier,
      boolean quarantined,
      Date timeBeforeReevaluation)
  {
    for (ProxyRepositoryComponent component : components) {
      if (component.getPathname().equals(pathname)) {
        assertThat(component.getMatchStateId()).isEqualTo(matchState.getId());
        assertThat(component.getIdentificationSourceId()).isEqualTo(identificationSource);
        assertThat(component.getComponentIdentifier()).isEqualTo(componentIdentifier);
        assertThat(component.isQuarantined()).isEqualTo(quarantined);
        assertThat(component.getLastEvaluationTime()).isAfterOrEqualTo(timeBeforeReevaluation);
        return;
      }
    }
    fail("Failed to locate component " + pathname);
  }

  private void createHdsResponse() {
    ComponentEvaluationDataList response = new ComponentEvaluationDataList();
    response.components.add(createComponentResponse(component.getHash(), component.getComponentIdentifier(),
        MatchState.EXACT.getId(), 0));
    response.components.add(createComponentResponse(unknownComponent.getHash(), newIdentifier,
        MatchState.EXACT.getId(), 1));

    when(auditHdsClient.post(any(), eq(ComponentEvaluationDataList.class),
        eq(RepositoryPolicyEvaluator.HDS_COMPONENT_DETAILS_PATH), isNull(),
        any(RepositoryComponentEvaluationDataRequestList.class))).thenReturn(response);
  }

  private ComponentEvaluationData createComponentResponse(
      String hash,
      ComponentIdentifier identifier,
      String matchState,
      int index)
  {
    ComponentEvaluationData componentEvaluationData = new ComponentEvaluationData();

    componentEvaluationData.requestIndex = index;
    componentEvaluationData.hash = hash;
    componentEvaluationData.componentIdentifier = identifier;
    componentEvaluationData.declaredLicenses = Collections.emptySet();
    componentEvaluationData.observedLicenses = Collections.emptySet();
    componentEvaluationData.matchState = matchState;
    componentEvaluationData.securityVulnerabilities = Collections.singletonList(new SecurityVulnerability("cve",
        "CVE-2015-1234", 9.0f));

    return componentEvaluationData;
  }

  private Policy createPolicy() {
    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "new", 9);
    Constraint constraintOrg = new Constraint(null, "Constraint Name Org", LogicalOperator.AND);
    constraintOrg.addCondition(new Condition(CoordinatesConditionType.ID, "match", "maven:com"));
    policy.setConstraints(Collections.singletonList(constraintOrg));

    policyDAO.update(policy);
    return policy;
  }
}
