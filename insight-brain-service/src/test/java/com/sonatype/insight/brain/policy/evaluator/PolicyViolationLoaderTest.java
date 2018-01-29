/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationStageView;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationLoader.ApplicationView;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class PolicyViolationLoaderTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyViolationLoader loader;

  private Application createApplication(StageType... stageTypes) {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy1 = tempEntity.newPolicy(app.getId(), "Test Policy 1", 10);
    Policy policy2 = tempEntity.newPolicy(app.getId(), "Test Policy 2", 1);
    long time = System.currentTimeMillis();
    for (StageType stageType : stageTypes) {
      PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stageType.getId(),
          stageType.getId() + "-scan-id", new Date(time - 2000));
      tempEntity.newPolicyViolation(eval, policy1);
      eval = tempEntity.newPolicyEvaluation(app.getId(), stageType.getId(), stageType.getId() + "-latest-scan-id",
          new Date(time - 1000));
      tempEntity.newPolicyViolation(eval, policy1);
      tempEntity.newPolicyViolation(eval, policy2);
    }
    return app;
  }

  @Test
  public void testGetViolations_BasicData() {
    Application app = createApplication(StageTypes.BUILD);

    Collection<ApplicationView> appViews = loader.getViolations(Arrays.asList(app), Arrays.asList(StageTypes.BUILD),
        violation -> true);

    assertThat(appViews, hasSize(1));
    ApplicationView appView = appViews.iterator().next();
    assertThat(appView.getApplication(), is(app));
    assertThat(appView.getStageViews(), hasSize(1));
    ApplicationStageView appStageView = appView.getStageViews().iterator().next();
    assertThat(appStageView.getStageType(), is(StageTypes.BUILD));
    assertThat(appStageView.getLastEvaluation(), is(notNullValue()));
    assertThat(appStageView.getLastEvaluation().getApplicationId(), is(app.getId()));
    assertThat(appStageView.getLastEvaluation().getStageTypeId(), is(StageTypes.BUILD.getId()));
    assertThat(appStageView.getLastEvaluation().getScanId(), is("build-latest-scan-id"));
    assertThat(appStageView.getFilteredViolations(), hasSize(2));
  }

  @Test
  public void testGetViolations_FilterByApplications() {
    Application app1 = createApplication(StageTypes.BUILD);
    createApplication(StageTypes.BUILD);
    Application app3 = createApplication(StageTypes.BUILD);

    Collection<ApplicationView> appViews = loader.getViolations(Arrays.asList(app1, app3),
        Arrays.asList(StageTypes.BUILD), violation -> true);

    assertThat(appViews, hasSize(2));
    assertThat(appViews.stream().map(ApplicationView::getApplication).collect(toSet()), containsInAnyOrder(app1, app3));
  }

  @Test
  public void testGetViolations_FilterByStageTypes() {
    Application app = createApplication(StageTypes.BUILD, StageTypes.RELEASE, StageTypes.OPERATE);

    Collection<ApplicationView> appViews = loader.getViolations(Arrays.asList(app),
        Arrays.asList(StageTypes.BUILD, StageTypes.RELEASE), violation -> true);

    assertThat(appViews, hasSize(1));
    ApplicationView appView = appViews.iterator().next();
    assertThat(appView.getApplication(), is(app));
    assertThat(appView.getStageViews(), hasSize(2));
    assertThat(appView.getStageViews().stream().map(ApplicationStageView::getStageType).collect(toSet()),
        containsInAnyOrder(StageTypes.BUILD, StageTypes.RELEASE));
  }

  @Test
  public void testGetViolations_FilterByViolations() {
    Application app = createApplication(StageTypes.BUILD);

    Collection<ApplicationView> appViews = loader.getViolations(Arrays.asList(app), Arrays.asList(StageTypes.BUILD),
        violation -> violation.getThreatLevel() == 10);

    assertThat(appViews, hasSize(1));
    ApplicationView appView = appViews.iterator().next();
    assertThat(appView.getApplication(), is(app));
    assertThat(appView.getStageViews(), hasSize(1));
    ApplicationStageView appStageView = appView.getStageViews().iterator().next();
    assertThat(appStageView.getStageType(), is(StageTypes.BUILD));
    assertThat(appStageView.getFilteredViolations(), hasSize(1));
    assertThat(appStageView.getFilteredViolations().iterator().next().getThreatLevel(), is(10));
  }

  @Test
  public void testGetViolations_NullViolationFilter() {
    Application app = createApplication(StageTypes.BUILD);

    Collection<ApplicationView> appViews = loader.getViolations(Arrays.asList(app), Arrays.asList(StageTypes.BUILD),
        null);

    assertThat(appViews, hasSize(1));
    ApplicationView appView = appViews.iterator().next();
    assertThat(appView.getApplication(), is(app));
    assertThat(appView.getStageViews(), hasSize(1));
    ApplicationStageView appStageView = appView.getStageViews().iterator().next();
    assertThat(appStageView.getStageType(), is(StageTypes.BUILD));
    assertThat(appStageView.getFilteredViolations(), hasSize(2));
  }

  @Test
  public void testGetViolations_StageWithoutEvaluation() {
    Application app = createApplication();

    Collection<ApplicationView> appViews = loader.getViolations(Arrays.asList(app), Arrays.asList(StageTypes.BUILD),
        violation -> true);

    assertThat(appViews, hasSize(1));
    ApplicationView appView = appViews.iterator().next();
    assertThat(appView.getApplication(), is(app));
    assertThat(appView.getStageViews(), hasSize(1));
    ApplicationStageView appStageView = appView.getStageViews().iterator().next();
    assertThat(appStageView.getStageType(), is(StageTypes.BUILD));
    assertThat(appStageView.getLastEvaluation(), is(nullValue()));
    assertThat(appStageView.getFilteredViolations(), hasSize(0));
  }
}
