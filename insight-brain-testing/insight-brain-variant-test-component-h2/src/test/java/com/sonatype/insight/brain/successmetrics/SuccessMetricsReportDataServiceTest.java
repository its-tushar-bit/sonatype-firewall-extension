/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReportData;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.successmetrics.ApplicationCountsDTO.ThreatCategoryApplicationCount;
import com.sonatype.insight.brain.successmetrics.AverageDiscoveredPolicyViolationsDTO.ThreatCategoryPolicyViolationsDTO;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsTestUtils.FakeDateRule;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Ordering;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDate;
import org.jooq.exception.DataAccessException;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.ORG_ID;
import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.discovered;
import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.fixed;
import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.openWithSampleData;
import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.waived;
import static com.sonatype.insight.brain.model.successmetrics.TimePeriod.MONTH;
import static com.sonatype.insight.brain.successmetrics.SuccessMetricsReportDataService.isReportDataOutOfDate;
import static com.sonatype.insight.brain.utils.ThreatLevel.SEVERE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.joda.time.DateTime.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class SuccessMetricsReportDataServiceTest
    extends AbstractComponentH2Test
{
  private static final double TOLERANCE = 0.00001;

  @Rule
  public FakeDateRule fakeDateRule = new FakeDateRule();

  @Inject
  private SuccessMetricsReportDataService service;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  private void fixViolations(PolicyEvaluation evaluation, Predicate<PolicyViolation> exclude) {
    List<PolicyViolation> policyViolations = policyViolationDAO
        .getUnfixedByOwnerIdAndStageId(evaluation.getOwnerId(), evaluation.getStageTypeId());
    policyViolationDAO.loadConstraintFacts(policyViolations);
    for (PolicyViolation fixedViolation : policyViolations) {
      if (exclude == null || !exclude.test(fixedViolation)) {
        fixedViolation.setFixTime(evaluation.getTime());
        policyViolationDAO.update(fixedViolation);
      }
    }
  }

  private SuccessMetricsReport createSuccessMetricsReport(
      Set<String> organizationIds,
      Set<String> applicationIds,
      String reportName,
      boolean includeLatestData)
  {
    SuccessMetricsReportScopeDTO scope = new SuccessMetricsReportScopeDTO();
    scope.organizationIds = organizationIds;
    scope.applicationIds = applicationIds;

    return tempEntity.newSuccessMetricsReport(USERNAME, reportName, JsonUtils.format(scope), includeLatestData);
  }

  private SuccessMetricsReport createSuccessMetricsReport(Set<String> organizationIds, Set<String> applicationIds) {
    return createSuccessMetricsReport(organizationIds, applicationIds, "report", false);
  }

  @Test
  public void testGetChartData_Mttrs_AggregationsAlreadyExist() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), applicationIds);
    List<MttrDTO> results = service.getChartData(successMetricsReport.getId()).mttrs;

    assertAggregationHistory(results);
  }

  @Test
  public void testGetChartData_Mttrs_AggregationsAlreadyExist_ByOrganizationId() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    // create an app in a separate org so we can check that it is filtered out
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());
    String appId2 = app2.getId();

    Date app2Eval1Date = new LocalDate().withDayOfMonth(2).minusMonths(1).toDateTimeAtStartOfDay().toDate();
    Date app2Eval2Date = new Date(app2Eval1Date.getTime() + 500000);

    Policy app2Policy = tempEntity.newPolicy(app2);
    StageType stageType = StageTypes.BUILD;

    PolicyEvaluation app2Eval1 = tempEntity.newPolicyEvaluation(appId2, stageType.getId(), "app2Eval1", app2Eval1Date);
    tempEntity.newPolicyEvaluation(appId2, stageType.getId(), "app2Eval2", app2Eval2Date);
    tempEntity.newPolicyViolation(app2Eval1, app2Policy);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(Collections.singleton(ORG_ID),
        new HashSet<>());
    List<MttrDTO> results = service.getChartData(successMetricsReport.getId()).mttrs;

    assertAggregationHistory(results);
  }

  @Test
  public void testGetChartData_Mttrs_AggregationsAlreadyExist_SpecificApplicationId() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    String appId = PolicyViolationAggregationDataHelper.APPLICATION_IDS[2];

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(),
        Collections.singleton(appId));
    List<MttrDTO> results = service.getChartData(successMetricsReport.getId()).mttrs;

    assertAggregationHistoryForThirdApp(results);
  }

  @Test
  public void testGetChartData_Mttrs_EmptyArguments() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), new HashSet<>());
    List<MttrDTO> results = service.getChartData(successMetricsReport.getId()).mttrs;

    assertAggregationHistory(results);
  }

  @Test
  public void testGetChartData_Mttrs_NullArguments() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);
    List<MttrDTO> results = service.getChartData(successMetricsReport.getId()).mttrs;

    assertAggregationHistory(results);
  }

  @Test
  public void testGetChartData_Mttrs_NoData() {
    Application application = tempEntity.newApplicationWithParent("no-evals-app");

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null,
        Collections.singleton(application.getId()));
    List<MttrDTO> results = service.getChartData(successMetricsReport.getId()).mttrs;

    assertThat(results).isEmpty();
  }

  @Test
  public void testGetChartData_Mttrs_GenerateAggregationsWithResolvedViolations() {
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
    tempEntity.newPolicyViolation(eval2, policy2);
    fixViolations(eval3, violation -> policy2.getId().equals(violation.getPolicyId()));
    tempEntity.newPolicyViolation(eval4, policy1);
    // no violation in eval5
    fixViolations(eval5, null);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, Collections.singleton(appId));
    List<MttrDTO> results = service.getChartData(successMetricsReport.getId()).mttrs;

    List<MttrDTO> expected = new ArrayList<>(5);

    MttrDTO dto = new MttrDTO();
    dto.timePeriodName = "Jul";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Aug";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Sep";
    dto.mttrInSeconds = (int) ((eval3.getTime().getTime() - eval1.getTime().getTime()) / 1000);
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Oct";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Nov";

    // two violations resolved here so average them
    dto.mttrInSeconds = (int) (((eval5.getTime().getTime() - eval2.getTime().getTime())
        + (eval5.getTime().getTime() - eval4.getTime().getTime())) / 2 / 1000);
    dto.criticalMttrInSeconds = (int) ((eval5.getTime().getTime() - eval2.getTime().getTime()) / 1000);
    expected.add(dto);

    assertMttrDTOs(results, expected);
  }

  @Test
  public void testGetChartData_Mttrs_GenerateAggregationsWithWaivedViolations() {
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

    // one violation is waived in evaluations 3 and 5 (but not 4). The other doesn't exist until evaluation 2 and then
    // is waived in evaluation 5
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy1);
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy2);
    violation1.setWaiveTime(eval3.getTime());
    policyViolationDAO.update(violation1);
    violation1 = tempEntity.newPolicyViolation(eval4, policy1);
    violation1.setWaiveTime(eval5.getTime());
    policyViolationDAO.update(violation1);
    violation2.setWaiveTime(eval5.getTime());
    policyViolationDAO.update(violation2);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, Collections.singleton(appId));
    List<MttrDTO> results = service.getChartData(successMetricsReport.getId()).mttrs;

    List<MttrDTO> expected = new ArrayList<>(5);

    MttrDTO dto = new MttrDTO();
    dto.timePeriodName = "Jul";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Aug";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Sep";
    dto.mttrInSeconds = (int) ((eval3.getTime().getTime() - eval1.getTime().getTime()) / 1000);
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Oct";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Nov";

    // two violations resolved here so average them
    dto.mttrInSeconds = (int) (((eval5.getTime().getTime() - eval2.getTime().getTime())
        + (eval5.getTime().getTime() - eval4.getTime().getTime())) / 2 / 1000);
    dto.criticalMttrInSeconds = (int) ((eval5.getTime().getTime() - eval2.getTime().getTime()) / 1000);
    expected.add(dto);

    assertMttrDTOs(results, expected);
  }

  @Test
  public void testGetChartData_Mttrs_GenerateAggregationsWithMixedStages() {
    LocalDate today = new LocalDate();

    Application application = tempEntity.newApplicationWithParent("aggregation-generation-app");
    String appId = application.getId();
    Policy policy1 = tempEntity.newPolicy(application);

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(appId, StageTypes.BUILD.getId(), "scan1",
        toDate(today.minusMonths(5)));
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(appId, StageTypes.STAGE_RELEASE.getId(), "scan2",
        toDate(today.minusMonths(4)));
    // Generate a policy violation in the develop stage so we can check that it is filtered out
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(appId, StageTypes.DEVELOP.getId(), "scan3",
        toDate(today.minusMonths(4)));
    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(appId, StageTypes.BUILD.getId(), "scan4",
        toDate(today.minusMonths(3)));
    tempEntity.newPolicyEvaluation(appId, StageTypes.OPERATE.getId(), "scan5",
        toDate(today.minusMonths(2)));
    PolicyEvaluation eval6 = tempEntity.newPolicyEvaluation(appId, StageTypes.STAGE_RELEASE.getId(), "scan6",
        toDate(today.minusMonths(1)));

    // violation appears in eval1, eval2 and eval3
    tempEntity.newPolicyViolation(eval1, policy1);
    tempEntity.newPolicyViolation(eval2, policy1);
    tempEntity.newPolicyViolation(eval3, policy1);
    fixViolations(eval4, null);
    fixViolations(eval6, null);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, Collections.singleton(appId));
    List<MttrDTO> results = service.getChartData(successMetricsReport.getId()).mttrs;
    List<MttrDTO> expected = new ArrayList<>(5);

    MttrDTO dto = new MttrDTO();
    dto.timePeriodName = "Jul";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Aug";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Sep";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Oct";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    // violation isn't cleared in all stages until eval6
    dto = new MttrDTO();
    dto.timePeriodName = "Nov";
    dto.mttrInSeconds = (int) ((eval6.getTime().getTime() - eval1.getTime().getTime()) / 1000);
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    assertMttrDTOs(results, expected);
  }

  // simulate multiple runs of getMttrs at different points in time to ensure that tracking violations across multiple
  // aggregation runs is working correctly
  @Test
  public void testGetChartData_Mttrs_GenerateAggregationsInMultipleBatches() {
    LocalDate today = new LocalDate();

    Application application = tempEntity.newApplicationWithParent("aggregation-generation-app");
    String appId = application.getId();
    Policy policy1 = tempEntity.newPolicy(application);
    StageType stageType = StageTypes.BUILD;
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan1",
        toDate(today.minusMonths(4)));

    tempEntity.newPolicyViolation(eval1, policy1);

    List<MttrDTO> results1;
    SuccessMetricsReport successMetricsReport;
    try {
      // tell joda time to pretend we are at the beginning of the month 2 months ago (relative to the mocked present)
      DateTimeUtils.setCurrentMillisFixed(today.minusMonths(2).withDayOfMonth(1).toDateTimeAtStartOfDay().getMillis());

      successMetricsReport = createSuccessMetricsReport(null, Collections.singleton(appId));
      results1 = service.getChartData(successMetricsReport.getId()).mttrs;
    }
    finally {
      // tell joda time to come back to the mocked present
      fakeDateRule.fakeDate();
    }

    List<MttrDTO> expected1 = new ArrayList<>(1);

    MttrDTO dto = new MttrDTO();
    dto.timePeriodName = "Aug";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected1.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Sep";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected1.add(dto);

    assertMttrDTOs(results1, expected1);

    tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan2",
        toDate(today.minusMonths(2)));
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan3",
        toDate(today.minusMonths(1)));

    // the violation that first appeared in eval1 continues to appear in eval2 but disappears in eval3
    fixViolations(eval3, null);

    List<MttrDTO> results2 = service.getChartData(successMetricsReport.getId()).mttrs;
    List<MttrDTO> expected2 = new ArrayList<>(3);

    dto = new MttrDTO();
    dto.timePeriodName = "Aug";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected2.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Sep";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected2.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Oct";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected2.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Nov";
    dto.mttrInSeconds = (int) ((eval3.getTime().getTime() - eval1.getTime().getTime()) / 1000);
    dto.criticalMttrInSeconds = null;
    expected2.add(dto);

    assertMttrDTOs(results2, expected2);
  }

  @Test
  public void testGetChartData_Mttrs_GenerateAggregationsWithMultipleEvaluationsPerMonth() {
    LocalDate today = new LocalDate();

    Application application = tempEntity.newApplicationWithParent("aggregation-generation-app");
    String appId = application.getId();
    Policy policy1 = tempEntity.newPolicy(application);
    StageType stageType = StageTypes.BUILD;
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan1",
        toDate(today.withDayOfMonth(2).minusMonths(2)));
    tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan2",
        toDate(today.withDayOfMonth(3).minusMonths(2)));
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(appId, stageType.getId(), "scan3",
        toDate(today.withDayOfMonth(4).minusMonths(2)));

    // violation appears in eval1, and eval2
    tempEntity.newPolicyViolation(eval1, policy1);
    fixViolations(eval3, null);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, Collections.singleton(appId));
    List<MttrDTO> results = service.getChartData(successMetricsReport.getId()).mttrs;
    List<MttrDTO> expected = new ArrayList<>(1);

    MttrDTO dto = new MttrDTO();
    dto.timePeriodName = "Oct";
    dto.mttrInSeconds = (int) ((eval3.getTime().getTime() - eval1.getTime().getTime()) / 1000);
    dto.criticalMttrInSeconds = null;
    expected.add(dto);
    MttrDTO blankCurrentMonthDueToPoCMode = new MttrDTO();
    blankCurrentMonthDueToPoCMode.timePeriodName = "Nov";
    expected.add(blankCurrentMonthDueToPoCMode);

    assertMttrDTOs(results, expected);
  }

  @Test
  public void testGetChartData_Averages_AggregationsAlreadyExist() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), applicationIds);
    AverageDiscoveredPolicyViolationsDTO result = service.getChartData(successMetricsReport.getId()).averages;

    assertAggregationAverageHistory(result);
  }

  @Test
  public void testGetChartData_Averages_AggregationsAlreadyExist_ByOrganizationId() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    // create an app in a separate org so we can check that it is filtered out
    Organization org2 = tempEntity.newOrganization();
    Application app2 = tempEntity.newApplication(org2.getId());
    String appId2 = app2.getId();

    Date app2Eval1Date = new LocalDate().withDayOfMonth(2).minusMonths(1).toDateTimeAtStartOfDay().toDate();
    Date app2Eval2Date = new Date(app2Eval1Date.getTime() + 500000);

    Policy app2Policy = tempEntity.newPolicy(app2);
    StageType stageType = StageTypes.BUILD;

    PolicyEvaluation app2Eval1 = tempEntity.newPolicyEvaluation(appId2, stageType.getId(), "app2Eval1", app2Eval1Date);
    tempEntity.newPolicyEvaluation(appId2, stageType.getId(), "app2Eval2", app2Eval2Date);
    tempEntity.newPolicyViolation(app2Eval1, app2Policy);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(Collections.singleton(ORG_ID),
        new HashSet<>());
    AverageDiscoveredPolicyViolationsDTO result = service.getChartData(successMetricsReport.getId()).averages;

    assertAggregationAverageHistory(result);
  }

  @Test
  public void testGetChartData_Averages_AggregationsAlreadyExist_SpecificApplicationId() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    String appId = PolicyViolationAggregationDataHelper.APPLICATION_IDS[2];

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(),
        Collections.singleton(appId));
    AverageDiscoveredPolicyViolationsDTO result = service.getChartData(successMetricsReport.getId()).averages;

    assertAggregationAverageHistoryForThirdApp(result);
  }

  @Test
  public void testGetChartData_Averages_EmptyArguments() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), new HashSet<>());
    AverageDiscoveredPolicyViolationsDTO result = service.getChartData(successMetricsReport.getId()).averages;

    assertAggregationAverageHistory(result);
  }

  @Test
  public void testGetChartData_Averages_NullArguments() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);
    AverageDiscoveredPolicyViolationsDTO result = service.getChartData(successMetricsReport.getId()).averages;

    assertAggregationAverageHistory(result);
  }

  @Test
  public void testGetChartData_Averages_NoData() {
    Application application = tempEntity.newApplicationWithParent("no-evals-app");
    DateTimeUtils.setCurrentMillisFixed(now().minusMonths(3).getMillis());

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null,
        Collections.singleton(application.getId()));
    AverageDiscoveredPolicyViolationsDTO result = service.getChartData(successMetricsReport.getId()).averages;

    assertThat(result.evaluationCount).isEqualTo(0.0);
    assertThat(result.totalViolations.averageDiscovered).isEqualTo(0.0);
    assertThat(result.totalViolations.averageDiscoveredCritical).isEqualTo(0.0);
    assertThat(result.securityViolations.averageDiscovered).isEqualTo(0.0);
    assertThat(result.securityViolations.averageDiscoveredCritical).isEqualTo(0.0);
    assertThat(result.licenseViolations.averageDiscovered).isEqualTo(0.0);
    assertThat(result.licenseViolations.averageDiscoveredCritical).isEqualTo(0.0);
    assertThat(result.qualityViolations.averageDiscovered).isEqualTo(0.0);
    assertThat(result.qualityViolations.averageDiscoveredCritical).isEqualTo(0.0);
    assertThat(result.otherViolations.averageDiscovered).isEqualTo(0.0);
    assertThat(result.otherViolations.averageDiscoveredCritical).isEqualTo(0.0);
  }

  @Test
  public void testGetChartData_Averages_ExcludesDevelopmentStage() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Application application = tempEntity.newApplicationWithParent("averages-app");
    String appId = application.getId();

    Policy policy = tempEntity.newPolicy(application);
    Date fiveMonthsAgo = new LocalDate().withDayOfMonth(1).minusMonths(5).toDate();

    // Generate a policy violation in the develop stage so we can check that it is filtered out
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(appId, StageTypes.DEVELOP.getId(), "eval", fiveMonthsAgo);
    tempEntity.newPolicyViolation(eval, policy);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), new HashSet<>());
    AverageDiscoveredPolicyViolationsDTO result = service.getChartData(successMetricsReport.getId()).averages;

    assertAggregationAverageHistory(result);
  }

  @Test
  public void testGetChartData_Averages_UpdateAggregationsAfterSeveralMonths() {
    Application app = tempEntity.newApplicationWithParent("appId");
    String stageId = StageTypes.BUILD.getId();
    DateTime now = now().withDayOfMonth(15);
    tempEntity.newPolicyEvaluation(app.getId(), stageId, "scan1", now.minusMonths(3).toDate());
    setTimeTo(now.minusMonths(2));

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);
    AverageDiscoveredPolicyViolationsDTO result = service.getChartData(successMetricsReport.getId()).averages;

    AverageDiscoveredPolicyViolationsDTO expected = createAveragesDTO(1, 0);
    assertAverageDTO(result, expected);

    tempEntity.newPolicyEvaluation(app.getId(), stageId, "scan2", now.minusMonths(2).toDate());
    tempEntity.newPolicyEvaluation(app.getId(), stageId, "scan3", now.toDate());

    setTimeTo(now.plusDays(1));

    result = service.getChartData(successMetricsReport.getId()).averages;

    expected = createAveragesDTO(0.66666, 0);
    assertAverageDTO(result, expected);
  }

  @Test
  public void testIncludeLatestData() {
    LocalDate startOfMonth = now().withDayOfMonth(1).toLocalDate();
    Policy policy = tempEntity.newPolicy();
    Application app = tempEntity.newApplicationWithParent("appy");

    DateTime fakeNow = setTimeTo(now());
    String stageTypeId = StageTypes.RELEASE.getId();
    PolicyEvaluation firstEval = tempEntity.newPolicyEvaluation(app.getId(), stageTypeId, "scan1", fakeNow.toDate());
    tempEntity.newPolicyViolation(firstEval, policy, "arti", "fact", "1", "artifact1hash", null);

    // With 'on load' reports we should get the violation data immediately.
    fakeNow = setTimeTo(fakeNow.plusSeconds(1));
    SuccessMetricsReport monthlySuccessMetricsReport = createSuccessMetricsReport(null, null, "monthly", false);
    SuccessMetricsReport latestSuccessMetricsReport = createSuccessMetricsReport(null, null, "latest", true);
    AverageDiscoveredPolicyViolationsDTO result = service.getChartData(latestSuccessMetricsReport.getId()).averages;

    AverageDiscoveredPolicyViolationsDTO firstMonthAverage = createAveragesDTO(1, 1);
    assertAverageDTO(result, firstMonthAverage);

    // We should see new violations for the same app.
    fakeNow = setTimeTo(fakeNow.plusSeconds(1));
    PolicyEvaluation secondEval = tempEntity.newPolicyEvaluation(app.getId(), stageTypeId, "scan1", fakeNow.toDate());
    tempEntity.newPolicyViolation(secondEval, policy, "arti", "fact", "2", "artifact2hash", null);
    setTimeTo(fakeNow.plusSeconds(1));
    result = service.getChartData(latestSuccessMetricsReport.getId()).averages;
    firstMonthAverage = createAveragesDTO(2, 2);
    assertAverageDTO(result, firstMonthAverage);

    // We should also see violations for a different app.
    fakeNow = setTimeTo(fakeNow.plusSeconds(1));
    Application anotherApp = tempEntity.newApplicationWithParent("poppy");
    PolicyEvaluation firstEvalForAnotherApp = tempEntity.newPolicyEvaluation(anotherApp.getId(), stageTypeId,
        "anotherScan", fakeNow.toDate());
    tempEntity.newPolicyViolation(firstEvalForAnotherApp, policy, "arti", "fact", "3", "artifact3hash", null);
    fakeNow = setTimeTo(fakeNow.plusSeconds(1));
    result = service.getChartData(latestSuccessMetricsReport.getId()).averages;
    firstMonthAverage = createAveragesDTO(3, 1.5);
    assertAverageDTO(result, firstMonthAverage);

    // Make sure that without the flag we get nothing.
    int activeApplicationCount = service
        .getChartData(monthlySuccessMetricsReport.getId()).applicationCounts.activeApplications;
    assertThat(activeApplicationCount).isEqualTo(0);

    // Now roll over to the next month.
    fakeNow = setTimeTo(now().plusMonths(1).withDayOfMonth(1));
    startOfMonth = startOfMonth.plusMonths(1);

    // We should see first month's data without the flag now.
    result = service.getChartData(monthlySuccessMetricsReport.getId()).averages;
    assertAverageDTO(result, firstMonthAverage);

    // With the flag on, we should also get results for today (none right now).
    result = service.getChartData(latestSuccessMetricsReport.getId()).averages;
    AverageDiscoveredPolicyViolationsDTO firstMonthPlusEmptyAverage = createAveragesDTO(1.5, 0.75);
    assertAverageDTO(result, firstMonthPlusEmptyAverage);

    // New evaluations should show up if the flag is on.
    PolicyEvaluation thirdEval = tempEntity.newPolicyEvaluation(app.getId(), stageTypeId, "scan2", fakeNow.toDate());
    tempEntity.newPolicyViolation(thirdEval, policy, "arti", "fact", "4", "artifact4hash", null);
    setTimeTo(fakeNow.plusSeconds(1));
    result = service.getChartData(latestSuccessMetricsReport.getId()).averages;
    AverageDiscoveredPolicyViolationsDTO firstTwoMonthsAverage = createAveragesDTO(2, 1.25);
    assertAverageDTO(result, firstTwoMonthsAverage);

    // They should not show up without the flag though.
    result = service.getChartData(monthlySuccessMetricsReport.getId()).averages;
    assertAverageDTO(result, firstMonthAverage);
  }

  private ViolationCountsDTO createViolationCountsDTO(String timePeriodName, int discoveredSecuritySevereCount) {
    ViolationCountsDTO result = new ViolationCountsDTO();
    result.timePeriodName = timePeriodName;
    result.discoveredCounts.get(PolicyThreatCategory.SECURITY).put(SEVERE, discoveredSecuritySevereCount);
    return result;
  }

  private AverageDiscoveredPolicyViolationsDTO createAveragesDTO(double evaluationCount, double numDiscoveredOther) {
    AverageDiscoveredPolicyViolationsDTO dto = new AverageDiscoveredPolicyViolationsDTO();
    dto.evaluationCount = evaluationCount;
    dto.totalViolations = new ThreatCategoryPolicyViolationsDTO(numDiscoveredOther, 0.0);
    dto.securityViolations = new ThreatCategoryPolicyViolationsDTO(0.0, 0.0);
    dto.licenseViolations = new ThreatCategoryPolicyViolationsDTO(0.0, 0.0);
    dto.qualityViolations = new ThreatCategoryPolicyViolationsDTO(0.0, 0.0);
    dto.otherViolations = new ThreatCategoryPolicyViolationsDTO(numDiscoveredOther, 0.0);

    return dto;
  }

  @Test
  public void testGetApplicationsCounts_AggregationsAlreadyExist() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper
        .createApplicationCountAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, applicationIds);
    ApplicationCountsDTO result = service.getChartData(successMetricsReport.getId()).applicationCounts;

    assertApplicationCountsDTO(result, makeAggregationHistoryCountsDTO());
  }

  @Test
  public void testGetApplicationsCounts_AggregationsAlreadyExist_ByOrganizationId() {
    PolicyViolationAggregationDataHelper.createApplicationCountAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(
        Collections.singleton(ORG_ID), null);
    ApplicationCountsDTO result = service.getChartData(successMetricsReport.getId()).applicationCounts;

    assertApplicationCountsDTO(result, makeAggregationHistoryCountsDTO());
  }

  @Test
  public void testGetApplicationsCounts_AggregationsAlreadyExist_SpecificApplicationId() {
    PolicyViolationAggregationDataHelper.createApplicationCountAggregationHistory(tempEntity);
    String appId = PolicyViolationAggregationDataHelper.APPLICATION_IDS[2];

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, Collections.singleton(appId));
    ApplicationCountsDTO result = service.getChartData(successMetricsReport.getId()).applicationCounts;

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
  public void testGetChartData_ApplicationCounts_EmptyArguments() {
    PolicyViolationAggregationDataHelper.createApplicationCountAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(),
        new HashSet<>());
    ApplicationCountsDTO result = service.getChartData(successMetricsReport.getId()).applicationCounts;

    assertApplicationCountsDTO(result, makeAggregationHistoryCountsDTO());
  }

  @Test
  public void testGetChartData_ApplicationCounts_NullArguments() {
    PolicyViolationAggregationDataHelper.createApplicationCountAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);
    ApplicationCountsDTO result = service.getChartData(successMetricsReport.getId()).applicationCounts;

    assertApplicationCountsDTO(result, makeAggregationHistoryCountsDTO());
  }

  @Test
  public void testGetChartData_ApplicationCounts_NoData() {
    Application application = tempEntity.newApplicationWithParent("no-evals-app");

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null,
        Collections.singleton(application.getId()));
    ApplicationCountsDTO result = service.getChartData(successMetricsReport.getId()).applicationCounts;

    ApplicationCountsDTO expected = new ApplicationCountsDTO();
    expected.activeApplications = 0;
    expected.totalApplications = 1;

    assertApplicationCountsDTO(result, expected);
  }

  @Test
  public void testGetChartData_ApplicationCounts_ExcludesDevelopmentStage() {
    PolicyViolationAggregationDataHelper.createApplicationCountAggregationHistory(tempEntity);

    Application application = tempEntity.newApplicationWithParent("counts-app");
    String appId = application.getId();

    Policy policy = tempEntity.newPolicy(application);
    Date fiveMonthsAgo = new LocalDate().withDayOfMonth(1).minusMonths(5).toDate();

    // Generate a policy violation in the develop stage so we can check that it is filtered out
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(appId, StageTypes.DEVELOP.getId(), "eval", fiveMonthsAgo);
    tempEntity.newPolicyViolation(eval, policy);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(),
        new HashSet<>());
    ApplicationCountsDTO result = service.getChartData(successMetricsReport.getId()).applicationCounts;
    ApplicationCountsDTO expected = makeAggregationHistoryCountsDTO();

    // total should increase but nothing else
    expected.totalApplications++;

    assertApplicationCountsDTO(result, expected);
  }

  @Test
  public void testGetChartData_lastUpdatedTimestamp_includingLatestData() {
    PolicyViolationAggregationDataHelper.createApplicationCountAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(Collections.emptySet(),
        Collections.emptySet(), "report", true);
    Date result = service.getChartData(successMetricsReport.getId()).lastUpdated;

    int millisFromNow = (int) (new LocalDate().toDate().getTime() - result.getTime());
    assertThat(millisFromNow).isLessThan(5000);
  }

  @Test
  public void testGetChartData_lastUpdatedTimestamp_monthlyData() {
    PolicyViolationAggregationDataHelper.createApplicationCountAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(Collections.emptySet(),
        Collections.emptySet(), "report", false);
    Date result = service.getChartData(successMetricsReport.getId()).lastUpdated;

    Date startOfThisMonth = new LocalDate().withDayOfMonth(1).toDate();
    Date startOfThisWeek = new LocalDate().withDayOfWeek(1).toDate();
    assertThat(result).isEqualTo(Ordering.natural().max(startOfThisMonth, startOfThisWeek));
  }

  @Test
  public void testGetChartData_SucessMetricsReportDataOutOfDateAndAggregationsAlreadyExist() {
    LocalDate today = new LocalDate();
    LocalDate firstGenerationTime = today.minusMonths(2).withDayOfMonth(2);
    LocalDate secondGenerationTime = firstGenerationTime.plusMonths(1);

    Application app = tempEntity.newApplicationWithParent();

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, Collections.singleton(app.getId()));

    DateTimeUtils.setCurrentMillisFixed(firstGenerationTime.toDateTimeAtStartOfDay().getMillis());

    tempEntity.newPolicyViolationAggregation(app.getId(), firstGenerationTime.withDayOfMonth(1).toDate(), MONTH,
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    // cause the initial report data to be generated
    service.getChartData(successMetricsReport.getId());

    DateTimeUtils.setCurrentMillisFixed(secondGenerationTime.toDateTimeAtStartOfDay().getMillis());

    tempEntity.newPolicyViolationAggregation(app.getId(), secondGenerationTime.withDayOfMonth(1).toDate(), MONTH,
        new DescriptiveStatistics(new double[]{1000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(1, 0, 0, 0).get(),
        fixed().security(1, 0, 0, 0).get(),
        waived().get(),
        openWithSampleData().get(), //
        1);

    // run the chart again
    SuccessMetricsChartDataDTO results = service.getChartData(successMetricsReport.getId());

    List<MttrDTO> expectedMttrs = Arrays.asList(new MttrDTO("Oct", null, null), new MttrDTO("Nov", 1, null));

    ApplicationCountsDTO expectedApplicationCounts = new ApplicationCountsDTO(1, 1,
        new ThreatCategoryApplicationCount(1, 0), //
        new ThreatCategoryApplicationCount(1, 0), //
        new ThreatCategoryApplicationCount(0, 0), //
        new ThreatCategoryApplicationCount(0, 0), //
        new ThreatCategoryApplicationCount(0, 0));

    AverageDiscoveredPolicyViolationsDTO expectedAverages = new AverageDiscoveredPolicyViolationsDTO(0.5,
        new ThreatCategoryPolicyViolationsDTO(0.5, 0), //
        new ThreatCategoryPolicyViolationsDTO(0.5, 0), //
        new ThreatCategoryPolicyViolationsDTO(0, 0), //
        new ThreatCategoryPolicyViolationsDTO(0, 0), //
        new ThreatCategoryPolicyViolationsDTO(0, 0));

    // make sure that the data that comes back is fresh and not still using the cached SuccessMetricsReportData
    // from the previous run
    assertMttrDTOs(results.mttrs, expectedMttrs);
    assertApplicationCountsDTO(results.applicationCounts, expectedApplicationCounts);
    assertAverageDTO(results.averages, expectedAverages);
  }

  @Test
  public void testGetChartData_SucessMetricsReportDataAlreadyExists() {
    LocalDate today = new LocalDate();
    LocalDate firstRunTime = today.minusMonths(2).withDayOfMonth(2);
    LocalDate secondRunTime = firstRunTime.plusDays(1);

    Application app = tempEntity.newApplicationWithParent();

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, Collections.singleton(app.getId()));

    DateTimeUtils.setCurrentMillisFixed(firstRunTime.toDateTimeAtStartOfDay().getMillis());

    tempEntity.newPolicyViolationAggregation(app.getId(), firstRunTime.withDayOfMonth(1).toDate(), MONTH,
        new DescriptiveStatistics(new double[]{1000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}),
        discovered().security(1, 0, 0, 0).get(),
        fixed().security(1, 0, 0, 0).get(),
        waived().get(),
        openWithSampleData().get(), //
        1);

    // cause the initial report data to be generated
    SuccessMetricsChartDataDTO results = service.getChartData(successMetricsReport.getId());

    List<MttrDTO> expectedMttrs = Collections.singletonList(new MttrDTO("Oct", 1, null));

    ApplicationCountsDTO expectedApplicationCounts = new ApplicationCountsDTO(1, 1,
        new ThreatCategoryApplicationCount(1, 0), //
        new ThreatCategoryApplicationCount(1, 0), //
        new ThreatCategoryApplicationCount(0, 0), //
        new ThreatCategoryApplicationCount(0, 0), //
        new ThreatCategoryApplicationCount(0, 0));

    AverageDiscoveredPolicyViolationsDTO expectedAverages = new AverageDiscoveredPolicyViolationsDTO(1,
        new ThreatCategoryPolicyViolationsDTO(1, 0), //
        new ThreatCategoryPolicyViolationsDTO(1, 0), //
        new ThreatCategoryPolicyViolationsDTO(0, 0), //
        new ThreatCategoryPolicyViolationsDTO(0, 0), //
        new ThreatCategoryPolicyViolationsDTO(0, 0));

    assertMttrDTOs(results.mttrs, expectedMttrs);
    assertApplicationCountsDTO(results.applicationCounts, expectedApplicationCounts);
    assertAverageDTO(results.averages, expectedAverages);

    DateTimeUtils.setCurrentMillisFixed(secondRunTime.toDateTimeAtStartOfDay().getMillis());

    // run the chart again
    results = service.getChartData(successMetricsReport.getId());

    assertMttrDTOs(results.mttrs, expectedMttrs);
    assertApplicationCountsDTO(results.applicationCounts, expectedApplicationCounts);
    assertAverageDTO(results.averages, expectedAverages);
  }

  @Test
  public void testIsReportDataOutOfDate() {
    DateTime reportLastUpdated = new DateTime(2018, 8, 30, 0, 0); // Thu
    Set<String> reportApplicationIds = new HashSet<>(Arrays.asList("1234", "5678"));
    SuccessMetricsReportData reportData = new SuccessMetricsReportData();
    reportData.setLastUpdated(reportLastUpdated.toDate());
    reportData.setIncludedApplicationIds(reportApplicationIds);

    // if the report data is null, true should always be returned
    assertThat(isReportDataOutOfDate(null, false, reportLastUpdated, reportApplicationIds)).isTrue();
    assertThat(isReportDataOutOfDate(null, true, reportLastUpdated.plusDays(1), Collections.singleton("1234")))
        .isTrue();

    // test differences in applicationIdsToInclude

    // the two application ids that are in the report, plus another one
    Set<String> moreApplicationIds = new HashSet<>(Arrays.asList("1234", "5678", "asdf"));

    assertThat(isReportDataOutOfDate(reportData, true, reportLastUpdated, reportApplicationIds)).isFalse();
    assertThat(isReportDataOutOfDate(reportData, true, reportLastUpdated, Collections.emptySet())).isTrue();
    assertThat(isReportDataOutOfDate(reportData, true, reportLastUpdated, Collections.singleton("1234"))).isTrue();
    assertThat(isReportDataOutOfDate(reportData, true, reportLastUpdated, moreApplicationIds)).isTrue();

    // test timeliness when includeLatestData is false
    DateTime lastInstantOfReportMonth = reportLastUpdated.dayOfMonth()
        .withMaximumValue()
        .millisOfDay()
        .withMaximumValue();
    DateTime lastInstantOfReportWeek = reportLastUpdated.dayOfWeek()
        .withMaximumValue()
        .millisOfDay()
        .withMaximumValue();
    DateTime firstInstantOfNextMonth = reportLastUpdated.plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay();
    DateTime firstInstantOfNextWeek = reportLastUpdated.plusWeeks(1).withDayOfWeek(1).withTimeAtStartOfDay();

    assertThat(isReportDataOutOfDate(reportData, false, reportLastUpdated.plusDays(1), reportApplicationIds)).isFalse();
    // end of this month is in the same week
    assertThat(isReportDataOutOfDate(reportData, false, lastInstantOfReportMonth, reportApplicationIds)).isFalse();
    // end of this week is the next month
    assertThat(isReportDataOutOfDate(reportData, false, lastInstantOfReportWeek, reportApplicationIds)).isTrue();
    assertThat(isReportDataOutOfDate(reportData, false, firstInstantOfNextMonth, reportApplicationIds)).isTrue();
    assertThat(isReportDataOutOfDate(reportData, false, firstInstantOfNextWeek, reportApplicationIds)).isTrue();

    // test timeliness when includeLatestData is true
    assertThat(isReportDataOutOfDate(reportData, true, reportLastUpdated, reportApplicationIds)).isFalse();
    assertThat(isReportDataOutOfDate(reportData, true, reportLastUpdated.plusDays(1), reportApplicationIds)).isTrue();
    assertThat(isReportDataOutOfDate(reportData, true, lastInstantOfReportMonth, reportApplicationIds)).isTrue();
    assertThat(isReportDataOutOfDate(reportData, true, lastInstantOfReportWeek, reportApplicationIds)).isTrue();
    assertThat(isReportDataOutOfDate(reportData, true, firstInstantOfNextMonth, reportApplicationIds)).isTrue();
    assertThat(isReportDataOutOfDate(reportData, true, firstInstantOfNextWeek, reportApplicationIds)).isTrue();
  }

  @Test
  public void testGetChartData_ViolationCounts_AggregationsAlreadyExist() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), applicationIds);
    List<ViolationCountsDTO> result = service.getChartData(successMetricsReport.getId()).violationCounts;

    assertAggregationViolationCountHistory(result);
  }

  @Test
  public void testGetChartData_ViolationCounts_AggregationsAlreadyExist_ByOrganizationId() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    // create an app in a separate org so we can check that it is filtered out
    Application app2 = tempEntity.newApplicationWithParent();
    String appId2 = app2.getId();

    Date app2Eval1Date = new LocalDate().withDayOfMonth(2).minusMonths(1).toDateTimeAtStartOfDay().toDate();
    Date app2Eval2Date = new Date(app2Eval1Date.getTime() + 500000);

    Policy app2Policy = tempEntity.newPolicy(app2);
    StageType stageType = StageTypes.BUILD;

    PolicyEvaluation app2Eval1 = tempEntity.newPolicyEvaluation(appId2, stageType.getId(), "app2Eval1", app2Eval1Date);
    tempEntity.newPolicyEvaluation(appId2, stageType.getId(), "app2Eval2", app2Eval2Date);
    tempEntity.newPolicyViolation(app2Eval1, app2Policy);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(Collections.singleton(ORG_ID),
        new HashSet<>());
    List<ViolationCountsDTO> result = service.getChartData(successMetricsReport.getId()).violationCounts;

    assertAggregationViolationCountHistory(result);
  }

  @Test
  public void testGetChartData_ViolationCounts_AggregationsAlreadyExist_SpecificApplicationId() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    String appId = PolicyViolationAggregationDataHelper.APPLICATION_IDS[2];

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(),
        Collections.singleton(appId));
    List<ViolationCountsDTO> result = service.getChartData(successMetricsReport.getId()).violationCounts;

    assertAggregationViolationCountHistoryForThirdApp(result);
  }

  @Test
  public void testGetChartData_ViolationCounts_EmptyArguments() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), new HashSet<>());
    List<ViolationCountsDTO> result = service.getChartData(successMetricsReport.getId()).violationCounts;

    assertAggregationViolationCountHistory(result);
  }

  @Test
  public void testGetChartData_ViolationCounts_NullArguments() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);
    AverageDiscoveredPolicyViolationsDTO result = service.getChartData(successMetricsReport.getId()).averages;

    assertAggregationAverageHistory(result);
  }

  @Test
  public void testGetChartData_ViolationCounts_NoData() {
    Application application = tempEntity.newApplicationWithParent("no-evals-app");
    DateTimeUtils.setCurrentMillisFixed(now().minusWeeks(3).getMillis());

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null,
        Collections.singleton(application.getId()));
    List<ViolationCountsDTO> result = service.getChartData(successMetricsReport.getId()).violationCounts;

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetChartData_ViolationCounts_ExcludesDevelopmentStage() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Application application = tempEntity.newApplicationWithParent("averages-app");
    String appId = application.getId();

    Policy policy = tempEntity.newPolicy(application);
    Date fiveWeeksAgo = new LocalDate().withDayOfWeek(1).minusWeeks(5).toDate();

    // Generate a policy violation in the develop stage so we can check that it is filtered out
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(appId, StageTypes.DEVELOP.getId(), "eval", fiveWeeksAgo);
    tempEntity.newPolicyViolation(eval, policy);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), new HashSet<>());
    List<ViolationCountsDTO> result = service.getChartData(successMetricsReport.getId()).violationCounts;

    assertAggregationViolationCountHistory(result);
  }

  @Test
  public void testGetChartData_ViolationCounts_UpdateAggregationsAfterSeveralWeeks() {
    Application app = tempEntity.newApplicationWithParent("appId");
    String stageId = StageTypes.BUILD.getId();
    Policy policy = tempEntity.newPolicy(app);
    DateTime now = now();
    tempEntity.newPolicyEvaluation(app.getId(), stageId, "scan1", now.minusWeeks(6).toDate());
    setTimeTo(now.minusWeeks(5));

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);
    List<ViolationCountsDTO> result = service.getChartData(successMetricsReport.getId()).violationCounts;

    ViolationCountsDTO expected = createViolationCountsDTO("Week of October 30th", 0);
    assertViolationCountsDTO(expected, result.get(0));

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stageId, "scan2", now.minusWeeks(4).toDate());
    tempEntity.newPolicyViolation(eval, policy);

    setTimeTo(now);

    result = service.getChartData(successMetricsReport.getId()).violationCounts;

    expected = createViolationCountsDTO("Week of November 13th", 1);
    assertViolationCountsDTO(expected, result.get(2));
  }

  @Test
  public void testGetChartData_ViolationTotalsByCategory_AggregationsAlreadyExist() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), applicationIds);
    List<ViolationsByCategoryDTO> result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    assertAggregationViolationTotalsByCategoryHistory(result);
  }

  @Test
  public void testGetChartData_ViolationTotalsByCategory_AggregationsAlreadyExist_IncludeLatestData() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), applicationIds, "report",
        true);
    List<ViolationsByCategoryDTO> result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    assertAggregationViolationTotalsByCategoryHistoryIncludeLatestData(result);
  }

  @Test
  public void testGetChartData_ViolationTotalsByCategory_AggregationsAlreadyExist_ByOrganizationId() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    // create an app in a separate org so we can check that it is filtered out
    Application app2 = tempEntity.newApplicationWithParent();
    String appId2 = app2.getId();

    Date app2Eval1Date = new LocalDate().withDayOfMonth(2).minusMonths(1).toDateTimeAtStartOfDay().toDate();
    Date app2Eval2Date = new Date(app2Eval1Date.getTime() + 500000);

    Policy app2Policy = tempEntity.newPolicy(app2);
    StageType stageType = StageTypes.BUILD;

    PolicyEvaluation app2Eval1 = tempEntity.newPolicyEvaluation(appId2, stageType.getId(), "app2Eval1", app2Eval1Date);
    tempEntity.newPolicyEvaluation(appId2, stageType.getId(), "app2Eval2", app2Eval2Date);
    tempEntity.newPolicyViolation(app2Eval1, app2Policy);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(Collections.singleton(ORG_ID),
        new HashSet<>());
    List<ViolationsByCategoryDTO> result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    assertAggregationViolationTotalsByCategoryHistory(result);
  }

  @Test
  public void testGetChartData_ViolationTotalsByCategory_AggregationsAlreadyExist_SpecificApplicationId() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);
    String appId = PolicyViolationAggregationDataHelper.APPLICATION_IDS[2];

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(),
        Collections.singleton(appId));
    List<ViolationsByCategoryDTO> result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    assertAggregationViolationTotalsByCategoryHistoryForThirdApp(result);
  }

  @Test
  public void testGetChartData_ViolationTotalsByCategory_EmptyArguments() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), new HashSet<>());
    List<ViolationsByCategoryDTO> result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    assertAggregationViolationTotalsByCategoryHistory(result);
  }

  @Test
  public void testGetChartData_ViolationTotalsByCategory_NullArguments() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);
    List<ViolationsByCategoryDTO> result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    assertAggregationViolationTotalsByCategoryHistory(result);
  }

  @Test
  public void testGetChartData_ViolationTotalsByCategory_NoData() {
    Application application = tempEntity.newApplicationWithParent("no-evals-app");
    DateTimeUtils.setCurrentMillisFixed(now().minusWeeks(3).getMillis());

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null,
        Collections.singleton(application.getId()));
    List<ViolationsByCategoryDTO> result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetChartData_ViolationTotalsByCategory_NoData_IncludeLatestData() {
    Application application = tempEntity.newApplicationWithParent("no-evals-app");
    DateTimeUtils.setCurrentMillisFixed(now().minusWeeks(3).getMillis());

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null,
        Collections.singleton(application.getId()), "latest", true);
    List<ViolationsByCategoryDTO> result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetChartData_ViolationTotalsByCategory_ExcludesDevelopmentStage() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    Application application = tempEntity.newApplicationWithParent("averages-app");
    String appId = application.getId();

    Policy policy = tempEntity.newPolicy(application);
    Date fiveWeeksAgo = new LocalDate().withDayOfWeek(1).minusWeeks(5).toDate();

    // Generate a policy violation in the develop stage so we can check that it is filtered out
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(appId, StageTypes.DEVELOP.getId(), "eval", fiveWeeksAgo);
    tempEntity.newPolicyViolation(eval, policy);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), new HashSet<>());
    List<ViolationsByCategoryDTO> result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    assertAggregationViolationTotalsByCategoryHistory(result);
  }

  @Test
  public void testGetChartData_ViolationTotalsByCategory_UpdateAggregationsAfterSeveralWeeks() {
    Application app = tempEntity.newApplicationWithParent("appId");
    String stageId = StageTypes.BUILD.getId();
    Policy policy = tempEntity.newPolicy(app);
    DateTime now = now();
    tempEntity.newPolicyEvaluation(app.getId(), stageId, "scan1", now.minusWeeks(8).toDate());
    setTimeTo(now.minusWeeks(7));

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);
    List<ViolationsByCategoryDTO> result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    List<ViolationsByCategoryDTO> expectedDTOs = Arrays.asList(
        createEmptyViolationsByCategoryDTO("07 Aug"),
        createEmptyViolationsByCategoryDTO("14 Aug"),
        createEmptyViolationsByCategoryDTO("21 Aug"),
        createEmptyViolationsByCategoryDTO("28 Aug"),
        createEmptyViolationsByCategoryDTO("04 Sep"),
        createEmptyViolationsByCategoryDTO("11 Sep"),
        createEmptyViolationsByCategoryDTO("18 Sep"),
        createEmptyViolationsByCategoryDTO("25 Sep"),
        createEmptyViolationsByCategoryDTO("02 Oct"),
        createEmptyViolationsByCategoryDTO("09 Oct"),
        createEmptyViolationsByCategoryDTO("16 Oct"),
        new ViolationsByCategoryDTO("23 Oct", 0, 0, 0, 0));

    assertAggregationViolationTotalsByCategoryHistory(result, expectedDTOs);

    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), stageId, "scan2", now.minusWeeks(6).toDate());
    tempEntity.newPolicyViolation(eval2, policy);

    setTimeTo(now.minusWeeks(4));

    result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    expectedDTOs = new ArrayList<>(expectedDTOs.subList(3, 12));
    expectedDTOs.add(new ViolationsByCategoryDTO("30 Oct", 0, 0, 0, 0));
    expectedDTOs.add(new ViolationsByCategoryDTO("06 Nov", 1, 0, 0, 0));
    expectedDTOs.add(new ViolationsByCategoryDTO("13 Nov", 1, 0, 0, 0));
    assertAggregationViolationTotalsByCategoryHistory(result, expectedDTOs);

    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), stageId, "scan3", now.minusWeeks(3).toDate());
    tempEntity.newPolicyViolation(eval3, policy, 9, PolicyThreatCategory.SECURITY, "ano", "ther", "artifact");

    setTimeTo(now);

    result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    expectedDTOs = new ArrayList<>(expectedDTOs.subList(4, 12));
    expectedDTOs.add(new ViolationsByCategoryDTO("20 Nov", 1, 0, 0, 0));
    expectedDTOs.add(new ViolationsByCategoryDTO("27 Nov", 2, 0, 0, 0));
    expectedDTOs.add(new ViolationsByCategoryDTO("04 Dec", 2, 0, 0, 0));
    expectedDTOs.add(new ViolationsByCategoryDTO("11 Dec", 2, 0, 0, 0));
    assertAggregationViolationTotalsByCategoryHistory(result, expectedDTOs);
  }

  @Test
  public void testGetChartData_ViolationTotalsByCategory_UpdateAggregationsAfterSeveralWeeks_IncludeLatestData() {
    Application app = tempEntity.newApplicationWithParent("appId");
    String stageId = StageTypes.BUILD.getId();
    Policy policy = tempEntity.newPolicy(app);
    DateTime now = now();
    tempEntity.newPolicyEvaluation(app.getId(), stageId, "scan1", now.minusWeeks(8).toDate());
    setTimeTo(now.minusWeeks(7));

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null, "report", true);
    List<ViolationsByCategoryDTO> result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    List<ViolationsByCategoryDTO> expectedDTOs = Arrays.asList(
        createEmptyViolationsByCategoryDTO("14 Aug"),
        createEmptyViolationsByCategoryDTO("21 Aug"),
        createEmptyViolationsByCategoryDTO("28 Aug"),
        createEmptyViolationsByCategoryDTO("04 Sep"),
        createEmptyViolationsByCategoryDTO("11 Sep"),
        createEmptyViolationsByCategoryDTO("18 Sep"),
        createEmptyViolationsByCategoryDTO("25 Sep"),
        createEmptyViolationsByCategoryDTO("02 Oct"),
        createEmptyViolationsByCategoryDTO("09 Oct"),
        createEmptyViolationsByCategoryDTO("16 Oct"),
        new ViolationsByCategoryDTO("23 Oct", 0, 0, 0, 0),
        new ViolationsByCategoryDTO("now", 0, 0, 0, 0));

    assertAggregationViolationTotalsByCategoryHistory(result, expectedDTOs);

    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), stageId, "scan2", now.minusWeeks(6).toDate());
    tempEntity.newPolicyViolation(eval2, policy);

    setTimeTo(now.minusWeeks(4));

    result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    expectedDTOs = new ArrayList<>(expectedDTOs.subList(3, 11));
    expectedDTOs.add(new ViolationsByCategoryDTO("30 Oct", 0, 0, 0, 0));
    expectedDTOs.add(new ViolationsByCategoryDTO("06 Nov", 1, 0, 0, 0));
    expectedDTOs.add(new ViolationsByCategoryDTO("13 Nov", 1, 0, 0, 0));
    expectedDTOs.add(new ViolationsByCategoryDTO("now", 1, 0, 0, 0));
    assertAggregationViolationTotalsByCategoryHistory(result, expectedDTOs);

    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), stageId, "scan3", now.minusWeeks(3).toDate());
    tempEntity.newPolicyViolation(eval3, policy, 9, PolicyThreatCategory.SECURITY, "ano", "ther", "artifact");

    setTimeTo(now);

    result = service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks;

    expectedDTOs = new ArrayList<>(expectedDTOs.subList(4, 11));
    expectedDTOs.add(new ViolationsByCategoryDTO("20 Nov", 1, 0, 0, 0));
    expectedDTOs.add(new ViolationsByCategoryDTO("27 Nov", 2, 0, 0, 0));
    expectedDTOs.add(new ViolationsByCategoryDTO("04 Dec", 2, 0, 0, 0));
    expectedDTOs.add(new ViolationsByCategoryDTO("11 Dec", 2, 0, 0, 0));
    expectedDTOs.add(new ViolationsByCategoryDTO("now", 2, 0, 0, 0));
    assertAggregationViolationTotalsByCategoryHistory(result, expectedDTOs);
  }

  @Test
  public void testGetChartData_ViolationTotalsByCategory_FullMonthWithPartialWeek() {
    Application app = tempEntity.newApplicationWithParent("appId");
    String stageId = StageTypes.BUILD.getId();
    Policy policy = tempEntity.newPolicy(app);

    // Add a violation at the end of the month
    DateTime now = now().withYear(2018).withMonthOfYear(7).withDayOfMonth(31);
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), stageId, "scan1", now.toDate());

    // Roll over to the next month (still in the same week)
    setTimeTo(now.withMonthOfYear(8).withDayOfMonth(1));
    tempEntity.newPolicyViolation(eval, policy);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(null, null);

    // Since we don't have a full week, no data is returned
    assertThat(service.getChartData(successMetricsReport.getId()).violationsByCategoryWeeks).isEmpty();

    // Sanity check for monthly data
    ApplicationCountsDTO expected = new ApplicationCountsDTO();
    expected.totalApplications = 1;
    expected.activeApplications = 1;
    expected.total.applicationsWithViolations = 1;
    expected.total.applicationsWithCriticalViolations = 0;
    expected.security.applicationsWithViolations = 1;
    expected.security.applicationsWithCriticalViolations = 0;
    expected.license.applicationsWithViolations = 0;
    expected.license.applicationsWithCriticalViolations = 0;
    expected.quality.applicationsWithViolations = 0;
    expected.quality.applicationsWithCriticalViolations = 0;
    expected.other.applicationsWithViolations = 0;
    expected.other.applicationsWithCriticalViolations = 0;

    assertApplicationCountsDTO(service.getChartData(successMetricsReport.getId()).applicationCounts, expected);
  }

  private ViolationsByCategoryDTO createEmptyViolationsByCategoryDTO(String timePeriodName) {
    return new ViolationsByCategoryDTO(timePeriodName, null, null, null, null);
  }

  /**
   * @return an ApplicationCountsDTO with the expected values from the
   *         PolicyViolationAggregationDataHelper.createApplicationCountAggregationHistory method
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

    dto = new MttrDTO();
    dto.timePeriodName = "Dec";
    dto.mttrInSeconds = 2;
    dto.criticalMttrInSeconds = 2;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Jan";
    dto.mttrInSeconds = 2;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Feb";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Mar";
    dto.mttrInSeconds = 1;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Apr";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "May";
    dto.mttrInSeconds = 5;
    dto.criticalMttrInSeconds = 5;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Jun";
    dto.mttrInSeconds = 16;
    dto.criticalMttrInSeconds = 16;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Jul";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Aug";
    dto.mttrInSeconds = 15;
    dto.criticalMttrInSeconds = 20;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Sep";
    dto.mttrInSeconds = 37;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Oct";
    dto.mttrInSeconds = 5;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Nov";
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

    dto = new MttrDTO();
    dto.timePeriodName = "Mar";
    dto.mttrInSeconds = 1;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Apr";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "May";
    dto.mttrInSeconds = 5;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Jun";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Jul";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Aug";
    dto.mttrInSeconds = 15;
    dto.criticalMttrInSeconds = 20;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Sep";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Oct";
    dto.mttrInSeconds = null;
    dto.criticalMttrInSeconds = null;
    expected.add(dto);

    dto = new MttrDTO();
    dto.timePeriodName = "Nov";
    dto.mttrInSeconds = 10;
    dto.criticalMttrInSeconds = 12;
    expected.add(dto);

    assertMttrDTOs(actual, expected);
  }

  private void assertAggregationAverageHistory(AverageDiscoveredPolicyViolationsDTO actualDTO) {
    AverageDiscoveredPolicyViolationsDTO expectedDTO = new AverageDiscoveredPolicyViolationsDTO(505,
        new ThreatCategoryPolicyViolationsDTO(10, 6), //
        new ThreatCategoryPolicyViolationsDTO(1, 0), //
        new ThreatCategoryPolicyViolationsDTO(2, 1), //
        new ThreatCategoryPolicyViolationsDTO(3, 2), //
        new ThreatCategoryPolicyViolationsDTO(4, 3));

    assertAverageDTO(actualDTO, expectedDTO);
  }

  /**
   * Test the results returned when filtered specifically to the third app defined in
   * PolicyViolationAggregationDataHelper
   */
  private void assertAggregationAverageHistoryForThirdApp(AverageDiscoveredPolicyViolationsDTO actualOverallDTO) {
    AverageDiscoveredPolicyViolationsDTO expectedDTO = new AverageDiscoveredPolicyViolationsDTO(335,
        new ThreatCategoryPolicyViolationsDTO(5, 3), //
        new ThreatCategoryPolicyViolationsDTO(1, 0), //
        new ThreatCategoryPolicyViolationsDTO(1, 1), //
        new ThreatCategoryPolicyViolationsDTO(1, 0), //
        new ThreatCategoryPolicyViolationsDTO(2, 2));

    assertAverageDTO(actualOverallDTO, expectedDTO);
  }

  private void assertAggregationViolationCountHistory(List<ViolationCountsDTO> actualDTOs) {
    List<ViolationCountsDTO> expectedDTOs = Arrays.asList(
        new ViolationCountsDTO("Week of September 18th",
            discovered().security(0, 0, 0, 0).license(0, 0, 0, 2).quality(1, 1, 0, 2).other(0, 0, 0, 6).asMap(),
            fixed().security(2, 0, 1, 0).license(0, 1, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 1, 0).other(0, 0, 0, 1).asMap()),
        new ViolationCountsDTO("Week of September 25th",
            discovered().security(0, 0, 0, 0).license(0, 0, 0, 2).quality(0, 0, 0, 4).other(1, 3, 2, 6).asMap(),
            fixed().security(1, 0, 0, 0).license(0, 1, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(1, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of October 2nd",
            discovered().security(1, 0, 0, 0).license(1, 0, 0, 1).quality(0, 0, 0, 3).other(0, 0, 0, 3).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(1, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of October 9th",
            discovered().security(1, 0, 5, 0).license(1, 2, 0, 3).quality(0, 3, 0, 6).other(0, 0, 0, 6).asMap(),
            fixed().security(3, 1, 0, 0).license(0, 1, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 1, 0, 0).license(0, 0, 0, 0).quality(1, 1, 0, 0).other(1, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of October 16th",
            discovered().security(0, 0, 0, 0).license(1, 1, 1, 1).quality(1, 0, 0, 3).other(0, 1, 1, 0).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of October 23rd",
            discovered().security(2, 1, 3, 0).license(0, 0, 0, 3).quality(1, 2, 0, 6).other(1, 7, 1, 9).asMap(),
            fixed().security(0, 0, 0, 1).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(2, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of October 30th",
            discovered().security(0, 3, 0, 0).license(0, 0, 0, 3).quality(2, 3, 4, 3).other(3, 0, 0, 12).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 1, 0, 0).other(3, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 2, 52).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of November 6th",
            discovered().security(0, 0, 0, 0).license(0, 1, 1, 2).quality(1, 0, 1, 4).other(2, 0, 0, 10).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of November 13th",
            discovered().security(1, 1, 0, 0).license(0, 2, 0, 2).quality(0, 1, 1, 2).other(0, 2, 0, 4).asMap(),
            fixed().security(0, 0, 0, 1).license(0, 1, 1, 1).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 1).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of November 20th",
            discovered().security(1, 0, 3, 0).license(1, 1, 4, 2).quality(0, 2, 0, 8).other(0, 2, 0, 10).asMap(),
            fixed().security(1, 2, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(1, 2, 0, 0).other(3, 1, 0, 0).asMap()),
        new ViolationCountsDTO("Week of November 27th",
            discovered().security(0, 0, 1, 0).license(0, 0, 2, 1).quality(1, 0, 0, 2).other(0, 0, 0, 3).asMap(),
            fixed().security(0, 0, 1, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of December 4th",
            discovered().security(0, 0, 8, 0).license(0, 0, 0, 4).quality(3, 0, 1, 4).other(0, 0, 0, 12).asMap(),
            fixed().security(1, 1, 0, 0).license(0, 0, 0, 0).quality(2, 2, 1, 1).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 2, 2).license(0, 0, 0, 0).quality(1, 1, 1, 1).other(0, 0, 0, 0).asMap()));
    assertThat(actualDTOs).hasSameSizeAs(expectedDTOs);
    for (int i = 0; i < expectedDTOs.size(); i++) {
      assertViolationCountsDTO(expectedDTOs.get(i), actualDTOs.get(i));
    }
  }

  private void assertAggregationViolationCountHistoryForThirdApp(List<ViolationCountsDTO> actualDTOs) {
    List<ViolationCountsDTO> expectedDTOs = Arrays.asList(
        new ViolationCountsDTO("Week of October 9th",
            discovered().security(0, 0, 4, 0).license(0, 0, 0, 3).quality(0, 0, 0, 0).other(0, 0, 0, 3).asMap(),
            fixed().security(2, 1, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(1, 0, 0, 0).other(1, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of October 16th",
            discovered().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of October 23rd",
            discovered().security(2, 1, 0, 0).license(0, 0, 0, 1).quality(0, 2, 0, 0).other(0, 0, 0, 3).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(1, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of October 30th",
            discovered().security(0, 0, 0, 0).license(0, 0, 0, 2).quality(1, 2, 3, 0).other(0, 0, 0, 7).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of November 6th",
            discovered().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 00).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of November 13th",
            discovered().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 1, 0, 1).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of November 20th",
            discovered().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of November 27th",
            discovered().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountsDTO("Week of December 4th",
            discovered().security(0, 0, 2, 0).license(0, 0, 0, 3).quality(0, 0, 1, 0).other(0, 0, 0, 5).asMap(),
            fixed().security(1, 1, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 1, 1).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()));
    assertThat(actualDTOs).hasSameSizeAs(expectedDTOs);
    for (int i = 0; i < expectedDTOs.size(); i++) {
      assertViolationCountsDTO(expectedDTOs.get(i), actualDTOs.get(i));
    }
  }

  private void assertViolationCountsDTO(ViolationCountsDTO expectedDTO, ViolationCountsDTO actualDTO) {
    assertThat(actualDTO.timePeriodName).isEqualTo(expectedDTO.timePeriodName);
    assertThat(actualDTO.discoveredCounts).as(actualDTO.timePeriodName + " discovered")
        .isEqualTo(expectedDTO.discoveredCounts);
    assertThat(actualDTO.fixedCounts).as(actualDTO.timePeriodName + " fixed").isEqualTo(expectedDTO.fixedCounts);
    assertThat(actualDTO.waivedCounts).as(actualDTO.timePeriodName + " waived").isEqualTo(expectedDTO.waivedCounts);
  }

  private void assertAggregationViolationTotalsByCategoryHistory(List<ViolationsByCategoryDTO> actualDTOs) {
    List<ViolationsByCategoryDTO> expectedDTOs = Arrays.asList(
        new ViolationsByCategoryDTO("25 Sep", 6, 12, 10, 10),
        new ViolationsByCategoryDTO("02 Oct", 6, 12, 10, 10),
        new ViolationsByCategoryDTO("09 Oct", 6, 12, 10, 10),
        new ViolationsByCategoryDTO("16 Oct", 9, 18, 15, 15),
        new ViolationsByCategoryDTO("23 Oct", 9, 18, 15, 15),
        new ViolationsByCategoryDTO("30 Oct", 9, 18, 15, 15),
        new ViolationsByCategoryDTO("06 Nov", 9, 18, 15, 15),
        new ViolationsByCategoryDTO("13 Nov", 9, 18, 15, 15),
        new ViolationsByCategoryDTO("20 Nov", 12, 24, 20, 20),
        new ViolationsByCategoryDTO("27 Nov", 12, 24, 20, 20),
        new ViolationsByCategoryDTO("04 Dec", 12, 24, 20, 20),
        new ViolationsByCategoryDTO("11 Dec", 12, 24, 20, 20));
    assertAggregationViolationTotalsByCategoryHistory(actualDTOs, expectedDTOs);
  }

  private void assertAggregationViolationTotalsByCategoryHistoryIncludeLatestData(
      List<ViolationsByCategoryDTO> actualDTOs)
  {
    List<ViolationsByCategoryDTO> expectedDTOs = Arrays.asList(
        new ViolationsByCategoryDTO("02 Oct", 6, 12, 10, 10),
        new ViolationsByCategoryDTO("09 Oct", 6, 12, 10, 10),
        new ViolationsByCategoryDTO("16 Oct", 9, 18, 15, 15),
        new ViolationsByCategoryDTO("23 Oct", 9, 18, 15, 15),
        new ViolationsByCategoryDTO("30 Oct", 9, 18, 15, 15),
        new ViolationsByCategoryDTO("06 Nov", 9, 18, 15, 15),
        new ViolationsByCategoryDTO("13 Nov", 9, 18, 15, 15),
        new ViolationsByCategoryDTO("20 Nov", 12, 24, 20, 20),
        new ViolationsByCategoryDTO("27 Nov", 12, 24, 20, 20),
        new ViolationsByCategoryDTO("04 Dec", 12, 24, 20, 20),
        new ViolationsByCategoryDTO("11 Dec", 12, 24, 20, 20),
        new ViolationsByCategoryDTO("now", 12, 24, 20, 20));
    assertAggregationViolationTotalsByCategoryHistory(actualDTOs, expectedDTOs);
  }

  private void assertAggregationViolationTotalsByCategoryHistoryForThirdApp(List<ViolationsByCategoryDTO> actualDTOs) {
    List<ViolationsByCategoryDTO> expectedDTOs = Arrays.asList(
        createEmptyViolationsByCategoryDTO("25 Sep"),
        createEmptyViolationsByCategoryDTO("02 Oct"),
        createEmptyViolationsByCategoryDTO("09 Oct"),
        new ViolationsByCategoryDTO("16 Oct", 3, 6, 5, 5),
        new ViolationsByCategoryDTO("23 Oct", 3, 6, 5, 5),
        new ViolationsByCategoryDTO("30 Oct", 3, 6, 5, 5),
        new ViolationsByCategoryDTO("06 Nov", 3, 6, 5, 5),
        new ViolationsByCategoryDTO("13 Nov", 3, 6, 5, 5),
        new ViolationsByCategoryDTO("20 Nov", 3, 6, 5, 5),
        new ViolationsByCategoryDTO("27 Nov", 3, 6, 5, 5),
        new ViolationsByCategoryDTO("04 Dec", 3, 6, 5, 5),
        new ViolationsByCategoryDTO("11 Dec", 3, 6, 5, 5));
    assertAggregationViolationTotalsByCategoryHistory(actualDTOs, expectedDTOs);
  }

  private void assertAggregationViolationTotalsByCategoryHistory(
      List<ViolationsByCategoryDTO> actualDTOs,
      List<ViolationsByCategoryDTO> expectedDTOs)
  {
    assertThat(actualDTOs).hasSameSizeAs(expectedDTOs);
    for (int i = 0; i < expectedDTOs.size(); i++) {
      assertViolationTotalsByCategoryDTO(expectedDTOs.get(i), actualDTOs.get(i));
    }
  }

  private void assertViolationTotalsByCategoryDTO(
      ViolationsByCategoryDTO expectedDTO,
      ViolationsByCategoryDTO actualDTO)
  {
    assertThat(actualDTO.timePeriodName).as("Time Period Name").isEqualTo(expectedDTO.timePeriodName);
    assertThat(actualDTO.security).as("Security").isEqualTo(expectedDTO.security);
    assertThat(actualDTO.license).as("License").isEqualTo(expectedDTO.license);
    assertThat(actualDTO.quality).as("Quality").isEqualTo(expectedDTO.quality);
    assertThat(actualDTO.other).as("Other").isEqualTo(expectedDTO.other);
  }

  private void assertApplicationCountsDTO(ApplicationCountsDTO actual, ApplicationCountsDTO expected) {
    assertThat(actual.totalApplications).isEqualTo(expected.totalApplications);
    assertThat(actual.activeApplications).isEqualTo(expected.activeApplications);
    assertThat(actual.total.applicationsWithViolations).isEqualTo(expected.total.applicationsWithViolations);
    assertThat(actual.total.applicationsWithCriticalViolations)
        .isEqualTo(expected.total.applicationsWithCriticalViolations);
    assertThat(actual.security.applicationsWithViolations).isEqualTo(expected.security.applicationsWithViolations);
    assertThat(actual.security.applicationsWithCriticalViolations)
        .isEqualTo(expected.security.applicationsWithCriticalViolations);
    assertThat(actual.license.applicationsWithViolations).isEqualTo(expected.license.applicationsWithViolations);
    assertThat(actual.license.applicationsWithCriticalViolations)
        .isEqualTo(expected.license.applicationsWithCriticalViolations);
    assertThat(actual.quality.applicationsWithViolations).isEqualTo(expected.quality.applicationsWithViolations);
    assertThat(actual.quality.applicationsWithCriticalViolations)
        .isEqualTo(expected.quality.applicationsWithCriticalViolations);
    assertThat(actual.other.applicationsWithViolations).isEqualTo(expected.other.applicationsWithViolations);
    assertThat(actual.other.applicationsWithCriticalViolations)
        .isEqualTo(expected.other.applicationsWithCriticalViolations);
  }

  private void assertMttrDTOs(List<MttrDTO> actual, List<MttrDTO> expected) {
    Comparator<MttrDTO> mttrDTOComparator = Comparator
        .<MttrDTO, String>comparing(dto -> dto.timePeriodName, Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.mttrInSeconds, Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.criticalMttrInSeconds, Comparator.nullsFirst(Comparator.naturalOrder()));
    assertThat(actual).usingElementComparator(mttrDTOComparator).isEqualTo(expected);
  }

  private void assertAverageDTO(
      AverageDiscoveredPolicyViolationsDTO actualDTO,
      AverageDiscoveredPolicyViolationsDTO expectedDTO)
  {
    assertThat(actualDTO.evaluationCount).isCloseTo(expectedDTO.evaluationCount, offset(TOLERANCE));

    assertThat(actualDTO.securityViolations.averageDiscoveredCritical)
        .isCloseTo(expectedDTO.securityViolations.averageDiscoveredCritical, offset(TOLERANCE));
    assertThat(actualDTO.securityViolations.averageDiscovered)
        .isCloseTo(expectedDTO.securityViolations.averageDiscovered, offset(TOLERANCE));

    assertThat(actualDTO.licenseViolations.averageDiscoveredCritical)
        .isCloseTo(expectedDTO.licenseViolations.averageDiscoveredCritical, offset(TOLERANCE));
    assertThat(actualDTO.licenseViolations.averageDiscovered).isCloseTo(expectedDTO.licenseViolations.averageDiscovered,
        offset(TOLERANCE));

    assertThat(actualDTO.qualityViolations.averageDiscoveredCritical)
        .isCloseTo(expectedDTO.qualityViolations.averageDiscoveredCritical, offset(TOLERANCE));
    assertThat(actualDTO.qualityViolations.averageDiscovered).isCloseTo(expectedDTO.qualityViolations.averageDiscovered,
        offset(TOLERANCE));

    assertThat(actualDTO.otherViolations.averageDiscoveredCritical)
        .isCloseTo(expectedDTO.otherViolations.averageDiscoveredCritical, offset(TOLERANCE));
    assertThat(actualDTO.otherViolations.averageDiscovered).isCloseTo(expectedDTO.otherViolations.averageDiscovered,
        offset(TOLERANCE));

    assertThat(actualDTO.totalViolations.averageDiscoveredCritical)
        .isCloseTo(expectedDTO.totalViolations.averageDiscoveredCritical, offset(TOLERANCE));
    assertThat(actualDTO.totalViolations.averageDiscovered).isCloseTo(expectedDTO.totalViolations.averageDiscovered,
        offset(TOLERANCE));
  }

  private Date toDate(LocalDate localDate) {
    return localDate.toDateTimeAtStartOfDay().toDate();
  }

  private DateTime setTimeTo(DateTime fakeNow) {
    DateTimeUtils.setCurrentMillisFixed(fakeNow.getMillis());
    return fakeNow;
  }

  @Test
  public void testGetComponentCounts() {
    String hash = "ababababab";
    String hash2 = "acacacacac";

    Date date = new Date();

    // app1 has the component without any policy violations
    Application app1 = tempEntity.newApplicationWithParent("app1");
    OwnerComponent component1 = tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"), null, MatchState.EXACT, false,
        date);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash2, null, null, MatchState.EXACT, false,
        date);

    // app2 has the component with policy violations
    Application app2 = tempEntity.newApplicationWithParent("app2");
    tempEntity.newApplicationComponent(app2.getId(), ReleaseStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));
    // add two policy violations for a stage
    Policy policy1 = tempEntity.newPolicy(app2);
    Policy policy2 = tempEntity.newPolicy(app2);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, "groupId", "artifactId", "version", hash, "reason1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy2, "groupId", "artifactId", "version", hash, "reason2");

    // app3 does not have the component
    tempEntity.newApplicationWithParent("app3");

    ComponentCountsDTO componentDetailsDTO = service.getComponentCounts(createSuccessMetricsReport(null, null).getId());
    assertThat(componentDetailsDTO).isNotNull();

    // app1=2 components, app2=1 component, app3=0 components
    assertThat(componentDetailsDTO.componentsPerApplication).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications).hasSize(2);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(0).count).isEqualTo(2);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(0).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component1.getComponentIdentifier()).toString());
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(1).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(1).componentDisplayName).isEqualTo("Unknown");

    assertThat(componentDetailsDTO.componentsWithTheMostViolations).hasSize(1);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).count).isEqualTo(2);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component1.getComponentIdentifier()).toString());
  }

  @Test
  public void testGetComponentCounts_NoComponents() {
    ComponentCountsDTO componentDetailsDTO = service.getComponentCounts(createSuccessMetricsReport(null, null).getId());
    assertThat(componentDetailsDTO).isNotNull();

    assertThat(componentDetailsDTO.componentsPerApplication).isEqualTo(0);
    assertThat(componentDetailsDTO.componentsInTheMostApplications).isEmpty();
    assertThat(componentDetailsDTO.componentsWithTheMostViolations).isEmpty();
  }

  @Test
  public void testGetComponentCounts_ExcludesDevelopStage() {
    String hash = "ababababab";
    String hash2 = "acacacacac";

    Application app1 = tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationComponent(app1.getId(), DevelopStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"));

    OwnerComponent buildComponent = tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash2,
        ComponentIdentifier.createMavenCoordinates("groupId1", "artifactId1", "version1"));

    Policy policy1 = tempEntity.newPolicy(app1);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "scanId1");
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scanId2");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, "groupId", "artifactId", "version", hash, "reason1");
    tempEntity.newPolicyViolation(policyEvaluation2, policy1, "groupId1", "artifactId1", "version1", hash2, "reason2");

    ComponentCountsDTO componentDetailsDTO = service.getComponentCounts(createSuccessMetricsReport(null, null).getId());
    assertThat(componentDetailsDTO).isNotNull();

    assertThat(componentDetailsDTO.componentsPerApplication).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications).hasSize(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(0).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(0).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(buildComponent.getComponentIdentifier()).toString());

    assertThat(componentDetailsDTO.componentsWithTheMostViolations).hasSize(1);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(buildComponent.getComponentIdentifier()).toString());
  }

  @Test
  public void testGetComponentCounts_ExcludesWaivedViolations() {
    Date now = new Date();
    Date before = new Date(now.getTime() - 1000);
    String hash1 = "ababababab";
    String hash2 = "acacacacac";

    ComponentIdentifier waivedComponentId = ComponentIdentifier.createMavenCoordinates("groupId", "artifactId",
        "version");
    ComponentIdentifier unwaivedComponentId = ComponentIdentifier.createMavenCoordinates("groupId1", "artifactId1",
        "version1");

    Application app1 = tempEntity.newApplicationWithParent("app1");
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, hash1, waivedComponentId);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash2, unwaivedComponentId);

    Policy policy1 = tempEntity.newPolicy(app1);

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scanId1",
        before);
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scanId2",
        now);

    PolicyWaiver waiver = tempEntity.newWaiver(hash1, policy1.getId(), app1.getId());
    tempEntity.newWaivedPolicyViolation(policyEvaluation1, policy1, waivedComponentId, hash1, waiver);
    tempEntity.newPolicyViolation(policyEvaluation2, policy1, "groupId1", "artifactId1", "version1", hash2, "reason2");

    ComponentCountsDTO componentDetailsDTO = service.getComponentCounts(createSuccessMetricsReport(null, null).getId());
    assertThat(componentDetailsDTO).isNotNull();

    assertThat(componentDetailsDTO.componentsPerApplication).isEqualTo(2);
    assertThat(componentDetailsDTO.componentsInTheMostApplications).hasSize(2);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(0).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(0).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(waivedComponentId).toString());
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(1).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(1).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(unwaivedComponentId).toString());

    assertThat(componentDetailsDTO.componentsWithTheMostViolations).hasSize(1);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(unwaivedComponentId).toString());
  }

  @Test
  public void testGetComponentCounts_AlphaSorting() {
    String hash = "hash1";
    String hash2 = "hash2";
    String hash3 = "hash3";

    Date date = new Date();

    Application app1 = tempEntity.newApplicationWithParent("app1");
    OwnerComponent component1 = tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"), null, MatchState.EXACT, false,
        date);
    OwnerComponent component2 = tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash2,
        ComponentIdentifier.createMavenCoordinates("Z2", "artifactId2", "version2"), null, MatchState.EXACT, false,
        date);
    OwnerComponent component3 = tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash3,
        ComponentIdentifier.createMavenCoordinates("groupId3", "artifactId3", "version3"), null, MatchState.EXACT,
        false, date);

    // add three policy violations for a stage
    Policy policy1 = tempEntity.newPolicy(app1);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, "groupId", "artifactId", "version", hash, "reason1");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, "Z2", "artifactId2", "version2", hash2, "reason2");
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, "groupId3", "artifactId3", "version3", hash3, "reason3");

    ComponentCountsDTO componentDetailsDTO = service.getComponentCounts(createSuccessMetricsReport(null, null).getId());
    assertThat(componentDetailsDTO).isNotNull();

    assertThat(componentDetailsDTO.componentsPerApplication).isEqualTo(3);
    assertThat(componentDetailsDTO.componentsInTheMostApplications).hasSize(3);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(0).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(0).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component1.getComponentIdentifier()).toString());
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(1).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(1).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component3.getComponentIdentifier()).toString());
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(2).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(2).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component2.getComponentIdentifier()).toString());

    assertThat(componentDetailsDTO.componentsWithTheMostViolations).hasSize(3);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component1.getComponentIdentifier()).toString());
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(1).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(1).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component3.getComponentIdentifier()).toString());
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(2).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(2).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component2.getComponentIdentifier()).toString());
  }

  @Test
  public void testGetComponentCounts_LimitedTo5() {
    Date date = new Date();

    Application app1 = tempEntity.newApplicationWithParent("app1");
    Policy policy1 = tempEntity.newPolicy(app1);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1");

    List<OwnerComponent> components = new ArrayList<>();

    // Create 6 components and 6 violations
    for (int i = 1; i < 7; i++) {
      components.add(tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash" + i,
          ComponentIdentifier.createMavenCoordinates("groupId" + i, "artifactId" + i, "version" + i), null,
          MatchState.EXACT, false, date));
      tempEntity
          .newPolicyViolation(policyEvaluation1, policy1, "groupId" + i, "artifactId" + i, "version" + i, "hash" + i,
              "reason1" + i);
    }

    ComponentCountsDTO componentDetailsDTO = service.getComponentCounts(createSuccessMetricsReport(null, null).getId());
    assertThat(componentDetailsDTO).isNotNull();

    assertThat(componentDetailsDTO.componentsPerApplication).isEqualTo(6);
    assertThat(componentDetailsDTO.componentsInTheMostApplications).hasSize(5);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations).hasSize(5);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).count).isEqualTo(1);

    for (int i = 0; i < 5; i++) {
      assertThat(componentDetailsDTO.componentsInTheMostApplications.get(i).count).isEqualTo(1);
      assertThat(componentDetailsDTO.componentsInTheMostApplications.get(i).componentDisplayName)
          .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(components.get(i).getComponentIdentifier()).toString());
      assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(i).count).isEqualTo(1);
      assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(i).componentDisplayName)
          .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(components.get(i).getComponentIdentifier()).toString());
    }
  }

  @Test
  public void testGetComponentCounts_MultipleStages() {
    String hash = "hash1";
    String hash2 = "hash2";
    String hash3 = "hash3";
    Date newerDate = new Date();
    Date olderDate = new Date(newerDate.getTime() - 2000);

    Application app1 = tempEntity.newApplicationWithParent("app1");

    // Create some application components
    OwnerComponent component1 = tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"), null, MatchState.EXACT, false,
        newerDate);
    // Same component, different stage. Components in applications count will not count this twice. However, policy
    // violation counts are aggregate and will be reflected accordingly.
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, hash,
        ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version"), null, MatchState.EXACT, false,
        newerDate);
    OwnerComponent component2 = tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, hash2,
        ComponentIdentifier.createMavenCoordinates("groupId2", "artifactId2", "version2"), null, MatchState.EXACT,
        false, olderDate);

    Policy policy1 = tempEntity.newPolicy(app1);
    PolicyEvaluation policyEvaluation1 = tempEntity
        .newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1", newerDate);
    tempEntity.newPolicyViolation(policyEvaluation1, policy1, "groupId", "artifactId", "version", hash, "reason1");

    // Create 2 evals for the release stage, policy violation results should consider the later/newer one.
    PolicyEvaluation policyEvaluation3 = tempEntity
        .newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scanId2", olderDate);
    tempEntity.newPolicyViolation(policyEvaluation3, policy1, "groupId2", "artifactId2", "version2", hash2, "reason2");
    PolicyEvaluation policyEvaluation2 = tempEntity
        .newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scanId1", newerDate);
    fixViolations(policyEvaluation2, null);
    tempEntity.newPolicyViolation(policyEvaluation2, policy1, "groupId", "artifactId", "version", hash, "reason1");

    Application app2 = tempEntity.newApplicationWithParent("app2");
    OwnerComponent component3 = tempEntity.newApplicationComponent(app2.getId(), OperateStageType.ID, hash3,
        ComponentIdentifier.createMavenCoordinates("groupId3", "artifactId3", "version3"), null, MatchState.EXACT,
        false, olderDate);

    Policy policy2 = tempEntity.newPolicy(app2);
    PolicyEvaluation policyEvaluation4 = tempEntity
        .newPolicyEvaluation(app2.getId(), OperateStageType.ID, "scanId3", olderDate);

    tempEntity.newPolicyViolation(policyEvaluation4, policy2, "groupId3", "artifactId3", "version3", hash3, "reason3");

    ComponentCountsDTO componentDetailsDTO = service.getComponentCounts(createSuccessMetricsReport(null, null).getId());
    assertThat(componentDetailsDTO).isNotNull();

    assertThat(componentDetailsDTO.componentsPerApplication).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications).hasSize(3);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(0).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(0).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component1.getComponentIdentifier()).toString());
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(1).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(1).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component2.getComponentIdentifier()).toString());
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(2).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(2).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component3.getComponentIdentifier()).toString());

    assertThat(componentDetailsDTO.componentsWithTheMostViolations).hasSize(2);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).count).isEqualTo(2);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component1.getComponentIdentifier()).toString());
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(1).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(1).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(component3.getComponentIdentifier()).toString());
  }

  @Test
  public void testGetComponentCounts_DisplayName() {
    String hash1 = "ababababab";
    String hash2 = "acacacacac";
    String hash3 = "adadadadad";
    String hash4 = "aeaeaeaeae";

    Date date = new Date();

    ComponentIdentifier componentId1 = ComponentIdentifier.createMavenCoordinates("groupId", "artifactId", "version");

    /*
     * This test scenario has four components, each of which is present and containing a violation.
     *
     * The first component has a component identifier and the display name should come from that. Present in one
     * app with one violation
     *
     * The second component has no component identifier but has a variety of pathnames. Present in three apps with
     * three total violations
     *
     * The third component has no component identifier but has a variety of pathnames which test the tie-breaking
     * logic for pathname selection. Present in two apps with two total violations
     *
     * The fourth component has no component id nor pathnames so it should show up as "Unknown". Present in one
     * app with one total violation
     */
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplication(org.getId());
    Application app2 = tempEntity.newApplication(org.getId());
    Application app3 = tempEntity.newApplication(org.getId());

    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash1, componentId1, "a.zip/b.jar",
        MatchState.EXACT, false, date);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash2, null, "a.zip/foo\nb.jar/",
        MatchState.EXACT, false, date);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, hash3, null, "a.zip/foo\na.zip/b.jar/\nb.jar",
        MatchState.EXACT, false, date);

    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, hash2, null, "foo", MatchState.EXACT, false,
        date);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, hash3, null, "foo", MatchState.EXACT, false,
        date);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, hash4, null, null, MatchState.EXACT, false,
        date);

    tempEntity.newApplicationComponent(app3.getId(), BuildStageType.ID, hash3, null, null, MatchState.EXACT, false,
        date);

    Policy policy = tempEntity.newPolicy(org);
    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1");
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId2");
    PolicyEvaluation policyEvaluation3 = tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, "scanId3");

    tempEntity.newPolicyViolation(policyEvaluation1, policy, componentId1, hash1, "reason1", "b.jar");
    ComponentIdentifier nullComponentIdentifier = null;
    tempEntity.newPolicyViolation(policyEvaluation1, policy, nullComponentIdentifier, hash2, "reason2", "foo");
    tempEntity.newPolicyViolation(policyEvaluation1, policy, nullComponentIdentifier, hash3, "reason3", "foo");

    tempEntity.newPolicyViolation(policyEvaluation2, policy, nullComponentIdentifier, hash2, "reason2", "foo");
    tempEntity.newPolicyViolation(policyEvaluation2, policy, nullComponentIdentifier, hash3, "reason3", "foo");
    tempEntity.newPolicyViolation(policyEvaluation2, policy, nullComponentIdentifier, hash4, "reason4");

    tempEntity.newPolicyViolation(policyEvaluation3, policy, nullComponentIdentifier, hash3, "reason3");

    ComponentCountsDTO componentDetailsDTO = service.getComponentCounts(createSuccessMetricsReport(null, null).getId());
    assertThat(componentDetailsDTO).isNotNull();

    assertThat(componentDetailsDTO.componentsInTheMostApplications).hasSize(4);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(0).componentDisplayName).isEqualTo("b.jar");
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(0).count).isEqualTo(3);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(1).componentDisplayName).isEqualTo("foo");
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(1).count).isEqualTo(2);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(2).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentId1).toString());
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(2).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(3).componentDisplayName).isEqualTo("Unknown");
    assertThat(componentDetailsDTO.componentsInTheMostApplications.get(3).count).isEqualTo(1);

    assertThat(componentDetailsDTO.componentsWithTheMostViolations).hasSize(4);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).componentDisplayName).isEqualTo("foo");
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(0).count).isEqualTo(3);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(1).componentDisplayName).isEqualTo("foo");
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(1).count).isEqualTo(2);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(2).componentDisplayName)
        .isEqualTo(ComponentDisplayNameUtil.fromIdentifier(componentId1).toString());
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(2).count).isEqualTo(1);
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(3).componentDisplayName).isEqualTo("Unknown");
    assertThat(componentDetailsDTO.componentsWithTheMostViolations.get(3).count).isEqualTo(1);
  }

  @Test
  public void testGetChartData_InsertRaceCondition() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    SuccessMetricsReport successMetricsReport = createSuccessMetricsReport(new HashSet<>(), applicationIds);
    String reportId = successMetricsReport.getId();

    SuccessMetricsReportData data = new SuccessMetricsReportData();
    data.setChartDataJson("{\"monthCount\": 82}");

    SuccessMetricsReportDataDAO mockDAO = mock(SuccessMetricsReportDataDAO.class);

    // on first getById call, data isn't saved yet. Then an insert will be attempted but will, as if another thread
    // inserted first, fail. Then another getById call should be made which should return the mocked data object
    // (ie the object created supposedly created by the other thread)
    when(mockDAO.getById(reportId)).thenAnswer(new Answer<SuccessMetricsReportData>()
    {
      private int callCount = 0;

      @Override
      public SuccessMetricsReportData answer(InvocationOnMock invocation) {
        if (callCount == 0) {
          callCount++;
          return null;
        }
        else {
          return data;
        }
      }
    });

    // Simulate a unique constraint violation (SQL state 23505 = unique_violation)
    PSQLException psqlEx = new PSQLException(
        "duplicate key value violates unique constraint", PSQLState.UNIQUE_VIOLATION);
    doThrow(new DataAccessException("insert failed", psqlEx)).when(mockDAO).insert(any());

    SuccessMetricsReportDataService service = new SuccessMetricsReportDataService(lookup(ApplicationService.class),
        lookup(OwnerComponentDAO.class), lookup(StageTypeService.class),
        lookup(PolicyViolationAggregationService.class), lookup(SuccessMetricsReportService.class), mockDAO,
        lookup(PolicyViolationAggregationDAO.class), policyViolationDAO, policyEvaluationDAO);

    assertThat(service.getChartData(reportId).monthCount).isEqualTo(82);
  }
}
