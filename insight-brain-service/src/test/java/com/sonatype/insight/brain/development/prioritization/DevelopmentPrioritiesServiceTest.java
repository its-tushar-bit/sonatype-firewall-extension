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
import com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent;
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

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS;
import static com.sonatype.insight.license.model.LicensedFeature.DEVELOPER_DASHBOARD;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

  private static final int DEFAULT_COMPONENT_COUNT = 11;

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
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10,
                null))
        .withFailMessage("This server is not licensed for Sonatype Developer.")
        .isInstanceOf(NotAuthorizedException.class);
  }

  @Test
  public void testGetPrioritizedFindings_ShouldCorrectlyOrderByThreatScoreDescendingIfPriorityIsTheSame() {
    // === GIVEN ===
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final List<PolicyViolation> component1Violations = Lists.newArrayList(
        createPolicyViolation(6, "a", "policy-a", false));
    Collections.shuffle(component1Violations);
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        component1Violations
    );

    // Although all components have same priority, since this has the highest threat, it should come first
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final List<PolicyViolation> component2Violations = Lists.newArrayList(
        createPolicyViolation(9, "b", "policy-b", false));
    Collections.shuffle(component2Violations);
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(component2, component2Violations);

    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component3");
    final List<PolicyViolation> component3Violations = Lists.newArrayList(
        createPolicyViolation(3, "c", "policy-c", false));
    Collections.shuffle(component2Violations);
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(component3, component3Violations);

    // has the highest threat security-reachable violations
    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final List<PolicyViolation> component4Violations = Lists.newArrayList(
        createPolicyViolation(
            7, "d", "policy-d", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), true));
    Collections.shuffle(component4Violations);
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(component4, component4Violations);

    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(
            component1Threats, component2Threats, component3Threats, component4Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<PrioritizedComponent> results = developmentPrioritiesService
        .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);

    assertThat(results).containsExactly(
        toPrioritizedComponent(component4, 7, "policy-d", 1, "Unknown", false, null, "none", true, null, 7),
        toPrioritizedComponent(component2, 9, "policy-b", 2, "Unknown", false, null, "none", false, null, 0),
        toPrioritizedComponent(component1, 6, "policy-a", 2, "Unknown", false, null, "none", false, null, 0),
        toPrioritizedComponent(component3, 3, "policy-c", 2, "Unknown", false, null, "none", false, null, 0)
    );

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldPrioritizeSecurityReachablePolicyViolationsCorrectly() {
    // === GIVEN ===
    //  has highest threat violations, but none are security-reachable violations
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final List<PolicyViolation> component1Violations = Lists.newArrayList(
        createPolicyViolation(2, "a", "policy-a", false),
        createPolicyViolation(6, "b", "policy-b", false),
        createPolicyViolation(9, "c", "policy-c", false),
        createPolicyViolation(6, "d", "policy-d", false),
        createPolicyViolation(10, "e", "policy-e", false));
    Collections.shuffle(component1Violations);
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        component1Violations,
        // add a violation that's not active, it should not affect our results
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", false))
    );

    // has the highest threat security-reachable violations
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final List<PolicyViolation> component2Violations = Lists.newArrayList(
        createPolicyViolation(
            7, "f", "policy-f", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), true),
        createPolicyViolation(
            9, "g", "policy-g", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), true));
    Collections.shuffle(component2Violations);
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(component2, component2Violations);

    // no violations
    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component3");

    // has lesser threat security-reachable violations
    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final List<PolicyViolation> component4Violations = Lists.newArrayList(
        createPolicyViolation(
            8, "h", "policy-h", false),
        createPolicyViolation(
            4, "i", "policy-i", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), true),
        createPolicyViolation(
            5, "j", "policy-j", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), true));
    Collections.shuffle(component4Violations);
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(component4, component4Violations);

    //  has highest threat violations, none are security-reachable violations, so will have same priority as component1
    final ApiReportComponentDTOV2 component5 = createComponent("eee", "component5");
    final List<PolicyViolation> component5Violations = Lists.newArrayList(
        createPolicyViolation(5, "k", "policy-k", false),
        createPolicyViolation(7, "l", "policy-l", false));
    Collections.shuffle(component5Violations);
    final PolicyThreats.Component component5Threats = createPolicyThreatsComponents(component5, component5Violations);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4, component5)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats, component2Threats, component4Threats,
            component5Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final DevelopmentPrioritizationResults results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10,
            null);

    assertThat(results.getTopPriorities()).containsExactly(
            toPrioritizedComponent(component2, 9, "policy-g", 1, "Unknown", false, null, "none", true, null, 9),
            toPrioritizedComponent(component4, 8, "policy-h", 2, "Unknown", false, null, "none", true, null, 5),
            toPrioritizedComponent(component1, 10, "policy-e", 3, "Unknown", false, null, "none", false, null, 0));

    assertThat(results.getAdditionalPriorities().getResults()).containsExactly(
            toPrioritizedComponent(component5, 7, "policy-l", 3, "Unknown", false, null, "none", false, null, 0));

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 1, 1, 1, 1);

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
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10, null);

    assertThat(results.getTopPriorities()).containsExactlyInAnyOrder(
        // "policy-f" of threat level 10 is a legacy violation, so not in the priority list.
        toPrioritizedComponent(component2, 7, "policy-g", null, 1),
        // "policy-b,d,e" of threat level 6 are a legacy violations, so not in the priority list.
        toPrioritizedComponent(component1, 5, "policy-c", null, 1)
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
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10,
                null);

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
        generateComponentAtEachThreatLevelWithFailActions(
            1, "reachable-component-with-fail-action", SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponents =
        generateComponentAtEachThreatLevelWithFailActions(
            1,"component-with-fail-action", "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithWarnActions(
            1,"component-with-warn-action", "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithWarnActions(
            1,"reachable-component-with-warn-action", SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponents =
        generateComponentAtEachThreatLevelWitNoActions(
            1,"component-with-no-action", "not-security", false, false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWitNoActions(
            1,"reachable-component-with-no-action", SECURITY.getName(), false, true);

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
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, 6, null);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 3, 3, 1, 1);

    final List<PrioritizedComponent> actualTop3 = results.getTopPriorities();
    final List<PrioritizedComponent> actualAdditionalPriorities = results.getAdditionalPriorities().getResults();

    // top 3 should be reachable and failings actions with descending threat levels
    assertThat(actualTop3).hasSize(3);

    PrioritizedComponent top3FirstComponent = actualTop3.get(0);
    assertThat(top3FirstComponent.getDisplayName()).isEqualTo("reachable-component-with-fail-action0");
    assertThat(top3FirstComponent.getPriority()).isEqualTo(1);
    assertThat(top3FirstComponent.getAction()).isEqualTo("fail");
    assertThat(top3FirstComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(top3FirstComponent.isSecurityReachable()).isTrue();
    assertThat(top3FirstComponent.getHighestThreat()).isEqualTo(9);
    assertThat(top3FirstComponent.getHighestReachableThreat()).isEqualTo(9);

    PrioritizedComponent top3SecondComponent = actualTop3.get(1);
    assertThat(top3SecondComponent.getDisplayName()).isEqualTo("component-with-fail-action0");
    assertThat(top3SecondComponent.getPriority()).isEqualTo(2);
    assertThat(top3SecondComponent.getAction()).isEqualTo("fail");
    assertThat(top3SecondComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(top3SecondComponent.isSecurityReachable()).isFalse();
    assertThat(top3SecondComponent.getHighestThreat()).isEqualTo(9);
    assertThat(top3SecondComponent.getHighestReachableThreat()).isEqualTo(0);

    PrioritizedComponent top3ThirdComponent = actualTop3.get(2);
    assertThat(top3ThirdComponent.getDisplayName()).isEqualTo("reachable-component-with-warn-action0");
    assertThat(top3ThirdComponent.getPriority()).isEqualTo(3);
    assertThat(top3ThirdComponent.getAction()).isEqualTo("warn");
    assertThat(top3ThirdComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(top3ThirdComponent.isSecurityReachable()).isTrue();
    assertThat(top3ThirdComponent.getHighestThreat()).isEqualTo(9);
    assertThat(top3ThirdComponent.getHighestReachableThreat()).isEqualTo(9);

    PrioritizedComponent additionalFirstComponent = actualAdditionalPriorities.get(0);
    assertThat(additionalFirstComponent.getDisplayName()).isEqualTo("component-with-warn-action0");
    assertThat(additionalFirstComponent.getPriority()).isEqualTo(4);
    assertThat(additionalFirstComponent.getAction()).isEqualTo("warn");
    assertThat(additionalFirstComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(additionalFirstComponent.isSecurityReachable()).isFalse();
    assertThat(additionalFirstComponent.getHighestThreat()).isEqualTo(9);
    assertThat(additionalFirstComponent.getHighestReachableThreat()).isEqualTo(0);

    PrioritizedComponent additionalSecondComponent = actualAdditionalPriorities.get(1);
    assertThat(additionalSecondComponent.getDisplayName()).isEqualTo("reachable-component-with-no-action0");
    assertThat(additionalSecondComponent.getPriority()).isEqualTo(5);
    assertThat(additionalSecondComponent.getAction()).isEqualTo("none");
    assertThat(additionalSecondComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(additionalSecondComponent.isSecurityReachable()).isTrue();
    assertThat(additionalSecondComponent.getHighestThreat()).isEqualTo(9);
    assertThat(additionalSecondComponent.getHighestReachableThreat()).isEqualTo(9);

    PrioritizedComponent additionalThirdComponent = actualAdditionalPriorities.get(2);
    assertThat(additionalThirdComponent.getDisplayName()).isEqualTo("component-with-no-action0");
    assertThat(additionalThirdComponent.getPriority()).isEqualTo(6);
    assertThat(additionalThirdComponent.getAction()).isEqualTo("none");
    assertThat(additionalThirdComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(additionalThirdComponent.isSecurityReachable()).isFalse();
    assertThat(additionalThirdComponent.getHighestThreat()).isEqualTo(9);
    assertThat(additionalThirdComponent.getHighestReachableThreat()).isEqualTo(0);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldSortCorrectlyWithAllPrioritizationCriteria_WithBulkRecommendations() {
    // === Given (in expected order of priority) ===
    // FAIL ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "reachable-component-with-fail-action-with-recommendation", "fail", SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponents =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "non-reachable-component-with-fail-action-with-recommendation", "fail", "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachableNoRecommendation =
        generateComponentAtEachThreatLevelWithFailActions(
            1, "reachable-component-with-fail-action-no-recommendation", SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWithFailActions(
            1, "non-reachable-component-with-fail-action-no-recommendation", "not-security", false);

    // WARN ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "reachable-component-with-warn-action-with-recommendation", "warn", SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "non-reachable-component-with-warn-action-with-recommendation", "warn", "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachableNoRecommendations =
        generateComponentAtEachThreatLevelWithWarnActions(1, "reachable-component-with-warn-action-no-recommendation",
            SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWithWarnActions(1,
            "non-reachable-component-with-warn-action-no-recommendation","not-security", false);

    // NONE ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWitNoActions(1, "reachable-component-with-no-action-with-recommendation",
            SECURITY.getName(), true, true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponents =
        generateComponentAtEachThreatLevelWitNoActions(1, "non-reachable-component-with-no-action-with-recommendation",
            "not-security", true, false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachableNoRecommendations
        = generateComponentAtEachThreatLevelWitNoActions(1, "reachable-component-with-no-action-no-recommendation",
        SECURITY.getName(), false, true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWitNoActions(1, "non-reachable-component-with-no-action-no-recommendation",
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
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, 12, null);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 9, 9, 1, 1);

    final List<PrioritizedComponent> actualTop3 = results.getTopPriorities();
    final List<PrioritizedComponent> actualAdditionalPriorities = results.getAdditionalPriorities().getResults();

    PrioritizedComponent top3FirstComponent = actualTop3.get(0);
    assertThat(top3FirstComponent.getDisplayName())
        .isEqualTo("reachable-component-with-fail-action-with-recommendation0");
    assertThat(top3FirstComponent.getPriority()).isEqualTo(1);
    assertThat(top3FirstComponent.getAction()).isEqualTo("fail");
    assertThat(top3FirstComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(top3FirstComponent.isSecurityReachable()).isTrue();
    assertThat(top3FirstComponent.getHighestThreat()).isEqualTo(9);
    assertThat(top3FirstComponent.getHighestReachableThreat()).isEqualTo(9);

    PrioritizedComponent top3SecondComponent = actualTop3.get(1);
    assertThat(top3SecondComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-fail-action-with-recommendation0");
    assertThat(top3SecondComponent.getPriority()).isEqualTo(2);
    assertThat(top3SecondComponent.getAction()).isEqualTo("fail");
    assertThat(top3SecondComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(top3SecondComponent.isSecurityReachable()).isFalse();
    assertThat(top3SecondComponent.getHighestThreat()).isEqualTo(9);
    assertThat(top3SecondComponent.getHighestReachableThreat()).isEqualTo(0);

    PrioritizedComponent top3ThirdComponent = actualTop3.get(2);
    assertThat(top3ThirdComponent.getDisplayName())
        .isEqualTo("reachable-component-with-fail-action-no-recommendation0");
    assertThat(top3ThirdComponent.getPriority()).isEqualTo(3);
    assertThat(top3ThirdComponent.getAction()).isEqualTo("fail");
    assertThat(top3ThirdComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(top3ThirdComponent.isSecurityReachable()).isTrue();
    assertThat(top3ThirdComponent.getHighestThreat()).isEqualTo(9);
    assertThat(top3ThirdComponent.getHighestReachableThreat()).isEqualTo(9);

    PrioritizedComponent additionalPrioritiesComponent = actualAdditionalPriorities.get(0);
    assertThat(additionalPrioritiesComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-fail-action-no-recommendation0");
    assertThat(additionalPrioritiesComponent.getPriority()).isEqualTo(4);
    assertThat(additionalPrioritiesComponent.getAction()).isEqualTo("fail");
    assertThat(additionalPrioritiesComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(additionalPrioritiesComponent.isSecurityReachable()).isFalse();
    assertThat(additionalPrioritiesComponent.getHighestThreat()).isEqualTo(9);
    assertThat(additionalPrioritiesComponent.getHighestReachableThreat()).isEqualTo(0);

    additionalPrioritiesComponent = actualAdditionalPriorities.get(1);
    assertThat(additionalPrioritiesComponent.getDisplayName())
        .isEqualTo("reachable-component-with-warn-action-with-recommendation0");
    assertThat(additionalPrioritiesComponent.getPriority()).isEqualTo(5);
    assertThat(additionalPrioritiesComponent.getAction()).isEqualTo("warn");
    assertThat(additionalPrioritiesComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(additionalPrioritiesComponent.isSecurityReachable()).isTrue();
    assertThat(additionalPrioritiesComponent.getHighestThreat()).isEqualTo(9);
    assertThat(additionalPrioritiesComponent.getHighestReachableThreat()).isEqualTo(9);

    additionalPrioritiesComponent = actualAdditionalPriorities.get(2);
    assertThat(additionalPrioritiesComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-warn-action-with-recommendation0");
    assertThat(additionalPrioritiesComponent.getPriority()).isEqualTo(6);
    assertThat(additionalPrioritiesComponent.getAction()).isEqualTo("warn");
    assertThat(additionalPrioritiesComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(additionalPrioritiesComponent.isSecurityReachable()).isFalse();
    assertThat(additionalPrioritiesComponent.getHighestThreat()).isEqualTo(9);
    assertThat(additionalPrioritiesComponent.getHighestReachableThreat()).isEqualTo(0);

    additionalPrioritiesComponent = actualAdditionalPriorities.get(3);
    assertThat(additionalPrioritiesComponent.getDisplayName())
        .isEqualTo("reachable-component-with-warn-action-no-recommendation0");
    assertThat(additionalPrioritiesComponent.getPriority()).isEqualTo(7);
    assertThat(additionalPrioritiesComponent.getAction()).isEqualTo("warn");
    assertThat(additionalPrioritiesComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(additionalPrioritiesComponent.isSecurityReachable()).isTrue();
    assertThat(additionalPrioritiesComponent.getHighestThreat()).isEqualTo(9);
    assertThat(additionalPrioritiesComponent.getHighestReachableThreat()).isEqualTo(9);

    additionalPrioritiesComponent = actualAdditionalPriorities.get(4);
    assertThat(additionalPrioritiesComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-warn-action-no-recommendation0");
    assertThat(additionalPrioritiesComponent.getPriority()).isEqualTo(8);
    assertThat(additionalPrioritiesComponent.getAction()).isEqualTo("warn");
    assertThat(additionalPrioritiesComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(additionalPrioritiesComponent.isSecurityReachable()).isFalse();
    assertThat(additionalPrioritiesComponent.getHighestThreat()).isEqualTo(9);
    assertThat(additionalPrioritiesComponent.getHighestReachableThreat()).isEqualTo(0);

    additionalPrioritiesComponent = actualAdditionalPriorities.get(5);
    assertThat(additionalPrioritiesComponent.getDisplayName())
        .isEqualTo("reachable-component-with-no-action-with-recommendation0");
    assertThat(additionalPrioritiesComponent.getPriority()).isEqualTo(9);
    assertThat(additionalPrioritiesComponent.getAction()).isEqualTo("none");
    assertThat(additionalPrioritiesComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(additionalPrioritiesComponent.isSecurityReachable()).isTrue();
    assertThat(additionalPrioritiesComponent.getHighestThreat()).isEqualTo(9);
    assertThat(additionalPrioritiesComponent.getHighestReachableThreat()).isEqualTo(9);

    additionalPrioritiesComponent = actualAdditionalPriorities.get(6);
    assertThat(additionalPrioritiesComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-no-action-with-recommendation0");
    assertThat(additionalPrioritiesComponent.getPriority()).isEqualTo(10);
    assertThat(additionalPrioritiesComponent.getAction()).isEqualTo("none");
    assertThat(additionalPrioritiesComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(additionalPrioritiesComponent.isSecurityReachable()).isFalse();
    assertThat(additionalPrioritiesComponent.getHighestThreat()).isEqualTo(9);
    assertThat(additionalPrioritiesComponent.getHighestReachableThreat()).isEqualTo(0);

    additionalPrioritiesComponent = actualAdditionalPriorities.get(7);
    assertThat(additionalPrioritiesComponent.getDisplayName())
        .isEqualTo("reachable-component-with-no-action-no-recommendation0");
    assertThat(additionalPrioritiesComponent.getPriority()).isEqualTo(11);
    assertThat(additionalPrioritiesComponent.getAction()).isEqualTo("none");
    assertThat(additionalPrioritiesComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(additionalPrioritiesComponent.isSecurityReachable()).isTrue();
    assertThat(additionalPrioritiesComponent.getHighestThreat()).isEqualTo(9);
    assertThat(additionalPrioritiesComponent.getHighestReachableThreat()).isEqualTo(9);

    additionalPrioritiesComponent = actualAdditionalPriorities.get(8);
    assertThat(additionalPrioritiesComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-no-action-no-recommendation0");
    assertThat(additionalPrioritiesComponent.getPriority()).isEqualTo(12);
    assertThat(additionalPrioritiesComponent.getAction()).isEqualTo("none");
    assertThat(additionalPrioritiesComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(additionalPrioritiesComponent.isSecurityReachable()).isFalse();
    assertThat(additionalPrioritiesComponent.getHighestThreat()).isEqualTo(9);
    assertThat(additionalPrioritiesComponent.getHighestReachableThreat()).isEqualTo(0);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldNotQueryCallflowWhenThereAreNoSecurityViolations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> components =
        generateComponentAtEachThreatLevelWithFailActions(DEFAULT_COMPONENT_COUNT,
            "component-with-fail-action", "not-security", false);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(components.getA()));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(components.getB()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final DevelopmentPrioritizationResults results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, 66, null);

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
        generateComponentAtEachThreatLevelWithFailActions(DEFAULT_COMPONENT_COUNT,
            "component-with-fail-action", SECURITY.getName(), true);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(components.getA()));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(components.getB()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final DevelopmentPrioritizationResults results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, 66, null);

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
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10,
                null);

    final List<PrioritizedComponent> actualTop3 = results.getTopPriorities();
    List<PrioritizedComponent> actualAdditionalPriorities = results.getAdditionalPriorities().getResults();

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 2, 2, 1, 1);
    assertThat(actualAdditionalPriorities).hasSize(2);
    assertThat(actualTop3).hasSize(3);

    assertThat(actualTop3.get(0).getComponentHash()).isEqualTo("ccc");
    assertThat(actualTop3.get(0).getPriority()).isEqualTo(1);

    assertThat(actualTop3.get(1).getComponentHash()).isEqualTo("aaa");
    assertThat(actualTop3.get(1).getPriority()).isEqualTo(1);

    assertThat(actualTop3.get(2).getComponentHash()).isEqualTo("bbb");
    assertThat(actualTop3.get(2).getPriority()).isEqualTo(1);

    assertThat(actualAdditionalPriorities.get(0).getComponentHash()).isEqualTo("ddd");
    assertThat(actualAdditionalPriorities.get(0).getPriority()).isEqualTo(1);

    assertThat(actualAdditionalPriorities.get(1).getComponentHash()).isEqualTo("eee");
    assertThat(actualAdditionalPriorities.get(1).getPriority()).isEqualTo(1);

    verifyServiceCallsInvokedWithExpectedArguments();

    // === THEN - Should cary forward priority even across pagination ===
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 2, 1, null);
    actualAdditionalPriorities = results.getAdditionalPriorities().getResults();

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 1, 2, 2, 2);
    assertThat(actualAdditionalPriorities.get(0).getComponentHash()).isEqualTo("eee");
    assertThat(actualAdditionalPriorities.get(0).getPriority()).isEqualTo(1);
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
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10,
                null);

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

    final Map<String, String> someCoordinate = new HashMap<>();
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
                Collections.emptyList(),
                Collections.emptyList(),
                SECURITY.getName(),
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
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10,
                null)
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
        generateComponentAtEachThreatLevelWithFailActions(DEFAULT_COMPONENT_COUNT,
            "component-with-fail-action", SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithWarnActions(DEFAULT_COMPONENT_COUNT,
            "component-with-warn-action", "not-security", false);

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
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10, null);

    // should be same regardless of page
    assertTop3Hashes(results.getTopPriorities(), expectedTop3HashesInOrder);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 10, 19, 2, 1);

    List<PrioritizedComponent> additionalPriorities = results.getAdditionalPriorities().getResults();

    // Assertion for first 8 security-reachable violations in additionalPriorities (3 already exist in topPriorities)
    int nonTop3SecurityReachableStartIndex = 3;
    for (int i = 0; i < additionalPriorities.size() - nonTop3SecurityReachableStartIndex; i++) {
      final PrioritizedComponent actualComponent = additionalPriorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + nonTop3SecurityReachableStartIndex + 1);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedAdditionalHashesInOrder.get(i));
    }

    // Assertion for first 2 non-security-reachable components in additionalPriorities
    int nonTop3NonSecurityReachableStartIndex = 8;
    for (int i = nonTop3NonSecurityReachableStartIndex; i < additionalPriorities.size(); i++) {
      final PrioritizedComponent actualComponent = additionalPriorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(12); //will be constant as highestReachableThreat = 0
      // Since all non-security-reachable components will have same priority, cannot check for correct order.
      // Instead, check if the hash exists.
      assertThat(expectedAdditionalHashesInOrder).contains(actualComponent.getComponentHash());
    }

    // check second page contains priorities 11-20 and the next 10 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 2, GIVEN_PAGE_SIZE_10, null);

    // should be same regardless of page
    assertTop3Hashes(results.getTopPriorities(), expectedTop3HashesInOrder);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 9, 19, 2, 2);

    additionalPriorities = results.getAdditionalPriorities().getResults();

    // second page will all be non-security-reachable components
    for (int i = 0; i < additionalPriorities.size(); i++) {
      final PrioritizedComponent actualComponent = additionalPriorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(12);
      // Since all non-security-reachable components will have same priority, cannot check for correct order.
      // Instead, check if the hash exists.
      assertThat(expectedAdditionalHashesInOrder).contains(actualComponent.getComponentHash());
    }

    // check last page contains priorities 20-22 and the final 2 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 3, GIVEN_PAGE_SIZE_10, null);

    // should be same regardless of page
    assertTop3Hashes(results.getTopPriorities(), expectedTop3HashesInOrder);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 0, 19, 2, 3);

    assertThat(results.getAdditionalPriorities().getResults()).isEmpty();

    // should return empty list if requesting a page past the end
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 4, GIVEN_PAGE_SIZE_10, null);
    assertPaginationResultCorrect(results.getAdditionalPriorities(), 0, 19, 2, 4);

    // should be same regardless of page
    assertTop3Hashes(results.getTopPriorities(), expectedTop3HashesInOrder);

    // check first page contains priorities 1-5 and the first 5 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 1, 5, null);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 5, 19, 4, 1);

    // should be same regardless of page
    assertTop3Hashes(results.getTopPriorities(), expectedTop3HashesInOrder);

    additionalPriorities = results.getAdditionalPriorities().getResults();

    int priorityOffset = 4;
    for (int i = 0; i < additionalPriorities.size(); i++) {
      final PrioritizedComponent actualComponent = additionalPriorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedAdditionalHashesInOrder.get(i));
    }

    // check second page contains priorities 6-10 and the next 5 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 2, 5, null);

    assertPaginationResultCorrect(results.getAdditionalPriorities(), 5, 19, 4, 2);

    // should be same regardless of page
    assertTop3Hashes(results.getTopPriorities(), expectedTop3HashesInOrder);

    additionalPriorities = results.getAdditionalPriorities().getResults();

    // Assertion for first 3 security-reachable components in 2nd page of additionalPriorities
    priorityOffset = 9;
    for (int i = 0; i < 3; i++) {
      final PrioritizedComponent actualComponent = additionalPriorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedAdditionalHashesInOrder.get(i + 5));
    }

    // Assertion for first 2 non-security-reachable components in additionalPriorities
    nonTop3NonSecurityReachableStartIndex = 3;
    for (int i = nonTop3NonSecurityReachableStartIndex; i < additionalPriorities.size(); i++) {
      final PrioritizedComponent actualComponent = additionalPriorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(12); //will be constant as highestReachableThreat = 0
      // Since all non-security-reachable components will have same priority, cannot check for correct order.
      // Instead, check if the hash exists.
      assertThat(expectedAdditionalHashesInOrder).contains(actualComponent.getComponentHash());
    }
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldThrowAppropiateExceptionIfDevelopmentNotEnabled() {
    assertThatThrownBy(() ->
        developmentPrioritiesService
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID))
        .withFailMessage("This server is not licensed for Sonatype Developer.")
        .isInstanceOf(NotAuthorizedException.class);
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldPrioritizeSecurityReachablePolicyViolationsCorrectly() {
    // === GIVEN ===
    //  has highest threat violations, but none are security-reachable violations
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final List<PolicyViolation> component1Violations = Lists.newArrayList(
        createPolicyViolation(2, "a", "policy-a", false),
        createPolicyViolation(6, "b", "policy-b", false),
        createPolicyViolation(9, "c", "policy-c", false),
        createPolicyViolation(6, "d", "policy-d", false),
        createPolicyViolation(10, "e", "policy-e", false));
    Collections.shuffle(component1Violations);
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        component1Violations,
        // add a violation that's not active, it should not affect our results
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", false))
    );

    // has the highest threat security-reachable violations
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final List<PolicyViolation> component2Violations = Lists.newArrayList(
        createPolicyViolation(
            7, "f", "policy-f", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), true),
        createPolicyViolation(
            9, "g", "policy-g", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), true));
    Collections.shuffle(component2Violations);
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(component2, component2Violations);

    // no violations
    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component3");

    // has lesser threat security-reachable violations
    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final List<PolicyViolation> component4Violations = Lists.newArrayList(
        createPolicyViolation(
            8, "h", "policy-h", false),
        createPolicyViolation(
            4, "i", "policy-i", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), true),
        createPolicyViolation(
            5, "j", "policy-j", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), true));
    Collections.shuffle(component4Violations);
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(component4, component4Violations);

    //  has highest threat violations, none are security-reachable violations, so will have same priority as component1
    final ApiReportComponentDTOV2 component5 = createComponent("eee", "component5");
    final List<PolicyViolation> component5Violations = Lists.newArrayList(
        createPolicyViolation(5, "k", "policy-k", false),
        createPolicyViolation(7, "l", "policy-l", false));
    Collections.shuffle(component5Violations);
    final PolicyThreats.Component component5Threats = createPolicyThreatsComponents(component5, component5Violations);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4, component5)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats, component2Threats, component4Threats,
            component5Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<PrioritizedComponent> results = developmentPrioritiesService.getAllPrioritizedFindings(
        GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);

    assertThat(results).containsExactly(
            toPrioritizedComponent(component2, 9, "policy-g", 1, "Unknown", false, null, "none", true, null, 9),
            toPrioritizedComponent(component4, 8, "policy-h", 2, "Unknown", false, null, "none", true, null, 5),
            toPrioritizedComponent(component1, 10, "policy-e", 3, "Unknown", false, null, "none", false, null, 0),
            toPrioritizedComponent(component5, 7, "policy-l", 3, "Unknown", false, null, "none", false, null, 0));

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldFilterOutPrioritiesWithZeroThreat() {
    // === GIVEN ===
    // will be middle priority with component2 (priority 2)
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Lists.newArrayList(createPolicyViolation(7, "a", "policy-a",
            false)));

    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component1");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        Lists.newArrayList(createPolicyViolation(7, "b", "policy-b",
            false)));

    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component1");
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        Lists.newArrayList(createPolicyViolation(9, "c", "policy-c",
            false)));

    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        Lists.newArrayList(createPolicyViolation(2, "d", "policy-d",
            false)));

    final ApiReportComponentDTOV2 component5 = createComponent("eee", "component5");
    final PolicyThreats.Component component5Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(0, "e", "policy-e",
            false)));

    final ApiReportComponentDTOV2 component6 = createComponent("fff", "component6");
    final PolicyThreats.Component component6Threats = createPolicyThreatsComponents(
        component6,
        Lists.newArrayList(createPolicyViolation(0, "g", "policy-g",
            false)));

    final ApiReportComponentDTOV2 component7 = createComponent("hhh", "component7");
    final PolicyThreats.Component component7Threats = createPolicyThreatsComponents(
        component7,
        Lists.newArrayList(createPolicyViolation(0, "h", "policy-h",
            false)));

    final ApiReportComponentDTOV2 component8 = createComponent("iii", "component8");
    final PolicyThreats.Component component8Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(0, "i", "policy-i",
            false)));

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
    List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);

    assertThat(results)
        .hasSize(4)
        .allSatisfy(prioritizedComponent -> assertThat(prioritizedComponent.getHighestThreat()).isPositive());

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldCorrectlyCombineBomAndPolicyThreatsIntoSingleResponse() {
    // === Given ====
    final String someHash = tempEntity.newRandomHash();
    final String someDisplayName = TemporaryEntity.uuid();
    final String someConstraintName = TemporaryEntity.uuid();

    final Map<String, String> someCoordinate = new HashMap<>();
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
                Collections.emptyList(),
                Collections.emptyList(),
                SECURITY.getName(),
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
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);

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
  public void testGetAllPrioritizedFindings_shouldReuseTheSamePriorityWhenTheyHaveTheSameScore() {
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
        Lists.newArrayList(createPolicyViolation(2, "e", "policy-e",
            false)));

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4,
            component5)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(
            component1Threats, component2Threats, component3Threats, component4Threats, component5Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === THEN ===
    List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);

    assertThat(results).hasSize(5);

    assertThat(results.get(0).getComponentHash()).isEqualTo("ccc");
    assertThat(results.get(0).getPriority()).isEqualTo(1);

    assertThat(results.get(1).getComponentHash()).isEqualTo("aaa");
    assertThat(results.get(1).getPriority()).isEqualTo(1);

    assertThat(results.get(2).getComponentHash()).isEqualTo("bbb");
    assertThat(results.get(2).getPriority()).isEqualTo(1);

    assertThat(results.get(3).getComponentHash()).isEqualTo("ddd");
    assertThat(results.get(3).getPriority()).isEqualTo(1);

    assertThat(results.get(4).getComponentHash()).isEqualTo("eee");
    assertThat(results.get(4).getPriority()).isEqualTo(1);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldQueryCallflowWhenThereAreSecurityViolations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> components =
        generateComponentAtEachThreatLevelWithFailActions(DEFAULT_COMPONENT_COUNT,
            "component-with-fail-action", SECURITY.getName(), true);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(components.getA()));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(components.getB()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);

    assertThat(results)
        .hasSize(11)
        .allSatisfy(result -> assertThat(result.isSecurityReachable()).isTrue());
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldNotQueryCallflowWhenThereAreNoSecurityViolations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> components =
        generateComponentAtEachThreatLevelWithFailActions(DEFAULT_COMPONENT_COUNT,
            "component-with-fail-action", "not-security", false);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(components.getA()));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(components.getB()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);

    assertThat(results)
        .hasSize(11)
        .allSatisfy(result -> assertThat(result.isSecurityReachable()).isFalse());
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldSortCorrectlyWithAllPrioritizationCriteria_WithBulkRecommendations() {
    // === Given (in expected order of priority) ===
    // FAIL ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "reachable-component-with-fail-action-with-recommendation", "fail", SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponents =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "non-reachable-component-with-fail-action-with-recommendation", "fail", "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachableNoRecommendation =
        generateComponentAtEachThreatLevelWithFailActions(
            1, "reachable-component-with-fail-action-no-recommendation", SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWithFailActions(
            1, "non-reachable-component-with-fail-action-no-recommendation", "not-security", false);

    // WARN ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "reachable-component-with-warn-action-with-recommendation", "warn", SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "non-reachable-component-with-warn-action-with-recommendation", "warn", "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachableNoRecommendations =
        generateComponentAtEachThreatLevelWithWarnActions(1, "reachable-component-with-warn-action-no-recommendation",
            SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWithWarnActions(
            1, "non-reachable-component-with-warn-action-no-recommendation",
            "not-security", false);

    // NONE ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWitNoActions(
            1, "reachable-component-with-no-action-with-recommendation",
            SECURITY.getName(), true, true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponents =
        generateComponentAtEachThreatLevelWitNoActions(
            1, "non-reachable-component-with-no-action-with-recommendation",
            "not-security", true, false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachableNoRecommendations
        = generateComponentAtEachThreatLevelWitNoActions(
        1, "reachable-component-with-no-action-no-recommendation",
        SECURITY.getName(), false, true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWitNoActions(
            1, "non-reachable-component-with-no-action-no-recommendation",
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
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);

    // first 11 should be reachable and failing actions with descending threat levels, and have recommendations
    assertThat(results).hasSize(12);
    PrioritizedComponent prioritizedComponent = results.get(0);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("reachable-component-with-fail-action-with-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(1);
    assertThat(prioritizedComponent.getAction()).isEqualTo("fail");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = results.get(1);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-fail-action-with-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(2);
    assertThat(prioritizedComponent.getAction()).isEqualTo("fail");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(prioritizedComponent.isSecurityReachable()).isFalse();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = results.get(2);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("reachable-component-with-fail-action-no-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(3);
    assertThat(prioritizedComponent.getAction()).isEqualTo("fail");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = results.get(3);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-fail-action-no-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(4);
    assertThat(prioritizedComponent.getAction()).isEqualTo("fail");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(prioritizedComponent.isSecurityReachable()).isFalse();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = results.get(4);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("reachable-component-with-warn-action-with-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(5);
    assertThat(prioritizedComponent.getAction()).isEqualTo("warn");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = results.get(5);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-warn-action-with-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(6);
    assertThat(prioritizedComponent.getAction()).isEqualTo("warn");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isFalse();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = results.get(6);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("reachable-component-with-warn-action-no-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(7);
    assertThat(prioritizedComponent.getAction()).isEqualTo("warn");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = results.get(7);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-warn-action-no-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(8);
    assertThat(prioritizedComponent.getAction()).isEqualTo("warn");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isFalse();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = results.get(8);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("reachable-component-with-no-action-with-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(9);
    assertThat(prioritizedComponent.getAction()).isEqualTo("none");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = results.get(9);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-no-action-with-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(10);
    assertThat(prioritizedComponent.getAction()).isEqualTo("none");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isFalse();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = results.get(10);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("reachable-component-with-no-action-no-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(11);
    assertThat(prioritizedComponent.getAction()).isEqualTo("none");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = results.get(11);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-no-action-no-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(12);
    assertThat(prioritizedComponent.getAction()).isEqualTo("none");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isFalse();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldSortWithAllPrioritizationCriteria_WithoutBulkRecommendations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithFailActions(1, "reachable-component-with-fail-action",
            SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponents =
        generateComponentAtEachThreatLevelWithFailActions(1, "component-with-fail-action",
            "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithWarnActions(1, "component-with-warn-action",
            "not-security", false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithWarnActions(1, "reachable-component-with-warn-action",
            SECURITY.getName(), true);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponents =
        generateComponentAtEachThreatLevelWitNoActions(1, "component-with-no-action",
            "not-security", false, false);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWitNoActions(1, "reachable-component-with-no-action",
            SECURITY.getName(), false, true);

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
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);

    assertThat(results).hasSize(6);

    PrioritizedComponent prioritizedComponent = results.get(0);
    assertThat(prioritizedComponent.getDisplayName()).isEqualTo("reachable-component-with-fail-action0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(1);
    assertThat(prioritizedComponent.getAction()).isEqualTo("fail");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = results.get(1);
    assertThat(prioritizedComponent.getDisplayName()).isEqualTo("component-with-fail-action0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(2);
    assertThat(prioritizedComponent.getAction()).isEqualTo("fail");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(prioritizedComponent.isSecurityReachable()).isFalse();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = results.get(2);
    assertThat(prioritizedComponent.getDisplayName()).isEqualTo("reachable-component-with-warn-action0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(3);
    assertThat(prioritizedComponent.getAction()).isEqualTo("warn");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = results.get(3);
    assertThat(prioritizedComponent.getDisplayName()).isEqualTo("component-with-warn-action0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(4);
    assertThat(prioritizedComponent.getAction()).isEqualTo("warn");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isFalse();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = results.get(4);
    assertThat(prioritizedComponent.getDisplayName()).isEqualTo("reachable-component-with-no-action0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(5);
    assertThat(prioritizedComponent.getAction()).isEqualTo("none");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = results.get(5);
    assertThat(prioritizedComponent.getDisplayName()).isEqualTo("component-with-no-action0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(6);
    assertThat(prioritizedComponent.getAction()).isEqualTo("none");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isFalse();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldResolveTheCorrectDependencyType() {
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
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4,
            component5)));
    when(reportService.getPolicyThreats(anyString(), anyString()))
        .thenReturn(createPolicyThreats(Lists.newArrayList(component1Threats, component2Threats,
            component3Threats, component4Threats, component5Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);

    assertThat(results).hasSize(5);
    assertThat(results.get(0).getDependencyType()).isEqualTo("Unknown");
    assertThat(results.get(1).getDependencyType()).isEqualTo("Transitive");
    assertThat(results.get(2).getDependencyType()).isEqualTo("Inner Source");
    assertThat(results.get(3).getDependencyType()).isEqualTo("Direct");
    assertThat(results.get(4).getDependencyType()).isEqualTo("Transitive");

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldExtractTheNonLegacyHighestThreatPolicyViolation() {
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
    final List<PrioritizedComponent> results = developmentPrioritiesService
        .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);

    assertThat(results).containsExactlyInAnyOrder(
        // "policy-f" of threat level 10 is a legacy violation, so not in the priority list.
        toPrioritizedComponent(component1, 5, "policy-c", null, 1),
        // "policy-b,d,e" of threat level 6 are a legacy violations, so not in the priority list.
        toPrioritizedComponent(component2, 7, "policy-g", null, 1)
    );

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_FilterByComponentDisplayName() {
    final ApiReportComponentDTOV2 component1 = new ApiReportComponentDTOV2();
    component1.displayName = "DoG";
    component1.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("com.sonatype", "dog", "1.1.1"));
    component1.hash = "aaa";

    final ApiReportComponentDTOV2 component2 = new ApiReportComponentDTOV2();
    component2.displayName = "More dogS";
    component2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createNpmCoordinates("more dogs", "1.1.1"));
    component2.hash = "bbb";

    final ApiReportComponentDTOV2 component3 = new ApiReportComponentDTOV2();
    component3.displayName = "Seal";
    component3.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createPypiCoordinates("seal", "1.1.1", "c", "c"));
    component3.hash = "ccc";

    final ApiReportComponentDTOV2 component4 = new ApiReportComponentDTOV2();
    component4.displayName = "d o g s b a r k";
    component4.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("com.sonatype", "dogsbark", "1.1.1"));
    component4.hash = "ddd";

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        List.of(createPolicyViolation(1, "a", "policy-a", false)),
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", false))
    );
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        List.of(createPolicyViolation(2, "b", "policy-a", false)),
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", false))
    );
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        List.of(createPolicyViolation(3, "c", "policy-a", false)),
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", false))
    );
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        List.of(createPolicyViolation(4, "d", "policy-a", false)),
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", false))
    );

    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(List.of(component1Threats, component2Threats, component3Threats, component4Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    DevelopmentPrioritizationResults results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10, "Dog");
    assertThat(results.getTopPriorities())
        .hasSize(2)
        .extracting(PrioritizedComponent::getDisplayName)
        .containsExactlyInAnyOrder("DoG", "More dogS");
    assertThat(results.getAdditionalPriorities().getResults())
        .isEmpty();

    results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10, "cat");
    assertThat(results.getTopPriorities())
        .isEmpty();
    assertThat(results.getAdditionalPriorities().getResults())
        .isEmpty();

    assertThatCode(() -> developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10,
            "*&[>)*"))
        .doesNotThrowAnyException();
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
    final Map<String, String> coordinate = new HashMap<>();
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
    return createPolicyThreatsComponents(fromComponent, activePolicyViolations, Collections.emptyList());
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
    return createPolicyViolation(
        threatLevel, policyViolationId, policyName, Collections.emptyList(), isSecurityReachable);
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
        Collections.emptyList(),
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
        null,
        0
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
      final DevelopmentPrioritizationComponentInfo prioritizationComponentInfo,
      final int highestReachableThreat
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
        prioritizationComponentInfo,
        highestReachableThreat
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
      final int count,
      final String componentBaseName,
      final String policyThreatCategory,
      final boolean isSecurityReachable
  )
  {
    return generateComponentAtEachThreatLevelWithAction(count, componentBaseName, "warn", policyThreatCategory, false,
        isSecurityReachable);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWithFailActions(
      final int count,
      final String componentBaseName,
      final String policyThreatCategory,
      final boolean isSecurityReachable
  )
  {
    return generateComponentAtEachThreatLevelWithAction(count, componentBaseName, "fail", policyThreatCategory, false,
        isSecurityReachable);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>>
      generateComponentAtEachThreatLevelWithActionWithRecommendations(
      final int count,
      final String componentBaseName,
      final String action,
      final String policyThreatCategory,
      final boolean isSecurityReachable
  )
  {
    return generateComponentAtEachThreatLevelWithAction(count, componentBaseName, action, policyThreatCategory, true,
        isSecurityReachable);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWithAction(
      final int count,
      final String componentBaseName,
      final String action,
      final String policyThreatCategory,
      final boolean includeRecommendations,
      final boolean isSecurityReachable
  )
  {
    final List<ApiReportComponentDTOV2> bomComponents = new ArrayList<>();
    final List<PolicyThreats.Component> policyThreatComponents = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      final PolicyAction policyAction = new PolicyAction();
      policyAction.actionType = action;
      final PolicyViolation policyViolation = createPolicyViolation(
          count == DEFAULT_COMPONENT_COUNT ? i + 1 : i + 9,
          TemporaryEntity.uuid(),
          TemporaryEntity.uuid(),
          Lists.newArrayList(policyAction),
          Collections.emptyList(),
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
      final int count,
      final String componentBaseName,
      final String policyThreatCategory,
      final boolean includeRecommendations,
      final boolean isSecurityReachable
  )
  {
    final List<ApiReportComponentDTOV2> bomComponents = new ArrayList<>();
    final List<PolicyThreats.Component> policyThreatComponents = new ArrayList<>();

    for (int i = 0; i < count; i++) {
      final String hash = TemporaryEntity.uuid().substring(0, 19);
      final ApiReportComponentDTOV2 component = createComponent(hash, componentBaseName + i);
      final PolicyThreats.Component componentThreats = createPolicyThreatsComponents(
          component,
          Lists.newArrayList(
              createPolicyViolation(
                  count == DEFAULT_COMPONENT_COUNT ? i + 1 : i + 9,
                  TemporaryEntity.uuid(),
                  "policy-name" + TemporaryEntity.uuid(),
                  Collections.emptyList(),
                  Collections.emptyList(),
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
