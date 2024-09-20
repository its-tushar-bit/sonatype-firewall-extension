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
import javax.inject.Inject;

import com.google.inject.Binder;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.experimental.development.prioritization.PrioritizedComponent;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.callflow.ComponentReachabilityService;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationComponentInfoDAO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.label.ComponentLabelService.AppliedLabels;
import com.sonatype.insight.brain.label.ComponentLabelService.LabelsByOwner;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.Component;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyAction;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyConstraint;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyViolation;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import oshi.util.tuples.Pair;

import static com.sonatype.insight.brain.api.experimental.ApiVulnerabilitySignatureService.SECURITY_REACHABLE_LABEL;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS;
import static com.sonatype.insight.license.model.LicensedFeature.DEVELOPER_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  private ReportService reportService;

  @Inject
  private ComponentReachabilityService componentReachabilityService;

  @Inject
  private DevelopmentPrioritizationComponentInfoDAO prioritizationComponentInfoDAO;

  private DevelopmentPrioritiesService developmentPrioritiesService;

  private String prioritizationId;

  @Before
  public void setup() {
    developmentPrioritiesService = new DevelopmentPrioritiesService(
        featuresService, developmentPrioritiesReportService, prioritizationComponentInfoDAO, reportService,
        componentReachabilityService);
    prioritizationId = tempEntity.newDevelopmentPrioritization(GIVEN_SOME_SCAN_ID).getId();
    tempEntity.newApplicationWithParent(GIVEN_SOME_PUBLIC_APP_ID);
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(FeaturesService.class).toInstance(featuresService);
    binder.bind(DevelopmentPrioritiesReportService.class).toInstance(developmentPrioritiesReportService);
    binder.bind(ReportService.class).toInstance(reportService);
    super.configure(binder);
  }

  @Test
  public void testGetPrioritizedFindings_shouldThrowAppropriateErrorIfDevelopmentNotEnabled() {
    assertThatThrownBy(() ->
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10))
        .withFailMessage("This server is not licensed for Sonatype Developer.")
        .isInstanceOf(NotAuthorizedException.class);
  }

  @Test
  public void testGetPrioritizedFindings_shouldExtractTheHighestThreatPolicyViolation() {
    // === GIVEN ===
    // max 6, resolves collision between multiple violations with the same threat level using policyViolationOrder
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final List<PolicyViolation> component1Violations = Lists.newArrayList(
        createPolicyViolation(2, "a", "policy-a", false),
        createPolicyViolation(6, "b", "policy-b", false),
        createPolicyViolation(5, "c", "policy-c", false),
        createPolicyViolation(6, "d", "policy-d", false),
        createPolicyViolation(6, "e", "policy-e", false));
    Collections.shuffle(component1Violations);
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        component1Violations,
        // add a violation that's not active, it should not affect our results
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", false))
    );

    // has the highest threat level of all the components
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final List<PolicyViolation> component2Violations = Lists.newArrayList(
        createPolicyViolation(10, "f", "policy-f", false),
        createPolicyViolation(7, "g", "policy-g", false));
    Collections.shuffle(component2Violations);
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(component2, component2Violations);

    // no violations
    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component3");

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats, component2Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final DevelopmentPrioritizationResults results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10);

    assertThat(results.getTopPriorities()).containsExactlyElementsOf(
        Lists.newArrayList(
            toPrioritizedComponent(component2, 10, "policy-f", null, 1),
            toPrioritizedComponent(component1, 6, "policy-b", null, 2)));

    // should be no additional priorities, everything is in top 3
    assertPaginationResultCorrect(results.getAdditionalPriorities(), 0, 0, 0, 1);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldExtractTheNonLegacyHighestThreatPolicyViolation() {
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final List<PolicyViolation> component1Violations = Lists.newArrayList(
        createPolicyViolation(2, "a", "policy-a", false),
        makeLegacy(createPolicyViolation(6, "b", "policy-b", false)),
        createPolicyViolation(5, "c", "policy-c", false),
        makeLegacy(createPolicyViolation(6, "d", "policy-d", false)),
        makeLegacy(createPolicyViolation(6, "e", "policy-e", false)));
    Collections.shuffle(component1Violations);
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        component1Violations,
        // add a violation that's not active, it should not affect our results
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", false))
    );

    // has the highest threat level of all the components
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final List<PolicyViolation> component2Violations = Lists.newArrayList(
        makeLegacy(createPolicyViolation(10, "f", "policy-f", false)),
        createPolicyViolation(7, "g", "policy-g", false));
    Collections.shuffle(component2Violations);
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(component2, component2Violations);

    // no violations
    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component3");

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats, component2Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final DevelopmentPrioritizationResults results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10);

    assertThat(results.getTopPriorities()).containsExactlyInAnyOrder(
        // "policy-f" of threat level 10 is a legacy violation, so not in the priority list.
        toPrioritizedComponent(component2, 7, "policy-g", null, 1),
        // "policy-b,d,e" of threat level 6 are a legacy violations, so not in the priority list.
        toPrioritizedComponent(component1, 5, "policy-c", null, 2)
    );

    // should be no additional priorities, everything is in top 3
    assertPaginationResultCorrect(results.getAdditionalPriorities(), 0, 0, 0, 1);

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

    final List<PolicyViolation> component1Violations =
        Collections.singletonList(createPolicyViolation(1, "a", "policy-a", false));
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(component1, component1Violations);
    final List<PolicyViolation> component2Violations =
        Collections.singletonList(createPolicyViolation(1, "b", "policy-b", false));
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(component2, component2Violations);
    final List<PolicyViolation> component3Violations =
        Collections.singletonList(createPolicyViolation(1, "c", "policy-c", false));
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(component3, component3Violations);
    final List<PolicyViolation> component4Violations =
        Collections.singletonList(createPolicyViolation(1, "d", "policy-d", false));
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(component4, component4Violations);
    final List<PolicyViolation> component5Violations =
        Collections.singletonList(createPolicyViolation(1, "e", "policy-e", false));
    final PolicyThreats.Component component5Threats = createPolicyThreatsComponents(component5, component5Violations);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4, component5)));
    when(reportService.getPolicyThreats(anyString(), anyString()))
        .thenReturn(createPolicyThreats(Lists.newArrayList(component1Threats, component2Threats,
            component3Threats, component4Threats, component5Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final DevelopmentPrioritizationResults results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10);

    final List<PrioritizedComponent> top3 = results.getTopPriorities();
    assertThat(top3).hasSize(3);
    assertThat(top3.get(0).getDependencyType()).isEqualTo("Unknown");
    assertThat(top3.get(1).getDependencyType()).isEqualTo("Transitive");
    assertThat(top3.get(2).getDependencyType()).isEqualTo("Inner Source");

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 2, 2, 1, 1);
    final List<PrioritizedComponent> additionalPriorities = results.getAdditionalPriorities().getResults();
    assertThat(additionalPriorities.get(0).getDependencyType()).isEqualTo("Direct");
    assertThat(additionalPriorities.get(1).getDependencyType()).isEqualTo("Transitive");

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldSortCorrectlyWithAllPrioritizationCriteria_WithoutBulkRecommendations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithFailActions("reachable-component-with-fail-action", "SECURITY", true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponents =
        generateComponentAtEachThreatLevelWithFailActions("component-with-fail-action", "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithWarnActions("component-with-warn-action", "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithWarnActions("reachable-component-with-warn-action", "SECURITY", true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponents =
        generateComponentAtEachThreatLevelWitNoActions("component-with-no-action", "not-security", false, false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWitNoActions("reachable-component-with-no-action", "SECURITY", false, true);

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
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(policyThreatComponents));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final DevelopmentPrioritizationResults results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, 66);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 63, 63, 1, 1);

    final List<PrioritizedComponent> actualTop3 = results.getTopPriorities();
    final List<PrioritizedComponent> actualAdditionalPriorities = results.getAdditionalPriorities().getResults();

    // top 3 should be reachable and failings actions with descending threat levels
    assertThat(actualTop3).hasSize(3);
    for (int i = 0; i < actualTop3.size(); i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualTop3.get(i);

      final String expectedComponentDisplayName = "reachable-component-with-fail-action" + (10 - i);

      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 1);
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("fail");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isTrue();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - i);
    }

    // first 7 additional should be reachable and failing actions with descending threat levels
    int offset = 0;
    for (int i = offset; i < offset + 8; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final int expectedThreat = 11 - i - offset - 3;
      final String expectedComponentDisplayName = "reachable-component-with-fail-action" + (expectedThreat - 1);

      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add for to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("fail");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isTrue();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(expectedThreat);
    }

    // next 10 should be fail actions that are not reachable with descending threat levels
    offset = 8;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "component-with-fail-action" + (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("fail");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isTrue();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isFalse();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
    }

    // next 10 should be reachable warn actions with descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "reachable-component-with-warn-action" + (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("warn");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
    }

    // next 10 should be warn actions with descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "component-with-warn-action" + (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("warn");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isFalse();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
    }

    // next 11 should be reachable no actions with descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "reachable-component-with-no-action" + (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("none");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
    }

    // next 11 should be no actions with descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "component-with-no-action" + (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4);  // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("none");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isFalse();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
    }

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldSortCorrectlyWithAllPrioritizationCriteria_WithBulkRecommendations() {
    // === Given (in expected order of priority) ===
    // FAIL ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            "reachable-component-with-fail-action-with-recommendation", "fail", "SECURITY", true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachableNoRecommendation =
        generateComponentAtEachThreatLevelWithFailActions(
            "reachable-component-with-fail-action-no-recommendation", "SECURITY", true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponents =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            "non-reachable-component-with-fail-action-with-recommendation", "fail", "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWithFailActions(
            "non-reachable-component-with-fail-action-no-recommendation", "not-security", false);

    // WARN ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            "reachable-component-with-warn-action-with-recommendation", "warn", "SECURITY", true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachableNoRecommendations =
        generateComponentAtEachThreatLevelWithWarnActions("reachable-component-with-warn-action-no-recommendation",
            "SECURITY", true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            "non-reachable-component-with-warn-action-with-recommendation", "warn", "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWithWarnActions("non-reachable-component-with-warn-action-no-recommendation",
            "not-security", false);

    // NONE ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWitNoActions("reachable-component-with-no-action-with-recommendation",
            "SECURITY", true, true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachableNoRecommendations
        = generateComponentAtEachThreatLevelWitNoActions("reachable-component-with-no-action-no-recommendation",
        "SECURITY", false, true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponents =
        generateComponentAtEachThreatLevelWitNoActions("non-reachable-component-with-no-action-with-recommendation",
            "not-security", true, false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWitNoActions("non-reachable-component-with-no-action-no-recommendation",
            "not-security", false, false);

    final List<ApiReportComponentDTOV2> bomComponents = new ArrayList<>();
    bomComponents.addAll(failingComponents.getA());
    bomComponents.addAll(warningComponents.getA());
    bomComponents.addAll(noActionComponents.getA());
    bomComponents.addAll(failingComponentsWithSecurityReachable.getA());
    bomComponents.addAll(warningComponentsWithSecurityReachable.getA());
    bomComponents.addAll(noActionComponentsWithSecurityReachable.getA());
    bomComponents.addAll(failingComponentsWithSecurityReachableNoRecommendation.getA());
    bomComponents.addAll(warningComponentsWithSecurityReachableNoRecommendations.getA());
    bomComponents.addAll(noActionComponentsWithSecurityReachableNoRecommendations.getA());
    bomComponents.addAll(failingComponentsWithNoRecommendations.getA());
    bomComponents.addAll(warningComponentsWithNoRecommendations.getA());
    bomComponents.addAll(noActionComponentsWithNoRecommendations.getA());

    Collections.shuffle(bomComponents);

    final List<PolicyThreats.Component> policyThreatComponents = new ArrayList<>();
    policyThreatComponents.addAll(failingComponents.getB());
    policyThreatComponents.addAll(warningComponents.getB());
    policyThreatComponents.addAll(noActionComponents.getB());
    policyThreatComponents.addAll(failingComponentsWithSecurityReachable.getB());
    policyThreatComponents.addAll(warningComponentsWithSecurityReachable.getB());
    policyThreatComponents.addAll(noActionComponentsWithSecurityReachable.getB());
    policyThreatComponents.addAll(failingComponentsWithSecurityReachableNoRecommendation.getB());
    policyThreatComponents.addAll(warningComponentsWithSecurityReachableNoRecommendations.getB());
    policyThreatComponents.addAll(noActionComponentsWithSecurityReachableNoRecommendations.getB());
    policyThreatComponents.addAll(failingComponentsWithNoRecommendations.getB());
    policyThreatComponents.addAll(warningComponentsWithNoRecommendations.getB());
    policyThreatComponents.addAll(noActionComponentsWithNoRecommendations.getB());

    Collections.shuffle(policyThreatComponents);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(bomComponents));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(policyThreatComponents));
    when(featuresService.getFeatures())
        .thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD, DEVELOPER_BULK_RECOMMENDATIONS));

    // === Then ===
    final DevelopmentPrioritizationResults results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, 129);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 129, 129, 1, 1);

    final List<PrioritizedComponent> actualTop3 = results.getTopPriorities();
    final List<PrioritizedComponent> actualAdditionalPriorities = results.getAdditionalPriorities().getResults();

    // top 3 should be reachable and failing actions with descending threat levels, and have recommendations
    assertThat(actualTop3).hasSize(3);
    for (int i = 0; i < actualTop3.size(); i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualTop3.get(i);

      final String expectedComponentDisplayName = "reachable-component-with-fail-action-with-recommendation" + (10 - i);

      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 1);
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("fail");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isTrue();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - i);
      // Currently the details of the recommendation do not matter to the prioritization
      assertThat(actualPrioritizedComponent.getRemediationType()).isNotNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNotNull();
    }

    // first 8 additional should be reachable and failing actions with recommendations and descending threat levels
    int offset = 0;
    for (int i = offset; i < offset + 8; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final int expectedThreat = 11 - i - offset - 3;
      final String expectedComponentDisplayName = "reachable-component-with-fail-action-with-recommendation" +
          (expectedThreat - 1);

      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add for to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("fail");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isTrue();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(expectedThreat);
      // Currently the details of the recommendation do not matter to the prioritization
      assertThat(actualPrioritizedComponent.getRemediationType()).isNotNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNotNull();
    }

    // next 11 should be fail actions that are reachable with no recommendations and descending threat levels
    offset = 8;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "reachable-component-with-fail-action-no-recommendation" +
          (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("fail");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isTrue();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
      assertThat(actualPrioritizedComponent.getRemediationType()).isNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNull();
    }

    // next 11 should be fail actions that are not reachable with recommendations and descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "non-reachable-component-with-fail-action-with-recommendation" +
          (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("fail");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isTrue();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isFalse();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
      // Currently the details of the recommendation do not matter to the prioritization
      assertThat(actualPrioritizedComponent.getRemediationType()).isNotNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNotNull();
    }

    // next 11 should be fail actions that are not reachable with no recommendations and descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "non-reachable-component-with-fail-action-no-recommendation" +
          (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("fail");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isTrue();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isFalse();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
      assertThat(actualPrioritizedComponent.getRemediationType()).isNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNull();
    }

    // next 11 should be reachable warn actions with recommendations and descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "reachable-component-with-warn-action-with-recommendation" +
          (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("warn");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
      // Currently the details of the recommendation do not matter to the prioritization
      assertThat(actualPrioritizedComponent.getRemediationType()).isNotNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNotNull();
    }

    // next 11 should be reachable warn actions with no recommendations and descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "reachable-component-with-warn-action-no-recommendation" +
          (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("warn");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
      assertThat(actualPrioritizedComponent.getRemediationType()).isNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNull();
    }

    // next 11 should be warn actions that are not reachable with recommendations and descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "non-reachable-component-with-warn-action-with-recommendation" +
          (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("warn");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isFalse();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
      // Currently the details of the recommendation do not matter to the prioritization
      assertThat(actualPrioritizedComponent.getRemediationType()).isNotNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNotNull();
    }

    // next 11 should be warn actions that are not reachable with no recommendations and descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "non-reachable-component-with-warn-action-no-recommendation" +
          (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("warn");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isFalse();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
      assertThat(actualPrioritizedComponent.getRemediationType()).isNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNull();
    }

    // next 11 should be reachable no actions with recommendations and descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "reachable-component-with-no-action-with-recommendation" +
          (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4); // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("none");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
      // Currently the details of the recommendation do not matter to the prioritization
      assertThat(actualPrioritizedComponent.getRemediationType()).isNotNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNotNull();
    }

    // next 11 should be reachable no actions with no recommendations and descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "reachable-component-with-no-action-no-recommendation" +
          (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4);  // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("none");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isTrue();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
      assertThat(actualPrioritizedComponent.getRemediationType()).isNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNull();
    }

    // next 11 should be no actions that are not reachable with recommendations and descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "non-reachable-component-with-no-action-with-recommendation" +
          (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4);  // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("none");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isFalse();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
      // Currently the details of the recommendation do not matter to the prioritization
      assertThat(actualPrioritizedComponent.getRemediationType()).isNotNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNotNull();
    }

    // next 11 should be no actions that are not reachable with no recommendations and descending threat levels
    offset += 11;
    for (int i = offset; i < offset + 11; i++) {
      final PrioritizedComponent actualPrioritizedComponent = actualAdditionalPriorities.get(i);

      final String expectedComponentDisplayName = "non-reachable-component-with-no-action-no-recommendation" +
          (10 - (i - offset));
      assertThat(actualPrioritizedComponent.getDisplayName()).isEqualTo(expectedComponentDisplayName);
      assertThat(actualPrioritizedComponent.getPriority()).isEqualTo(i + 4);  // add 4 to account for top 3
      assertThat(actualPrioritizedComponent.getAction()).isEqualTo("none");
      assertThat(actualPrioritizedComponent.getHasFailActionOnComponent()).isFalse();
      assertThat(actualPrioritizedComponent.isSecurityReachable()).isFalse();
      assertThat(actualPrioritizedComponent.getHighestThreat()).isEqualTo(11 - (i - offset));
      assertThat(actualPrioritizedComponent.getRemediationType()).isNull();
      assertThat(actualPrioritizedComponent.getRemediationVersion()).isNull();
    }

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldNotQueryCallflowWhenThereAreNoSecurityViolations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> components =
        generateComponentAtEachThreatLevelWithFailActions("component-with-fail-action", "not-security", false);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(components.getA()));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(components.getB()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final DevelopmentPrioritizationResults results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, 66);

    final List<PrioritizedComponent> actualTop3 = results.getTopPriorities();
    final List<PrioritizedComponent> actualAdditionalPriorities = results.getAdditionalPriorities().getResults();

    assertThat(actualTop3)
        .hasSize(3)
        .allSatisfy(result -> assertThat(result.isSecurityReachable()).isFalse());

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 8, 8, 1, 1);

    assertThat(actualAdditionalPriorities)
        .allSatisfy(result -> assertThat(result.isSecurityReachable()).isFalse());
  }

  @Test
  public void testGetPrioritizedFindings_shouldQueryCallflowWhenThereAreSecurityViolations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> components =
        generateComponentAtEachThreatLevelWithFailActions("component-with-fail-action", "SECURITY", true);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(components.getA()));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(components.getB()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final DevelopmentPrioritizationResults results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, 66);

    final List<PrioritizedComponent> actualTop3 = results.getTopPriorities();
    final List<PrioritizedComponent> actualAdditionalProperties = results.getAdditionalPriorities().getResults();

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 8, 8, 1, 1);

    assertThat(actualTop3)
        .hasSize(3)
        .allSatisfy(result -> assertThat(result.isSecurityReachable()).isTrue());

    assertThat(actualAdditionalProperties)
        .allSatisfy(result -> assertThat(result.isSecurityReachable()).isTrue());
  }

  @Test
  public void testGetPrioritizedFindings_shouldReuseTheSamePriorityWhenTheyHaveTheSameScore() {
    // === GIVEN ===
    // will be middle priority with component2 (priority 2)
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Lists.newArrayList(createPolicyViolation(7, "a", "policy-a", false)));

    // will be middle priority with component1 (priority 2)
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component1");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        Lists.newArrayList(createPolicyViolation(7, "b", "policy-b", false)));

    // will be the highest priority (priority 1)
    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component1");
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        Lists.newArrayList(createPolicyViolation(9, "c", "policy-c", false)));

    // will be the lowest priority (3) and in additional priorities
    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        Lists.newArrayList(createPolicyViolation(2, "d", "policy-d", false)));

    // will also be the lowest priority (3) and in additional priorities
    final ApiReportComponentDTOV2 component5 = createComponent("eee", "component5");
    final PolicyThreats.Component component5Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(2, "e", "policy-e", false)));

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4, component5)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(
            component1Threats, component2Threats, component3Threats, component4Threats, component5Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === THEN ===
    DevelopmentPrioritizationResults results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10);

    final List<PrioritizedComponent> actualTop3 = results.getTopPriorities();
    List<PrioritizedComponent> actualAdditionalPriorities = results.getAdditionalPriorities().getResults();

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 2, 2, 1, 1);
    assertThat(actualAdditionalPriorities).hasSize(2);
    assertThat(actualTop3).hasSize(3);

    assertThat(actualTop3.get(0).getComponentHash()).isEqualTo("ccc");
    assertThat(actualTop3.get(0).getPriority()).isEqualTo(1);

    assertThat(actualTop3.get(1).getComponentHash()).isEqualTo("aaa");
    assertThat(actualTop3.get(1).getPriority()).isEqualTo(2);

    assertThat(actualTop3.get(2).getComponentHash()).isEqualTo("bbb");
    assertThat(actualTop3.get(2).getPriority()).isEqualTo(2);

    assertThat(actualAdditionalPriorities.get(0).getComponentHash()).isEqualTo("ddd");
    assertThat(actualAdditionalPriorities.get(0).getPriority()).isEqualTo(3);

    assertThat(actualAdditionalPriorities.get(1).getComponentHash()).isEqualTo("eee");
    assertThat(actualAdditionalPriorities.get(1).getPriority()).isEqualTo(3);

    verifyServiceCallsInvokedWithExpectedArguments();

    // === THEN - Should cary forward priority even across pagination ===
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 2, 1);
    actualAdditionalPriorities = results.getAdditionalPriorities().getResults();

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 1, 2, 2, 2);
    assertThat(actualAdditionalPriorities.get(0).getComponentHash()).isEqualTo("eee");
    assertThat(actualAdditionalPriorities.get(0).getPriority()).isEqualTo(3);
  }

  @Test
  public void testGetPrioritizedFindings_shouldFilterOutPrioritiesWithZeroThreat() {
    // === GIVEN ===
    // will be middle priority with component2 (priority 2)
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Lists.newArrayList(createPolicyViolation(7, "a", "policy-a", false)));

    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component1");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        Lists.newArrayList(createPolicyViolation(7, "b", "policy-b", false)));

    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component1");
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        Lists.newArrayList(createPolicyViolation(9, "c", "policy-c", false)));

    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        Lists.newArrayList(createPolicyViolation(2, "d", "policy-d", false)));

    final ApiReportComponentDTOV2 component5 = createComponent("eee", "component5");
    final PolicyThreats.Component component5Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(0, "e", "policy-e", false)));

    final ApiReportComponentDTOV2 component6 = createComponent("fff", "component6");
    final PolicyThreats.Component component6Threats = createPolicyThreatsComponents(
        component6,
        Lists.newArrayList(createPolicyViolation(0, "g", "policy-g", false)));

    final ApiReportComponentDTOV2 component7 = createComponent("hhh", "component7");
    final PolicyThreats.Component component7Threats = createPolicyThreatsComponents(
        component7,
        Lists.newArrayList(createPolicyViolation(0, "h", "policy-h", false)));

    final ApiReportComponentDTOV2 component8 = createComponent("iii", "component8");
    final PolicyThreats.Component component8Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(0, "i", "policy-i", false)));

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4, component5,
            component6, component7, component8)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(
            component1Threats, component2Threats, component3Threats, component4Threats, component5Threats,
            component6Threats, component7Threats, component8Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === THEN ===
    DevelopmentPrioritizationResults results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10);

    final List<PrioritizedComponent> actualTop3 = results.getTopPriorities();
    List<PrioritizedComponent> actualAdditionalPriorities = results.getAdditionalPriorities().getResults();

    assertThat(actualTop3)
        .hasSize(3)
        .allSatisfy(prioritizedComponent -> assertThat(prioritizedComponent.getHighestThreat()).isPositive());
    assertThat(actualAdditionalPriorities)
        .hasSize(1)
        .allSatisfy(prioritizedComponent -> assertThat(prioritizedComponent.getHighestThreat()).isPositive());

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
                "some-category",
                false),
            // at least one component with a security violation,
            // so that we check reachable (it does not have to be highest)
            createPolicyViolation(
                2,
                "b",
                "policy-b",
                Lists.newArrayList(),
                Lists.newArrayList(),
                "SECURITY",
                true)));

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(
            Lists.newArrayList(component1Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === THEN ===
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10)
            .getTopPriorities();

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
        generateComponentAtEachThreatLevelWithFailActions("component-with-fail-action", "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithWarnActions("component-with-warn-action", "not-security", false);

    final List<ApiReportComponentDTOV2> bomComponents = new ArrayList<>();
    bomComponents.addAll(failingComponents.getA());
    bomComponents.addAll(warningComponents.getA());

    final List<PolicyThreats.Component> policyThreatComponents = new ArrayList<>();
    policyThreatComponents.addAll(failingComponents.getB());
    policyThreatComponents.addAll(warningComponents.getB());

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(bomComponents));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(policyThreatComponents));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<String> expectedTop3HashesInOrder = new ArrayList<>();
    final List<String> expectedAdditionalHashesInOrder = new ArrayList<>();

    // the mock data was created in the ascending threat order, the sorted results returned will be in descending
    Collections.reverse(failingComponents.getA());
    Collections.reverse(warningComponents.getA());

    // add the expected top 3 components hashes
    expectedTop3HashesInOrder.addAll(
        failingComponents.getA().subList(0, 3).stream().map(comp -> comp.hash).collect(Collectors.toList()));

    // add the additional failing component hashes in order
    expectedAdditionalHashesInOrder.addAll(
        failingComponents.getA().subList(3, failingComponents.getA().size())
            .stream()
            .map(comp -> comp.hash)
            .collect(Collectors.toList()));

    // finally add the component hashes with warnings in order
    expectedAdditionalHashesInOrder.addAll(warningComponents.getA()
        .stream()
        .map(comp -> comp.hash)
        .collect(Collectors.toList())
        .subList(0, warningComponents.getA().size()));

    // check first page contains priorities 1-10 and the first 10 hashes
    DevelopmentPrioritizationResults results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10);

    // should be same regardless of page
    assertTop3Hashes(results.getTopPriorities(), expectedTop3HashesInOrder);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 10, 19, 2, 1);

    List<PrioritizedComponent> additionalPriorities = results.getAdditionalPriorities().getResults();
    int priorityOffset = 4;
    for (int i = 0; i < additionalPriorities.size(); i++) {
      final PrioritizedComponent actualComponent = additionalPriorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedAdditionalHashesInOrder.get(i));
    }

    // check second page contains priorities 11-20 and the next 10 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 2, GIVEN_PAGE_SIZE_10);

    // should be same regardless of page
    assertTop3Hashes(results.getTopPriorities(), expectedTop3HashesInOrder);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 9, 19, 2, 2);

    additionalPriorities = results.getAdditionalPriorities().getResults();
    priorityOffset = 14;
    for (int i = 0; i < additionalPriorities.size(); i++) {
      final PrioritizedComponent actualComponent = additionalPriorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedAdditionalHashesInOrder.get(i + 10));
    }

    // check last page contains priorities 20-22 and the final 2 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 3, GIVEN_PAGE_SIZE_10);

    // should be same regardless of page
    assertTop3Hashes(results.getTopPriorities(), expectedTop3HashesInOrder);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 0, 19, 2, 3);

    assertThat(results.getAdditionalPriorities().getResults()).isEmpty();

    // should return empty list if requesting a page past the end
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 4, GIVEN_PAGE_SIZE_10);
    assertPaginationResultCorrect(results.getAdditionalPriorities(), 0, 19, 2,4);

    // should be same regardless of page
    assertTop3Hashes(results.getTopPriorities(), expectedTop3HashesInOrder);

    // check first page contains priorities 1-5 and the first 5 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 1, 5);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 5, 19, 4, 1);

    // should be same regardless of page
    assertTop3Hashes(results.getTopPriorities(), expectedTop3HashesInOrder);

    additionalPriorities = results.getAdditionalPriorities().getResults();
    priorityOffset = 4;
    for (int i = 0; i < additionalPriorities.size(); i++) {
      final PrioritizedComponent actualComponent = additionalPriorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedAdditionalHashesInOrder.get(i));
    }

    // check second page contains priorities 6-10 and the next 5 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 2, 5);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 5, 19, 4, 2);

    // should be same regardless of page
    assertTop3Hashes(results.getTopPriorities(), expectedTop3HashesInOrder);

    additionalPriorities = results.getAdditionalPriorities().getResults();
    priorityOffset = 9;
    for (int i = 0; i < additionalPriorities.size(); i++) {
      final PrioritizedComponent actualComponent = additionalPriorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedAdditionalHashesInOrder.get(i + 5));
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

    return policyThreats;
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
      final String policyName,
      final boolean isSecurityReachable
  )
  {
    return createPolicyViolation(threatLevel, policyViolationId, policyName, Lists.newArrayList(), isSecurityReachable);
  }

  private PolicyViolation makeLegacy(PolicyViolation policyViolation) {
    policyViolation.legacyViolation = true;
    return policyViolation;
  }

  private PolicyViolation createPolicyViolation(
      final int threatLevel,
      final String policyViolationId,
      final String policyName,
      final List<PolicyAction> policyActions,
      final boolean isSecurityReachable
  )
  {
    return createPolicyViolation(
        threatLevel,
        policyViolationId,
        policyName,
        policyActions,
        Lists.newArrayList(),
        "some-category",
        isSecurityReachable);
  }

  private PolicyViolation createPolicyViolation(
      final int threatLevel,
      final String policyViolationId,
      final String policyName,
      final List<PolicyAction> policyActions,
      final List<PolicyConstraint> constraints,
      final String policyThreatCategory,
      final boolean isSecurityReachable
  )
  {
    final PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.policyThreatLevel = threatLevel;
    policyViolation.policyViolationId = policyViolationId;
    policyViolation.actions = policyActions;
    policyViolation.constraints = constraints;
    policyViolation.policyName = policyName;
    policyViolation.policyThreatCategory = policyThreatCategory;
    policyViolation.reachabilityStatus =
        isSecurityReachable ? ReachabilityStatus.REACHABLE : ReachabilityStatus.NON_REACHABLE;
    policyViolation.policyId = "some-policy-id";

    return policyViolation;
  }

  private PrioritizedComponent toPrioritizedComponent(
      final ApiReportComponentDTOV2 component,
      final int highestThreat,
      final String highestPolicyName,
      final String highestThreatPolicyConstraintName,
      final int priority)
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
        false,
        null
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
      final boolean securityReachable,
      final DevelopmentPrioritizationComponentInfo prioritizationComponentInfo
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
        priority,
        prioritizationComponentInfo
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
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWithWarnActions(
      final String componentBaseName,
      final String policyThreatCategory,
      final boolean isSecurityReachable
  )
  {
    return generateComponentAtEachThreatLevelWithAction(componentBaseName, "warn", policyThreatCategory, false,
        isSecurityReachable);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWithFailActions(
      final String componentBaseName,
      final String policyThreatCategory,
      final boolean isSecurityReachable
  )
  {
    return generateComponentAtEachThreatLevelWithAction(componentBaseName, "fail", policyThreatCategory, false,
        isSecurityReachable);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>>
      generateComponentAtEachThreatLevelWithActionWithRecommendations(
      final String componentBaseName,
      final String action,
      final String policyThreatCategory,
      final boolean isSecurityReachable
  )
  {
    return generateComponentAtEachThreatLevelWithAction(componentBaseName, action, policyThreatCategory, true,
        isSecurityReachable);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWithAction(
      final String componentBaseName,
      final String action,
      final String policyThreatCategory,
      final boolean includeRecommendations,
      final boolean isSecurityReachable
  )
  {
    final List<ApiReportComponentDTOV2> bomComponents = new ArrayList<>();
    final List<PolicyThreats.Component> policyThreatComponents = new ArrayList<>();

    for (int i = 0; i < 11; i++) {
      final PolicyAction policyAction = new PolicyAction();
      policyAction.actionType = action;
      final PolicyViolation policyViolation = createPolicyViolation(
          i + 1,
          TemporaryEntity.uuid(),
          TemporaryEntity.uuid(),
          Lists.newArrayList(policyAction),
          Lists.newArrayList(),
          policyThreatCategory,
          isSecurityReachable);

      final String hash = TemporaryEntity.uuid().substring(0, 19);
      final ApiReportComponentDTOV2 component = createComponent(hash, componentBaseName + i);

      final PolicyThreats.Component componentThreats;

      componentThreats = createPolicyThreatsComponents(
          component,
          Lists.newArrayList(policyViolation));

      if (includeRecommendations) {
        tempEntity.newDevelopmentPrioritizationComponentInfo(
            prioritizationId,
            GIVEN_SOME_SCAN_ID,
            component.componentIdentifier.toComponentIdentifier().toSyntheticHash(),
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, // type doesn't matter for prioritization
            String.format("1.0.%s", i)
        );
      }

      bomComponents.add(component);
      policyThreatComponents.add(componentThreats);
    }

    return new Pair<>(bomComponents, policyThreatComponents);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWitNoActions(
      final String componentBaseName,
      final String policyThreatCategory,
      final boolean includeRecommendations,
      final boolean isSecurityReachable
  )
  {
    final List<ApiReportComponentDTOV2> bomComponents = new ArrayList<>();
    final List<PolicyThreats.Component> policyThreatComponents = new ArrayList<>();

    for (int i = 0; i < 11; i++) {
      final String hash = TemporaryEntity.uuid().substring(0, 19);
      final ApiReportComponentDTOV2 component = createComponent(hash, componentBaseName + i);
      final PolicyThreats.Component componentThreats = createPolicyThreatsComponents(
          component,
          Lists.newArrayList(
              createPolicyViolation(
                  i + 1,
                  TemporaryEntity.uuid(),
                  "policy-name" + TemporaryEntity.uuid(),
                  Lists.newArrayList(),
                  Lists.newArrayList(),
                  policyThreatCategory,
                  isSecurityReachable)));

      if (includeRecommendations) {
        tempEntity.newDevelopmentPrioritizationComponentInfo(
            prioritizationId,
            GIVEN_SOME_SCAN_ID,
            component.componentIdentifier.toComponentIdentifier().toSyntheticHash(),
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, // type doesn't matter for prioritization
            String.format("1.0.%s", i)
        );
      }

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

  private void assertPaginationResultCorrect(
      ApiPageResult<PrioritizedComponent> actualPageResult,
      final int expectedResultSize,
      final int expectedTotal,
      final int expectedPageCount,
      final int expectedPage
  )
  {
    // should be no additional priorities, everything is in top 3
    assertThat(actualPageResult.getResults()).hasSize(expectedResultSize);
    assertThat(actualPageResult.getTotal()).isEqualTo(expectedTotal);
    assertThat(actualPageResult.getPageCount()).isEqualTo(expectedPageCount);
    assertThat(actualPageResult.getPage()).isEqualTo(expectedPage);
  }

  private void assertTop3Hashes(
      final List<PrioritizedComponent> actualTop3,
      final List<String> expectedComponentHashes
  )
  {
    assertThat(actualTop3)
        .hasSize(3)
        .extracting("componentHash")
        .containsExactlyElementsOf(expectedComponentHashes);
  }
}
