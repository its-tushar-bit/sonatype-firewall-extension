/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DashboardFilterServiceTest
    extends AbstractComponentTest
{
  @Inject
  private DashboardFilterService dashboardFilterService;

  private Organization org;
  private Application app1;
  private Application app2;
  private Policy orgPolicy;
  private Policy app1Policy;
  private PolicyEvaluation app1PolicyEvaluation;
  private PolicyEvaluation app2PolicyEvaluation;
  private PolicyViolation orgPolicyViolation;
  private PolicyViolation app1PolicyViolation;
  private PolicyViolation app2PolicyViolation;
  private Tag tag1;
  private Tag tag2;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("app1", "app1", org.getId());
    app2 = tempEntity.newApplication("app2", "app2", org.getId());
    tempEntity.newPolicy(org.getParentOrganizationId(), "root org owned policy", 4);
    orgPolicy = tempEntity.newPolicy(org.getId(), "org owned policy", 3);
    app1Policy = tempEntity.newPolicy(app1.getId(), "app owned policy", 5);
    long time = System.currentTimeMillis() - 1000;
    app1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id",
        new Date(time));
    app2PolicyEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 id",
        new Date(time + 1));
    orgPolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy);
    app1PolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);
    app2PolicyViolation = tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(orgPolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newFirstOccurrencePolicyViolation(app1PolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newFirstOccurrencePolicyViolation(app2PolicyViolation.getId(), app2.getId(), BuildStageType.ID);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-3", MatchState.SIMILAR, false);
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-4", MatchState.UNKNOWN, false);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"));
    tag1 = tempEntity.newTag(org.getId());
    tag2 = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app1.getId(), tag1.getId());
    tempEntity.newApplicationTag(app1.getId(), tag2.getId());
  }

  @Test
  public void testGetFilterSummary_NoFilter() throws Exception {
    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(null, null, null, null, null);
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.matchedPolicies, is(3));
    assertThat(summary.matchedComponents, is(4));
  }

  @Test
  public void testGetFilterSummary_FilterByApp() throws Exception {
    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(Collections.singleton(app2.getId()), null, null,
        null, null);
    assertThat(summary.matchedApplications, is(1));
    assertThat(summary.matchedPolicies, is(2));
    assertThat(summary.matchedComponents, is(1));
  }

  @Test
  public void testGetFilterSummary_FilterByTag() throws Exception {
    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());

    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(null, null,
        Collections.singleton(app2Tag.getId()), null, null);
    assertThat(summary.matchedApplications, is(1));
    assertThat(summary.matchedPolicies, is(2));
    assertThat(summary.matchedComponents, is(1));
  }

  @Test
  public void testGetFilterSummary_FilterByPolicyThreatLevel() throws Exception {
    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(null, null, null, null,
        new PolicyThreatLevelFilter(orgPolicy.getThreatLevel(), orgPolicy.getThreatLevel()));
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.matchedPolicies, is(1));
    assertThat(summary.matchedComponents, is(4));
  }

  @Test
  public void testGetFilterSummary_FilterByPolicyThreatCategory() throws Exception {
    Constraint constraint = new Constraint(null, "Test Constraint", LogicalOperator.AND);
    constraint.addCondition(new Condition(LicenseConditionType.ID, "is", "GPL-2.0"));
    orgPolicy.setConstraints(Collections.singletonList(constraint));
    new PolicyDAO().update(orgPolicy);

    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(null, null, null,
        new PolicyThreatCategoryFilter(PolicyThreatCategory.LICENSE), null);
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.matchedPolicies, is(1));
    assertThat(summary.matchedComponents, is(4));
  }

  @Test
  public void testGetFilterSummary_FilterByStage() throws Exception {
    FilterSummaryDTO summary = dashboardFilterService.getFilterSummary(null,
        Collections.singleton(ReleaseStageType.ID), null, null, null);
    assertThat(summary.matchedApplications, is(2));
    assertThat(summary.matchedPolicies, is(3));
    assertThat(summary.matchedComponents, is(2));
  }

  @Test
  public void testDashboardFilterDefaultFilter() throws Exception {
    DashboardFilterDTO actual = dashboardFilterService.getDashboardFilterForCurrentUser();
    // Register to make sure the the filter is deleted after the test
    tempEntity.register(new DashboardFilterDAO().getByUsername(USERNAME));
    assertThat(actual, notNullValue());

    assertThat(actual.minPolicyThreatLevel, is(2));
    assertThat(actual.maxPolicyThreatLevel, is(10));
    assertThat(actual.applicationFilters, hasSize(0));
    assertThat(actual.tagFilters, hasSize(0));
    assertThat(actual.policyThreatCategoryFilters, hasSize(0));
    assertThat(actual.stageTypeFilters, hasSize(0));
  }
}
