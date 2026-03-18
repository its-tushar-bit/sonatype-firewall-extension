/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.development.prioritization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.callflow.ComponentReachabilityService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.development.prioritization.DevelopmentPrioritizationComponentInfoDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.innersource.InnerSourceService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.Component;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyAction;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyConstraint;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats.PolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import oshi.util.tuples.Pair;

import static com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent.DEPENDENCY_TYPE_INNER_SOURCE_DIRECT;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.NON_REACHABLE;
import static com.sonatype.insight.brain.model.policy.ReachabilityStatus.REACHABLE;
import static com.sonatype.insight.license.model.LicensedFeature.DEVELOPER_DASHBOARD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

  @Mock
  private ApiComponentRemediationService componentRemediationService;

  @Inject
  private ComponentReachabilityService componentReachabilityService;

  @Inject
  private DevelopmentPrioritizationComponentInfoDAO prioritizationComponentInfoDAO;

  @Inject
  private DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

  @Mock
  private PolicyEvaluationDiffService policyEvaluationDiffService;

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  @Inject
  private InnerSourceService innerSourceService;

  private DevelopmentPrioritiesService developmentPrioritiesService;

  private String prioritizationId;

  @Before
  public void setup() {
    developmentPrioritiesService = new DevelopmentPrioritiesService(featuresService, developmentPrioritiesReportService,
        prioritizationComponentInfoDAO, reportService, componentReachabilityService, componentRemediationService,
        developmentPrioritiesUtilsService, policyEvaluationDiffService, policyEvaluationDAO, applicationDAO,
        policyWaiverDAO, innerSourceService, autoPolicyWaiverDAO);
    prioritizationId = tempEntity.newDevelopmentPrioritization(GIVEN_SOME_SCAN_ID).getId();
    tempEntity.newApplicationWithParent(GIVEN_SOME_PUBLIC_APP_ID);
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(FeaturesService.class).toInstance(featuresService);
    binder.bind(DevelopmentPrioritiesReportService.class).toInstance(developmentPrioritiesReportService);
    binder.bind(ReportService.class).toInstance(reportService);
    binder.bind(ApiComponentRemediationService.class).toInstance(componentRemediationService);
    binder.bind(PolicyEvaluationDiffService.class).toInstance(policyEvaluationDiffService);
    super.configure(binder);
  }

  @Test
  public void testGetPrioritizedFindings_shouldThrowAppropriateErrorIfDevelopmentNotEnabled() {
    assertThatThrownBy(() -> developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID,
            GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10, null, false, false))
                .withFailMessage("This server is not licensed for Sonatype Developer.")
                .isInstanceOf(NotAuthorizedException.class);
  }

  @Test
  public void testGetPrioritizedFindings_ShouldCorrectlyOrderByThreatScoreDescendingIfThreatLevelIsTheSame() {
    // === GIVEN ===
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final List<PolicyViolation> component1Violations = Lists.newArrayList(
        createPolicyViolation(6, "a", "policy-a", NON_REACHABLE));
    Collections.shuffle(component1Violations);
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        component1Violations);

    // Has the highest non-security-reachable threat
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final List<PolicyViolation> component2Violations = Lists.newArrayList(
        createPolicyViolation(9, "b", "policy-b", NON_REACHABLE));
    Collections.shuffle(component2Violations);
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(component2, component2Violations);

    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component3");
    final List<PolicyViolation> component3Violations = Lists.newArrayList(
        createPolicyViolation(3, "c", "policy-c", NON_REACHABLE));
    Collections.shuffle(component2Violations);
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(component3, component3Violations);

    // has the highest threat security-reachable threat
    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final List<PolicyViolation> component4Violations = Lists.newArrayList(
        createPolicyViolation(
            7, "d", "policy-d", Collections.emptyList(), Collections.emptyList(),
            SECURITY.getName(), REACHABLE, false));
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
        .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null, null);

    assertThat(results).containsExactly(
        toPrioritizedComponent(component4, 7, "policy-d", 1, "Unknown",
            false, null, "none", true, null, null,
            7, false, false, false, false, "", 0, false),
        toPrioritizedComponent(component2, 9, "policy-b", 2, "Unknown",
            false, null, "none", null, null, null,
            0, false, false, false, false, "", 0, false),
        toPrioritizedComponent(component1, 6, "policy-a", 3, "Unknown",
            false, null, "none", null, null, null,
            0, false, false, false, false, "", 0, false),
        toPrioritizedComponent(component3, 3, "policy-c", 4, "Unknown",
            false, null, "none", null, null, null,
            0, false, false, false, false, "", 0, false));

    verifyServiceCallsInvokedWithExpectedArguments();

    verify(reportService).getPolicyThreats(anyString(), anyString());
  }

  @Test
  public void testGetPrioritizedFindings_shouldPrioritizeSecurityReachablePolicyViolationsCorrectly() {
    // === GIVEN ===
    // has highest threat violations, but none are security-reachable violations
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final List<PolicyViolation> component1Violations = Lists.newArrayList(
        createPolicyViolation(2, "a", "policy-a", NON_REACHABLE),
        createPolicyViolation(6, "b", "policy-b", NON_REACHABLE),
        createPolicyViolation(9, "c", "policy-c", NON_REACHABLE),
        createPolicyViolation(6, "d", "policy-d", NON_REACHABLE),
        createPolicyViolation(10, "e", "policy-e", NON_REACHABLE));
    Collections.shuffle(component1Violations);
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        component1Violations,
        // add a violation that's not active, it should not affect our results
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", NON_REACHABLE)));

    // has the highest threat security-reachable violations
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final List<PolicyViolation> component2Violations = Lists.newArrayList(
        createPolicyViolation(
            7, "f", "policy-f", Collections.emptyList(), Collections.emptyList(),
            SECURITY.getName(), REACHABLE, false),
        createPolicyViolation(
            9, "g", "policy-g", Collections.emptyList(), Collections.emptyList(),
            SECURITY.getName(), REACHABLE, false));
    Collections.shuffle(component2Violations);
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(component2, component2Violations);

    // no violations
    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component3");

    // has lesser threat security-reachable violations
    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final List<PolicyViolation> component4Violations = Lists.newArrayList(
        createPolicyViolation(
            8, "h", "policy-h", NON_REACHABLE),
        createPolicyViolation(
            4, "i", "policy-i", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), REACHABLE),
        createPolicyViolation(
            5, "j", "policy-j", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), REACHABLE));
    Collections.shuffle(component4Violations);
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(component4, component4Violations);

    // has the highest threat violations, none are security-reachable, so will have same priority as component1
    final ApiReportComponentDTOV2 component5 = createComponent("eee", "component5");
    final List<PolicyViolation> component5Violations = Lists.newArrayList(
        createPolicyViolation(5, "k", "policy-k", NON_REACHABLE),
        createPolicyViolation(7, "l", "policy-l", NON_REACHABLE));
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
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1,
            GIVEN_PAGE_SIZE_10, null, false, false);

    assertThat(results.priorities().getResults()).containsExactly(
        toPrioritizedComponent(component2, 9, "policy-g", 1, "Unknown", false, null, "none", true, null, null, 9),
        toPrioritizedComponent(component4, 8, "policy-h", 2, "Unknown", false, null, "none", true, null, null, 5),
        toPrioritizedComponent(component1, 10, "policy-e", 3, "Unknown", false, null, "none", null, null, null, 0,
            false, false, false, false, "", 1, false),
        toPrioritizedComponent(component5, 7, "policy-l", 4, "Unknown", false, null, "none", null, null, null, 0));

    assertPaginationResultCorrect(results.priorities(), 4, 4, 1, 1);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldExtractTheNonLegacyHighestThreatPolicyViolation() {
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final List<PolicyViolation> component1Violations = Lists.newArrayList(
        createPolicyViolation(2, "a", "policy-a", NON_REACHABLE),
        makeLegacy(createPolicyViolation(6, "b", "policy-b", NON_REACHABLE)),
        createPolicyViolation(5, "c", "policy-c", NON_REACHABLE),
        makeLegacy(createPolicyViolation(6, "d", "policy-d", NON_REACHABLE)),
        makeLegacy(createPolicyViolation(6, "e", "policy-e", NON_REACHABLE)));
    Collections.shuffle(component1Violations);
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        component1Violations,
        // add a violation that's not active, it should not affect our results
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", NON_REACHABLE)));

    // has the highest threat level of all the components
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final List<PolicyViolation> component2Violations = Lists.newArrayList(
        makeLegacy(createPolicyViolation(10, "f", "policy-f", NON_REACHABLE)),
        createPolicyViolation(7, "g", "policy-g", NON_REACHABLE));
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
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1,
            GIVEN_PAGE_SIZE_10, null, false, false);

    assertThat(results.priorities().getResults()).containsExactlyInAnyOrder(
        // "policy-f" of threat level 10 is a legacy violation, so not in the priority list.
        toPrioritizedComponent(component2, 7, "policy-g", null, 1),
        // "policy-b,d,e" of threat level 6 are a legacy violations, so not in the priority list.
        toPrioritizedComponent(component1, 5, "policy-c", 2, "Unknown", false, null, "none", null, null, null, 0, false,
            false, false, false, "", 1, false));

    assertPaginationResultCorrect(results.priorities(), 2, 2, 1, 1);

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
        Collections.singletonList(createPolicyViolation(1, "a", "policy-a", NON_REACHABLE));
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(component1, component1Violations);
    final List<PolicyViolation> component2Violations =
        Collections.singletonList(createPolicyViolation(1, "b", "policy-b", NON_REACHABLE));
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(component2, component2Violations);
    final List<PolicyViolation> component3Violations =
        Collections.singletonList(createPolicyViolation(1, "c", "policy-c", NON_REACHABLE));
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(component3, component3Violations);
    final List<PolicyViolation> component4Violations =
        Collections.singletonList(createPolicyViolation(1, "d", "policy-d", NON_REACHABLE));
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(component4, component4Violations);
    final List<PolicyViolation> component5Violations =
        Collections.singletonList(createPolicyViolation(1, "e", "policy-e", NON_REACHABLE));
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
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID,
                GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10, null, false, false);

    assertPaginationResultCorrect(results.priorities(), 5, 5, 1, 1);

    final List<PrioritizedComponent> priorities = results.priorities().getResults();
    assertThat(priorities).hasSize(5);
    assertThat(priorities.get(0).getDependencyType()).isEqualTo("Unknown");
    assertThat(priorities.get(1).getDependencyType()).isEqualTo("Transitive");
    assertThat(priorities.get(2).getDependencyType()).isEqualTo("Inner Source Transitive");
    assertThat(priorities.get(3).getDependencyType()).isEqualTo("Direct");
    assertThat(priorities.get(4).getDependencyType()).isEqualTo("Transitive");

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldSortCorrectlyWithAllPrioritizationCriteria_WithoutBulkRecommendations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithFailActions(
            1, "reachable-component-with-fail-action", SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponents =
        generateComponentAtEachThreatLevelWithFailActions(
            1, "component-with-fail-action", "not-security", NON_REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithWarnActions(
            1, "component-with-warn-action", "not-security", NON_REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithWarnActions(
            1, "reachable-component-with-warn-action", SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponents =
        generateComponentAtEachThreatLevelWitNoActions(
            1, "component-with-no-action", "not-security", false, NON_REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWitNoActions(
            1, "reachable-component-with-no-action", SECURITY.getName(), false, REACHABLE);

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
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID,
                GIVEN_PAGE_1, 66, null, false, false);

    assertPaginationResultCorrect(results.priorities(), 6, 6, 1, 1);

    final List<PrioritizedComponent> priorities = results.priorities().getResults();

    // should be ordered by reachable and failings actions with descending threat levels
    assertThat(priorities).hasSize(6);

    PrioritizedComponent prioritizedComponent = priorities.get(0);
    assertThat(prioritizedComponent.getDisplayName()).isEqualTo("reachable-component-with-fail-action0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(1);
    assertThat(prioritizedComponent.getAction()).isEqualTo("fail");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = priorities.get(1);
    assertThat(prioritizedComponent.getDisplayName()).isEqualTo("component-with-fail-action0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(2);
    assertThat(prioritizedComponent.getAction()).isEqualTo("fail");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = priorities.get(2);
    assertThat(prioritizedComponent.getDisplayName()).isEqualTo("reachable-component-with-warn-action0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(3);
    assertThat(prioritizedComponent.getAction()).isEqualTo("warn");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = priorities.get(3);
    assertThat(prioritizedComponent.getDisplayName()).isEqualTo("component-with-warn-action0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(4);
    assertThat(prioritizedComponent.getAction()).isEqualTo("warn");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = priorities.get(4);
    assertThat(prioritizedComponent.getDisplayName()).isEqualTo("reachable-component-with-no-action0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(5);
    assertThat(prioritizedComponent.getAction()).isEqualTo("none");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = priorities.get(5);
    assertThat(prioritizedComponent.getDisplayName()).isEqualTo("component-with-no-action0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(6);
    assertThat(prioritizedComponent.getAction()).isEqualTo("none");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldSortCorrectlyWithAllPrioritizationCriteria_WithBulkRecommendations() {
    // === Given ===
    // FAIL ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "reachable-component-with-fail-action-with-recommendation", "fail", SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponents =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "non-reachable-component-with-fail-action-with-recommendation", "fail", "not-security", NON_REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachableNoRecommendation =
        generateComponentAtEachThreatLevelWithFailActions(
            1, "reachable-component-with-fail-action-no-recommendation", SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWithFailActions(
            1, "non-reachable-component-with-fail-action-no-recommendation", "not-security", NON_REACHABLE);

    // WARN ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "reachable-component-with-warn-action-with-recommendation", "warn", SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "non-reachable-component-with-warn-action-with-recommendation", "warn", "not-security", NON_REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachableNoRecommendations =
        generateComponentAtEachThreatLevelWithWarnActions(1, "reachable-component-with-warn-action-no-recommendation",
            SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWithWarnActions(1,
            "non-reachable-component-with-warn-action-no-recommendation", "not-security", NON_REACHABLE);

    // NONE ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWitNoActions(1, "reachable-component-with-no-action-with-recommendation",
            SECURITY.getName(), true, REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponents =
        generateComponentAtEachThreatLevelWitNoActions(1, "non-reachable-component-with-no-action-with-recommendation",
            "not-security", true, NON_REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachableNoRecommendations =
        generateComponentAtEachThreatLevelWitNoActions(1, "reachable-component-with-no-action-no-recommendation",
            SECURITY.getName(), false, REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWitNoActions(1, "non-reachable-component-with-no-action-no-recommendation",
            "not-security", false, NON_REACHABLE);

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
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID,
                GIVEN_PAGE_1, 129, null, false, false);

    assertPaginationResultCorrect(results.priorities(), 12, 12, 1, 1);

    final List<PrioritizedComponent> priorities = results.priorities().getResults();

    PrioritizedComponent prioritizedComponent = priorities.get(0);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("reachable-component-with-fail-action-with-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(1);
    assertThat(prioritizedComponent.getAction()).isEqualTo("fail");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = priorities.get(1);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-fail-action-with-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(2);
    assertThat(prioritizedComponent.getAction()).isEqualTo("fail");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = priorities.get(2);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("reachable-component-with-fail-action-no-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(3);
    assertThat(prioritizedComponent.getAction()).isEqualTo("fail");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = priorities.get(3);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-fail-action-no-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(4);
    assertThat(prioritizedComponent.getAction()).isEqualTo("fail");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isTrue();
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = priorities.get(4);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("reachable-component-with-warn-action-with-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(5);
    assertThat(prioritizedComponent.getAction()).isEqualTo("warn");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = priorities.get(5);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-warn-action-with-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(6);
    assertThat(prioritizedComponent.getAction()).isEqualTo("warn");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = priorities.get(6);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("reachable-component-with-warn-action-no-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(7);
    assertThat(prioritizedComponent.getAction()).isEqualTo("warn");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = priorities.get(7);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-warn-action-no-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(8);
    assertThat(prioritizedComponent.getAction()).isEqualTo("warn");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = priorities.get(8);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("reachable-component-with-no-action-with-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(9);
    assertThat(prioritizedComponent.getAction()).isEqualTo("none");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = priorities.get(9);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-no-action-with-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(10);
    assertThat(prioritizedComponent.getAction()).isEqualTo("none");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    prioritizedComponent = priorities.get(10);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("reachable-component-with-no-action-no-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(11);
    assertThat(prioritizedComponent.getAction()).isEqualTo("none");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isTrue();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(9);

    prioritizedComponent = priorities.get(11);
    assertThat(prioritizedComponent.getDisplayName())
        .isEqualTo("non-reachable-component-with-no-action-no-recommendation0");
    assertThat(prioritizedComponent.getPriority()).isEqualTo(12);
    assertThat(prioritizedComponent.getAction()).isEqualTo("none");
    assertThat(prioritizedComponent.getHasFailActionOnComponent()).isFalse();
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetPrioritizedFindings_shouldNotQueryCallflowWhenThereAreNoSecurityViolations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> components =
        generateComponentAtEachThreatLevelWithFailActions(DEFAULT_COMPONENT_COUNT,
            "component-with-fail-action", "not-security", NON_REACHABLE);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(components.getA()));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(components.getB()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final DevelopmentPrioritizationResults results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID,
                GIVEN_PAGE_1, 66, null, false, false);

    final List<PrioritizedComponent> priorities = results.priorities().getResults();

    assertThat(priorities)
        .hasSize(11)
        .allSatisfy(result -> assertThat(result.isSecurityReachable()).isNull());

    assertPaginationResultCorrect(results.priorities(), 11, 11, 1, 1);
  }

  @Test
  public void testGetPrioritizedFindings_shouldQueryCallflowWhenThereAreSecurityViolations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> components =
        generateComponentAtEachThreatLevelWithFailActions(DEFAULT_COMPONENT_COUNT,
            "component-with-fail-action", SECURITY.getName(), REACHABLE);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(components.getA()));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(components.getB()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final DevelopmentPrioritizationResults results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID,
                GIVEN_PAGE_1, 66, null, false, false);

    final List<PrioritizedComponent> priorities = results.priorities().getResults();

    assertPaginationResultCorrect(results.priorities(), 11, 11, 1, 1);

    assertThat(priorities)
        .hasSize(11)
        .allSatisfy(result -> assertThat(result.isSecurityReachable()).isTrue());
  }

  @Test
  public void testGetPrioritizedFindings_shouldReuseTheSamePriorityWhenTheyHaveTheSameScore() {
    // === GIVEN ===
    // will be middle priority with component2 (priority 2)
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Lists.newArrayList(createPolicyViolation(7, "a", "policy-a", NON_REACHABLE)));

    // will be middle priority with component1 (priority 2)
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component1");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        Lists.newArrayList(createPolicyViolation(7, "b", "policy-b", NON_REACHABLE)));

    // will be the highest priority (priority 1)
    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component1");
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        Lists.newArrayList(createPolicyViolation(9, "c", "policy-c", NON_REACHABLE)));

    // will be the lowest priority (3)
    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        Lists.newArrayList(createPolicyViolation(2, "d", "policy-d", NON_REACHABLE)));

    // will also be the lowest priority (3)
    final ApiReportComponentDTOV2 component5 = createComponent("eee", "component5");
    final PolicyThreats.Component component5Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(2, "e", "policy-e", NON_REACHABLE)));

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
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID,
                GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10, null, false, false);

    List<PrioritizedComponent> priorities = results.priorities().getResults();

    assertPaginationResultCorrect(results.priorities(), 5, 5, 1, 1);
    assertThat(priorities).hasSize(5);

    assertThat(priorities.get(0).getComponentHash()).isEqualTo("ccc");
    assertThat(priorities.get(0).getPriority()).isEqualTo(1);

    assertThat(priorities.get(1).getComponentHash()).isEqualTo("aaa");
    assertThat(priorities.get(1).getPriority()).isEqualTo(2);

    assertThat(priorities.get(2).getComponentHash()).isEqualTo("bbb");
    assertThat(priorities.get(2).getPriority()).isEqualTo(3);

    assertThat(priorities.get(3).getComponentHash()).isEqualTo("ddd");
    assertThat(priorities.get(3).getPriority()).isEqualTo(4);

    assertThat(priorities.get(4).getComponentHash()).isEqualTo("eee");
    assertThat(priorities.get(4).getPriority()).isEqualTo(5);

    verifyServiceCallsInvokedWithExpectedArguments();

    // === THEN - Should carry forward priority even across pagination ===
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 2, 1, null, false, false);
    priorities = results.priorities().getResults();

    assertPaginationResultCorrect(results.priorities(), 1, 5, 5, 2);
    assertThat(priorities.get(0).getComponentHash()).isEqualTo("aaa");
    assertThat(priorities.get(0).getPriority()).isEqualTo(2);
  }

  @Test
  public void testGetPrioritizedFindings_shouldFilterOutPrioritiesWithZeroThreat() {
    // === GIVEN ===
    // will be middle priority with component2 (priority 2)
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Lists.newArrayList(createPolicyViolation(7, "a", "policy-a", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component1");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        Lists.newArrayList(createPolicyViolation(7, "b", "policy-b", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component1");
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        Lists.newArrayList(createPolicyViolation(9, "c", "policy-c", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        Lists.newArrayList(createPolicyViolation(2, "d", "policy-d", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component5 = createComponent("eee", "component5");
    final PolicyThreats.Component component5Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(0, "e", "policy-e", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component6 = createComponent("fff", "component6");
    final PolicyThreats.Component component6Threats = createPolicyThreatsComponents(
        component6,
        Lists.newArrayList(createPolicyViolation(0, "g", "policy-g", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component7 = createComponent("hhh", "component7");
    final PolicyThreats.Component component7Threats = createPolicyThreatsComponents(
        component7,
        Lists.newArrayList(createPolicyViolation(0, "h", "policy-h", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component8 = createComponent("iii", "component8");
    final PolicyThreats.Component component8Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(0, "i", "policy-i", NON_REACHABLE)));

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
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID,
                GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10, null, false, false);

    final List<PrioritizedComponent> priorities = results.priorities().getResults();

    assertThat(priorities)
        .hasSize(4)
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
        someComponentIdentifier);

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
                NON_REACHABLE),
            // at least one component with a security violation,
            // so that we check reachable (it does not have to be highest)
            createPolicyViolation(
                2,
                "b",
                "policy-b",
                Collections.emptyList(),
                Collections.emptyList(),
                SECURITY.getName(),
                REACHABLE)));

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
                null, false, false)
            .priorities()
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
        generateComponentAtEachThreatLevelWithFailActions(DEFAULT_COMPONENT_COUNT,
            "component-with-fail-action", SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithWarnActions(DEFAULT_COMPONENT_COUNT,
            "component-with-warn-action", "not-security", NON_REACHABLE);

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
    final List<String> expectedHashesInOrder = new ArrayList<>();

    // the mock data was created in the ascending threat order, the sorted results returned will be in descending
    Collections.reverse(failingComponents.getA());
    Collections.reverse(warningComponents.getA());

    // add the expected components hashes
    expectedHashesInOrder.addAll(
        failingComponents.getA().stream().map(comp -> comp.hash).collect(Collectors.toList()));
    expectedHashesInOrder.addAll(
        warningComponents.getA().stream().map(comp -> comp.hash).collect(Collectors.toList()));

    // check first page contains priorities 1-10 and the first 10 hashes
    DevelopmentPrioritizationResults results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1,
            GIVEN_PAGE_SIZE_10, null, false, false);

    assertPaginationResultCorrect(results.priorities(), 10, 22, 3, 1);

    List<PrioritizedComponent> priorities = results.priorities().getResults();

    // Assertion for first 10 security-reachable violations
    for (int i = 0; i < priorities.size(); i++) {
      final PrioritizedComponent actualComponent = priorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + 1);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i));
    }

    // check second page contains priorities 11-20 and the next 10 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 2,
                GIVEN_PAGE_SIZE_10, null, false, false);

    assertPaginationResultCorrect(results.priorities(), 10, 22, 3, 2);

    priorities = results.priorities().getResults();

    // Assert first component to be security-reachable
    int priorityOffset = 10;
    PrioritizedComponent firstComponent = priorities.get(0);
    assertThat(firstComponent.getPriority()).isEqualTo(1 + priorityOffset);
    assertThat(firstComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(priorityOffset));

    // second page will all be non-security-reachable components
    for (int i = 1; i < priorities.size(); i++) {
      final PrioritizedComponent actualComponent = priorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(1 + priorityOffset + i);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i + priorityOffset));
    }

    // check last page contains the final hash
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 3,
                GIVEN_PAGE_SIZE_10, null, false, false);

    priorities = results.priorities().getResults();

    assertPaginationResultCorrect(results.priorities(), 2, 22, 3, 3);

    priorityOffset = 20;
    // third page will all be non-security-reachable components
    for (int i = 0; i < priorities.size(); i++) {
      final PrioritizedComponent actualComponent = priorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(1 + priorityOffset + i);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i + priorityOffset));
    }

    // should return empty list if requesting a page past the end
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 4, GIVEN_PAGE_SIZE_10, null, false,
                false);
    assertPaginationResultCorrect(results.priorities(), 0, 22, 3, 4);

    // check first page contains priorities 1-5 and the first 5 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 1, 5,
                null, false, false);

    assertPaginationResultCorrect(results.priorities(), 5, 22, 5, 1);

    priorities = results.priorities().getResults();

    for (int i = 0; i < priorities.size(); i++) {
      final PrioritizedComponent actualComponent = priorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + 1);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i));
    }

    // check second page contains priorities 6-10 and the next 5 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 2, 5,
                null, false, false);

    assertPaginationResultCorrect(results.priorities(), 5, 22, 5, 2);

    priorities = results.priorities().getResults();

    // Assertion for security-reachable components in 2nd page
    priorityOffset = 5;
    for (int i = 0; i < priorities.size(); i++) {
      final PrioritizedComponent actualComponent = priorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset + 1);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i + priorityOffset));
    }

    // check third page contains priorities 10-15 and the next 5 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 3, 5, null, false, false);

    assertPaginationResultCorrect(results.priorities(), 5, 22, 5, 3);

    priorities = results.priorities().getResults();

    // Assertion for security-reachable components in 2nd page
    priorityOffset = 10;
    firstComponent = priorities.get(0);
    assertThat(firstComponent.getPriority()).isEqualTo(1 + priorityOffset);
    assertThat(firstComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(priorityOffset));

    // Non-security-reachable components
    for (int i = 1; i < priorities.size(); i++) {
      final PrioritizedComponent actualComponent = priorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset + 1);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i + priorityOffset));
    }

    // check fourth page contains the next 5 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 4, 5, null, false, false);

    assertPaginationResultCorrect(results.priorities(), 5, 22, 5, 4);

    priorities = results.priorities().getResults();

    priorityOffset = 15;
    for (int i = 0; i < priorities.size(); i++) {
      final PrioritizedComponent actualComponent = priorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset + 1);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i + priorityOffset));
    }

    // check fifth page contains the next 2 hashes
    results =
        developmentPrioritiesService
            .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 5, 5, null, false, false);

    assertPaginationResultCorrect(results.priorities(), 2, 22, 5, 5);

    priorities = results.priorities().getResults();

    priorityOffset = 20;
    for (int i = 0; i < priorities.size(); i++) {
      final PrioritizedComponent actualComponent = priorities.get(i);
      assertThat(actualComponent.getPriority()).isEqualTo(i + priorityOffset + 1);
      assertThat(actualComponent.getComponentHash()).isEqualTo(expectedHashesInOrder.get(i + priorityOffset));
    }
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldThrowAppropiateExceptionIfDevelopmentNotEnabled() {
    assertThatThrownBy(() -> developmentPrioritiesService
        .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null,
            null))
                .withFailMessage("This server is not licensed for Sonatype Developer.")
                .isInstanceOf(NotAuthorizedException.class);
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldPrioritizeSecurityReachablePolicyViolationsCorrectly() {
    // === GIVEN ===
    // has highest threat violations, but none are security-reachable violations
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final List<PolicyViolation> component1Violations = Lists.newArrayList(
        createPolicyViolation(2, "a", "policy-a", NON_REACHABLE),
        createPolicyViolation(6, "b", "policy-b", NON_REACHABLE),
        createPolicyViolation(9, "c", "policy-c", NON_REACHABLE),
        createPolicyViolation(6, "d", "policy-d", NON_REACHABLE),
        createPolicyViolation(10, "e", "policy-e", NON_REACHABLE));
    Collections.shuffle(component1Violations);
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        component1Violations,
        // add a violation that's not active, it should not affect our results
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", NON_REACHABLE)));

    // has the highest threat security-reachable violations
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final List<PolicyViolation> component2Violations = Lists.newArrayList(
        createPolicyViolation(
            7, "f", "policy-f", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), REACHABLE),
        createPolicyViolation(
            9, "g", "policy-g", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), REACHABLE));
    Collections.shuffle(component2Violations);
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(component2, component2Violations);

    // no violations
    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component3");

    // has lesser threat security-reachable violations
    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final List<PolicyViolation> component4Violations = Lists.newArrayList(
        createPolicyViolation(
            8, "h", "policy-h", NON_REACHABLE),
        createPolicyViolation(
            4, "i", "policy-i", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), REACHABLE),
        createPolicyViolation(
            5, "j", "policy-j", Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), REACHABLE));
    Collections.shuffle(component4Violations);
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(component4, component4Violations);

    // has highest threat violations, none are security-reachable violations, so will have same priority as component1
    final ApiReportComponentDTOV2 component5 = createComponent("eee", "component5");
    final List<PolicyViolation> component5Violations = Lists.newArrayList(
        createPolicyViolation(5, "k", "policy-k", NON_REACHABLE),
        createPolicyViolation(7, "l", "policy-l", NON_REACHABLE));
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
        GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null, null);

    assertThat(results).containsExactly(
        toPrioritizedComponent(component2, 9, "policy-g", 1, "Unknown", false, null, "none", true, null, null, 9),
        toPrioritizedComponent(component4, 8, "policy-h", 2, "Unknown", false, null, "none", true, null, null, 5),
        toPrioritizedComponent(component1, 10, "policy-e", 3, "Unknown", false, null, "none", null, null, null, 0,
            false, false, false, false, "", 1, false),
        toPrioritizedComponent(component5, 7, "policy-l", 4, "Unknown", false, null, "none", null, null, null, 0));

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
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component1");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        Lists.newArrayList(createPolicyViolation(7, "b", "policy-b",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component1");
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        Lists.newArrayList(createPolicyViolation(9, "c", "policy-c",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        Lists.newArrayList(createPolicyViolation(2, "d", "policy-d",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component5 = createComponent("eee", "component5");
    final PolicyThreats.Component component5Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(0, "e", "policy-e",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component6 = createComponent("fff", "component6");
    final PolicyThreats.Component component6Threats = createPolicyThreatsComponents(
        component6,
        Lists.newArrayList(createPolicyViolation(0, "g", "policy-g",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component7 = createComponent("hhh", "component7");
    final PolicyThreats.Component component7Threats = createPolicyThreatsComponents(
        component7,
        Lists.newArrayList(createPolicyViolation(0, "h", "policy-h",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component8 = createComponent("iii", "component8");
    final PolicyThreats.Component component8Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(0, "i", "policy-i",
            NON_REACHABLE)));

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
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null,
                null);

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
        someComponentIdentifier);

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
                NON_REACHABLE),
            // at least one component with a security violation,
            // so that we check reachable (it does not have to be highest)
            createPolicyViolation(
                2,
                "b",
                "policy-b",
                Collections.emptyList(),
                Collections.emptyList(),
                SECURITY.getName(),
                REACHABLE)));

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
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null,
                null);

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
  public void testGetAllPrioritizedFindings_shouldHaveDescendingPriorityWhenTheyHaveTheSameScore() {
    // === GIVEN ===
    // will be middle priority with component2 (priority 2)
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Lists.newArrayList(createPolicyViolation(7, "a", "policy-a", NON_REACHABLE)));

    // will be middle priority with component1 (priority 2)
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component1");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        Lists.newArrayList(createPolicyViolation(7, "b", "policy-b", NON_REACHABLE)));

    // will be the highest priority (priority 1)
    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component1");
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        Lists.newArrayList(createPolicyViolation(9, "c", "policy-c", NON_REACHABLE)));

    // will be the lowest priority (3)
    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        Lists.newArrayList(createPolicyViolation(2, "d", "policy-d", NON_REACHABLE)));

    // will also be the lowest priority (3)
    final ApiReportComponentDTOV2 component5 = createComponent("eee", "component5");
    final PolicyThreats.Component component5Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(2, "e", "policy-e",
            NON_REACHABLE)));

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
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null,
                null);

    assertThat(results).hasSize(5);

    assertThat(results.get(0).getComponentHash()).isEqualTo("ccc");
    assertThat(results.get(0).getPriority()).isEqualTo(1);

    assertThat(results.get(1).getComponentHash()).isEqualTo("aaa");
    assertThat(results.get(1).getPriority()).isEqualTo(2);

    assertThat(results.get(2).getComponentHash()).isEqualTo("bbb");
    assertThat(results.get(2).getPriority()).isEqualTo(3);

    assertThat(results.get(3).getComponentHash()).isEqualTo("ddd");
    assertThat(results.get(3).getPriority()).isEqualTo(4);

    assertThat(results.get(4).getComponentHash()).isEqualTo("eee");
    assertThat(results.get(4).getPriority()).isEqualTo(5);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldQueryCallflowWhenThereAreSecurityViolations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> components =
        generateComponentAtEachThreatLevelWithFailActions(DEFAULT_COMPONENT_COUNT,
            "component-with-fail-action", SECURITY.getName(), REACHABLE);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(components.getA()));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(components.getB()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null,
                null);

    assertThat(results)
        .hasSize(11)
        .allSatisfy(result -> assertThat(result.isSecurityReachable()).isTrue());
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldNotQueryCallflowWhenThereAreNoSecurityViolations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> components =
        generateComponentAtEachThreatLevelWithFailActions(DEFAULT_COMPONENT_COUNT,
            "component-with-fail-action", "not-security", NON_REACHABLE);

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(components.getA()));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(components.getB()));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null,
                null);

    assertThat(results)
        .hasSize(11)
        .allSatisfy(result -> assertThat(result.isSecurityReachable()).isNull());
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldSortCorrectlyWithAllPrioritizationCriteria_WithBulkRecommendations() {
    // === Given ===
    // FAIL ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "reachable-component-with-fail-action-with-recommendation", "fail", SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponents =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "non-reachable-component-with-fail-action-with-recommendation", "fail", "not-security", NON_REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachableNoRecommendation =
        generateComponentAtEachThreatLevelWithFailActions(
            1, "reachable-component-with-fail-action-no-recommendation", SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWithFailActions(
            1, "non-reachable-component-with-fail-action-no-recommendation", "not-security", NON_REACHABLE);

    // WARN ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "reachable-component-with-warn-action-with-recommendation", "warn", SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithActionWithRecommendations(
            1, "non-reachable-component-with-warn-action-with-recommendation", "warn", "not-security", NON_REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachableNoRecommendations =
        generateComponentAtEachThreatLevelWithWarnActions(1, "reachable-component-with-warn-action-no-recommendation",
            SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWithWarnActions(
            1, "non-reachable-component-with-warn-action-no-recommendation",
            "not-security", NON_REACHABLE);

    // NONE ACTION
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWitNoActions(
            1, "reachable-component-with-no-action-with-recommendation",
            SECURITY.getName(), true, REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponents =
        generateComponentAtEachThreatLevelWitNoActions(
            1, "non-reachable-component-with-no-action-with-recommendation",
            "not-security", true, NON_REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachableNoRecommendations =
        generateComponentAtEachThreatLevelWitNoActions(
            1, "reachable-component-with-no-action-no-recommendation",
            SECURITY.getName(), false, REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithNoRecommendations =
        generateComponentAtEachThreatLevelWitNoActions(
            1, "non-reachable-component-with-no-action-no-recommendation",
            "not-security", false, NON_REACHABLE);

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
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null,
                null);

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
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
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
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
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
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
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
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
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
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
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
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldSortWithAllPrioritizationCriteria_WithoutBulkRecommendations() {
    // === Given ===
    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithFailActions(1, "reachable-component-with-fail-action",
            SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> failingComponents =
        generateComponentAtEachThreatLevelWithFailActions(1, "component-with-fail-action",
            "not-security", NON_REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponents =
        generateComponentAtEachThreatLevelWithWarnActions(1, "component-with-warn-action",
            "not-security", NON_REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> warningComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWithWarnActions(1, "reachable-component-with-warn-action",
            SECURITY.getName(), REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponents =
        generateComponentAtEachThreatLevelWitNoActions(1, "component-with-no-action",
            "not-security", false, NON_REACHABLE);

    final Pair<List<ApiReportComponentDTOV2>, List<Component>> noActionComponentsWithSecurityReachable =
        generateComponentAtEachThreatLevelWitNoActions(1, "reachable-component-with-no-action",
            SECURITY.getName(), false, REACHABLE);

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
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null,
                null);

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
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
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
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
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
    assertThat(prioritizedComponent.isSecurityReachable()).isNull();
    assertThat(prioritizedComponent.getHighestThreat()).isEqualTo(9);
    assertThat(prioritizedComponent.getHighestReachableThreat()).isEqualTo(0);

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetAllPrioritizedFindings_HandlesUnknownReachability() {
    Pair<List<ApiReportComponentDTOV2>, List<Component>> reachable =
        generateComponentAtEachThreatLevelWithFailActions(1, "reachable-component-with-fail-action",
            SECURITY.getName(), REACHABLE);
    Pair<List<ApiReportComponentDTOV2>, List<Component>> nonReachable =
        generateComponentAtEachThreatLevelWithFailActions(1, "non-reachable-component-with-fail-action",
            SECURITY.getName(), NON_REACHABLE);
    Pair<List<ApiReportComponentDTOV2>, List<Component>> unknownReachability =
        generateComponentAtEachThreatLevelWithFailActions(1, "unknown-reachability-component-with-fail-action",
            SECURITY.getName(), null);
    List<ApiReportComponentDTOV2> bomComponents = new ArrayList<>();
    bomComponents.addAll(reachable.getA());
    bomComponents.addAll(nonReachable.getA());
    bomComponents.addAll(unknownReachability.getA());
    List<PolicyThreats.Component> policyThreatComponents = new ArrayList<>();
    policyThreatComponents.addAll(reachable.getB());
    policyThreatComponents.addAll(nonReachable.getB());
    policyThreatComponents.addAll(unknownReachability.getB());
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString()))
        .thenReturn(createApiReportRawDataDTOV2(bomComponents));
    when(reportService.getPolicyThreats(anyString(), anyString()))
        .thenReturn(createPolicyThreats(policyThreatComponents));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    List<PrioritizedComponent> results = developmentPrioritiesService.getAllPrioritizedFindings(
        GIVEN_SOME_PUBLIC_APP_ID,
        GIVEN_SOME_SCAN_ID,
        null,
        null);

    assertThat(results).hasSize(3);
    assertThat(results.get(0).getDisplayName()).isEqualTo("reachable-component-with-fail-action0");
    assertThat(results.get(0).isSecurityReachable()).isTrue();
    assertThat(results.get(1).getDisplayName()).isEqualTo("non-reachable-component-with-fail-action0");
    assertThat(results.get(1).isSecurityReachable()).isFalse();
    assertThat(results.get(2).getDisplayName()).isEqualTo("unknown-reachability-component-with-fail-action0");
    assertThat(results.get(2).isSecurityReachable()).isNull();
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
        Collections.singletonList(createPolicyViolation(1, "a", "policy-a", NON_REACHABLE));
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(component1, component1Violations);
    final List<PolicyViolation> component2Violations =
        Collections.singletonList(createPolicyViolation(1, "b", "policy-b", NON_REACHABLE));
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(component2, component2Violations);
    final List<PolicyViolation> component3Violations =
        Collections.singletonList(createPolicyViolation(1, "c", "policy-c", NON_REACHABLE));
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(component3, component3Violations);
    final List<PolicyViolation> component4Violations =
        Collections.singletonList(createPolicyViolation(1, "d", "policy-d", NON_REACHABLE));
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(component4, component4Violations);
    final List<PolicyViolation> component5Violations =
        Collections.singletonList(createPolicyViolation(1, "e", "policy-e", NON_REACHABLE));
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
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null,
                null);

    assertThat(results).hasSize(5);
    assertThat(results.get(0).getDependencyType()).isEqualTo("Unknown");
    assertThat(results.get(1).getDependencyType()).isEqualTo("Transitive");
    assertThat(results.get(2).getDependencyType()).isEqualTo("Inner Source Transitive");
    assertThat(results.get(3).getDependencyType()).isEqualTo("Direct");
    assertThat(results.get(4).getDependencyType()).isEqualTo("Transitive");

    verifyServiceCallsInvokedWithExpectedArguments();
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldExtractTheNonLegacyHighestThreatPolicyViolation() {
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final List<PolicyViolation> component1Violations = Lists.newArrayList(
        createPolicyViolation(2, "a", "policy-a", NON_REACHABLE),
        makeLegacy(createPolicyViolation(6, "b", "policy-b", NON_REACHABLE)),
        createPolicyViolation(5, "c", "policy-c", NON_REACHABLE),
        makeLegacy(createPolicyViolation(6, "d", "policy-d", NON_REACHABLE)),
        makeLegacy(createPolicyViolation(6, "e", "policy-e", NON_REACHABLE)));
    Collections.shuffle(component1Violations);
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        component1Violations,
        // add a violation that's not active, it should not affect our results
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", NON_REACHABLE)));

    // has the highest threat level of all the components
    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final List<PolicyViolation> component2Violations = Lists.newArrayList(
        makeLegacy(createPolicyViolation(10, "f", "policy-f", NON_REACHABLE)),
        createPolicyViolation(7, "g", "policy-g", NON_REACHABLE));
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
        .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null,
            null);

    assertThat(results).containsExactlyInAnyOrder(
        // "policy-f" of threat level 10 is a legacy violation, so not in the priority list.
        toPrioritizedComponent(component2, 7, "policy-g", null, 1),
        // "policy-b,d,e" of threat level 6 are a legacy violations, so not in the priority list.
        toPrioritizedComponent(component1, 5, "policy-c", 2, "Unknown", false, null, "none", null, null, null, 0, false,
            false, false, false, "", 1, false));

  }

  @Test
  public void testGetAllPrioritizedFindings_shouldReturnPaginatedResultWithRemediationInAllComponents() {
    // === Given ===
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Lists.newArrayList(createPolicyViolation(7, "a", "policy-a",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        Lists.newArrayList(createPolicyViolation(7, "b", "policy-b",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component3");
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        Lists.newArrayList(createPolicyViolation(9, "c", "policy-c",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        Lists.newArrayList(createPolicyViolation(2, "d", "policy-d",
            NON_REACHABLE)));

    // === WHEN ===
    when(componentRemediationService.getSuggestedRemediationForComponentNoAuthz(
        any(), any(), any(), any(), any(), any(), any(), anyBoolean())).thenReturn(createComponentRemediation());
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats, component2Threats,
            component3Threats, component4Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    final List<PrioritizedComponent> results = developmentPrioritiesService
        .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 0,
            10);

    assertThat(results).hasSize(4);
    assertThat(results).containsExactly(
        toPrioritizedComponentWithRemediation(component3, 9, "policy-c", null, 1,
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, "v3"),
        toPrioritizedComponentWithRemediation(component1, 7, "policy-a", null, 2,
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, "v3"),
        toPrioritizedComponentWithRemediation(component2, 7, "policy-b", null, 3,
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, "v3"),
        toPrioritizedComponentWithRemediation(component4, 2, "policy-d", null, 4,
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, "v3"));
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldReturnPaginatedResultWithRemediationInTopAndPage() {
    // === Given ===
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Lists.newArrayList(createPolicyViolation(7, "a", "policy-a",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        Lists.newArrayList(createPolicyViolation(7, "b", "policy-b",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component3");
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        Lists.newArrayList(createPolicyViolation(9, "c", "policy-c",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        Lists.newArrayList(createPolicyViolation(2, "d", "policy-d",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component5 = createComponent("fff", "component5");
    final PolicyThreats.Component component5Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(7, "f", "policy-f",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component6 = createComponent("ggg", "component6");
    final PolicyThreats.Component component6Threats = createPolicyThreatsComponents(
        component6,
        Lists.newArrayList(createPolicyViolation(9, "g", "policy-g",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component7 = createComponent("hhh", "component7");
    final PolicyThreats.Component component7Threats = createPolicyThreatsComponents(
        component7,
        Lists.newArrayList(createPolicyViolation(2, "h", "policy-h",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component8 = createComponent("iii", "component8");
    final PolicyThreats.Component component8Threats = createPolicyThreatsComponents(
        component8,
        Lists.newArrayList(createPolicyViolation(7, "i", "policy-i",
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component9 = createComponent("jjj", "component9");
    final PolicyThreats.Component component9Threats = createPolicyThreatsComponents(
        component9,
        Lists.newArrayList(createPolicyViolation(9, "j", "policy-j",
            NON_REACHABLE)));

    // === WHEN ===
    when(componentRemediationService.getSuggestedRemediationForComponentNoAuthz(
        any(), any(), any(), any(), any(), any(), any(), anyBoolean())).thenReturn(createComponentRemediation());
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3,
            component4, component5, component6, component7, component8, component9)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats, component2Threats,
            component3Threats, component4Threats, component5Threats, component6Threats,
            component7Threats, component8Threats, component9Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === Then ===
    // Testing for first page
    List<PrioritizedComponent> results = developmentPrioritiesService
        .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 0,
            2);

    assertThat(results)
        .hasSize(9)
        .containsExactly(
            toPrioritizedComponentWithRemediation(component3, 9, "policy-c", null, 1,
                ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, "v3"),
            toPrioritizedComponentWithRemediation(component6, 9, "policy-g", null, 2,
                ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, "v3"),
            toPrioritizedComponent(component9, 9, "policy-j", null, 3),
            toPrioritizedComponent(component1, 7, "policy-a", null, 4),
            toPrioritizedComponent(component2, 7, "policy-b", null, 5),
            toPrioritizedComponent(component5, 7, "policy-f", null, 6),
            toPrioritizedComponent(component8, 7, "policy-i", null, 7),
            toPrioritizedComponent(component4, 2, "policy-d", null, 8),
            toPrioritizedComponent(component7, 2, "policy-h", null, 9));

    // === Then ===
    // Testing for different page
    results = developmentPrioritiesService
        .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, 2, 2);

    assertThat(results)
        .hasSize(9)
        .containsExactly(
            toPrioritizedComponent(component3, 9, "policy-c", null, 1),
            toPrioritizedComponent(component6, 9, "policy-g", null, 2),
            toPrioritizedComponentWithRemediation(component9, 9, "policy-j", null, 3,
                ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, "v3"),
            toPrioritizedComponentWithRemediation(component1, 7, "policy-a", null, 4,
                ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES, "v3"),
            toPrioritizedComponent(component2, 7, "policy-b", null, 5),
            toPrioritizedComponent(component5, 7, "policy-f", null, 6),
            toPrioritizedComponent(component8, 7, "policy-i", null, 7),
            toPrioritizedComponent(component4, 2, "policy-d", null, 8),
            toPrioritizedComponent(component7, 2, "policy-h", null, 9));
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
        List.of(createPolicyViolation(1, "a", "policy-a", NON_REACHABLE)),
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", NON_REACHABLE)));
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        List.of(createPolicyViolation(2, "b", "policy-a", NON_REACHABLE)),
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", NON_REACHABLE)));
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        List.of(createPolicyViolation(3, "c", "policy-a", NON_REACHABLE)),
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", NON_REACHABLE)));
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        List.of(createPolicyViolation(4, "d", "policy-a", NON_REACHABLE)),
        Lists.newArrayList(createPolicyViolation(9, "z", "policy-z", NON_REACHABLE)));

    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(List.of(component1Threats, component2Threats, component3Threats, component4Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    DevelopmentPrioritizationResults results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10,
            "Dog", false, false);
    assertThat(results.priorities().getResults())
        .hasSize(2)
        .extracting(PrioritizedComponent::getDisplayName)
        .containsExactlyInAnyOrder("DoG", "More dogS");

    results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10,
            "cat", false, false);
    assertThat(results.priorities().getResults())
        .isEmpty();

    assertThatCode(() -> developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10,
            "*&[>)*", false, false))
                .doesNotThrowAnyException();
  }

  @Test
  public void testGetPrioritizedFindings_FilterByFailWarnActions() {
    final PolicyAction failAction = new PolicyAction();
    failAction.actionType = Action.ID_FAIL;
    final PolicyAction warnAction = new PolicyAction();
    warnAction.actionType = Action.ID_WARN;
    final PolicyAction otherAction = new PolicyAction();
    otherAction.actionType = Action.ID_NOTIFY;

    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Lists.newArrayList(createPolicyViolation(7, "a", "policy-a", List.of(failAction),
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        Lists.newArrayList(createPolicyViolation(7, "b", "policy-b", List.of(failAction, otherAction),
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component3");
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        Lists.newArrayList(createPolicyViolation(9, "c", "policy-c", List.of(warnAction),
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        Lists.newArrayList(createPolicyViolation(2, "d", "policy-d", List.of(otherAction),
            NON_REACHABLE)));

    final ApiReportComponentDTOV2 component5 = createComponent("fff", "component5");
    final PolicyThreats.Component component5Threats = createPolicyThreatsComponents(
        component5,
        Lists.newArrayList(createPolicyViolation(7, "f", "policy-f",
            NON_REACHABLE)));

    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4, component5)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(List.of(component1Threats, component2Threats, component3Threats, component4Threats,
            component5Threats)));
    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    DevelopmentPrioritizationResults results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10,
            null, false, true);
    assertThat(results.priorities().getResults())
        .hasSize(3)
        .extracting(PrioritizedComponent::getAction)
        .containsOnly(Action.ID_FAIL, Action.ID_WARN);

    results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10,
            null, false, false);
    assertThat(results.priorities().getResults())
        .hasSize(5);
  }

  @Test
  public void testGetPrioritizedFindings_ShouldReturnResultsWithCorrectWaiverInfo() {
    DateTime now = DateTime.now();
    String appId = applicationDAO.getByPublicId(GIVEN_SOME_PUBLIC_APP_ID).getId();
    Policy policy1 = tempEntity.newPolicy(applicationDAO.getByPublicId(GIVEN_SOME_PUBLIC_APP_ID));
    Policy policy2 = tempEntity.newPolicy(applicationDAO.getByPublicId(GIVEN_SOME_PUBLIC_APP_ID));
    tempEntity.newWaiver("aaa", policy1.getId(), appId,
        null, "comment", now.toDate(), now.plusDays(3).toDate());
    tempEntity.newWaiver("bbb", policy2.getId(), appId,
        null, "comment", now.toDate(), now.minusDays(3).toDate());

    // === Given ===
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Collections.emptyList(),
        List.of(createPolicyViolation(9, "b", "policy-b", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        List.of(createPolicyViolation(7, "c", "policy-c",
            Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), NON_REACHABLE, true)),
        List.of(createPolicyViolation(9, "d", "policy-d", REACHABLE)));

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(List.of(component1, component2)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(List.of(component1Threats, component2Threats)));
    when(featuresService.getFeatures()).thenReturn(Set.of(DEVELOPER_DASHBOARD));
    // === Then ===
    DevelopmentPrioritizationResults results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID,
            GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10, null, false, false);

    final List<PrioritizedComponent> priorities = results.priorities().getResults();

    assertThat(priorities).hasSize(2);

    assertThat(priorities.get(0).getHasSoonToExpireWaiver()).isFalse();
    assertThat(priorities.get(0).getHasExpiredWaiver()).isTrue();
    assertThat(priorities.get(0).getIsAllViolationsWaived()).isFalse();
    assertThat(priorities.get(0).getWaiverExpirationDetails())
        .isEqualTo("Applied waiver expired 3 days ago");
    assertThat(priorities.get(0).getWaivedViolationsCount()).isEqualTo(1);
    assertThat(priorities.get(0).getHasAutoWaiver()).isTrue();

    assertThat(priorities.get(1).getHasSoonToExpireWaiver()).isTrue();
    assertThat(priorities.get(1).getHasExpiredWaiver()).isFalse();
    assertThat(priorities.get(1).getIsAllViolationsWaived()).isTrue();
    assertThat(priorities.get(1).getWaiverExpirationDetails())
        .isEqualTo("Applied waiver will expire in 3 days");
    assertThat(priorities.get(1).getWaivedViolationsCount()).isEqualTo(1);
    assertThat(priorities.get(1).getHasAutoWaiver()).isFalse();
  }

  @Test
  public void testGetPrioritizedFindings_ShouldReturnScanIdFromLatestBuildStageEvaluation() {
    tempEntity.newPolicyEvaluation(
        applicationDAO.getByPublicId(GIVEN_SOME_PUBLIC_APP_ID).getId(), "build", "scan-id-123");
    tempEntity.newPolicyEvaluation(
        applicationDAO.getByPublicId(GIVEN_SOME_PUBLIC_APP_ID).getId(), "release", GIVEN_SOME_SCAN_ID);

    // === Given ===
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Collections.emptyList(),
        List.of(createPolicyViolation(9, "b", "policy-b", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        List.of(createPolicyViolation(7, "c", "policy-c",
            Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), NON_REACHABLE, true)),
        List.of(createPolicyViolation(9, "d", "policy-d", REACHABLE)));

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(List.of(component1, component2)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(List.of(component1Threats, component2Threats)));
    when(featuresService.getFeatures()).thenReturn(Set.of(DEVELOPER_DASHBOARD));
    // === Then ===
    DevelopmentPrioritizationResults results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID,
            GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10, null, false, false);

    final String scanId = results.scanIdFromLatestBuildStageEvaluation();
    assertThat(scanId).isEqualTo("scan-id-123");
  }

  @Test
  public void testGetPrioritizedFindings_ShouldReturnHasAutoWaiversConfigured() {
    tempEntity.newAutoPolicyWaiver();

    // === Given ===
    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Collections.emptyList(),
        List.of(createPolicyViolation(9, "b", "policy-b", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component2");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        List.of(createPolicyViolation(7, "c", "policy-c",
            Collections.emptyList(), Collections.emptyList(), SECURITY.getName(), NON_REACHABLE, true)),
        List.of(createPolicyViolation(9, "d", "policy-d", REACHABLE)));

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(List.of(component1, component2)));
    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(List.of(component1Threats, component2Threats)));
    when(featuresService.getFeatures()).thenReturn(Set.of(DEVELOPER_DASHBOARD));
    // === Then ===
    DevelopmentPrioritizationResults results = developmentPrioritiesService
        .getPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID,
            GIVEN_PAGE_1, GIVEN_PAGE_SIZE_10, null, false, false);

    final boolean hasAutoWaiversConfigured = results.hasAutoWaiversConfigured();
    assertThat(hasAutoWaiversConfigured).isTrue();
  }

  @Test
  public void testGetPrioritizedFindings_shouldHaveCorrectValueForHasSameViolationsOnMain() {
    // === GIVEN ===
    Application application = applicationDAO.getByPublicId(GIVEN_SOME_PUBLIC_APP_ID);
    PolicyEvaluation featureEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), "develop", GIVEN_SOME_SCAN_ID, new Date());
    tempEntity.newPolicyEvaluation(application.getId(), "build", "buildScanId", new Date());

    final ApiReportComponentDTOV2 component1 = createComponent("aaa", "component1");
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        component1,
        Lists.newArrayList(createPolicyViolation(7, "a", "policy-a", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component2 = createComponent("bbb", "component1");
    final PolicyThreats.Component component2Threats = createPolicyThreatsComponents(
        component2,
        Lists.newArrayList(createPolicyViolation(7, "b", "policy-b", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component3 = createComponent("ccc", "component1");
    final PolicyThreats.Component component3Threats = createPolicyThreatsComponents(
        component3,
        Lists.newArrayList(createPolicyViolation(9, "c", "policy-c", NON_REACHABLE)));

    final ApiReportComponentDTOV2 component4 = createComponent("ddd", "component4");
    final PolicyThreats.Component component4Threats = createPolicyThreatsComponents(
        component4,
        Lists.newArrayList(createPolicyViolation(2, "d", "policy-d", NON_REACHABLE)));

    // === WHEN ===
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString())).thenReturn(
        createApiReportRawDataDTOV2(Lists.newArrayList(component1, component2, component3, component4)));

    when(policyEvaluationDiffService.createPolicyViolationDiff(
        any(PolicyEvaluation.class),
        any(PolicyEvaluation.class),
        eq(1)))
            .thenReturn(Optional.of(createPolicyViolationDiff(featureEvaluation, "bbb", "ccc")));

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(
            component1Threats, component2Threats, component3Threats, component4Threats)));

    when(featuresService.getFeatures()).thenReturn(Sets.newHashSet(DEVELOPER_DASHBOARD));

    // === THEN ===
    List<PrioritizedComponent> results =
        developmentPrioritiesService
            .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null, null);

    assertThat(results)
        .hasSize(4)
        .allSatisfy(prioritizedComponent -> assertThat(prioritizedComponent.getHighestThreat()).isPositive());

    assertThat(results)
        .filteredOn(prioritizedComponent -> prioritizedComponent.getComponentHash().equals("bbb") ||
            prioritizedComponent.getComponentHash().equals("ccc"))
        .allSatisfy(prioritizedComponent -> assertThat(prioritizedComponent.getHasSameViolationsOnMain()).isTrue());

    assertThat(results)
        .filteredOn(prioritizedComponent -> prioritizedComponent.getComponentHash().equals("aaa") ||
            prioritizedComponent.getComponentHash().equals("ddd"))
        .allSatisfy(prioritizedComponent -> assertThat(prioritizedComponent.getHasSameViolationsOnMain()).isFalse());
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldHandleInnerSourceTransitiveDependencies() {
    Application app = applicationDAO.getByPublicId(GIVEN_SOME_PUBLIC_APP_ID);
    ComponentIdentifier transitiveComponentId = ComponentIdentifier.createMavenCoordinates(
        "com.example.innersource", "transitive-lib", "1.0.0", "", "jar");
    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(transitiveComponentId);
    InnerSourceApplication innerSourceApp = tempEntity.newInnerSourceApplication(
        packageUrl.getPackageUrl(), app);
    tempEntity.newInnerSourceVersion(innerSourceApp, "1.5.0", StageTypes.RELEASE.getId());

    ApiReportComponentDTOV2 transitiveComponent = createComponent(
        "transitiveHash",
        "com.example.innersource : transitive-lib : 1.0.0",
        getInnerSourceDependencyType(false),
        transitiveComponentId);

    when(featuresService.getFeatures()).thenReturn(Set.of(DEVELOPER_DASHBOARD));
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString()))
        .thenReturn(createApiReportRawDataDTOV2(Lists.newArrayList(transitiveComponent)));
    when(reportService.getPolicyThreats(anyString(), anyString()))
        .thenReturn(createPolicyThreats(List.of()));

    List<PrioritizedComponent> results = developmentPrioritiesService
        .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null, null);

    // we don't return transitive inner source dependencies
    assertThat(results).hasSize(0);
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldHandleInnerSourceDirectDependenciesWithUpgradePath() {
    Application app = applicationDAO.getByPublicId(GIVEN_SOME_PUBLIC_APP_ID);
    ComponentIdentifier directComponentId = ComponentIdentifier.createMavenCoordinates(
        "com.example.innersource", "direct-lib", "1.0.0", "", "jar");
    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(directComponentId);
    InnerSourceApplication innerSourceApp = tempEntity.newInnerSourceApplication(
        packageUrl.getPackageUrl(), app);
    tempEntity.newInnerSourceVersion(innerSourceApp, "1.5.0", StageTypes.RELEASE.getId());

    ApiReportComponentDTOV2 directComponent = createComponent(
        "directHash",
        "com.example.innersource : direct-lib : 1.0.0",
        getInnerSourceDependencyType(true),
        directComponentId);

    PolicyThreats.Component directComponentThreat = createPolicyThreatsComponents(directComponent, List.of());

    when(featuresService.getFeatures()).thenReturn(Set.of(DEVELOPER_DASHBOARD));
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString()))
        .thenReturn(createApiReportRawDataDTOV2(Lists.newArrayList(directComponent)));
    when(reportService.getPolicyThreats(anyString(), anyString()))
        .thenReturn(createPolicyThreats(Lists.newArrayList(directComponentThreat)));

    List<PrioritizedComponent> results = developmentPrioritiesService
        .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null, null);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getDependencyType()).isEqualTo(DEPENDENCY_TYPE_INNER_SOURCE_DIRECT);
    assertThat(results.get(0).getDisplayName()).isEqualTo("com.example.innersource : direct-lib : 1.0.0");
  }

  @Test
  public void testGetAllPrioritizedFindings_shouldHandleInnerSourceDirectDependenciesWithNoUpgradePath() {
    Application app = applicationDAO.getByPublicId(GIVEN_SOME_PUBLIC_APP_ID);
    ComponentIdentifier directComponentId = ComponentIdentifier.createMavenCoordinates(
        "com.example.innersource", "direct-lib", "1.0.0", "", "jar");
    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(directComponentId);
    InnerSourceApplication innerSourceApp = tempEntity.newInnerSourceApplication(
        packageUrl.getPackageUrl(), app);
    tempEntity.newInnerSourceVersion(innerSourceApp, "1.0.0", StageTypes.RELEASE.getId());

    ApiReportComponentDTOV2 directComponent = createComponent(
        "directHash",
        "com.example.innersource : direct-lib : 1.0.0",
        getInnerSourceDependencyType(true),
        directComponentId);

    PolicyThreats.Component directComponentThreat = createPolicyThreatsComponents(directComponent, List.of());

    when(featuresService.getFeatures()).thenReturn(Set.of(DEVELOPER_DASHBOARD));
    when(developmentPrioritiesReportService.getDependencyInformation(anyString(), anyString()))
        .thenReturn(createApiReportRawDataDTOV2(Lists.newArrayList(directComponent)));
    when(reportService.getPolicyThreats(anyString(), anyString()))
        .thenReturn(createPolicyThreats(Lists.newArrayList(directComponentThreat)));

    List<PrioritizedComponent> results = developmentPrioritiesService
        .getAllPrioritizedFindings(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID, null, null);

    assertThat(results).hasSize(0);
  }

  private ApiDependencyDataDTO getInnerSourceDependencyType(boolean isDirect) {
    ApiDependencyDataDTO dependencyDataDTO = new ApiDependencyDataDTO();
    dependencyDataDTO.innerSource = true;
    dependencyDataDTO.directDependency = isDirect;
    return dependencyDataDTO;
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
      final ApiDependencyDataDTO dependencyDataDTO)
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
      final ComponentIdentifier componentIdentifier)
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
      final List<PolicyThreats.PolicyViolation> activePolicyViolations)
  {
    return createPolicyThreatsComponents(fromComponent, activePolicyViolations, Collections.emptyList());
  }

  private PolicyThreats.Component createPolicyThreatsComponents(
      final ApiReportComponentDTOV2 fromComponent,
      final List<PolicyThreats.PolicyViolation> activePolicyViolations,
      final List<PolicyThreats.PolicyViolation> inactivePolicyViolations)
  {
    final PolicyThreats.Component component = new PolicyThreats.Component();
    component.hash = fromComponent.hash;
    component.componentIdentifier = fromComponent.componentIdentifier.toComponentIdentifier();
    component.activeViolations.addAll(activePolicyViolations);
    component.waivedViolations.addAll(inactivePolicyViolations);
    component.allViolations.addAll(activePolicyViolations);
    component.allViolations.addAll(inactivePolicyViolations);

    return component;
  }

  private PolicyViolation createPolicyViolation(
      final int threatLevel,
      final String policyViolationId,
      final String policyName,
      final ReachabilityStatus reachabilityStatus)
  {
    return createPolicyViolation(
        threatLevel, policyViolationId, policyName, Collections.emptyList(), reachabilityStatus);
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
      final ReachabilityStatus reachabilityStatus)
  {
    return createPolicyViolation(
        threatLevel,
        policyViolationId,
        policyName,
        policyActions,
        Collections.emptyList(),
        "some-category",
        reachabilityStatus);
  }

  private PolicyViolation createPolicyViolation(
      final int threatLevel,
      final String policyViolationId,
      final String policyName,
      final List<PolicyAction> policyActions,
      final List<PolicyConstraint> constraints,
      final String policyThreatCategory,
      final ReachabilityStatus reachabilityStatus)
  {
    return createPolicyViolation(
        threatLevel,
        policyViolationId,
        policyName,
        policyActions,
        constraints,
        policyThreatCategory,
        reachabilityStatus,
        false);
  }

  private PolicyViolation createPolicyViolation(
      final int threatLevel,
      final String policyViolationId,
      final String policyName,
      final List<PolicyAction> policyActions,
      final List<PolicyConstraint> constraints,
      final String policyThreatCategory,
      final ReachabilityStatus reachabilityStatus,
      final boolean waivedWithAutoWaiver)
  {
    final PolicyViolation policyViolation = new PolicyViolation();
    policyViolation.policyThreatLevel = threatLevel;
    policyViolation.policyViolationId = policyViolationId;
    policyViolation.actions = policyActions;
    policyViolation.constraints = constraints;
    policyViolation.policyName = policyName;
    policyViolation.policyThreatCategory = policyThreatCategory;
    policyViolation.reachabilityStatus = reachabilityStatus;
    policyViolation.policyId = "some-policy-id";
    policyViolation.waivedWithAutoWaiver = waivedWithAutoWaiver;

    return policyViolation;
  }

  private ApiComponentRemediationDTO createComponentRemediation() {
    final ApiComponentRemediationDTO componentRemediation = new ApiComponentRemediationDTO();
    final ApiComponentRemediationValueDTO remediation = new ApiComponentRemediationValueDTO();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v2");
    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v3");
    remediation.versionChanges = Lists.newArrayList(
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NON_FAILING_WITH_DEPENDENCIES,
            componentIdentifier1),
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NON_FAILING, componentIdentifier2),
        createVersionChangeOption(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES,
            componentIdentifier3));

    componentRemediation.remediation = remediation;
    return componentRemediation;
  }

  private ApiVersionChangeOptionDTO createVersionChangeOption(
      final ApiVersionChangeOptionType type,
      final ComponentIdentifier componentIdentifier)
  {
    final ApiVersionChangeOptionDTO versionChangeOption = new ApiVersionChangeOptionDTO();
    versionChangeOption.setType(type);
    versionChangeOption.setData(createChangeAction(componentIdentifier));
    return versionChangeOption;
  }

  private ApiComponentChangeActionDTO createChangeAction(ComponentIdentifier componentIdentifier) {
    final ApiComponentChangeActionDTO changeAction = new ApiComponentChangeActionDTO();
    changeAction.setComponent(createComponent(componentIdentifier));
    return changeAction;
  }

  private ApiComponentDTOV2 createComponent(ComponentIdentifier componentIdentifier) {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();

    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    return component;
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
        null,
        null,
        null,
        0,
        false,
        false,
        false,
        false,
        "",
        0,
        false);
  }

  private PrioritizedComponent toPrioritizedComponentWithRemediation(
      final ApiReportComponentDTOV2 component,
      final int highestThreat,
      final String highestPolicyName,
      final String highestThreatPolicyConstraintName,
      final int priority,
      final ApiVersionChangeOptionType remediationType,
      final String remediationVersion)
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
        null,
        remediationType,
        remediationVersion,
        0,
        false,
        false,
        false,
        false,
        "",
        0,
        false);
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
      final Boolean securityReachable,
      final ApiVersionChangeOptionType remediationType,
      final String remediationVersion,
      final int highestReachableThreat)
  {
    return toPrioritizedComponent(
        component,
        highestThreat,
        highestThreatPolicyName,
        priority,
        dependencyType,
        hasFailActionComponent,
        highestThreatPolicyConstraintName,
        action,
        securityReachable,
        remediationType,
        remediationVersion,
        highestReachableThreat,
        false,
        false,
        false,
        false,
        "",
        0,
        false);
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
      final Boolean securityReachable,
      final ApiVersionChangeOptionType remediationType,
      final String remediationVersion,
      final int highestReachableThreat,
      final boolean hasSameViolationsOnMain,
      final boolean hasExpiredWaiver,
      final boolean hasSoonToExpireWaiver,
      final boolean isAllViolationsWaived,
      final String waiverExpirationDetails,
      final int waivedViolationsCount,
      final boolean hasAutoWaiver)
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
        remediationType,
        remediationVersion,
        highestReachableThreat,
        hasSameViolationsOnMain,
        hasExpiredWaiver,
        hasSoonToExpireWaiver,
        isAllViolationsWaived,
        waiverExpirationDetails,
        waivedViolationsCount,
        hasAutoWaiver);
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
      final ReachabilityStatus reachabilityStatus)
  {
    return generateComponentAtEachThreatLevelWithAction(count, componentBaseName, "warn", policyThreatCategory, false,
        reachabilityStatus);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWithFailActions(
      final int count,
      final String componentBaseName,
      final String policyThreatCategory,
      final ReachabilityStatus reachabilityStatus)
  {
    return generateComponentAtEachThreatLevelWithAction(count, componentBaseName, "fail", policyThreatCategory, false,
        reachabilityStatus);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWithActionWithRecommendations(
      final int count,
      final String componentBaseName,
      final String action,
      final String policyThreatCategory,
      final ReachabilityStatus reachabilityStatus)
  {
    return generateComponentAtEachThreatLevelWithAction(count, componentBaseName, action, policyThreatCategory, true,
        reachabilityStatus);
  }

  private Pair<List<ApiReportComponentDTOV2>, List<Component>> generateComponentAtEachThreatLevelWithAction(
      final int count,
      final String componentBaseName,
      final String action,
      final String policyThreatCategory,
      final boolean includeRecommendations,
      final ReachabilityStatus reachabilityStatus)
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
          reachabilityStatus);

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
            String.format("1.0.%s", i));
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
      final ReachabilityStatus reachabilityStatus)
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
                  reachabilityStatus)));

      if (includeRecommendations) {
        tempEntity.newDevelopmentPrioritizationComponentInfo(
            prioritizationId,
            GIVEN_SOME_SCAN_ID,
            component.componentIdentifier.toComponentIdentifier().toSyntheticHash(),
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS, // type doesn't matter for prioritization
            String.format("1.0.%s", i));
      }

      bomComponents.add(component);
      policyThreatComponents.add(componentThreats);
    }

    return new Pair<>(bomComponents, policyThreatComponents);
  }

  private ApiDependencyDataDTO getDependencyTypeWithNulls() {
    return new ApiDependencyDataDTO();
  }

  private PolicyViolationDiff<com.sonatype.insight.brain.model.policy.PolicyViolation> createPolicyViolationDiff(
      final PolicyEvaluation evaluation,
      final String... componentHashes)
  {
    PolicyViolationDiff<com.sonatype.insight.brain.model.policy.PolicyViolation> policyViolationDiff =
        new PolicyViolationDiff<>();
    for (String componentHash : componentHashes) {
      policyViolationDiff.addSame(
          null,
          new com.sonatype.insight.brain.model.policy.PolicyViolation(
              evaluation,
              tempEntity.newPolicy(),
              componentHash,
              null,
              Collections.singletonList(new ConstraintFact()),
              null));
    }

    return policyViolationDiff;
  }

  private void assertPaginationResultCorrect(
      ApiPageResult<PrioritizedComponent> actualPageResult,
      final int expectedResultSize,
      final int expectedTotal,
      final int expectedPageCount,
      final int expectedPage)
  {
    // should be no additional priorities, everything is in top 3
    assertThat(actualPageResult.getResults()).hasSize(expectedResultSize);
    assertThat(actualPageResult.getTotal()).isEqualTo(expectedTotal);
    assertThat(actualPageResult.getPageCount()).isEqualTo(expectedPageCount);
    assertThat(actualPageResult.getPage()).isEqualTo(expectedPage);
  }
}
