/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.ApiFirewallMetricsService;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
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
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.scan.matcher.firewall.RepositoryPathnameParser;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
public class RepositoryReevaluationTaskTest
    extends AbstractComponentTest
{
  private RepositoryReevaluationTask task;

  private Repository repository;

  @Inject
  private ComponentPolicyEvaluator componentPolicyEvaluator;

  @Inject
  private Policy policy;

  @Inject
  private RepositoryComponentDAO repositoryComponentDAO;

  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private PolicyViolationLoggerFactory policyViolationLoggerFactory;

  @Inject
  private FirewallIgnorePatternService firewallIgnorePatternService;

  @Inject
  private RepositoryComponentDeleteService repositoryComponentDeleteService;

  @Inject
  private RepositoryPolicyAlertEmailer repositoryPolicyAlertEmailer;

  @Inject
  private ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  @Inject
  private RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator;

  @Inject
  private ClusterLockManager clusterLockManager;

  @Inject
  private ApiFirewallMetricsService firewallMetricsService;

  @Inject
  private RepositoryPathnameParser repositoryPathnameParser;

  @Mock
  private FirewallAuditHdsClient auditHdsClient;

  @Mock
  private HdsClient mockHdsClient;

  @Mock
  private AsyncEventBus mockEventBus;

  private RepositoryComponent unknownComponent;

  private RepositoryComponent component;

  private final ComponentIdentifier claimedIdentifier =
      ComponentIdentifier.createMavenCoordinates("com", "claimed", "3.0");

  private final ComponentIdentifier newIdentifier =
      ComponentIdentifier.createMavenCoordinates("com", "new-component", "2.0");

  private final ExecutorService spyExecutorService = spy(Executors.newFixedThreadPool(1));

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
    binder.bind(AsyncEventBus.class).toInstance(mockEventBus);
    super.configure(binder);
  }

  /*
   * Setup:
   * - Existing repository, one unknown component no violations, one component with a violation & quarantined
   * - New policy that both components will violate when reevaluated
   * - New claim for the unknown component
   */
  @Before
  public void setup() throws Exception {
    repository = tempEntity.newRepository();

    component = tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("org", "known", "1.0.0"));
    component.setQuarantineTime(new Date());
    repositoryComponentDAO.update(component);
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
        new RepositoryPolicyEvaluator(componentPolicyEvaluator, repositoryComponentDAO, repositoryPolicyViolationDAO,
            policyDAO, auditHdsClient, null, policyViolationLoggerFactory, firewallIgnorePatternService,
            componentDetailsLoaderFactory, repositoryComponentDeleteService, repositoryPolicyAlertEmailer,
            repositoryComponentTelemetryCreator, clusterLockManager, mockEventBus, firewallMetricsService,
            repositoryPathnameParser),
        spyExecutorService, 10, repositoryComponentDAO, clusterLockManager);
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

    List<RepositoryComponent> components = repositoryComponentDAO.getByRepositoryId(repository.getId());
    assertThat(components).hasSize(2);
    assertHasComponent(components, component.getPathname(), MatchState.EXACT, IdentificationSource.MANUAL.getId(),
        claimedIdentifier, false /* quarantined */, timeBeforeReevaluation);
    assertHasComponent(components, unknownComponent.getPathname(), MatchState.EXACT,
        IdentificationSource.SONATYPE.getId(), newIdentifier, false /* quarantined */, timeBeforeReevaluation);

    List<RepositoryPolicyViolation> violations = repositoryPolicyViolationDAO.getByRepositoryId(repository.getId());
    assertThat(violations).hasSize(2);
    assertHasViolation(violations, component.getPathname(), policy.getName(), policy.getThreatLevel(),
        claimedIdentifier, false);
    assertHasViolation(violations, unknownComponent.getPathname(), policy.getName(), policy.getThreatLevel(),
        newIdentifier, true);
  }

  private static void assertHasViolation(
      List<RepositoryPolicyViolation> violations,
      String pathname,
      String policyName,
      int threatLevel,
      ComponentIdentifier componentIdentifier,
      boolean waived)
  {
    for (RepositoryPolicyViolation violation : violations) {
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
      List<RepositoryComponent> components,
      String pathname,
      MatchState matchState,
      String identificationSource,
      ComponentIdentifier componentIdentifier,
      boolean quarantined,
      Date timeBeforeReevaluation)
  {
    for (RepositoryComponent component : components) {
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
