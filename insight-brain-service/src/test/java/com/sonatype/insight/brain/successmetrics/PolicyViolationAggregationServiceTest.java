/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationResolutionStateDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationResolutionState;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.ThreatLevel;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class PolicyViolationAggregationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyViolationAggregationService service;

  @Inject
  private PolicyViolationDAO violationDAO;

  @Inject
  private PolicyViolationResolutionStateDAO resolutionStateDAO;

  @Inject
  private PolicyViolationAggregationDAO aggregationDAO;

  @Test
  public void testGeneratePolicyViolationAggregations_ViolationsWithoutHash_AllResolved() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    DateTime now = new DateTime().withDayOfMonth(10);

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusHours(72).toDate());
    // three distinct violation entities which are "equal" per PolicyViolationComparator
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, null, null, "unknown component");
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval1, policy, null, null, "unknown component");
    PolicyViolation violation3 = tempEntity.newPolicyViolation(eval1, policy, null, null, "unknown component");

    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan2",
        now.minusHours(48).toDate());
    // first violation resolved after 24 hours
    violation1.setFixTime(eval2.getTime());
    violationDAO.update(violation1);

    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan3",
        now.minusHours(24).toDate());
    // second and third violation resolved after 48 hours
    violation2.setFixTime(eval3.getTime());
    violationDAO.update(violation2);
    violation3.setFixTime(eval3.getTime());
    violationDAO.update(violation3);

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    assertThat(resolutionStateDAO.getByApplicationId(app.getId()), hasSize(0));

    PolicyViolationAggregation aggregation = aggregationDAO.getMostRecentByApplicationId(app.getId());

    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL), is(1));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.SEVERE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.MODERATE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.LOW), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.LICENSE, ThreatLevel.CRITICAL), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.LICENSE, ThreatLevel.SEVERE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.LICENSE, ThreatLevel.MODERATE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.LICENSE, ThreatLevel.LOW), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.QUALITY, ThreatLevel.CRITICAL), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.QUALITY, ThreatLevel.SEVERE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.QUALITY, ThreatLevel.MODERATE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.QUALITY, ThreatLevel.LOW), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.OTHER, ThreatLevel.CRITICAL), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.OTHER, ThreatLevel.SEVERE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.OTHER, ThreatLevel.MODERATE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.OTHER, ThreatLevel.LOW), is(0));

    assertThat(aggregation.getResolvedCountCriticalThreat(), is(1));
    assertThat(aggregation.getResolvedCountSevereThreat(), is(0));
    assertThat(aggregation.getResolvedCountModerateThreat(), is(0));
    assertThat(aggregation.getResolvedCountLowThreat(), is(0));

    assertThat(aggregation.getMttrCriticalThreat(), is(TimeUnit.HOURS.toMillis(48)));
    assertThat(aggregation.getMttrSevereThreat(), is(nullValue()));
    assertThat(aggregation.getMttrModerateThreat(), is(nullValue()));
    assertThat(aggregation.getMttrLowThreat(), is(nullValue()));
  }

  @Test
  public void testGeneratePolicyViolationAggregations_ViolationsWithoutHash_SomeUnresolved() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    DateTime now = new DateTime().withDayOfMonth(10);

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusHours(72).toDate());
    // two distinct violation entities which are "equal" per PolicyViolationComparator
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, null, null, "unknown component");
    tempEntity.newPolicyViolation(eval1, policy, null, null, "unknown component");

    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan2",
        now.minusHours(48).toDate());
    // first violation resolved after 24 hours, second violation remains unresolved
    violation1.setFixTime(eval2.getTime());
    violationDAO.update(violation1);

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    List<PolicyViolationResolutionState> resolutionStates = resolutionStateDAO.getByApplicationId(app.getId());
    assertThat(resolutionStates, hasSize(1));
    PolicyViolationResolutionState resolutionState = resolutionStates.get(0);
    assertThat(resolutionState.getFirstOccurrenceTime(), is(eval1.getTime()));
    assertThat(resolutionState.getPolicyId(), is(policy.getId()));
    assertThat(resolutionState.getHash(), is(violation1.getHash()));
    assertThat(resolutionState.getComponentIdentifier(), is(violation1.getComponentIdentifier()));
    assertThat(resolutionState.getBuildStageType(), is(true));

    PolicyViolationAggregation aggregation = aggregationDAO.getMostRecentByApplicationId(app.getId());

    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL), is(1));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.SEVERE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.MODERATE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.LOW), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.LICENSE, ThreatLevel.CRITICAL), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.LICENSE, ThreatLevel.SEVERE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.LICENSE, ThreatLevel.MODERATE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.LICENSE, ThreatLevel.LOW), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.QUALITY, ThreatLevel.CRITICAL), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.QUALITY, ThreatLevel.SEVERE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.QUALITY, ThreatLevel.MODERATE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.QUALITY, ThreatLevel.LOW), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.OTHER, ThreatLevel.CRITICAL), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.OTHER, ThreatLevel.SEVERE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.OTHER, ThreatLevel.MODERATE), is(0));
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.OTHER, ThreatLevel.LOW), is(0));

    assertThat(aggregation.getResolvedCountCriticalThreat(), is(0));
    assertThat(aggregation.getResolvedCountSevereThreat(), is(0));
    assertThat(aggregation.getResolvedCountModerateThreat(), is(0));
    assertThat(aggregation.getResolvedCountLowThreat(), is(0));

    assertThat(aggregation.getMttrCriticalThreat(), is(nullValue()));
    assertThat(aggregation.getMttrSevereThreat(), is(nullValue()));
    assertThat(aggregation.getMttrModerateThreat(), is(nullValue()));
    assertThat(aggregation.getMttrLowThreat(), is(nullValue()));
  }
}
