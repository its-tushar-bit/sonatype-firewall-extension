/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Sets;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PolicySummaryServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicySummaryService policySummaryService;

  private Organization org;
  private Application app1;
  private Application app2;
  private Policy orgPolicy;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("app1", "app1", org.getId());
    app2 = tempEntity.newApplication("app2", "app2", org.getId());
    orgPolicy = tempEntity.newPolicy(org.getId(), "org owned policy", 3);
  }

  @Test
  public void testGetPolicySummary_FilterByApplication() throws Exception {
    DateTime now = new DateTime();
    // One policy violation for app1, week 1
    PolicyEvaluation pe1App1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1App1", now
        .minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1App1, orgPolicy, "g1", "a1", "v1", "h1", "r1");
    // One policy violation for app1, week 2. Fixes policy violation from week 1.
    PolicyEvaluation pe2App1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId2App1", now
        .minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS - 1).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe2App1, orgPolicy, "g2", "a2", "v2", "h2", "r2");
    // One policy violation for app2, week 1
    PolicyEvaluation pe1App2 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1App2", now
        .minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1App2, orgPolicy, "g3", "a3", "v3", "h3", "r3");

    PolicySummaryDTO dto = policySummaryService.getPolicySummary(Collections.singleton(app1.getId()), null, null, null,
        null);
    assertCounters(dto, 2, 0, 1, 1);
    assertCountersWeek(dto, 0, 1, 0, 0, 1);
    DescriptiveStatistics ageWaivedStatistics = createStatistics(0);
    DescriptiveStatistics ageFixedStatistics = createStatistics(pe2App1.getTime().getTime()
        - pe1App1.getTime().getTime());
    DescriptiveStatistics ageUnresolvedStatistics = createStatistics(dto.timestamp - pe2App1.getTime().getTime());
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);

    dto = policySummaryService.getPolicySummary(Sets.newHashSet(app1.getId(), app2.getId()), null, null, null, null);
    assertCounters(dto, 3, 0, 1, 2);
    assertCountersWeek(dto, 0, 2, 0, 0, 2);
    ageUnresolvedStatistics = createStatistics(dto.timestamp - pe2App1.getTime().getTime(), dto.timestamp
        - pe1App2.getTime().getTime());
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);
  }

  @Test
  public void testGetPolicySummary_FilterByStage() throws Exception {
    DateTime now = new DateTime();
    // One policy violation for BuildStageType
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1, orgPolicy, "g1", "a1", "v1", "h1", "r1");
    // Two policy violations for ReleaseStageType
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "scanId2",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe2, orgPolicy, "g2", "a2", "v2", "h2", "r2");
    tempEntity.newPolicyViolation(pe2, orgPolicy, "g3", "a3", "v3", "h3", "r3");

    PolicySummaryDTO dto = policySummaryService.getPolicySummary(null, Collections.singleton(BuildStageType.ID), null,
        null, null);
    assertCounters(dto, 1, 0, 0, 1);
    assertCountersWeek(dto, 0, 1, 0, 0, 1);
    DescriptiveStatistics ageWaivedStatistics = createStatistics(0);
    DescriptiveStatistics ageFixedStatistics = createStatistics(0);
    DescriptiveStatistics ageUnresolvedStatistics = createStatistics(dto.timestamp - pe1.getTime().getTime());
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);

    dto = policySummaryService.getPolicySummary(null, Sets.newHashSet(BuildStageType.ID, ReleaseStageType.ID), null,
        null,
        null);
    assertCounters(dto, 3, 0, 0, 3);
    assertCountersWeek(dto, 0, 3, 0, 0, 3);
    ageUnresolvedStatistics = createStatistics(dto.timestamp - pe1.getTime().getTime(), dto.timestamp
        - pe2.getTime().getTime(), dto.timestamp - pe2.getTime().getTime());
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);
  }

  @Test
  public void testGetPolicySummary_FilterByStage_ExcludesDevelop() throws Exception {
    DateTime now = new DateTime();
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1, orgPolicy, "g1", "a1", "v1", "h1", "r1");
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(app2.getId(), DevelopStageType.ID, "scanId2",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe2, orgPolicy, "g2", "a2", "v2", "h2", "r2");

    PolicySummaryDTO dto = policySummaryService.getPolicySummary(null, null, null, null, null);
    assertCounters(dto, 1, 0, 0, 1);
    assertCountersWeek(dto, 0, 1, 0, 0, 1);

    try {
      policySummaryService.getPolicySummary(null, Collections.singleton(DevelopStageType.ID), null, null, null);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid stage type: develop."));
    }
  }

  @Test
  public void testGetPolicySummary_FilterByTag() throws Exception {
    DateTime now = new DateTime();
    Tag app1Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app1.getId(), app1Tag.getId());
    Tag app2Tag = tempEntity.newTag(org.getId());
    tempEntity.newApplicationTag(app2.getId(), app2Tag.getId());
    // One policy violation for app1Tag
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1, orgPolicy, "g1", "a1", "v1", "h1", "r1");
    // Two policy violations for app2Tag
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId2",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe2, orgPolicy, "g2", "a2", "v2", "h2", "r2");
    tempEntity.newPolicyViolation(pe2, orgPolicy, "g3", "a3", "v3", "h3", "r3");

    PolicySummaryDTO dto = policySummaryService.getPolicySummary(null, null, Collections.singleton(app1Tag.getId()),
        null,
        null);
    assertCounters(dto, 1, 0, 0, 1);
    assertCountersWeek(dto, 0, 1, 0, 0, 1);
    DescriptiveStatistics ageWaivedStatistics = createStatistics(0);
    DescriptiveStatistics ageFixedStatistics = createStatistics(0);
    DescriptiveStatistics ageUnresolvedStatistics = createStatistics(dto.timestamp - pe1.getTime().getTime());
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);

    dto = policySummaryService.getPolicySummary(null, null, Sets.newHashSet(app1Tag.getId(), app2Tag.getId()), null,
        null);
    assertCounters(dto, 3, 0, 0, 3);
    assertCountersWeek(dto, 0, 3, 0, 0, 3);
    ageUnresolvedStatistics = createStatistics(dto.timestamp - pe1.getTime().getTime(), dto.timestamp
        - pe2.getTime().getTime(), dto.timestamp - pe2.getTime().getTime());
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);
  }

  @Test
  public void testGetPolicySummary_FilterByPolicyThreatCategory() throws Exception {
    DateTime now = new DateTime();
    // One policy violation with policy threat category LICENSE
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1, orgPolicy, 5, PolicyThreatCategory.LICENSE, "g1", "a1", "v1", "h1");
    // Two policy violations with policy threat category SECURITY
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId2",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe2, orgPolicy, 5, PolicyThreatCategory.SECURITY, "g2", "a2", "v2", "h2");
    tempEntity.newPolicyViolation(pe2, orgPolicy, 5, PolicyThreatCategory.SECURITY, "g3", "a3", "v3", "h3");

    PolicySummaryDTO dto = policySummaryService.getPolicySummary(null, null, null, new PolicyThreatCategoryFilter(
        PolicyThreatCategory.LICENSE), null);
    assertCounters(dto, 1, 0, 0, 1);
    assertCountersWeek(dto, 0, 1, 0, 0, 1);
    DescriptiveStatistics ageWaivedStatistics = createStatistics(0);
    DescriptiveStatistics ageFixedStatistics = createStatistics(0);
    DescriptiveStatistics ageUnresolvedStatistics = createStatistics(dto.timestamp - pe1.getTime().getTime());
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);

    dto = policySummaryService.getPolicySummary(null, null, null, new PolicyThreatCategoryFilter(
        PolicyThreatCategory.LICENSE, PolicyThreatCategory.SECURITY), null);
    assertCounters(dto, 3, 0, 0, 3);
    assertCountersWeek(dto, 0, 3, 0, 0, 3);
    ageUnresolvedStatistics = createStatistics(dto.timestamp - pe1.getTime().getTime(), dto.timestamp
        - pe2.getTime().getTime(), dto.timestamp - pe2.getTime().getTime());
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);
  }

  @Test
  public void testGetPolicySummary_FilterByPolicyThreatLevel() throws Exception {
    DateTime now = new DateTime();
    // One policy violation for policy threat level 5
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1, orgPolicy, 5, PolicyThreatCategory.LICENSE, "g1", "a1", "v1", "h1");
    // Two policy violations for policy threat level 8
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId2",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe2, orgPolicy, 8, PolicyThreatCategory.SECURITY, "g2", "a2", "v2", "h2");
    tempEntity.newPolicyViolation(pe2, orgPolicy, 8, PolicyThreatCategory.SECURITY, "g3", "a3", "v3", "h3");

    PolicySummaryDTO dto = policySummaryService.getPolicySummary(null, null, null, null, new PolicyThreatLevelFilter(5,
        5));
    assertCounters(dto, 1, 0, 0, 1);
    assertCountersWeek(dto, 0, 1, 0, 0, 1);
    DescriptiveStatistics ageWaivedStatistics = createStatistics(0);
    DescriptiveStatistics ageFixedStatistics = createStatistics(0);
    DescriptiveStatistics ageUnresolvedStatistics = createStatistics(dto.timestamp - pe1.getTime().getTime());
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);

    dto = policySummaryService.getPolicySummary(null, null, null, null, new PolicyThreatLevelFilter(5, 8));
    assertCounters(dto, 3, 0, 0, 3);
    assertCountersWeek(dto, 0, 3, 0, 0, 3);
    ageUnresolvedStatistics = createStatistics(dto.timestamp - pe1.getTime().getTime(), dto.timestamp
        - pe2.getTime().getTime(), dto.timestamp - pe2.getTime().getTime());
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);
  }

  @Test
  public void testGetPolicySummary_InitialData() throws Exception {
    DateTime now = new DateTime();

    // Data before week 0. One app1 violation.
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).minusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    // Data during week 0. Adds one app1 violation.
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scanId2",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe2, orgPolicy, "g2", "a2", "v2", "h2", "r2");

    // Data during week 1. Fixes initial app1 violation.
    PolicyEvaluation pe3 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId3",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS - 1).plusDays(1).toDate());

    // Data during week 1. Adds one app2 violation
    PolicyEvaluation pe4 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId4",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS - 1).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe4, orgPolicy, "g4", "a4", "v4", "h4", "r4");

    // Data during week 2. Waives app2 violation
    PolicyWaiver waiver = tempEntity.newWaiver("abababab", orgPolicy.getId(), app2.getId());

    PolicyEvaluation pe5 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId5",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS - 2).plusDays(1).toDate());
    tempEntity.newWaivedPolicyViolation(pe5, orgPolicy, "g4", "a4", "v4", "h4", waiver);

    // Data during week 3. Fixes app2 violation
    PolicyEvaluation pe6 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId6",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS - 3).plusDays(1).toDate());

    PolicySummaryDTO dto = policySummaryService.getPolicySummary(null, null, null, null, null);
    assertCounters(dto, 3, 0, 2, 1);
    assertCountersWeek(dto, 0, 1, 0, 0, 1);
    assertCountersWeek(dto, 1, 1, 0, 1, 0);
    assertCountersWeek(dto, 2, 0, 1, 0, -1);
    assertCountersWeek(dto, 3, 0, -1, 1, 0);
    for (int iWeek = 4; iWeek < PolicySummaryService.POLICY_SUMMARY_WEEKS; iWeek++) {
      assertCountersWeek(dto, iWeek, 0, 0, 0, 0);
    }
    DescriptiveStatistics ageWaivedStatistics = createStatistics(pe6.getTime().getTime() - pe5.getTime().getTime());
    DescriptiveStatistics ageFixedStatistics = createStatistics(pe3.getTime().getTime() - pe1.getTime().getTime(), pe6
        .getTime().getTime() - pe4.getTime().getTime());
    DescriptiveStatistics ageUnresolvedStatistics = createStatistics(dto.timestamp - pe2.getTime().getTime());
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);
  }

  @Test
  public void testGetPolicySummary_Waived() {
    DateTime now = new DateTime();

    // Data before week 0. One app1 violation.
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).minusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    // Data during week 0. Waives app1 violation.
    PolicyWaiver waiver = tempEntity.newWaiver("abababab", orgPolicy.getId(), app1.getId());
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId2",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newWaivedPolicyViolation(pe2, orgPolicy, "g1", "a1", "v1", "h1", waiver);

    PolicySummaryDTO dto = policySummaryService.getPolicySummary(null, null, null, null, null);
    assertCounters(dto, 1, 1, 0, 0);
    assertCountersWeek(dto, 0, 0, 1, 0, -1);
    DescriptiveStatistics ageWaivedStatistics = createStatistics(dto.timestamp - pe2.getTime().getTime());
    DescriptiveStatistics ageFixedStatistics = createStatistics(0);
    DescriptiveStatistics ageUnresolvedStatistics = createStatistics(0);
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);
  }

  @Test
  public void testGetPolicySummary_WaivedAndUnwaived() {
    DateTime now = new DateTime();

    // Data before week 0. One app1 violation.
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).minusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    // Data during week 0. Waives app1 violation.
    PolicyWaiver waiver = tempEntity.newWaiver("abababab", orgPolicy.getId(), app1.getId());
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId2",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newWaivedPolicyViolation(pe2, orgPolicy, "g1", "a1", "v1", "h1", waiver);

    // Data during week 1. Unwaives app1 violation.
    PolicyEvaluation pe3 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId3",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS - 1).plusDays(2).toDate());
    tempEntity.newPolicyViolation(pe3, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    PolicySummaryDTO dto = policySummaryService.getPolicySummary(null, null, null, null, null);
    assertCounters(dto, 1, 0, 0, 1);
    assertCountersWeek(dto, 0, 0, 1, 0, -1);
    assertCountersWeek(dto, 1, 0, -1, 0, 1);
    DescriptiveStatistics ageWaivedStatistics = createStatistics(pe3.getTime().getTime() - pe2.getTime().getTime());
    DescriptiveStatistics ageFixedStatistics = createStatistics(0);
    DescriptiveStatistics ageUnresolvedStatistics = createStatistics(dto.timestamp - pe1.getTime().getTime());
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);
  }

  @Test
  public void testGetPolicySummary_WaivedAndFixedSameWeek() {
    DateTime now = new DateTime();

    // Data before week 0. One app1 violation.
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).minusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    // Data during week 2. Waives app1 violation
    PolicyWaiver waiver = tempEntity.newWaiver("abababab", orgPolicy.getId(), app1.getId());
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId2",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS - 2).plusDays(1).toDate());
    tempEntity.newWaivedPolicyViolation(pe2, orgPolicy, "g1", "a1", "v1", "h1", waiver);

    // Data during week 2. Fixes app1 violation
    PolicyEvaluation pe3 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId3",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS - 2).plusDays(2).toDate());

    PolicySummaryDTO dto = policySummaryService.getPolicySummary(null, null, null, null, null);
    assertCounters(dto, 1, 0, 1, 0);
    assertCountersWeek(dto, 0, 0, 0, 0, 0);
    assertCountersWeek(dto, 1, 0, 0, 0, 0);
    assertCountersWeek(dto, 2, 0, 0, 1, -1);
    DescriptiveStatistics ageWaivedStatistics = createStatistics(pe3.getTime().getTime() - pe2.getTime().getTime());
    DescriptiveStatistics ageFixedStatistics = createStatistics(pe3.getTime().getTime() - pe1.getTime().getTime());
    DescriptiveStatistics ageUnresolvedStatistics = createStatistics(0);
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);
  }

  @Test
  public void testGetPolicySummary_WaivedAndFixedDifferentWeeks() {
    DateTime now = new DateTime();

    // Data before week 0. One app1 violation.
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).minusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    // Data during week 2. Waives app1 violation
    PolicyWaiver waiver = tempEntity.newWaiver("abababab", orgPolicy.getId(), app1.getId());
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId2",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS - 2).plusDays(1).toDate());
    tempEntity.newWaivedPolicyViolation(pe2, orgPolicy, "g1", "a1", "v1", "h1", waiver);

    // Data during week 2. Fixes app1 violation
    PolicyEvaluation pe3 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId3",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS - 3).plusDays(2).toDate());

    PolicySummaryDTO dto = policySummaryService.getPolicySummary(null, null, null, null, null);
    assertCounters(dto, 1, 0, 1, 0);
    assertCountersWeek(dto, 0, 0, 0, 0, 0);
    assertCountersWeek(dto, 1, 0, 0, 0, 0);
    assertCountersWeek(dto, 2, 0, 1, 0, -1);
    assertCountersWeek(dto, 3, 0, -1, 1, 0);
    DescriptiveStatistics ageWaivedStatistics = createStatistics(pe3.getTime().getTime() - pe2.getTime().getTime());
    DescriptiveStatistics ageFixedStatistics = createStatistics(pe3.getTime().getTime() - pe1.getTime().getTime());
    DescriptiveStatistics ageUnresolvedStatistics = createStatistics(0);
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);
  }

  @Test
  public void testGetPolicySummary_SameViolationTwoStages() throws Exception {
    DateTime now = new DateTime();

    // Data during week 0. Adds one violation for two stages.
    PolicyEvaluation pe1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(1).toDate());
    tempEntity.newPolicyViolation(pe1, orgPolicy, "g1", "a1", "v1", "h1", "r1");
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scanId2",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS).plusDays(2).toDate());
    tempEntity.newPolicyViolation(pe2, orgPolicy, "g1", "a1", "v1", "h1", "r1");

    // Data during week 1. Fixes one violation for one stage.
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId3",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS - 1).plusDays(1).toDate());

    // Data during week 2. Fixes one violation for the other stage.
    PolicyEvaluation pe4 = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scanId4",
        now.minusWeeks(PolicySummaryService.POLICY_SUMMARY_WEEKS - 2).plusDays(1).toDate());

    PolicySummaryDTO dto = policySummaryService.getPolicySummary(null, null, null, null, null);
    assertCounters(dto, 1, 0, 1, 0);
    assertCountersWeek(dto, 0, 1, 0, 0, 1);
    assertCountersWeek(dto, 1, 0, 0, 0, 0);
    assertCountersWeek(dto, 2, 0, 0, 1, -1);
    for (int iWeek = 3; iWeek < PolicySummaryService.POLICY_SUMMARY_WEEKS; iWeek++) {
      assertCountersWeek(dto, iWeek, 0, 0, 0, 0);
    }
    DescriptiveStatistics ageWaivedStatistics = createStatistics(0);
    DescriptiveStatistics ageFixedStatistics = createStatistics(pe4.getTime().getTime() - pe1.getTime().getTime());
    DescriptiveStatistics ageUnresolvedStatistics = createStatistics(0);
    assertAges(dto, ageWaivedStatistics, ageFixedStatistics, ageUnresolvedStatistics);
  }

  private void assertCounters(PolicySummaryDTO actual, int totalNew, int totalWaived, int totalFixed,
      int currentUnresolved)
  {
    assertThat(actual, notNullValue());

    assertThat(actual.totalNew, is(totalNew));
    assertThat(actual.totalWaived, is(totalWaived));
    assertThat(actual.totalFixed, is(totalFixed));
    assertThat(actual.currentUnresolved, is(currentUnresolved));

    assertThat(actual.weeklyDeltaNew, hasSize(PolicySummaryService.POLICY_SUMMARY_WEEKS));
    assertThat(actual.weeklyDeltaWaived, hasSize(PolicySummaryService.POLICY_SUMMARY_WEEKS));
    assertThat(actual.weeklyDeltaFixed, hasSize(PolicySummaryService.POLICY_SUMMARY_WEEKS));
    assertThat(actual.weeklyDeltaUnresolved, hasSize(PolicySummaryService.POLICY_SUMMARY_WEEKS));
  }

  private void assertCountersWeek(PolicySummaryDTO actual, int week, int newCount, int waivedCount, int fixedCount,
      int unresolvedCount)
  {
    assertThat("weeklyDeltaNew", actual.weeklyDeltaNew.get(week), is(newCount));
    assertThat("weeklyDeltaWaived", actual.weeklyDeltaWaived.get(week), is(waivedCount));
    assertThat("weeklyDeltaFixed", actual.weeklyDeltaFixed.get(week), is(fixedCount));
    assertThat("weeklyDeltaUnresolved", actual.weeklyDeltaUnresolved.get(week), is(unresolvedCount));
  }

  private void assertAges(PolicySummaryDTO actual, DescriptiveStatistics ageWaivedStatistics,
      DescriptiveStatistics ageFixedStatistics, DescriptiveStatistics ageUnresolvedStatistics)
  {
    assertThat(actual.ageAverageWaived, is((long) ageWaivedStatistics.getMean()));
    assertThat(actual.agePercentile90Waived, is((long) ageWaivedStatistics.getPercentile(90)));
    assertThat(actual.ageAverageFixed, is((long) ageFixedStatistics.getMean()));
    assertThat(actual.agePercentile90Fixed, is((long) ageFixedStatistics.getPercentile(90)));
    assertThat(actual.ageAverageUnresolved, is((long) ageUnresolvedStatistics.getMean()));
    assertThat(actual.agePercentile90Unresolved, is((long) ageUnresolvedStatistics.getPercentile(90)));
  }

  private DescriptiveStatistics createStatistics(long... values) {
    DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();
    for (long value : values) {
      descriptiveStatistics.addValue(value);
    }
    return descriptiveStatistics;
  }
}
