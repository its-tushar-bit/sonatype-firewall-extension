/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.experimental.development.prioritization.PrioritizedComponent;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.label.ComponentLabelService;
import com.sonatype.insight.brain.label.ComponentLabelService.AppliedLabels;
import com.sonatype.insight.brain.label.ComponentLabelService.LabelsByOwner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.Component;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyAction;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyConstraint;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyViolation;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import oshi.util.tuples.Pair;

import static com.sonatype.insight.brain.api.experimental.ApiVulnerabilitySignatureService.SECURITY_REACHABLE_LABEL;
import static com.sonatype.insight.license.model.LicensedFeature.DEVELOPER_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DevelopmentPrioritiesServiceTest
    extends AbstractComponentTest
{
  private static final String GIVEN_SOME_PUBLIC_APP_ID = "some-public-app-id";

  private static final String GIVEN_SOME_SCAN_ID = "some-some-scan-id";

  private static final int GIVEN_PAGE_1 = 1;

  private static final int GIVEN_PAGE_SIZE_10 = 10;

  @Mock
  private FeaturesService featuresService;

  @Mock
  private DevelopmentPrioritiesReportService developmentPrioritiesReportService;

  @Mock
  private ComponentLabelService componentLabelService;

  private  DevelopmentPrioritiesService developmentPrioritiesService;

  @Before
  public void setup() {
    developmentPrioritiesService = new DevelopmentPrioritiesService(
        featuresService, developmentPrioritiesReportService, componentLabelService);
  }

  @Test
  public void testGetPrioritizedFindings_shouldThrowAppropriateErrorIfDevelopmentNotEnabled() {
    assertThatThrownBy(() ->
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10))
        .withFailMessage("This server is not licensed for Sonatype Development.")
        .isInstanceOf(NotAuthorizedException.class);
  }

  @Test
  public void testGetPrioritizedFindings_shouldExtractTheHighestThreatPolicyViolation() {
    // === GIVEN ===
    // max 6, resolves collision between multiple violations with the same threat level using policyViolationOrder
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final List<PolicyViolation> component1Violations = Lists.newArrayList(
        createPolicyViolation(2, "a", "policy-a"),
        createPolicyViolation(6, "b", "policy-b"),
        createPolicyViolation(5, "c", "policy-c"),
        createPolicyViolation(6, "d", "policy-d"),
        createPolicyViolation(6, "e", "policy-e"));
    Collections.shuffle(component1Violations);
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        component1Violations,
        // add a violation that's not active, it should not affect our results
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z"))
    );

    // has the highest threat level of all the components
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final List<PolicyViolation> component2Violations = Lists.newArrayList(
        createPolicyViolation(10, "f", "policy-f"),
        createPolicyViolation(7, "g", "policy-g"));
    Collections.shuffle(component2Violations);
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(component2, component2Violations);

    // no violations
    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component3");

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3)));
    when(developmentPrioritiesReportService.getPolicyThreatsNoAuth(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats, component2Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<PrioritizedComponent> results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10)
        .getResults();

    assertThat(results).containsExactlyElementsOf(
        Lists.newArrayList(
            toPrioritizedComponent(component2, 10, "policy-f", null, 1),
            toPrioritizedComponent(component1, 6, "policy-b", null, 2),
            toPrioritizedComponent(component3, 0, null, null, 3)));

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldResolveTheCorrectDependencyType() {
    // === Given ===
    final ApiReportComponentDTOV2 component1 =
        createComponent("aaa", "component1");
    final ApiReportComponentDTOV2 component2 =
        createComponent("bbb", "component2", getTransitiveDependencyType());
    final ApiReportComponentDTOV2 component3 =
        createComponent("ccc", "component3", getInnerSourceDependencyType());
    final ApiReportComponentDTOV2 component4 =
        createComponent("ddd", "component4", getDirectDependencyType());
    final ApiReportComponentDTOV2 component5 =
        createComponent("eee", "component5", getDependencyTypeWithNulls());

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4, component5)));
    when(developmentPrioritiesReportService.getPolicyThreatsNoAuth(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10)
            .getResults();

    assertThat(results).hasSize(5);
    assertThat(results.get(0).getDependencyType()).isEqualTo("Unknown");
    assertThat(results.get(1).getDependencyType()).isEqualTo("Transitive");
    assertThat(results.get(2).getDependencyType()).isEqualTo("Inner Source");
    assertThat(results.get(3).getDependencyType()).isEqualTo("Direct");
    assertThat(results.get(4).getDependencyType()).isEqualTo("Transitive");

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldSortCorrectlyAccordingToAllPrioritizationDataPoints() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithFailActions("reachable-component-with-fail-action", "SECURITY");

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponents =
        generateComponentAtEachThreatLevelWithFailActions("component-with-fail-action", "not-security");

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithWarnActions("component-with-warn-action", "not-security");

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithWarnActions("reachable-component-with-warn-action", "SECURITY");

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponents =
        generateComponentAtEachThreatLevelWitNoActions("component-with-no-action", "not-security");

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWitNoActions("reachable-component-with-no-action", "SECURITY");

    final List<ApiReportComponentDTOV2> bomComponents = new ArrayList<>();
    bomComponents.addAll(failingComponents.getA());
    bomComponents.addAll(warningComponents.getA());
    bomComponents.addAll(noActionComponents.getA());
    bomComponents.addAll(failingComponentsWithSecurityReachable.getA());
    bomComponents.addAll(warningComponentsWithSecurityReachable.getA());
    bomComponents.addAll(noActionComponentsWithSecurityReachable.getA());

    Collections.shuffle(bomComponents);

    final List<PolicyThreats.Component> policyThreatComponents = new ArrayList<>();
    policyThreatComponents.addAll(failingComponents.getB());
    policyThreatComponents.addAll(warningComponents.getB());
    policyThreatComponents.addAll(noActionComponents.getB());
    policyThreatComponents.addAll(failingComponentsWithSecurityReachable.getB());
    policyThreatComponents.addAll(warningComponentsWithSecurityReachable.getB());
    policyThreatComponents.addAll(noActionComponentsWithSecurityReachable.getB());

    Collections.shuffle(policyThreatComponents);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(bomComponents));
    when(developmentPrioritiesReportService.getPolicyThreatsNoAuth(anyString(), anyString())).thenReturn(
        createPolicyThreats(policyThreatComponents));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));
    when(componentLabelService.getComponentLabelsNoAuth(any(OwnerType.class), anyString(), anyString()))
        .thenReturn(getAppliedLabelsForSecurityReachable());

    // === Then ===
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, 66)
            .getResults();

    assertThat(results).hasSize(66);

    // first 11 should be reachable and failings actions with descending threat levels
    int offset = 0;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = results.get(i);

      final String expectedComponentDisplayName = "reachable-component-with-fail-action" + (10 - i - offset);

      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 1);
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("fail");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isTrue();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(10 - i - offset);
    }

    // first 11 should be fail actions that are not reachable with descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = results.get(i);

      final String expectedComponentDisplayName = "component-with-fail-action" + (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 1);
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("fail");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isTrue();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isEqualTo(false);
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(10 - (i - offset));
    }

    // next 11 should be reachable warn actions with descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = results.get(i);

      final String expectedComponentDisplayName = "reachable-component-with-warn-action" + (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 1);
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("warn");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(10 - (i - offset));
    }

    // next 11 should be warn actions with descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = results.get(i);

      final String expectedComponentDisplayName = "component-with-warn-action" + (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 1);
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("warn");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isFalse();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(10 - (i - offset));
    }

    // next 11 should be reachable no actions with descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = results.get(i);

      final String expectedComponentDisplayName = "reachable-component-with-no-action" + (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 1);
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("none");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(10 - (i - offset));
    }

    // next 11 should be no actions with descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = results.get(i);

      final String expectedComponentDisplayName = "component-with-no-action" + (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 1);
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("none");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isEqualTo(false);
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isEqualTo(false);
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(10 - (i - offset));
    }

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldNotQueryCallflowWhenThereAreNoSecurityViolations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> components =
        generateComponentAtEachThreatLevelWithFailActions("component-with-fail-action", "not-security");

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(components.getA()));
    when(developmentPrioritiesReportService.getPolicyThreatsNoAuth(anyString(), anyString())).thenReturn(
        createPolicyThreats(components.getB()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, 66)
            .getResults();

    verify(componentLabelService, never()).getComponentLabelsNoAuth(any(OwnerType.class), anyString(), anyString());

    assertThat(results).hasSize(11);
    results.forEach(result -> {
      assertThat(result.isSecurityReachable()).isFalse();
    });
  }

  @Test
  public void testGetPrioritizedFindings_shouldNotQueryCallflowWhenThereArSecurityViolations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> components =
        generateComponentAtEachThreatLevelWithFailActions("component-with-fail-action", "SECURITY");

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(components.getA()));
    when(developmentPrioritiesReportService.getPolicyThreatsNoAuth(anyString(), anyString())).thenReturn(
        createPolicyThreats(components.getB()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));
    when(componentLabelService.getComponentLabelsNoAuth(any(OwnerType.class), anyString(), anyString()))
        .thenReturn(getAppliedLabelsForSecurityReachable());

    // === Then ===
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, 66)
            .getResults();

    verify(componentLabelService, times(11)).getComponentLabelsNoAuth(any(OwnerType.class), anyString(), anyString());

    assertThat(results).hasSize(11);
    results.stream().forEach(result -> {
      assertThat(result.isSecurityReachable()).isTrue();
    });
  }

  @Test
  public void testGetPrioritizedFindings_shouldReuseTheSamePriorityWhenTheyHaveTheSameScore() {
    // === GIVEN ===
    // will be middle priority with component2 (priority 2)
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Lists.newArrayList(createPolicyViolation(7, "a", "policy-a")));

    // will be middle priority with component1 (priority 2)
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component1");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        Lists.newArrayList(createPolicyViolation(7, "b", "policy-b")));

    // will be the highest priority (priority 1)
    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component1");
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        Lists.newArrayList(createPolicyViolation(9, "c", "policy-c")));

    // will be the lowest priority (3)
    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        Lists.newArrayList(createPolicyViolation(2, "d", "policy-d")));

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4)));
    when(developmentPrioritiesReportService.getPolicyThreatsNoAuth(anyString(), anyString())).thenReturn(
        createPolicyThreats(
            Lists.newArrayList(component1Threats, component2Threats, component3Threats, component4Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === THEN ===
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10)
            .getResults();

    assertThat(results).hasSize(4);

    assertThat(results.get(0).getComponentHash()).isEqualTo("ccc");
    assertThat(results.get(0).getPriority()).isEqualTo(1);

    assertThat(results.get(1).getComponentHash()).isEqualTo("aaa");
    assertThat(results.get(1).getPriority()).isEqualTo(2);

    assertThat(results.get(2).getComponentHash()).isEqualTo("bbb");
    assertThat(results.get(2).getPriority()).isEqualTo(2);

    assertThat(results.get(3).getComponentHash()).isEqualTo("ddd");
    assertThat(results.get(3).getPriority()).isEqualTo(3);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldCorrectlyCombineBomAndPolicyThreatsIntoSingleResponse() {
    // === Given ====
    final String someHash = tempEntity.newRandomHash();
    final String someDisplayName = TemporaryEntity.uuid();
    final String someConstraintName = TemporaryEntity.uuid();

    final Map<String, String > someCoordinate = new HashMap<>();
    someCoordinate.put("extension", "jar");
    someCoordinate.put("groupId", "org.someplace");
    someCoordinate.put("artifactId", TemporaryEntity.uuid());
    someCoordinate.put("version", "7.2.3");
    final ComponentIdentifier someComponentIdentifier = new ComponentIdentifier("maven", someCoordinate);

    final ApiReportComponentDTOV2 component1 = createComponent(
        someHash,
        someDisplayName,
        getDirectDependencyType(),
        someComponentIdentifier
    );

    final PolicyAction policyAction = new PolicyAction();
    policyAction.actionType = "fail";

    final PolicyConstraint constraint = new PolicyConstraint();
    constraint.constraintName = someConstraintName;

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Lists.newArrayList(
            createPolicyViolation(
                7,
                "a",
                "policy-a",
                Lists.newArrayList(policyAction),
                Lists.newArrayList(constraint),
                "some-category"),
            // at least one component with a security violation,
            // so that we check reachable (it does not have to be highest)
            createPolicyViolation(
                2,
                "b",
                "policy-b",
                Lists.newArrayList(),
                Lists.newArrayList(),
                "SECURITY")));

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1)));
    when(developmentPrioritiesReportService.getPolicyThreatsNoAuth(anyString(), anyString())).thenReturn(
        createPolicyThreats(
            Lists.newArrayList(component1Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));
    when(componentLabelService.getComponentLabelsNoAuth(any(OwnerType.class), anyString(), anyString()))
        .thenReturn(getAppliedLabelsForSecurityReachable());

    // === THEN ===
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10)
            .getResults();

    final ComponentIdentifier actualComponentIdentifier = results.get(0).getComponentIdentifier();
    assertThat(actualComponentIdentifier).isEqualTo(someComponentIdentifier);

    final PrioritizedComponent actualComponent = results.get(0);
    assertThat(actualComponent.getDisplayName()).isEqualTo(someDisplayName);
    assertThat(actualComponent.getComponentHash()).isEqualTo(someHash);
    assertThat(actualComponent.getHighestThreatPolicyName()).isEqualTo("policy-a");
    assertThat(actualComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(actualComponent.getHighestThreat()).isEqualTo(7);
    assertThat(actualComponent.getHighestThreatPolicyConstraintName()).isEqualTo(someConstraintName);
    assertThat(actualComponent.isSecurityReachable()).isTrue();
  }

  @Test
  public void testGetPrioritizedFindings_shouldCorrectlyPaginateResults() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponents =
        generateComponentAtEachThreatLevelWithFailActions("component-with-fail-action", "not-security");

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithWarnActions("component-with-warn-action", "not-security");

    final List<ApiReportComponentDTOV2> bomComponents = new ArrayList<>();
    bomComponents.addAll(failingComponents.getA());
    bomComponents.addAll(warningComponents.getA());

    final List<PolicyThreats.Component> policyThreatComponents = new ArrayList<>();
    policyThreatComponents.addAll(failingComponents.getB());
    policyThreatComponents.addAll(warningComponents.getB());

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(bomComponents));
    when(developmentPrioritiesReportService.getPolicyThreatsNoAuth(anyString(), anyString())).thenReturn(
        createPolicyThreats(policyThreatComponents));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<String> expectedHashesInOrder = new ArrayList<>();
    Collections.reverse(failingComponents.getA());
    Collections.reverse(warningComponents.getA());
    expectedHashesInOrder.addAll(failingComponents.getA().stream().map(comp -> comp.hash).collect(Collectors.toList()));
    expectedHashesInOrder.addAll(warningComponents.getA().stream().map(comp -> comp.hash).collect(Collectors.toList()));

    // check first page contains priorities 1-10 and the first 10 hashes
    ApiPageResult<PrioritizedComponent> apiPageResult = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10);
    List<PrioritizedComponent> results = apiPageResult.getResults();

    assertThat(apiPageResult.getPage()).isEqualTo(1);
    assertThat(apiPageResult.getPageSize()).isEqualTo(10);
    assertThat(apiPageResult.getPageCount()).isEqualTo(3);
    assertThat(apiPageResult.getTotal()).isEqualTo(22);
    assertThat(results).hasSize(10);
    int priorityOffset = 1;
    for (int i = 0; i < results.size(); i++) {
      final PrioritizedComponent actualComponent = results.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i));
    }

    // check second page contains priorities 11-20 and the next 10 hashes
    apiPageResult =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 2, GIVEN_PAGE_SIZE_10);
    results = apiPageResult.getResults();

    assertThat(apiPageResult.getPage()).isEqualTo(2);
    assertThat(apiPageResult.getPageSize()).isEqualTo(10);
    assertThat(apiPageResult.getPageCount()).isEqualTo(3);
    assertThat(apiPageResult.getTotal()).isEqualTo(22);
    assertThat(results).hasSize(10);
    priorityOffset = 11;
    for (int i = 0; i < results.size(); i++) {
      final PrioritizedComponent actualComponent = results.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i + 10));
    }

    // check last page contains priorities 20-22 and the final 2 hashes
    apiPageResult =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 3, GIVEN_PAGE_SIZE_10);
    results = apiPageResult.getResults();

    assertThat(apiPageResult.getPage()).isEqualTo(3);
    assertThat(apiPageResult.getPageSize()).isEqualTo(10);
    assertThat(apiPageResult.getPageCount()).isEqualTo(3);
    assertThat(apiPageResult.getTotal()).isEqualTo(22);
    assertThat(results).hasSize(2);
    priorityOffset = 21;
    for (int i = 0; i < results.size(); i++) {
      final PrioritizedComponent actualComponent = results.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i + 20));
    }

    // should return empty list if requesting a page past the end
    assertThat(apiPageResult.getPage()).isEqualTo(3); // caps page number
    assertThat(apiPageResult.getPageSize()).isEqualTo(10);
    assertThat(apiPageResult.getPageCount()).isEqualTo(3);
    assertThat(apiPageResult.getTotal()).isEqualTo(22);
    apiPageResult =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 4, GIVEN_PAGE_SIZE_10);
    results = apiPageResult.getResults();

    assertThat(results).isEmpty();

    // check first page contains priorities 1-5 and the first 5 hashes
    apiPageResult =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 1, 5);
    results = apiPageResult.getResults();

    assertThat(apiPageResult.getPage()).isEqualTo(1);
    assertThat(apiPageResult.getPageSize()).isEqualTo(5);
    assertThat(apiPageResult.getPageCount()).isEqualTo(5);
    assertThat(apiPageResult.getTotal()).isEqualTo(22);
    assertThat(results).hasSize(5);
    priorityOffset = 1;
    for (int i = 0; i < results.size(); i++) {
      final PrioritizedComponent actualComponent = results.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i));
    }

    // check second page contains priorities 6-10 and the next 5 hashes
    apiPageResult =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 2, 5);
    results = apiPageResult.getResults();

    assertThat(apiPageResult.getPage()).isEqualTo(2);
    assertThat(apiPageResult.getPageSize()).isEqualTo(5);
    assertThat(apiPageResult.getPageCount()).isEqualTo(5);
    assertThat(apiPageResult.getTotal()).isEqualTo(22);
    assertThat(results).hasSize(5);
    priorityOffset = 6;
    for (int i = 0; i < results.size(); i++) {
      final PrioritizedComponent actualComponent = results.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i + 5));
    }
  }

  private ApiReportRawDataDTOV2 createApiReportRawDataDTOV2(final List<ApiReportComponentDTOV2> components) {
    final ApiReportRawDataDTOV2 apiReportRawDataDTOV2 = new ApiReportRawDataDTOV2();
    apiReportRawDataDTOV2.components = components;

    return apiReportRawDataDTOV2;
  }

  private ApiReportComponentDTOV2 createComponent(final String hash, final String artifactId) {
    return createComponent(hash, artifactId, null);
  }

  private ApiReportComponentDTOV2 createComponent(
      final String hash,
      final String artifactId,
      final ApiDependencyDataDTO dependencyDataDTO
  )
  {
    final Map<String, String > coordinate = new HashMap<>();
    coordinate.put("extension", "jar");
    coordinate.put("groupId", "com.sonatype");
    coordinate.put("artifactId", artifactId);
    coordinate.put("version", "1.1.1");

    return createComponent(hash, artifactId, dependencyDataDTO, new ComponentIdentifier("maven", coordinate));
  }

  private ApiReportComponentDTOV2 createComponent(
      final String hash,
      final String displayName,
      final ApiDependencyDataDTO dependencyDataDTO,
      final ComponentIdentifier componentIdentifier
  )
  {
    final ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.hash = hash;
    component.displayName = displayName;
    component.dependencyData = dependencyDataDTO;

    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);

    return component;
  }

  private PolicyThreats createPolicyThreats(final List<PolicyThreats.Component> components) {
    final PolicyThreats policyThreats = new PolicyThreats();
    policyThreats.aaData.addAll(components);

    return  policyThreats;
  }

  private PolicyThreats.Component createPolicyThreatsComponents(
      final ApiReportComponentDTOV2 fromComponent,
      final List<PolicyThreats.PolicyViolation> activePolicyViolations
  )
  {
    return createPolicyThreatsComponents(fromComponent, activePolicyViolations, Lists.newArrayList());
  }

  private PolicyThreats.Component createPolicyThreatsComponents(
      final ApiReportComponentDTOV2 fromComponent,
      final List<PolicyThreats.PolicyViolation> activePolicyViolations,
      final List<PolicyThreats.PolicyViolation> inactivePolicyViolations
  )
  {
    final PolicyThreats.Component component = new PolicyThreats.Component();
    component.hash = fromComponent.hash;
    component.componentIdentifier = fromComponent.componentIdentifier.toComponentIdentifier();
    component.activeViolations.addAll(activePolicyViolations);
    component.allViolations.addAll(inactivePolicyViolations);

    return component;
  }

  private PolicyViolation createPolicyViolation(
      final int threatLevel,
      final String policyViolationId,
      final String policyName
  )
  {
    return createPolicyViolation(threatLevel, policyViolationId, policyName, Lists.newArrayList());
  }

  private PolicyViolation createPolicyViolation(
      final int threatLevel,
      final String policyViolationId,
      final String policyName,
      final List<PolicyAction> policyActions
  )
  {
    return createPolicyViolation(
        threatLevel,
        policyViolationId,
        policyName,
        policyActions,
        Lists.newArrayList(),
        "some-category");
  }

  private PolicyViolation createPolicyViolation(
      final int threatLevel,
      final String policyViolationId,
      final String policyName,
      final List<PolicyAction> policyActions,
      final List<PolicyConstraint> constraints,
      final String policyThreatCategory
  )
  {
    final PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.policyThreatLevel = threatLevel;
    policyViolation.policyViolationId = policyViolationId;
    policyViolation.actions = policyActions;
    policyViolation.constraints = constraints;
    policyViolation.policyName = policyName;
    policyViolation.policyThreatCategory = policyThreatCategory;

    policyViolation.policyId = "some-policy-id";

    return policyViolation;
  }

  private PrioritizedComponent toPrioritizedComponent(
      final ApiReportComponentDTOV2 component,
      final int highestThreat,
      final String highestPolicyName,
      final String highestThreatPolicyConstraintName,
      final int priority
  )
  {
    return toPrioritizedComponent(
        component,
        highestThreat,
        highestPolicyName,
        priority,
        "Unknown",
        false,
        highestThreatPolicyConstraintName,
        "none",
        false
    );
  }

  private PrioritizedComponent toPrioritizedComponent(
      final ApiReportComponentDTOV2 component,
      final int highestThreat,
      final String highestThreatPolicyName,
      final int priority,
      final String dependencyType,
      final boolean hasFailActionComponent,
      final String highestThreatPolicyConstraintName,
      final String action,
      final boolean securityReachable
  )
  {
    return new PrioritizedComponent(
        component.displayName,
        component.componentIdentifier.toComponentIdentifier(),
        component.hash,
        dependencyType,
        hasFailActionComponent,
        action,
        highestThreat,
        highestThreatPolicyName,
        highestThreatPolicyConstraintName,
        securityReachable,
        priority
    );
  }

  private ApiDependencyDataDTO getDirectDependencyType() {
    final ApiDependencyDataDTO dependencyDataDTO = new ApiDependencyDataDTO();
    dependencyDataDTO.directDependency = true;

    return dependencyDataDTO;
  }

  private ApiDependencyDataDTO getInnerSourceDependencyType() {
    final ApiDependencyDataDTO dependencyDataDTO = new ApiDependencyDataDTO();
    dependencyDataDTO.innerSource = true;

    return dependencyDataDTO;
  }

  private ApiDependencyDataDTO getTransitiveDependencyType() {
    final ApiDependencyDataDTO dependencyDataDTO = new ApiDependencyDataDTO();
    dependencyDataDTO.innerSource = false;
    dependencyDataDTO.directDependency = false;

    return dependencyDataDTO;
  }

  private void verifyServiceCallsInvokedWithExpectedArguments() {
    verify(developmentPrioritiesReportService).getDependencyInformation(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);
    verify(developmentPrioritiesReportService).getPolicyThreatsNoAuth(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWithWarnActions(
      final String componentBaseName,
      final String policyThreatCategory
  )
  {
    return generateComponentAtEachThreatLevelWithAction(componentBaseName, "warn", policyThreatCategory);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWithFailActions(
      final String componentBaseName,
      final String policyThreatCategory
  )
  {
    return generateComponentAtEachThreatLevelWithAction(componentBaseName, "fail", policyThreatCategory);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWithAction(
      final String componentBaseName,
      final String action,
      final String policyThreatCategory
  )
  {
    final List<ApiReportComponentDTOV2> bomComponents = new ArrayList<>();
    final List<PolicyThreats.Component> policyThreatComponents = new ArrayList<>();

    for (int i = 0; i < 11; i++) {
      final PolicyAction policyAction = new PolicyAction();
      policyAction.actionType = action;
      final PolicyViolation policyViolation = createPolicyViolation(
          i,
          TemporaryEntity.uuid(),
          TemporaryEntity.uuid(),
          Lists.newArrayList(policyAction),
          Lists.newArrayList(),
          policyThreatCategory);

      final ApiReportComponentDTOV2 component = createComponent(TemporaryEntity.uuid(), componentBaseName + i);

      final PolicyThreats.Component componentThreats;

      componentThreats = createPolicyThreatsComponents(
          component,
          Lists.newArrayList(policyViolation));

      bomComponents.add(component);
      policyThreatComponents.add(componentThreats);
    }

    return new Pair<>(bomComponents, policyThreatComponents);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWitNoActions(
      final String componentBaseName,
      final String policyThreatCategory
  )
  {
    final List<ApiReportComponentDTOV2> bomComponents = new ArrayList<>();
    final List<PolicyThreats.Component> policyThreatComponents = new ArrayList<>();

    for (int i = 0; i < 11; i++) {
      final ApiReportComponentDTOV2 component = createComponent(TemporaryEntity.uuid(), componentBaseName + i);
      final PolicyThreats.Component componentThreats = createPolicyThreatsComponents(
          component,
          Lists.newArrayList(
              createPolicyViolation(
                  i,
                  TemporaryEntity.uuid(),
                  "policy-name" + TemporaryEntity.uuid(),
                  Lists.newArrayList(),
                  Lists.newArrayList(),
                  policyThreatCategory)));

      bomComponents.add(component);
      policyThreatComponents.add(componentThreats);
    }

    return new Pair<>(bomComponents, policyThreatComponents);
  }

  private ApiDependencyDataDTO getDependencyTypeWithNulls() {
    return new ApiDependencyDataDTO();
  }

  private AppliedLabels getAppliedLabelsForSecurityReachable() {
    final Label label = new Label();
    label.setLabel(SECURITY_REACHABLE_LABEL);
    final LabelsByOwner labelsByOwner = new LabelsByOwner();
    labelsByOwner.labels = Lists.newArrayList(label);

    final AppliedLabels appliedLabels = new AppliedLabels();
    appliedLabels.labelsByOwner.add(labelsByOwner);

    return appliedLabels;
  }
}
