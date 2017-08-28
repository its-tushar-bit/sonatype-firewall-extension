/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aggregation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.aggregation.AverageDiscoveredPolicyViolationsDTO.AverageDiscoveredThreatCategoryPolicyViolationsDTO;
import com.sonatype.insight.brain.dataaccess.aggregation.PolicyViolationAggregationDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.array;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PolicyViolationAggregationServiceTest
    extends AbstractComponentTest
{
  private static final double TOLERANCE = 0.00001;

  @Inject
  private PolicyViolationAggregationService service;

  @After
  public void after() {
    DateTimeUtils.setCurrentMillisSystem();
  }

  @Test
  public void testGetMttrs_AggregationsAlreadyExist() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    List<MttrDTO> results = service.getMttrs(new HashSet<String>(), applicationIds);

    assertAggregationHistory(results);
  }

  @Test
  public void testGetMttrs_AggregationsAlreadyExist_ByOrganizationId() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    // create an app in a separate org so we can check that it is filtered out
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());
    String appId2 = app2.getId();

    Date app2Eval1Date = new LocalDate().withDayOfMonth(2).minusMonths(1).toDateTimeAtStartOfDay().toDate();
    Date app2Eval2Date = new Date(app2Eval1Date.getTime() + 500000);

    Policy app2Policy = tempEntity.newPolicy(appId2, "test policy name", 5);
    StageType stageType = StageTypes.BUILD;

    PolicyEvaluation app2Eval1 = tempEntity.newPolicyEvaluation(appId2, stageType.getId(), "app2Eval1", app2Eval1Date);
    tempEntity.newPolicyEvaluation(appId2, stageType.getId(), "app2Eval2", app2Eval2Date);
    tempEntity.newPolicyViolation(app2Eval1, app2Policy);

    List<MttrDTO> results = service.getMttrs(Collections.singleton(org.getId()), new HashSet<String>());

    assertAggregationHistory(results);
  }

  @Test
  public void testGetMttrs_AggregationsAlreadyExist_SpecificApplicationId() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    String appId = PolicyViolationAggregationDataHelper.APPLICATION_IDS[2];

    Organization org = tempEntity.newOrganization();
    tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());

    List<MttrDTO> results = service.getMttrs(new HashSet<String>(), Collections.singleton(appId));

    assertAggregationHistoryForThirdApp(results);
  }

  @Test
  public void testGetMttrs_EmptyArguments() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    List<MttrDTO> results = service.getMttrs(new HashSet<String>(), new HashSet<String>());

    assertAggregationHistory(results);
  }

  @Test
  public void testGetMttrs_NullArguments() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    List<MttrDTO> results = service.getMttrs(null, null);

    assertAggregationHistory(results);
  }

  @Test
  public void testGetMttrs_NoData() {
    Application application = tempEntity.newApplicationWithParent("no-evals-app");
    DateTimeUtils.setCurrentMillisFixed(DateTime.now().minusMonths(3).getMillis());

    List<MttrDTO> results = service.getMttrs(null, Collections.singleton(application.getId()));

    // Having no data puts it in PoC mode which is indicated by a null return.
    assertThat(results.size(), is(0));
  }

  @Test
  public void testGetMttrs_GenerateAggregationsWithResolvedViolations() {
    LocalDate today = new LocalDate();

    Application application = tempEntity.newApplicationWithParent("aggregation-generation-app");
    String appId = application.getId();
    Policy policy1 = tempEntity.newPolicy(appId, "policy1", 5);
    Policy policy2 = tempEntity.newPolicy(appId, "policy2", 10);
    StageType stageType = StageTypes.BUILD;
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan1",
        toDate(today.minusMonths(5)));
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan2",
        toDate(today.minusMonths(4)));
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan3",
        toDate(today.minusMonths(3)));
    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan4",
        toDate(today.minusMonths(2)));
    PolicyEvaluation eval5 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan5",
        toDate(today.minusMonths(1)));

    // one violation exists in evaluations 1, 2 and 4. The other exists in violations 2, 3, and 4
    tempEntity.newPolicyViolation(eval1, policy1);
    tempEntity.newPolicyViolation(eval2, policy1);
    tempEntity.newPolicyViolation(eval2, policy2);
    tempEntity.newPolicyViolation(eval3, policy2);
    tempEntity.newPolicyViolation(eval4, policy1);
    tempEntity.newPolicyViolation(eval4, policy2);
    // no violation in eval5

    List<MttrDTO> results = service.getMttrs(null, Collections.singleton(appId));

    List<MttrDTO> expected = new ArrayList<>(5);

    MttrDTO dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(5));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(4));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(3));
    dto.mttrInSeconds = (int) ((eval3.getTime().getTime() - eval1.getTime().getTime()) / 1000);
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(2));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(1));

    // two violations resolved here so average them
    dto.mttrInSeconds = (int) (((eval5.getTime().getTime() - eval2.getTime().getTime())
        + (eval5.getTime().getTime() - eval4.getTime().getTime())) / 2 / 1000);
    dto.criticalMttrInSeconds = (int) ((eval5.getTime().getTime() - eval2.getTime().getTime()) / 1000);
    expected.add(dto);

    assertMttrDTOs(results, expected);
  }

  @Test
  public void testGetMttrs_GenerateAggregationsWithWaivedViolations() {
    LocalDate today = new LocalDate();

    Application application = tempEntity.newApplicationWithParent("aggregation-generation-app");
    String appId = application.getId();
    Policy policy1 = tempEntity.newPolicy(appId, "policy1", 5);
    Policy policy2 = tempEntity.newPolicy(appId, "policy2", 10);
    PolicyWaiver waiver1 = tempEntity.newWaiver(policy1.getId(), appId);
    PolicyWaiver waiver2 = tempEntity.newWaiver(policy2.getId(), appId);
    StageType stageType = StageTypes.BUILD;
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan1",
        toDate(today.minusMonths(5)));
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan2",
        toDate(today.minusMonths(4)));
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan3",
        toDate(today.minusMonths(3)));
    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan4",
        toDate(today.minusMonths(2)));
    PolicyEvaluation eval5 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan5",
        toDate(today.minusMonths(1)));

    // one violation is waived in evaluations 3 and 5 (but not 4). The other doesn't exist until evaluation 2 and then
    // is waived in evaluation 5
    tempEntity.newPolicyViolation(eval1, policy1);
    tempEntity.newPolicyViolation(eval2, policy1);
    tempEntity.newPolicyViolation(eval2, policy2);
    tempEntity.newWaivedPolicyViolation(tempEntity.newPolicyViolation(eval3, policy1), waiver1);
    tempEntity.newPolicyViolation(eval3, policy2);
    tempEntity.newPolicyViolation(eval4, policy1);
    tempEntity.newPolicyViolation(eval4, policy2);
    tempEntity.newWaivedPolicyViolation(tempEntity.newPolicyViolation(eval5, policy1), waiver1);
    tempEntity.newWaivedPolicyViolation(tempEntity.newPolicyViolation(eval5, policy2), waiver2);

    List<MttrDTO> results = service.getMttrs(null, Collections.singleton(appId));

    List<MttrDTO> expected = new ArrayList<>(5);

    MttrDTO dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(5));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(4));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(3));
    dto.mttrInSeconds = (int) ((eval3.getTime().getTime() - eval1.getTime().getTime()) / 1000);
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(2));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(1));

    // two violations resolved here so average them
    dto.mttrInSeconds = (int) (((eval5.getTime().getTime() - eval2.getTime().getTime())
        + (eval5.getTime().getTime() - eval4.getTime().getTime())) / 2 / 1000);
    dto.criticalMttrInSeconds = (int) ((eval5.getTime().getTime() - eval2.getTime().getTime()) / 1000);
    expected.add(dto);

    assertMttrDTOs(results, expected);
  }

  @Test
  public void testGetMttrs_GenerateAggregationsWithMixedStages() {
    LocalDate today = new LocalDate();

    Application application = tempEntity.newApplicationWithParent("aggregation-generation-app");
    String appId = application.getId();
    Policy policy1 = tempEntity.newPolicy(appId, "policy1", 5);

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(appId, StageTypes.BUILD.getId(), "scan1",
        toDate(today.minusMonths(5)));
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(appId,  StageTypes.STAGE_RELEASE.getId(), "scan2",
        toDate(today.minusMonths(4)));
    // Generate a policy violation in the develop stage so we can check that it is filtered out
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(appId, StageTypes.DEVELOP.getId(), "scan3",
        toDate(today.minusMonths(4)));
    tempEntity.newPolicyEvaluation(appId, StageTypes.BUILD.getId(), "scan4", toDate(today.minusMonths(3)));
    tempEntity.newPolicyEvaluation(appId, StageTypes.OPERATE.getId(), "scan5", toDate(today.minusMonths(2)));

    PolicyEvaluation eval6 = tempEntity.newPolicyEvaluation(appId,  StageTypes.STAGE_RELEASE.getId(), "scan6",
        toDate(today.minusMonths(1)));

    // violation appears in eval1, eval2 and eval3
    tempEntity.newPolicyViolation(eval1, policy1);
    tempEntity.newPolicyViolation(eval2, policy1);
    tempEntity.newPolicyViolation(eval3, policy1);

    List<MttrDTO> results = service.getMttrs(null, Collections.singleton(appId));
    List<MttrDTO> expected = new ArrayList<>(5);

    MttrDTO dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(5));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(4));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(3));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(2));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    // violation isn't cleared in all stages until eval6
    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(1));
    dto.mttrInSeconds = (int) ((eval6.getTime().getTime() - eval1.getTime().getTime()) / 1000);
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    assertMttrDTOs(results, expected);
  }

  // simulate multiple runs of getMttrs at different points in time to ensure that the
  // PolicyViolationResolutionState mechanism is working correctly
  @Test
  public void testGetMttrs_GenerateAggregationsInMultipleBatches() {
    LocalDate today = new LocalDate();

    Application application = tempEntity.newApplicationWithParent("aggregation-generation-app");
    String appId = application.getId();
    Policy policy1 = tempEntity.newPolicy(appId, "policy1", 5);
    StageType stageType = StageTypes.BUILD;
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan1",
        toDate(today.minusMonths(4)));

    tempEntity.newPolicyViolation(eval1, policy1);

    List<MttrDTO> results1;
    try {
      // tell joda time to pretend we are at the beginning of the month 2 months ago
      DateTimeUtils.setCurrentMillisFixed(today.minusMonths(2).withDayOfMonth(1).toDateTimeAtStartOfDay().getMillis());

      results1 = service.getMttrs(null, Collections.singleton(appId));
    }
    finally {
      // tell joda time to come back to the present
      DateTimeUtils.setCurrentMillisSystem();
    }

    List<MttrDTO> expected1 = new ArrayList<>(1);

    MttrDTO dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(4));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected1.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(3));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected1.add(dto);

    assertMttrDTOs(results1, expected1);

    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan2",
        toDate(today.minusMonths(2)));
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan3",
        toDate(today.minusMonths(1)));

    // the violation that first appeared in eval1 continues to appear in eval2 but disappears in eval3
    tempEntity.newPolicyViolation(eval2, policy1);

    List<MttrDTO> results2 = service.getMttrs(null, Collections.singleton(appId));
    List<MttrDTO> expected2 = new ArrayList<>(3);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(4));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected2.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(3));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected2.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(2));
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected2.add(dto);

    dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(1));
    dto.mttrInSeconds = (int) ((eval3.getTime().getTime() - eval1.getTime().getTime()) / 1000);
    dto.criticalMttrInSeconds = null;
    expected2.add(dto);

    assertMttrDTOs(results2, expected2);
  }

  @Test
  public void testGetMttrs_GenerateAggregationsWithMultipleEvaluationsPerMonth() {
    LocalDate today = new LocalDate();

    Application application = tempEntity.newApplicationWithParent("aggregation-generation-app");
    String appId = application.getId();
    Policy policy1 = tempEntity.newPolicy(appId, "policy1", 5);
    StageType stageType = StageTypes.BUILD;
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan1",
        toDate(today.withDayOfMonth(2).minusMonths(2)));
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan2",
        toDate(today.withDayOfMonth(3).minusMonths(2)));
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan3",
        toDate(today.withDayOfMonth(4).minusMonths(2)));

    // violation appears in eval1, and eval2
    tempEntity.newPolicyViolation(eval1, policy1);
    tempEntity.newPolicyViolation(eval2, policy1);

    List<MttrDTO> results = service.getMttrs(null, Collections.singleton(appId));
    List<MttrDTO> expected = new ArrayList<>(1);

    MttrDTO dto = new MttrDTO();
    dto.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(2));
    dto.mttrInSeconds = (int) ((eval3.getTime().getTime() - eval1.getTime().getTime()) / 1000);
    dto.criticalMttrInSeconds = null;
    expected.add(dto);
    MttrDTO blankCurrentMonthDueToPoCMode = new MttrDTO();
    blankCurrentMonthDueToPoCMode.timePeriodStart = toDate(today.withDayOfMonth(1).minusMonths(1));
    expected.add(blankCurrentMonthDueToPoCMode);

    assertMttrDTOs(results, expected);
  }

  @Test
  public void testGetAverages_AggregationsAlreadyExist() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    SuccessMetricsAveragesDTO result = service.getAverages(new HashSet<String>(), applicationIds);

    assertAggregationAverageHistory(result);
  }

  @Test
  public void testGetAverages_AggregationsAlreadyExist_ByOrganizationId() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    // create an app in a separate org so we can check that it is filtered out
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());
    String appId2 = app2.getId();

    Date app2Eval1Date = new LocalDate().withDayOfMonth(2).minusMonths(1).toDateTimeAtStartOfDay().toDate();
    Date app2Eval2Date = new Date(app2Eval1Date.getTime() + 500000);

    Policy app2Policy = tempEntity.newPolicy(appId2, "test policy name", 5);
    StageType stageType = StageTypes.BUILD;

    PolicyEvaluation app2Eval1 = tempEntity.newPolicyEvaluation(appId2, stageType.getId(), "app2Eval1", app2Eval1Date);
    tempEntity.newPolicyEvaluation(appId2, stageType.getId(), "app2Eval2", app2Eval2Date);
    tempEntity.newPolicyViolation(app2Eval1, app2Policy);

    SuccessMetricsAveragesDTO result = service.getAverages(Collections.singleton(org.getId()), new HashSet<String>());

    assertAggregationAverageHistory(result);
  }

  @Test
  public void testGetAverages_AggregationsAlreadyExist_SpecificApplicationId() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    String appId = PolicyViolationAggregationDataHelper.APPLICATION_IDS[2];

    Organization org = tempEntity.newOrganization();
    tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());

    SuccessMetricsAveragesDTO result = service.getAverages(new HashSet<String>(), Collections.singleton(appId));

    assertAggregationAverageHistoryForThirdApp(result);
  }

  @Test
  public void testGetAverages_EmptyArguments() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    SuccessMetricsAveragesDTO result = service.getAverages(new HashSet<String>(), new HashSet<String>());

    assertAggregationAverageHistory(result);
  }

  @Test
  public void testGetAverages_NullArguments() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    SuccessMetricsAveragesDTO result = service.getAverages(null, null);

    assertAggregationAverageHistory(result);
  }

  @Test
  public void testGetAverages_NoData() {
    Application application = tempEntity.newApplicationWithParent("no-evals-app");
    DateTimeUtils.setCurrentMillisFixed(DateTime.now().minusMonths(3).getMillis());

    SuccessMetricsAveragesDTO result = service.getAverages(null, Collections.singleton(application.getId()));

    assertThat(result.averageDiscoveredPolicyViolations, hasSize(0));
    assertThat(result.activeApplicationCount, is(0));
  }

  @Test
  public void testGetAverages_ExcludesDevelopmentStage() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    Application application = tempEntity.newApplicationWithParent("averages-app");
    String appId = application.getId();
    
    Policy policy = tempEntity.newPolicy(appId, "policy", 5);
    Date fiveMonthsAgo = new LocalDate().withDayOfMonth(1).minusMonths(5).toDate();

    // Generate a policy violation in the develop stage so we can check that it is filtered out
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(appId, StageTypes.DEVELOP.getId(), "eval", fiveMonthsAgo);
    tempEntity.newPolicyViolation(eval, policy);

    SuccessMetricsAveragesDTO result = service.getAverages(new HashSet<String>(), new HashSet<String>());

    assertAggregationAverageHistory(result);
  }

  /**
   * If the first evaluation happened no earlier than last month, aggregations are gathered up to today.
   */
  @Test
  public void testGetAverages_PoCMode() {
    Policy policy = tempEntity.newPolicy("some policy");
    LocalDate fakeNow = setTimeTo(LocalDate.now().withDayOfMonth(15));
    Application app = tempEntity.newApplicationWithParent("appy");
    Set<String> appId = Collections.singleton(app.getId());

    String stageTypeId = StageTypes.RELEASE.getId();
    PolicyEvaluation firstEval = tempEntity.newPolicyEvaluation(app.getId(), stageTypeId, "scan1", fakeNow.toDate());
    tempEntity.newPolicyViolation(firstEval, policy, "arti", "fact", "1", "artifact1hash", null);

    // We want the user to see results on first load.
    SuccessMetricsAveragesDTO result = service.getAverages(Collections.<String>emptySet(), appId);

    AverageDiscoveredPolicyViolationsDTO firstMonthAverage = createAverageMonth(fakeNow.withDayOfMonth(1), 1, 1);

    assertAverageDTOs(result.averageDiscoveredPolicyViolations, singletonList(firstMonthAverage));

    // From this point on, however, the aggregations are updated on a daily basis.
    // Therefore, another evaluation later the same day will not appear until tomorrow.
    PolicyEvaluation secondEval = tempEntity.newPolicyEvaluation(app.getId(), stageTypeId, "scan2", fakeNow.toDate());
    tempEntity.newPolicyViolation(secondEval, policy, "arti", "fact", "2", "artifact2hash", null);
    AverageDiscoveredPolicyViolationsDTO updatedFirstMonthAverage = createAverageMonth(fakeNow.withDayOfMonth(1), 2, 2);

    result = service.getAverages(Collections.<String>emptySet(), appId);
    assertAverageDTOs(result.averageDiscoveredPolicyViolations, singletonList(firstMonthAverage));

    // Roll over to next day and check that second evaluation is now included.
    fakeNow = setTimeTo(fakeNow.plusDays(1));
    result = service.getAverages(Collections.<String>emptySet(), appId);
    assertAverageDTOs(result.averageDiscoveredPolicyViolations, singletonList(updatedFirstMonthAverage));

    LocalDate startOfMonth2 = fakeNow.plusMonths(1).withDayOfMonth(1);
    PolicyEvaluation thirdEval = tempEntity
        .newPolicyEvaluation(app.getId(), stageTypeId, "scan3", startOfMonth2.toDate());
    tempEntity.newPolicyViolation(thirdEval, policy, "arti", "fact", "3", "artifact3hash", null);
    AverageDiscoveredPolicyViolationsDTO secondMonthAverage = createAverageMonth(startOfMonth2, 1, 1);

    fakeNow = setTimeTo(startOfMonth2.withDayOfMonth(2));
    result = service.getAverages(Collections.<String>emptySet(), appId);

    assertAverageDTOs(result.averageDiscoveredPolicyViolations, asList(updatedFirstMonthAverage, secondMonthAverage));

    LocalDate startOfMonth3 = fakeNow.plusMonths(1).withDayOfMonth(1);
    PolicyEvaluation fourthEval = tempEntity
        .newPolicyEvaluation(app.getId(), stageTypeId, "scan4", startOfMonth3.toDate());
    tempEntity.newPolicyViolation(fourthEval, policy, "arti", "fact", "4", "artifact4hash", null);

    setTimeTo(startOfMonth3.plusDays(1));
    result = service.getAverages(Collections.<String>emptySet(), appId);

    // We're out of PoC mode, so evaluations for current month are no longer counted.
    assertAverageDTOs(result.averageDiscoveredPolicyViolations, asList(updatedFirstMonthAverage, secondMonthAverage));
  }

  private AverageDiscoveredPolicyViolationsDTO createAverageMonth(LocalDate localDate,
                                                                  int evaluationCount,
                                                                  double numDiscoveredSevereOther)
  {
    AverageDiscoveredPolicyViolationsDTO averageMonth = new AverageDiscoveredPolicyViolationsDTO();
    averageMonth.timePeriodStart = localDate.toDate();
    averageMonth.evaluationCount = evaluationCount;
    averageMonth.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    averageMonth.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    averageMonth.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    averageMonth.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, numDiscoveredSevereOther,
        0.0);
    return averageMonth;
  }

  @Test
  public void testGetApplicationsCounts_AggregationsAlreadyExist() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper
        .createApplicationCountAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    ApplicationCountsDTO result = service.getApplicationCounts(null, applicationIds);

    assertApplicationCountsDTO(result, makeAggregationHistoryCountsDTO());
  }

  @Test
  public void testGetApplicationsCounts_AggregationsAlreadyExist_ByOrganizationId() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper
        .createApplicationCountAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    ApplicationCountsDTO result = service.getApplicationCounts(Collections.singleton(org.getId()), null);

    assertApplicationCountsDTO(result, makeAggregationHistoryCountsDTO());
  }

  @Test
  public void testGetApplicationsCounts_AggregationsAlreadyExist_SpecificApplicationId() {
    PolicyViolationAggregationDataHelper.createApplicationCountAggregationHistory(tempEntity);
    String appId = PolicyViolationAggregationDataHelper.APPLICATION_IDS[2];

    Organization org = tempEntity.newOrganization();
    tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());

    ApplicationCountsDTO result = service.getApplicationCounts(null, Collections.singleton(appId));

    ApplicationCountsDTO expected = new ApplicationCountsDTO();

    expected.totalApplications = 1;
    expected.activeApplications = 1;
    expected.total.applicationsWithViolations = 1;
    expected.total.applicationsWithCriticalViolations = 1;
    expected.security.applicationsWithViolations = 1;
    expected.security.applicationsWithCriticalViolations = 1;
    expected.license.applicationsWithViolations = 1;
    expected.license.applicationsWithCriticalViolations = 0;
    expected.quality.applicationsWithViolations = 0;
    expected.quality.applicationsWithCriticalViolations = 0;
    expected.other.applicationsWithViolations = 0;
    expected.other.applicationsWithCriticalViolations = 0;

    assertApplicationCountsDTO(result, expected);
  }

  @Test
  public void testGetApplicationCounts_EmptyArguments() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper
        .createApplicationCountAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    ApplicationCountsDTO result = service.getApplicationCounts(new HashSet<String>(), new HashSet<String>());

    assertApplicationCountsDTO(result, makeAggregationHistoryCountsDTO());
  }

  @Test
  public void testGetApplicationCounts_NullArguments() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper
        .createApplicationCountAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    ApplicationCountsDTO result = service.getApplicationCounts(null, null);

    assertApplicationCountsDTO(result, makeAggregationHistoryCountsDTO());
  }

  @Test
  public void testGetApplicationCounts_NoData() {
    Application application = tempEntity.newApplicationWithParent("no-evals-app");

    ApplicationCountsDTO result = service.getApplicationCounts(null, Collections.singleton(application.getId()));

    ApplicationCountsDTO expected = new ApplicationCountsDTO();
    expected.totalApplications = 1;

    assertApplicationCountsDTO(result, expected);
  }

  @Test
  public void testGetApplicationCounts_ExcludesDevelopmentStage() {
    Set<String> applicationIds = 
        PolicyViolationAggregationDataHelper.createApplicationCountAggregationHistory(tempEntity);

    Organization org = tempEntity.newOrganization();
    for (String appId : applicationIds) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    Application application = tempEntity.newApplicationWithParent("counts-app");
    String appId = application.getId();

    Policy policy = tempEntity.newPolicy(appId, "policy", 5);
    Date fiveMonthsAgo = new LocalDate().withDayOfMonth(1).minusMonths(5).toDate();

    // Generate a policy violation in the develop stage so we can check that it is filtered out
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(appId, StageTypes.DEVELOP.getId(), "eval", fiveMonthsAgo);
    tempEntity.newPolicyViolation(eval, policy);

    ApplicationCountsDTO result = service.getApplicationCounts(new HashSet<String>(), new HashSet<String>());
    ApplicationCountsDTO expected = makeAggregationHistoryCountsDTO();
  
    // total should increase but nothing else
    expected.totalApplications++;

    assertApplicationCountsDTO(result, expected);
  }

  /**
   * @return an ApplicationCountsDTO with the expected values from the
   * PolicyViolationAggregationDataHelper.createApplicationCountAggregationHistory method
   */
  private ApplicationCountsDTO makeAggregationHistoryCountsDTO() {
    ApplicationCountsDTO dto = new ApplicationCountsDTO();

    dto.totalApplications = 6;
    dto.activeApplications = 4;
    dto.total.applicationsWithViolations = 4;
    dto.total.applicationsWithCriticalViolations = 3;
    dto.security.applicationsWithViolations = 4;
    dto.security.applicationsWithCriticalViolations = 2;
    dto.license.applicationsWithViolations = 3;
    dto.license.applicationsWithCriticalViolations = 1;
    dto.quality.applicationsWithViolations = 1;
    dto.quality.applicationsWithCriticalViolations = 0;
    dto.other.applicationsWithViolations = 1;
    dto.other.applicationsWithCriticalViolations = 1;

    return dto;
  }

  /**
   * Checks the results of running getMttrs with the data from PolicyViolationAggregationDataHelper
   */
  private void assertAggregationHistory(List<MttrDTO> actual) {
    List<MttrDTO> expected = new ArrayList<>(12);
    MttrDTO dto;
    LocalDate dtoDate;

    dto = new MttrDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusYears(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 2;
    dto.criticalMttrInSeconds = 2;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 2;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 1;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 5;
    dto.criticalMttrInSeconds = 5;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 16;
    dto.criticalMttrInSeconds = 16;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 15;
    dto.criticalMttrInSeconds = 20;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 37;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 5;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 8;
    dto.criticalMttrInSeconds = 10;
    expected.add(dto);

    assertMttrDTOs(actual, expected);
  }

  /**
   * Test the results returned when filtered specifically to the third app defined in
   * PolicyViolationAggregationDataHelper
   */
  private void assertAggregationHistoryForThirdApp(List<MttrDTO> actual) {
    List<MttrDTO> expected = new ArrayList<>(9);
    MttrDTO dto;
    LocalDate dtoDate;

    dto = new MttrDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(9);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 1;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 5;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 15;
    dto.criticalMttrInSeconds = 20;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dtoDate = dtoDate.plusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.mttrInSeconds = 10;
    dto.criticalMttrInSeconds = 12;
    expected.add(dto);

    assertMttrDTOs(actual, expected);
  }

  private void assertAggregationAverageHistory(SuccessMetricsAveragesDTO actualDTO) {
    List<AverageDiscoveredPolicyViolationsDTO> actualAveragesDTOs = actualDTO.averageDiscoveredPolicyViolations;

    List<AverageDiscoveredPolicyViolationsDTO> expectedAveragesDTOs = new ArrayList<>(12);
    AverageDiscoveredPolicyViolationsDTO dto;
    LocalDate dtoDate;

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusYears(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(2.0, 3.0, 4.0, 5.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(3.0, 4.0, 5.0, 6.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(4.0, 5.0, 6.0, 7.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(5.0, 6.0, 7.0, 8.0);
    dto.evaluationCount = 6;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(11);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(1.0, 1.0, 1.0, 1.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(2.0, 2.0, 2.0, 2.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(3.0, 3.0, 3.0, 3.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(4.0, 4.0, 4.0, 4.0);
    dto.evaluationCount = 3;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(10);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 0;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(9);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(5.0, 5.0, 5.0, 5.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(5.0, 5.0, 5.0, 5.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(5.0, 5.0, 5.0, 5.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 30;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(8);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 0;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(7);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(6.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(6.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(6.0, 0.0, 0.0, 0.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(6.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 6;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(6);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 6000;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(5);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 0;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(4);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 1.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 1.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 1.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 1.0);
    dto.evaluationCount = 4;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(3);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(2.0, 2.0, 2.0, 2.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(2.0, 2.0, 2.0, 2.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(2.0, 2.0, 2.0, 2.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(2.0, 2.0, 2.0, 2.0);
    dto.evaluationCount = 5;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(2);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 0;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(2.5, 3.5, 4.5, 5.5);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(2.5, 3.5, 4.5, 5.5);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(2.5, 3.5, 4.5, 5.5);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(2.5, 3.5, 4.5, 5.5);
    dto.evaluationCount = 10;
    expectedAveragesDTOs.add(dto);

    assertAverageDTOs(actualAveragesDTOs, expectedAveragesDTOs);
    assertThat(actualDTO.activeApplicationCount, is(4));
  }

  /**
   * Test the results returned when filtered specifically to the third app defined in
   * PolicyViolationAggregationDataHelper
   */
  private void assertAggregationAverageHistoryForThirdApp(SuccessMetricsAveragesDTO actualOverallDTO) {
    List<AverageDiscoveredPolicyViolationsDTO> actualAveragesDTOs = actualOverallDTO.averageDiscoveredPolicyViolations;
    List<AverageDiscoveredPolicyViolationsDTO> expectedAveragesDTOs = new ArrayList<>(9);
    AverageDiscoveredPolicyViolationsDTO dto;
    LocalDate dtoDate;

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(9);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(15.0, 15.0, 15.0, 15.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 10;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(8);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 0;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(7);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(9.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(9.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(9.0, 0.0, 0.0, 0.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(9.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 3;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(6);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 3000;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(5);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 0;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(4);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 4.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 1;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(3);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 0;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(2);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(0.0, 0.0, 0.0, 0.0);
    dto.evaluationCount = 0;
    expectedAveragesDTOs.add(dto);

    dto = new AverageDiscoveredPolicyViolationsDTO();
    dtoDate = new LocalDate().withDayOfMonth(1).minusMonths(1);
    dto.timePeriodStart = dtoDate.toDateTimeAtStartOfDay().toDate();
    dto.security = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(3.0, 4.0, 5.0, 6.0);
    dto.license = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(3.0, 4.0, 5.0, 6.0);
    dto.quality = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(3.0, 4.0, 5.0, 6.0);
    dto.other = new AverageDiscoveredThreatCategoryPolicyViolationsDTO(3.0, 4.0, 5.0, 6.0);
    dto.evaluationCount = 3;
    expectedAveragesDTOs.add(dto);

    assertAverageDTOs(actualAveragesDTOs, expectedAveragesDTOs);
    assertThat(actualOverallDTO.activeApplicationCount, is(1));
  }

  private static class MttrDTOMatcher
      extends BaseMatcher<MttrDTO>
  {
    private final MttrDTO expected;

    public MttrDTOMatcher(MttrDTO expected) {
      this.expected = expected;
    }

    @Override
    public boolean matches(Object actual) {
      if (actual instanceof MttrDTO) {
        MttrDTO actualDTO = (MttrDTO) actual;

        return Objects.equals(actualDTO.timePeriodStart, expected.timePeriodStart)
            && Objects.equals(actualDTO.mttrInSeconds, expected.mttrInSeconds)
            && Objects.equals(actualDTO.criticalMttrInSeconds, expected.criticalMttrInSeconds);
      }
      else {
        return false;
      }
    }

    @Override
    public void describeTo(Description description) {
      description.appendValue(expected);
    }
  }

  private void assertApplicationCountsDTO(ApplicationCountsDTO actual, ApplicationCountsDTO expected) {
    assertThat(actual.totalApplications, is(expected.totalApplications));
    assertThat(actual.activeApplications, is(expected.activeApplications));
    assertThat(actual.total.applicationsWithViolations, is(expected.total.applicationsWithViolations));
    assertThat(actual.total.applicationsWithCriticalViolations, is(expected.total.applicationsWithCriticalViolations));
    assertThat(actual.security.applicationsWithViolations, is(expected.security.applicationsWithViolations));
    assertThat(actual.security.applicationsWithCriticalViolations,
        is(expected.security.applicationsWithCriticalViolations));
    assertThat(actual.license.applicationsWithViolations, is(expected.license.applicationsWithViolations));
    assertThat(actual.license.applicationsWithCriticalViolations,
        is(expected.license.applicationsWithCriticalViolations));
    assertThat(actual.quality.applicationsWithViolations, is(expected.quality.applicationsWithViolations));
    assertThat(actual.quality.applicationsWithCriticalViolations,
        is(expected.quality.applicationsWithCriticalViolations));
    assertThat(actual.other.applicationsWithViolations, is(expected.other.applicationsWithViolations));
    assertThat(actual.other.applicationsWithCriticalViolations, is(expected.other.applicationsWithCriticalViolations));
  }

  @SuppressWarnings({ "unchecked" }) // to use the array() method with a parameterized type
  private void assertMttrDTOs(List<MttrDTO> actual, List<MttrDTO> expected) {
    List<Matcher<MttrDTO>> matchers = new ArrayList<>(expected.size());
    for (MttrDTO dto : expected) {
      matchers.add(new MttrDTOMatcher(dto));
    }

    assertThat(actual.toArray(new MttrDTO[0]), is(array(matchers.toArray(new Matcher[0]))));
  }

  private void assertAverageDTOs(List<AverageDiscoveredPolicyViolationsDTO> actualDTOs, List<AverageDiscoveredPolicyViolationsDTO> expectedDTOs) {
    assertThat(actualDTOs, hasSize(expectedDTOs.size()));
    for (int i = 0; i < expectedDTOs.size(); i++) {
      AverageDiscoveredPolicyViolationsDTO actualDTO = actualDTOs.get(i);
      AverageDiscoveredPolicyViolationsDTO expectedDTO = expectedDTOs.get(i);
      assertAverageDTO(i, "time period start", actualDTO.timePeriodStart, is(expectedDTO.timePeriodStart));
      assertAverageDTO(i, "evaluation count", actualDTO.evaluationCount, is(expectedDTO.evaluationCount));
      assertAverageDTO(i, "average discovered security low", actualDTO.security.averageDiscoveredLow, closeTo(expectedDTO.security.averageDiscoveredLow, TOLERANCE));
      assertAverageDTO(i, "average discovered security moderate", actualDTO.security.averageDiscoveredModerate, closeTo(expectedDTO.security.averageDiscoveredModerate, TOLERANCE));
      assertAverageDTO(i, "average discovered security severe", actualDTO.security.averageDiscoveredSevere, closeTo(expectedDTO.security.averageDiscoveredSevere, TOLERANCE));
      assertAverageDTO(i, "average discovered security critical", actualDTO.security.averageDiscoveredCritical, closeTo(expectedDTO.security.averageDiscoveredCritical, TOLERANCE));
      assertAverageDTO(i, "average discovered license low", actualDTO.license.averageDiscoveredLow, closeTo(expectedDTO.license.averageDiscoveredLow, TOLERANCE));
      assertAverageDTO(i, "average discovered license moderate", actualDTO.license.averageDiscoveredModerate, closeTo(expectedDTO.license.averageDiscoveredModerate, TOLERANCE));
      assertAverageDTO(i, "average discovered license severe", actualDTO.license.averageDiscoveredSevere, closeTo(expectedDTO.license.averageDiscoveredSevere, TOLERANCE));
      assertAverageDTO(i, "average discovered license critical", actualDTO.license.averageDiscoveredCritical, closeTo(expectedDTO.license.averageDiscoveredCritical, TOLERANCE));
      assertAverageDTO(i, "average discovered quality low", actualDTO.quality.averageDiscoveredLow, closeTo(expectedDTO.quality.averageDiscoveredLow, TOLERANCE));
      assertAverageDTO(i, "average discovered quality moderate", actualDTO.quality.averageDiscoveredModerate, closeTo(expectedDTO.quality.averageDiscoveredModerate, TOLERANCE));
      assertAverageDTO(i, "average discovered quality severe", actualDTO.quality.averageDiscoveredSevere, closeTo(expectedDTO.quality.averageDiscoveredSevere, TOLERANCE));
      assertAverageDTO(i, "average discovered quality critical", actualDTO.quality.averageDiscoveredCritical, closeTo(expectedDTO.quality.averageDiscoveredCritical, TOLERANCE));
      assertAverageDTO(i, "average discovered other low", actualDTO.other.averageDiscoveredLow, closeTo(expectedDTO.other.averageDiscoveredLow, TOLERANCE));
      assertAverageDTO(i, "average discovered other moderate", actualDTO.other.averageDiscoveredModerate, closeTo(expectedDTO.other.averageDiscoveredModerate, TOLERANCE));
      assertAverageDTO(i, "average discovered other severe", actualDTO.other.averageDiscoveredSevere, closeTo(expectedDTO.other.averageDiscoveredSevere, TOLERANCE));
      assertAverageDTO(i, "average discovered other critical", actualDTO.other.averageDiscoveredCritical, closeTo(expectedDTO.other.averageDiscoveredCritical, TOLERANCE));
    }
  }

  private <T> void assertAverageDTO(int index, String variableName, T actual, Matcher<T> matcher) {
    assertThat("Average DTO at position " + index + " had unexpected " + variableName + " value", actual, matcher);
  }

  private Date toDate(LocalDate localDate) {
    return localDate.toDateTimeAtStartOfDay().toDate();
  }

  private LocalDate setTimeTo(LocalDate fakeNow) {
    DateTimeUtils.setCurrentMillisFixed(fakeNow.toDateTimeAtStartOfDay().getMillis());
    return fakeNow;
  }
}
