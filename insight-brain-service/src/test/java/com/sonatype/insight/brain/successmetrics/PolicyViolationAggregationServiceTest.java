/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.SourceStageType;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.successmetrics.TimePeriod.MONTH;
import static com.sonatype.insight.brain.model.successmetrics.TimePeriod.WEEK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class PolicyViolationAggregationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyViolationAggregationService service;

  @Inject
  private PolicyViolationDAO violationDAO;

  @Inject
  private PolicyViolationAggregationDAO aggregationDAO;

  @Inject
  private Configuration configuration;

  @Before
  public void resetSuccessMetricsStageConfiguration() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID, null);
    configuration.configurationChanged(Sets.newHashSet(SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID));
  }

  @Test
  public void testGeneratePolicyViolationAggregations_ViolationsWithoutHash_AllResolved() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    // for weekly aggregations - make sure we have enough days in the week to work with
    DateTime now = new DateTime().withDayOfMonth(1).plusWeeks(2).withDayOfWeek(4);

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
    violation3.setWaiveTime(eval3.getTime());
    violationDAO.update(violation3);

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    PolicyViolationAggregation aggregation = aggregationDAO
        .getMostRecentByApplicationIdAndTimePeriod(app.getId(), MONTH);

    assertAllResoloved(now, aggregation);

    aggregation = aggregationDAO.getMostRecentByApplicationIdAndTimePeriod(app.getId(), WEEK);

    assertAllResoloved(now, aggregation);
  }

  @Test
  public void testGeneratePolicyViolationAggregations_MultipleStages_AllResolved() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    // for weekly aggregations - make sure we have enough days in the week to work with
    DateTime now = new DateTime().withDayOfMonth(1).plusWeeks(2).withDayOfWeek(4);

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusHours(72).toDate());
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, "scan2",
        now.minusHours(72).toDate());
    // two distinct violation entities which are "equal" per PolicyViolationComparator
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, null, null, "unknown component");
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval2, policy, null, null, "unknown component");

    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan3",
        now.minusHours(24).toDate());
    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, "scan4",
        now.minusHours(24).toDate());
    // both violations resolved after 48 hours
    violation1.setFixTime(eval3.getTime());
    violationDAO.update(violation1);
    violation2.setFixTime(eval4.getTime());
    violationDAO.update(violation2);

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    PolicyViolationAggregation aggregation = aggregationDAO
        .getMostRecentByApplicationIdAndTimePeriod(app.getId(), MONTH);

    assertAllResoloved(now, aggregation);

    aggregation = aggregationDAO.getMostRecentByApplicationIdAndTimePeriod(app.getId(), WEEK);

    assertAllResoloved(now, aggregation);
  }

  @Test
  public void testGeneratePolicyViolationAggregations_SingleStage_SpecifiedBySystemConfiguration() {
    // === Given ===
    final var now = new DateTime()
        .withDayOfMonth(1)
        .plusWeeks(2)
        .withDayOfWeek(4);

    final var app = tempEntity.newApplicationWithParent();
    final var policy = tempEntity.newPolicy(app.getId(), "test policy", 10);

    final var evalSource1 = tempEntity.newPolicyEvaluation(app.getId(), SourceStageType.ID, "scan1",
        now.minusHours(72).toDate());
    final var evalSource2 = tempEntity.newPolicyEvaluation(app.getId(), SourceStageType.ID, "scan3",
        now.minusHours(48).toDate());

    final var evalRelease1 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, "scan2",
        now.minusHours(72).toDate());
    final var evalRelease2 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, "scan4",
        now.minusHours(24).toDate());

    PolicyViolation violationStaging = tempEntity
        .newPolicyViolation(evalSource1, policy, null, null, "unknown component");
    PolicyViolation violationRelease = tempEntity
        .newPolicyViolation(evalRelease1, policy, null, null, "unknown component");

    // source violations resolved on second scan (24 hours)
    violationStaging.setFixTime(evalSource2.getTime());
    violationDAO.update(violationStaging);

    // release violation resolved on secod scan (48 hours)
    violationRelease.setFixTime(evalRelease2.getTime());
    violationDAO.update(violationRelease);

    // === When Source Stage ===
    setSuccessMetricsStage(SourceStageType.ID);
    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    // === Then ===
    PolicyViolationAggregation aggregation = aggregationDAO
        .getMostRecentByApplicationIdAndTimePeriod(app.getId(), MONTH);

    // 86400000 = 24 hours day
    assertThat(aggregation.getMttrCriticalThreat()).isEqualTo(86400000L);
    assertThat(aggregation.getResolvedCountCriticalThreat()).isEqualTo(1);

    // === When Release Stage ===
    tempEntity.deleteAllPolicyViolationAggregations();
    setSuccessMetricsStage(ReleaseStageType.ID);
    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    // === Then ===
    aggregation = aggregationDAO
        .getMostRecentByApplicationIdAndTimePeriod(app.getId(), MONTH);

    // 86400000 = 48 hours day
    assertThat(aggregation.getMttrCriticalThreat()).isEqualTo(172800000L);
    assertThat(aggregation.getResolvedCountCriticalThreat()).isEqualTo(1);
  }

  @Test
  public void testGeneratePolicyViolationAggregations_metricsDoNotIncludeProxy() {
    // === Given ===
    final var now = new DateTime()
        .withDayOfMonth(1)
        .plusWeeks(2)
        .withDayOfWeek(4);

    final var app = tempEntity.newApplicationWithParent();
    final var policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    final var proxyEvalWithOpenViolation = tempEntity.newPolicyEvaluation(app.getId(), ProxyStageType.ID, "scan1",
        now.minusHours(72).toDate());
    tempEntity
        .newPolicyViolation(proxyEvalWithOpenViolation, policy, null, null, "unknown component");

    // === When Source Stage ===
    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    // === Then ===
    final var aggregation = aggregationDAO
        .getMostRecentByApplicationIdAndTimePeriod(app.getId(), MONTH);

    assertThat(aggregation.getOpenCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL)).isEqualTo(0);
  }

  @Test
  public void testGeneratePolicyViolationAggregations_throwsExcepctionIfConfiguredStageIsNotLicensed() {
    final var app = tempEntity.newApplicationWithParent();
    setSuccessMetricsStage(ProxyStageType.ID);

    final var thrown = assertThrows(BadRequestException.class, () -> service.generatePolicyViolationAggregations(
        Collections.singleton(app.getId()), DateTime.now(), true));

    assertThat(thrown.getMessage()).isEqualTo(
        "Invalid value 'proxy' provided for successMetricsStageId. Allowed values are: " +
            "'[operate, build, release, source, stage-release]'");
  }

  private void assertAllResoloved(DateTime now, PolicyViolationAggregation aggregation) {
    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL, aggregation.getDiscoveredAsTable());
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL)).isEqualTo(1);

    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL, aggregation.getFixedAsTable());
    assertThat(aggregation.getFixedCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL)).isEqualTo(1);

    assertAllCountsZero(aggregation.getWaivedAsTable());

    assertAllCountsZero(aggregation.getOpenAsTable());

    assertThat(aggregation.getResolvedCountCriticalThreat()).isEqualTo(1);
    assertThat(aggregation.getResolvedCountSevereThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountModerateThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountLowThreat()).isEqualTo(0);

    assertThat(aggregation.getMttrCriticalThreat()).isEqualTo(TimeUnit.HOURS.toMillis(48));
    assertThat(aggregation.getMttrSevereThreat()).isNull();
    assertThat(aggregation.getMttrModerateThreat()).isNull();
    assertThat(aggregation.getMttrLowThreat()).isNull();

    LocalDate expectedTimePeriodStart =
        aggregation.getTimePeriod() == MONTH
            ? new LocalDate(now.withDayOfMonth(1))
            : new LocalDate(
                now.withDayOfWeek(1));
    assertThat(aggregation.getTimePeriodStart()).isEqualTo(expectedTimePeriodStart.toDate());
    assertThat(aggregation.getTimePeriodEnd()).isEqualTo(now.toDate());
  }

  @Test
  public void testGeneratePolicyViolationAggregations_ViolationsWithoutHash_SomeUnresolved() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    // for weekly aggregations - make sure we have enough days in the week to work with
    DateTime now = new DateTime().withDayOfMonth(1).plusWeeks(2).withDayOfWeek(4);

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

    PolicyViolationAggregation aggregation = aggregationDAO
        .getMostRecentByApplicationIdAndTimePeriod(app.getId(), MONTH);

    assertSomeResolved(now, aggregation);

    aggregation = aggregationDAO.getMostRecentByApplicationIdAndTimePeriod(app.getId(), WEEK);

    assertSomeResolved(now, aggregation);
  }

  @Test
  public void testGeneratePolicyViolationAggregations_MultipleStages_SomeUnresolved() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    // for weekly aggregations - make sure we have enough days in the week to work with
    DateTime now = new DateTime().withDayOfMonth(1).plusWeeks(2).withDayOfWeek(4);

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusHours(72).toDate());
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, "scan2",
        now.minusHours(72).toDate());
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), OperateStageType.ID, "scan3",
        now.minusHours(72).toDate());
    // three distinct violation entities which are "equal" per PolicyViolationComparator
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, null, null, "unknown component");
    tempEntity.newPolicyViolation(eval2, policy, null, null, "unknown component");
    tempEntity.newPolicyViolation(eval3, policy, null, null, "unknown component");

    PolicyEvaluation eval4 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan4",
        now.minusHours(48).toDate());
    // first violation resolved after 24 hours, second violation remains unresolved
    violation1.setFixTime(eval4.getTime());
    violationDAO.update(violation1);

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    PolicyViolationAggregation aggregation = aggregationDAO
        .getMostRecentByApplicationIdAndTimePeriod(app.getId(), MONTH);

    assertSomeResolved(now, aggregation);

    aggregation = aggregationDAO.getMostRecentByApplicationIdAndTimePeriod(app.getId(), WEEK);

    assertSomeResolved(now, aggregation);
  }

  private void assertSomeResolved(DateTime now, PolicyViolationAggregation aggregation) {
    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL, aggregation.getDiscoveredAsTable());
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL)).isEqualTo(1);

    assertAllCountsZero(aggregation.getFixedAsTable());
    assertAllCountsZero(aggregation.getWaivedAsTable());

    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL, aggregation.getOpenAsTable());
    assertThat(aggregation.getOpenAsTable().get(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL)).isEqualTo(1);

    assertThat(aggregation.getResolvedCountCriticalThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountSevereThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountModerateThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountLowThreat()).isEqualTo(0);

    assertThat(aggregation.getMttrCriticalThreat()).isNull();
    assertThat(aggregation.getMttrSevereThreat()).isNull();
    assertThat(aggregation.getMttrModerateThreat()).isNull();
    assertThat(aggregation.getMttrLowThreat()).isNull();

    LocalDate expectedTimePeriodStart =
        aggregation.getTimePeriod() == MONTH
            ? new LocalDate(now.withDayOfMonth(1))
            : new LocalDate(
                now.withDayOfWeek(1));
    assertThat(aggregation.getTimePeriodStart()).isEqualTo(expectedTimePeriodStart.toDate());
    assertThat(aggregation.getTimePeriodEnd()).isEqualTo(now.toDate());
  }

  @Test
  public void testGeneratePolicyViolationAggregations_TimePeriodRollovers() {
    Application app = tempEntity.newApplicationWithParent();
    DateTime now = new DateTime().withDayOfMonth(10);
    int daysInPast12Months = Days.daysBetween(now.minusMonths(12), now).getDays();

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusMonths(12).toDate());

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, false);

    List<PolicyViolationAggregation> weekAggregations = aggregationDAO.getByTimePeriod(WEEK);
    List<PolicyViolationAggregation> monthAggregations = aggregationDAO.getByTimePeriod(MONTH);

    // If now starts on monday we will have an extra week since we're able to get a full week's worth of data
    // for the first week (there are 52 complete weeks in a year, leaving one extra day).
    // The same holds if now starts on Tuesday during a leap year (which has 52 weeks plus 2 extra days)
    assertThat(weekAggregations).hasSize(now.getDayOfWeek() <= (daysInPast12Months % 7) ? 53 : 52);
    assertThat(monthAggregations).hasSize(12);
    DateTime aggregationStart = new LocalDate(now.minusMonths(12).withDayOfWeek(1)).toDateTimeAtStartOfDay();

    for (int i = 0; i < weekAggregations.size(); i++) {
      PolicyViolationAggregation aggregation = weekAggregations.get(i);
      assertThat(aggregation.getTimePeriod()).isEqualTo(WEEK);
      assertThat(aggregation.getTimePeriodStart()).isEqualTo(aggregationStart.plusWeeks(i).toDate());
      assertThat(aggregation.getTimePeriodEnd()).isNull();
    }

    aggregationStart = new LocalDate(now.minusMonths(12).withDayOfMonth(1)).toDateTimeAtStartOfDay();
    for (int i = 0; i < monthAggregations.size(); i++) {
      PolicyViolationAggregation aggregation = monthAggregations.get(i);
      assertThat(aggregation.getTimePeriod()).isEqualTo(MONTH);
      assertThat(aggregation.getTimePeriodStart()).isEqualTo(aggregationStart.plusMonths(i).toDate());
      assertThat(aggregation.getTimePeriodEnd()).isNull();
    }
  }

  @Test
  public void testGeneratePolicyViolationAggregations_IncludeLatestData_TimePeriodRollovers() {
    Application app = tempEntity.newApplicationWithParent();
    DateTime now = new DateTime().withDayOfMonth(10);
    int daysInPast12Months = Days.daysBetween(now.minusMonths(12), now).getDays();

    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusMonths(12).toDate());

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    List<PolicyViolationAggregation> weekAggregations = aggregationDAO.getByTimePeriod(WEEK);
    List<PolicyViolationAggregation> monthAggregations = aggregationDAO.getByTimePeriod(MONTH);

    // If now starts on monday we will have an extra week since we're able to get a full week's worth of data
    // for the first week (there are 52 complete weeks in a year, leaving one extra day).
    // The same holds if now starts on Tuesday during a leap year (which has 52 weeks plus 2 extra days)
    assertThat(weekAggregations).hasSize(now.getDayOfWeek() <= (daysInPast12Months % 7) ? 54 : 53);
    assertThat(monthAggregations).hasSize(13);

    DateTime aggregationStart = new LocalDate(now.minusMonths(12).withDayOfWeek(1)).toDateTimeAtStartOfDay();

    for (int i = 0; i < weekAggregations.size(); i++) {
      PolicyViolationAggregation aggregation = weekAggregations.get(i);
      assertThat(aggregation.getTimePeriod()).isEqualTo(WEEK);
      assertThat(aggregation.getTimePeriodStart()).isEqualTo(aggregationStart.plusWeeks(i).toDate());
      assertThat(aggregation.getTimePeriodEnd()).isEqualTo(i < weekAggregations.size() - 1 ? null : now.toDate());
    }

    aggregationStart = new LocalDate(now.minusMonths(12).withDayOfMonth(1)).toDateTimeAtStartOfDay();
    for (int i = 0; i < monthAggregations.size(); i++) {
      PolicyViolationAggregation aggregation = monthAggregations.get(i);
      assertThat(aggregation.getTimePeriod()).isEqualTo(MONTH);
      assertThat(aggregation.getTimePeriodStart()).isEqualTo(aggregationStart.plusMonths(i).toDate());
      assertThat(aggregation.getTimePeriodEnd()).isEqualTo(i < monthAggregations.size() - 1 ? null : now.toDate());
    }
  }

  @Test
  public void testGeneratePolicyViolationAggregations_ViolationsWithHash() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    // for weekly aggregations - make sure we have enough days in the week to work with
    DateTime now = new DateTime().withDayOfMonth(1).plusWeeks(2).withDayOfWeek(4);

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusHours(72).toDate());
    // two distinct violations
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, null, "hash1", "component 2");
    PolicyViolation violation2 = tempEntity.newPolicyViolation(eval1, policy, null, "hash2", "component 2");

    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan2",
        now.minusHours(48).toDate());
    violation1.setFixTime(eval2.getTime());
    violation2.setWaiveTime(eval2.getTime());
    violationDAO.update(violation1);
    violationDAO.update(violation2);

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    PolicyViolationAggregation aggregation = aggregationDAO
        .getMostRecentByApplicationIdAndTimePeriod(app.getId(), WEEK);

    assertViolationsWithHash(aggregation);

    aggregation = aggregationDAO.getMostRecentByApplicationIdAndTimePeriod(app.getId(), MONTH);

    assertViolationsWithHash(aggregation);
  }

  private void assertViolationsWithHash(PolicyViolationAggregation aggregation) {
    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL, aggregation.getDiscoveredAsTable());
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL)).isEqualTo(2);

    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL, aggregation.getFixedAsTable());
    assertThat(aggregation.getFixedCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL)).isEqualTo(1);

    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL, aggregation.getWaivedAsTable());
    assertThat(aggregation.getWaivedCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL)).isEqualTo(1);

    assertAllCountsZero(aggregation.getOpenAsTable());

    assertThat(aggregation.getResolvedCountCriticalThreat()).isEqualTo(2);
    assertThat(aggregation.getResolvedCountSevereThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountModerateThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountLowThreat()).isEqualTo(0);

    assertThat(aggregation.getMttrCriticalThreat()).isEqualTo(TimeUnit.HOURS.toMillis(24));
    assertThat(aggregation.getMttrSevereThreat()).isNull();
    assertThat(aggregation.getMttrModerateThreat()).isNull();
    assertThat(aggregation.getMttrLowThreat()).isNull();
  }

  @Test
  public void testGeneratePolicyViolationAggregations_OnlyConstraintFactsAreDifferent() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    // for weekly aggregations - make sure we have enough days in the week to work with
    DateTime now = new DateTime().withDayOfMonth(1).plusWeeks(2).withDayOfWeek(4);

    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusHours(72).toDate());
    // two distinct violations
    tempEntity.newPolicyViolation(eval, policy, null, "hash", "reason1");
    tempEntity.newPolicyViolation(eval, policy, null, "hash", "reason2");

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    PolicyViolationAggregation aggregation = aggregationDAO
        .getMostRecentByApplicationIdAndTimePeriod(app.getId(), WEEK);

    assertViolationsDifferentConstraintFacts(aggregation);

    aggregation = aggregationDAO.getMostRecentByApplicationIdAndTimePeriod(app.getId(), MONTH);

    assertViolationsDifferentConstraintFacts(aggregation);
  }

  private void assertViolationsDifferentConstraintFacts(PolicyViolationAggregation aggregation) {
    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL, aggregation.getDiscoveredAsTable());
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL)).isEqualTo(1);

    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL, aggregation.getFixedAsTable());
    assertThat(aggregation.getFixedCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL)).isEqualTo(0);

    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL, aggregation.getWaivedAsTable());
    assertThat(aggregation.getWaivedCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL)).isEqualTo(0);

    assertThat(aggregation.getResolvedCountCriticalThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountSevereThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountModerateThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountLowThreat()).isEqualTo(0);

    assertThat(aggregation.getMttrCriticalThreat()).isNull();
    assertThat(aggregation.getMttrSevereThreat()).isNull();
    assertThat(aggregation.getMttrModerateThreat()).isNull();
    assertThat(aggregation.getMttrLowThreat()).isNull();
  }

  @Test
  public void testGeneratePolicyViolationAggregations_ViolationOneWeekAgoFromMidMonth() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    DateTime now = new DateTime().withDayOfMonth(15);

    // generate a violation 1 week ago
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.minusWeeks(1).toDate());
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, null, "hash1", "component 1");

    violationDAO.update(violation1);

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    List<PolicyViolationAggregation> aggregations = aggregationDAO.getByTimePeriod(MONTH);

    assertThat(aggregations).hasSize(1);
    assertViolationOneWeekAgoFromMidMonth(aggregations.get(0), true);

    aggregations = aggregationDAO.getByTimePeriod(WEEK);

    assertThat(aggregations).hasSizeGreaterThan(1);
    for (int i = 0; i < aggregations.size(); i++) {
      // only last weeks aggregation should expect a violation
      assertViolationOneWeekAgoFromMidMonth(aggregations.get(i), i == aggregations.size() - 2);
    }
  }

  /**
   * This test is for the situation that caused CLM-15728. This happens when a previous run of the aggregation service
   * without includeLatestData results in the month aggregations being ahead of the week aggregations or vice-versa.
   * To give a specific example, the test setup here has an evaluation on March 30th 2020. This would have been
   * during the week of March 30th - April 4. When an aggregation is run during this week but after the evaluation
   * (on April 2nd), this creates an aggregation record for the month of March, but not one for the week of March 30th
   * (because the week isn't over yet). Then when another aggregation is done later on, in May, the aggregation
   * generation logic tries to generate aggregations for the month of April, the week of March 30th, and a number of
   * weeks thereafter. The bug was that the logic was getting a little bit mixed up here, and attributing that
   * evaluation from March 30th to both the week of March 30th, and to the month of April.
   */
  @Test
  public void testGeneratePolicyViolationAggregations_OverlappingNewAggregationUpdates() {

    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);

    DateTime evalTime1 = new LocalDate(2020, 3, 30).toDateTimeAtStartOfDay();
    DateTime aggregatingTime1 = new LocalDate(2020, 4, 2).toDateTimeAtStartOfDay();
    DateTime aggregatingTime2 = new LocalDate(2020, 5, 2).toDateTimeAtStartOfDay();

    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        evalTime1.toDate());

    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, null, "hash1", "component 1");
    violationDAO.update(violation1);

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), aggregatingTime1, false);

    assertThat(aggregationDAO.getByTimePeriod(MONTH)).extracting(PolicyViolationAggregation::getEvaluationCount)
        .containsExactly(1);
    assertThat(aggregationDAO.getByTimePeriod(WEEK)).isEmpty();

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), aggregatingTime2, false);

    assertThat(aggregationDAO.getByTimePeriod(MONTH)).extracting(PolicyViolationAggregation::getEvaluationCount)
        .containsExactly(1, 0);
    assertThat(aggregationDAO.getByTimePeriod(WEEK)).extracting(PolicyViolationAggregation::getEvaluationCount)
        .containsExactly(1, 0, 0, 0);
  }

  private void assertViolationOneWeekAgoFromMidMonth(
      PolicyViolationAggregation aggregation,
      boolean violationExpected)
  {
    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL, aggregation.getDiscoveredAsTable());
    assertThat(aggregation.getDiscoveredCount(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL))
        .isEqualTo(violationExpected ? 1 : 0);

    assertAllCountsZero(aggregation.getFixedAsTable());
    assertAllCountsZero(aggregation.getWaivedAsTable());

    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL, aggregation.getOpenAsTable());
    assertThat(aggregation.getOpenAsTable().get(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL)).isEqualTo(1);

    assertThat(aggregation.getResolvedCountCriticalThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountSevereThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountModerateThreat()).isEqualTo(0);
    assertThat(aggregation.getResolvedCountLowThreat()).isEqualTo(0);

    assertThat(aggregation.getMttrCriticalThreat()).isNull();
    assertThat(aggregation.getMttrSevereThreat()).isNull();
    assertThat(aggregation.getMttrModerateThreat()).isNull();
    assertThat(aggregation.getMttrLowThreat()).isNull();
  }

  @Test
  public void testGeneratePolicyViolationAggregations_OpenCounts() {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getId(), "test policy", 10);
    // for weekly aggregations - make sure we have enough days in the week to work with
    DateTime now = new DateTime().withDayOfMonth(1).plusWeeks(2).withDayOfWeek(4);

    // generate 2 violations at the beginning of the month
    PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1",
        now.withDayOfMonth(1).toDate());
    PolicyViolation violation1 = tempEntity.newPolicyViolation(eval1, policy, null, "hash1", "component 1");
    tempEntity.newPolicyViolation(eval1, policy, null, "hash2", "component 2");

    // generate a violation 1 week ago
    PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan2",
        now.minusWeeks(1).toDate());
    tempEntity.newPolicyViolation(eval2, policy, null, "hash3", "component 3");

    // now resolve the first violation 2 days ago
    PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan3",
        now.minusHours(48).toDate());
    violation1.setFixTime(eval3.getTime());
    violationDAO.update(violation1);

    service.generatePolicyViolationAggregations(Collections.singleton(app.getId()), now, true);

    List<PolicyViolationAggregation> aggregations = aggregationDAO.getByTimePeriod(MONTH);

    assertThat(aggregations.size()).isEqualTo(1);
    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL,
        aggregations.get(0).getOpenAsTable());
    assertThat(aggregations.get(0).getOpenAsTable().get(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL))
        .isEqualTo(2);

    aggregations = aggregationDAO.getByTimePeriod(WEEK);

    assertThat(aggregations).hasSize(3);
    // 2 weeks ago
    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL,
        aggregations.get(0).getOpenAsTable());
    assertThat(aggregations.get(0).getOpenAsTable().get(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL))
        .isEqualTo(2);
    // 1 week ago
    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL,
        aggregations.get(1).getOpenAsTable());
    assertThat(aggregations.get(1).getOpenAsTable().get(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL))
        .isEqualTo(3);
    // this week
    assertAllCountsZeroExcept(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL,
        aggregations.get(2).getOpenAsTable());
    assertThat(aggregations.get(2).getOpenAsTable().get(PolicyThreatCategory.SECURITY, ThreatLevel.CRITICAL))
        .isEqualTo(2);
  }

  private void assertAllCountsZero(Table<PolicyThreatCategory, ThreatLevel, Integer> countsAsTable) {
    assertAllCountsZeroExcept(null, null, countsAsTable);
  }

  private void assertAllCountsZeroExcept(
      PolicyThreatCategory category,
      ThreatLevel level,
      Table<PolicyThreatCategory, ThreatLevel, Integer> countsAsTable)
  {
    for (Cell<PolicyThreatCategory, ThreatLevel, Integer> cell : countsAsTable.cellSet()) {
      if (!(cell.getRowKey().equals(category) && cell.getColumnKey().equals(level))) {
        assertThat(cell.getValue()).isEqualTo(0);
      }
    }
  }

  private void setSuccessMetricsStage(final String stageTypeId) {
    tempEntity.newSystemConfigurationProperty(
        SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID, stageTypeId);
    configuration.configurationChanged(Sets.newHashSet(SystemConfigurationProperty.SUCCESS_METRICS_STAGE_ID));
  }
}
