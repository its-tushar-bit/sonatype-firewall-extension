/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dashboard.PolicyViolationState;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationLoaderTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyViolationLoader loader;

  @Inject
  private ApiConfigurationService configurationService;

  private Application createApplication(StageType... stageTypes) {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy1 = tempEntity.newPolicy(app.getId(), "Test Policy 1", 10);
    Policy policy2 = tempEntity.newPolicy(app.getId(), "Test Policy 2", 1);
    PolicyWaiver waiver = tempEntity.newWaiver(policy1.getId(), app.getId());
    long time = System.currentTimeMillis();
    for (StageType stageType : stageTypes) {
      PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stageType.getId(),
          stageType.getId() + "-scan-id", new Date(time - 2000));
      eval = tempEntity.newPolicyEvaluation(app.getId(), stageType.getId(), stageType.getId() + "-latest-scan-id",
          new Date(time - 1000));
      tempEntity.newWaivedPolicyViolation(eval, policy1, waiver);
      tempEntity.newPolicyViolation(eval, policy2);
    }
    return app;
  }

  @Test
  public void testGetViolations_BasicData() {
    Application app = createApplication(StageTypes.BUILD);

    Collection<ApplicationView> appViews = loader.getViolations(Collections.singletonList(app),
        Collections.singletonList(StageTypes.BUILD),
        false, violation -> true);

    assertThat(appViews).hasSize(1);
    ApplicationView appView = appViews.iterator().next();
    assertThat(appView.getApplication()).isEqualTo(app);
    assertThat(appView.getStageViews()).hasSize(1);
    ApplicationStageView appStageView = appView.getStageViews().iterator().next();
    assertThat(appStageView.getStageType()).isEqualTo(StageTypes.BUILD);
    assertThat(appStageView.getLastEvaluation()).isNotNull();
    assertThat(appStageView.getLastEvaluation().getOwnerId()).isEqualTo(app.getId());
    assertThat(appStageView.getLastEvaluation().getStageTypeId()).isEqualTo(StageTypes.BUILD.getId());
    assertThat(appStageView.getLastEvaluation().getScanId()).isEqualTo("build-latest-scan-id");
    assertThat(appStageView.getFilteredViolations()).hasSize(2);
  }

  @Test
  public void testGetViolations_FilterByApplications() {
    Application app1 = createApplication(StageTypes.BUILD);
    createApplication(StageTypes.BUILD);
    Application app3 = createApplication(StageTypes.BUILD);

    Collection<ApplicationView> appViews = loader.getViolations(Arrays.asList(app1, app3),
        Collections.singletonList(StageTypes.BUILD), false, violation -> true);

    assertThat(appViews).extracting(ApplicationView::getApplication).containsExactlyInAnyOrder(app1, app3);
  }

  @Test
  public void testGetViolations_FilterByApplications_WithLimit() {
    try {
      configurationService.setConfigurationInDatabaseNoAuthz(
          SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD, 2);
      configurationService.applyConfigurationToClients(
          SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD);
      Application app1 = createApplication(StageTypes.BUILD, StageTypes.RELEASE);
      Application app2 = createApplication(StageTypes.BUILD);
      Application app3 = createApplication(StageTypes.BUILD, StageTypes.RELEASE);

      Collection<ApplicationView> appViews = loader.getViolations(Arrays.asList(app1, app2, app3),
          Arrays.asList(StageTypes.BUILD, StageTypes.RELEASE), false, violation -> true);

      Iterator<ApplicationView> appViewsIterator = appViews.iterator();

      // Since it's limited this app should not have filtered violations since it has the oldest evaluations
      ApplicationView appView = appViewsIterator.next();
      assertThat(appView.getApplication()).isEqualTo(app1);
      assertThat(appView.getStageViews()).hasSize(2);
      Iterator<ApplicationStageView> iterator = appView.getStageViews().iterator();
      ApplicationStageView appStageView = iterator.next();
      assertThat(appStageView.getStageType()).isEqualTo(StageTypes.BUILD);
      assertThat(appStageView.getFilteredViolations()).isEmpty();
      appStageView = iterator.next();
      assertThat(appStageView.getStageType()).isEqualTo(StageTypes.RELEASE);
      assertThat(appStageView.getFilteredViolations()).isEmpty();

      // Since it's limited to 2 applications, this app should have violations in the stage that has an evaluation
      appView = appViewsIterator.next();
      assertThat(appView.getApplication()).isEqualTo(app2);
      assertThat(appView.getStageViews()).hasSize(2);
      iterator = appView.getStageViews().iterator();
      appStageView = iterator.next();
      assertThat(appStageView.getStageType()).isEqualTo(StageTypes.BUILD);
      assertThat(appStageView.getFilteredViolations()).hasSize(2);
      appStageView = iterator.next();
      assertThat(appStageView.getStageType()).isEqualTo(StageTypes.RELEASE);
      assertThat(appStageView.getFilteredViolations()).isEmpty();

      // App3 has the latest evaluation for both stages, so it should have the filtered violations
      appView = appViewsIterator.next();
      assertThat(appView.getApplication()).isEqualTo(app3);
      assertThat(appView.getStageViews()).hasSize(2);
      iterator = appView.getStageViews().iterator();
      appStageView = iterator.next();
      assertThat(appStageView.getStageType()).isEqualTo(StageTypes.BUILD);
      assertThat(appStageView.getFilteredViolations()).hasSize(2);
      appStageView = iterator.next();
      assertThat(appStageView.getStageType()).isEqualTo(StageTypes.RELEASE);
      assertThat(appStageView.getFilteredViolations()).hasSize(2);
    }
    finally {
      configurationService.deleteConfigurationInDatabaseNoAuthz(
          SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD);
      configurationService.applyConfigurationToClients(
          SystemConfigurationProperty.MAX_APPLICATIONS_TO_QUERY_ON_DASHBOARD);
    }
  }

  @Test
  public void testGetViolations_FilterByStageTypes() {
    Application app = createApplication(StageTypes.BUILD, StageTypes.RELEASE, StageTypes.OPERATE);

    Collection<ApplicationView> appViews = loader.getViolations(Collections.singletonList(app),
        Arrays.asList(StageTypes.BUILD, StageTypes.RELEASE), false, violation -> true);

    assertThat(appViews).hasSize(1);
    ApplicationView appView = appViews.iterator().next();
    assertThat(appView.getApplication()).isEqualTo(app);
    assertThat(appView.getStageViews()).extracting(ApplicationStageView::getStageType)
        .containsExactlyInAnyOrder(StageTypes.BUILD, StageTypes.RELEASE);
  }

  @Test
  public void testGetViolations_NullStageTypes() {
    testGetViolations_AllStageTypes(null);
  }

  @Test
  public void testGetViolations_EmptyStageTypes() {
    testGetViolations_AllStageTypes(Collections.emptyList());
  }

  private void testGetViolations_AllStageTypes(Collection<StageType> stageTypes) {
    StageType[] evaluatedStageTypes = {
      StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE,
      StageTypes.OPERATE
    };
    Application app = createApplication(evaluatedStageTypes);

    Collection<ApplicationView> appViews =
        loader.getViolations(Collections.singletonList(app), stageTypes, false, violation -> true);

    assertThat(appViews).hasSize(1);
    ApplicationView appView = appViews.iterator().next();
    assertThat(appView.getApplication()).isEqualTo(app);
    assertThat(appView.getStageViews()).extracting(ApplicationStageView::getStageType)
        .containsExactlyInAnyOrderElementsOf(StageTypes.getAll());
    for (ApplicationStageView appStageView : appView.getStageViews()) {
      StageType stageType = appStageView.getStageType();
      if (Arrays.asList(evaluatedStageTypes).contains(stageType)) {
        assertThat(appStageView.getLastEvaluation()).as(stageType.toString()).isNotNull();
        assertThat(appStageView.getFilteredViolations()).as(stageType.toString()).hasSize(2);
      }
      else {
        assertThat(appStageView.getLastEvaluation()).as(stageType.toString()).isNull();
        assertThat(appStageView.getFilteredViolations()).as(stageType.toString()).isEmpty();
      }
    }
  }

  @Test
  public void testGetViolations_ActiveViolationsOnly() {
    Application app = createApplication(StageTypes.BUILD);

    Collection<ApplicationView> appViews = loader.getViolations(Collections.singletonList(app),
        Collections.singletonList(StageTypes.BUILD),
        true, violation -> true);

    assertThat(appViews).hasSize(1);
    ApplicationView appView = appViews.iterator().next();
    assertThat(appView.getApplication()).isEqualTo(app);
    assertThat(appView.getStageViews()).hasSize(1);
    ApplicationStageView appStageView = appView.getStageViews().iterator().next();
    assertThat(appStageView.getStageType()).isEqualTo(StageTypes.BUILD);
    assertThat(appStageView.getFilteredViolations()).hasSize(1);
    assertThat(appStageView.getFilteredViolations().iterator().next().isWaived()).isFalse();
  }

  @Test
  public void testGetViolations_FilterByViolations() {
    Application app = createApplication(StageTypes.BUILD);

    Collection<ApplicationView> appViews = loader.getViolations(Collections.singletonList(app),
        Collections.singletonList(StageTypes.BUILD),
        false, violation -> violation.getThreatLevel() == 10);

    assertThat(appViews).hasSize(1);
    ApplicationView appView = appViews.iterator().next();
    assertThat(appView.getApplication()).isEqualTo(app);
    assertThat(appView.getStageViews()).hasSize(1);
    ApplicationStageView appStageView = appView.getStageViews().iterator().next();
    assertThat(appStageView.getStageType()).isEqualTo(StageTypes.BUILD);
    assertThat(appStageView.getFilteredViolations()).hasSize(1);
    assertThat(appStageView.getFilteredViolations().iterator().next().getThreatLevel()).isEqualTo(10);
  }

  @Test
  public void testGetViolations_NullViolationFilter() {
    Application app = createApplication(StageTypes.BUILD);

    Collection<ApplicationView> appViews = loader.getViolations(Collections.singletonList(app),
        Collections.singletonList(StageTypes.BUILD),
        false, null);

    assertThat(appViews).hasSize(1);
    ApplicationView appView = appViews.iterator().next();
    assertThat(appView.getApplication()).isEqualTo(app);
    assertThat(appView.getStageViews()).hasSize(1);
    ApplicationStageView appStageView = appView.getStageViews().iterator().next();
    assertThat(appStageView.getStageType()).isEqualTo(StageTypes.BUILD);
    assertThat(appStageView.getFilteredViolations()).hasSize(2);
  }

  @Test
  public void testGetViolations_StageWithoutEvaluation() {
    Application app = createApplication();

    Collection<ApplicationView> appViews = loader.getViolations(Collections.singletonList(app),
        Collections.singletonList(StageTypes.BUILD),
        false, violation -> true);

    assertThat(appViews).hasSize(1);
    ApplicationView appView = appViews.iterator().next();
    assertThat(appView.getApplication()).isEqualTo(app);
    assertThat(appView.getStageViews()).hasSize(1);
    ApplicationStageView appStageView = appView.getStageViews().iterator().next();
    assertThat(appStageView.getStageType()).isEqualTo(StageTypes.BUILD);
    assertThat(appStageView.getLastEvaluation()).isNull();
    assertThat(appStageView.getFilteredViolations()).isEmpty();
  }

  @Test
  public void testGetViolations_OpenedAfterDate() {
    Date beforeAppCreation = new Date(Instant.now().minus(Duration.ofMinutes(1)).toEpochMilli());

    Application app = createApplication(StageTypes.BUILD);

    Collection<ApplicationView> appViewsFilteredWithBeforeDate = loader.getViolations(Collections.singletonList(app),
        Collections.singletonList(StageTypes.BUILD), false, violation -> true, beforeAppCreation, null, null, null);

    assertThat(appViewsFilteredWithBeforeDate).hasSize(1);
    ApplicationView appViewBefore = appViewsFilteredWithBeforeDate.iterator().next();
    assertThat(appViewBefore.getApplication()).isEqualTo(app);
    assertThat(appViewBefore.getStageViews()).hasSize(1);
    ApplicationStageView appStageViewBefore = appViewBefore.getStageViews().iterator().next();
    assertThat(appStageViewBefore.getStageType()).isEqualTo(StageTypes.BUILD);
    assertThat(appStageViewBefore.getFilteredViolations()).hasSize(2);

    Date afterAppCreation = new Date(Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli());
    Collection<ApplicationView> appViewsFilteredWithAfterDate = loader.getViolations(Collections.singletonList(app),
        Collections.singletonList(StageTypes.BUILD), false, violation -> true, afterAppCreation, null, null, null);
    assertThat(appViewsFilteredWithAfterDate).hasSize(1);
    ApplicationView appViewAfter = appViewsFilteredWithAfterDate.iterator().next();
    assertThat(appViewAfter.getApplication()).isEqualTo(app);
    assertThat(appViewAfter.getStageViews()).hasSize(1);
    ApplicationStageView appStageViewAfter = appViewAfter.getStageViews().iterator().next();
    assertThat(appStageViewAfter.getStageType()).isEqualTo(StageTypes.BUILD);
    assertThat(appStageViewAfter.getLastEvaluation()).isNull();
    assertThat(appStageViewAfter.getFilteredViolations()).isEmpty();
  }

  @Test
  public void testGetViolations_FilterByWaived_DoesNotIncludeExcludedViolations() {
    final String scanId = "scan-id";
    final Organization org = tempEntity.newOrganization();
    final Application app = tempEntity.newApplication(org.getId());
    final Policy policy = tempEntity.newPolicy(org.getId());
    final PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(),
        scanId);
    final AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(org.getId());
    final PolicyViolation policyViolation1 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);
    final PolicyViolation policyViolation2 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);
    final PolicyViolation policyViolation3 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);

    // No exclusions exist
    Collection<PolicyViolationLoader.ApplicationView> results = loader.getViolations(List.of(app),
        List.of(StageTypes.BUILD), false, violation -> true, null, null, null,
        new PolicyViolationStateFilter(Set.of(PolicyViolationState.WAIVED)));

    List<PolicyViolation> violations = extractPolicyViolations(results);
    assertThat(violations)
        .hasSize(3)
        .extracting(AbstractPolicyViolation::getId)
        .containsExactlyInAnyOrder(policyViolation1.getId(), policyViolation2.getId(), policyViolation3.getId());

    // Add an exclusion for policyViolation1, so it should not be included in the results
    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), "", "", waiver.getId(), scanId, policyViolation1);

    results = loader.getViolations(List.of(app), List.of(StageTypes.BUILD), false, violation -> true,
        null, null, null, new PolicyViolationStateFilter(Set.of(PolicyViolationState.WAIVED)));

    violations = extractPolicyViolations(results);
    assertThat(violations)
        .hasSize(2)
        .extracting(AbstractPolicyViolation::getId)
        .containsExactlyInAnyOrder(policyViolation2.getId(), policyViolation3.getId());
  }

  @Test
  public void testGetViolations_FilterByWaived_DoesNotCheckForExcludedViolations_WhenFilteringByWaivedPlusOtherState() {
    final String scanId = "scan-id";
    final Organization org = tempEntity.newOrganization();
    final Application app = tempEntity.newApplication(org.getId());
    final Policy policy = tempEntity.newPolicy(org.getId());
    final PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), StageTypes.BUILD.getId(),
        scanId);
    final AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(org.getId());
    final PolicyViolation policyViolation1 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);
    final PolicyViolation policyViolation2 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);
    final PolicyViolation policyViolation3 = tempEntity.newAutoWaivedPolicyViolation(evaluation, policy, waiver);

    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), "", "", waiver.getId(), scanId, policyViolation1);

    final Collection<PolicyViolationLoader.ApplicationView> results = loader.getViolations(List.of(app),
        List.of(StageTypes.BUILD), false, violation -> true, null, null, null,
        new PolicyViolationStateFilter(Set.of(PolicyViolationState.WAIVED, PolicyViolationState.LEGACY_VIOLATION)));

    final List<PolicyViolation> violations = extractPolicyViolations(results);
    assertThat(violations)
        .hasSize(3)
        .extracting(AbstractPolicyViolation::getId)
        .containsExactlyInAnyOrder(policyViolation1.getId(), policyViolation2.getId(), policyViolation3.getId());
  }

  private static List<PolicyViolation> extractPolicyViolations(
      final Collection<PolicyViolationLoader.ApplicationView> appViews)
  {
    return appViews.stream()
        .map(PolicyViolationLoader.ApplicationView::getStageViews)
        .flatMap(Collection::stream)
        .map(PolicyViolationLoader.ApplicationStageView::getFilteredViolations)
        .flatMap(Collection::stream)
        .toList();
  }
}
