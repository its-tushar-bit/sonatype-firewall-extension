/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.ApplicationCountsByThreat;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.AverageMonth;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.AverageThreatCategoryMonth;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.MttrMonth;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.OpenViolationCountsWeek;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.ViolationCountPeriod;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;
import com.sonatype.insight.brain.utils.ThreatLevel;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.LocalDate;
import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.discovered;
import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.fixed;
import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.minusTimePeriod;
import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.plusTimePeriod;
import static com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDataHelper.waived;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.model.successmetrics.TimePeriod.MONTH;
import static com.sonatype.insight.brain.model.successmetrics.TimePeriod.WEEK;
import static com.sonatype.insight.brain.utils.ThreatLevel.LOW;
import static com.sonatype.insight.brain.utils.ThreatLevel.MODERATE;
import static com.sonatype.insight.brain.utils.ThreatLevel.SEVERE;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PolicyViolationAggregationDAOTest
    extends AbstractDbDAOTest
{
  private static final double TOLERANCE = 0.00001;

  private PolicyViolationAggregationDAO dao = new PolicyViolationAggregationDAO();

  @Test
  public void testCRUD() {
    String applicationId = "test-app-id";
    Date timePeriodStart = new Date();
    Date timePeriodEnd = new Date();

    DescriptiveStatistics mttrLowThreatStats = new DescriptiveStatistics();
    DescriptiveStatistics mttrModerateThreatStats = new DescriptiveStatistics();
    DescriptiveStatistics mttrSevereThreatStats = new DescriptiveStatistics();
    DescriptiveStatistics mttrCriticalThreatStats = new DescriptiveStatistics();

    Table<PolicyThreatCategory,ThreatLevel,Integer> discoveredCounts = HashBasedTable.create();
    Table<PolicyThreatCategory,ThreatLevel,Integer> fixedCounts = HashBasedTable.create();
    Table<PolicyThreatCategory,ThreatLevel,Integer> waivedCounts = HashBasedTable.create();
    Map<PolicyThreatCategory, Integer> openCounts = new EnumMap<>(PolicyThreatCategory.class);

    mttrLowThreatStats.addValue(1);
    mttrLowThreatStats.addValue(2);
    mttrLowThreatStats.addValue(3);
    fixedCounts.put(SECURITY, LOW, 1);
    waivedCounts.put(LICENSE, LOW, 2);

    mttrModerateThreatStats.addValue(8);
    waivedCounts.put(QUALITY, MODERATE, 1);

    mttrSevereThreatStats.addValue(50);
    mttrSevereThreatStats.addValue(35);
    mttrSevereThreatStats.addValue(47);
    fixedCounts.put(SECURITY, SEVERE, 3);

    openCounts.put(QUALITY, 2);

    int evaluationCount = 30;

    PolicyViolationAggregation aggregation = new PolicyViolationAggregation(applicationId, timePeriodStart,
        timePeriodEnd, MONTH, mttrLowThreatStats, mttrModerateThreatStats, mttrSevereThreatStats,
        mttrCriticalThreatStats, discoveredCounts, fixedCounts, waivedCounts, openCounts, evaluationCount);

    // create
    assertThat(aggregation.getId(), is(nullValue()));
    dao.insert(aggregation);
    assertThat(aggregation.getId(), is(notNullValue()));

    // read
    aggregation = dao.getById(aggregation.getId());
    assertThat(aggregation, is(notNullValue()));
    assertThat(aggregation.getApplicationId(), is(applicationId));
    assertThat(aggregation.getTimePeriodStart(), is(timePeriodStart));
    assertThat(aggregation.getTimePeriodEnd(), is(timePeriodEnd));
    assertThat(aggregation.getTimePeriod(), is(MONTH));
    assertThat(aggregation.getMttrLowThreat(), is(2L));
    assertThat(aggregation.getMttrModerateThreat(), is(8L));
    assertThat(aggregation.getMttrSevereThreat(), is(44L));
    assertThat(aggregation.getMttrCriticalThreat(), is(nullValue()));
    assertThat(aggregation.getResolvedCountLowThreat(), is(3));
    assertThat(aggregation.getResolvedCountModerateThreat(), is(1));
    assertThat(aggregation.getResolvedCountSevereThreat(), is(3));
    assertThat(aggregation.getResolvedCountCriticalThreat(), is(0));
    assertThat(aggregation.getOpenCountQuality(), is(2));
    assertThat(aggregation.getEvaluationCount(), is(evaluationCount));

    // update
    aggregation.setTimePeriodStart(new Date(aggregation.getTimePeriodStart().getTime() + 5000L));
    aggregation.setApplicationId(applicationId + "-2");
    dao.update(aggregation);

    aggregation = dao.getById(aggregation.getId());

    assertThat(aggregation, is(notNullValue()));
    assertThat(aggregation.getApplicationId(), is("test-app-id-2"));
    assertThat(aggregation.getTimePeriodStart().getTime(), is(timePeriodStart.getTime() + 5000L));

    // delete
    String id = aggregation.getId();
    dao.delete(aggregation);

    assertThat(dao.getById(id), is(nullValue()));
  }

  @Test
  public void testGetMostRecentByApplicationIdAndTimePeriod() {
    String applicationId = "test-app-id";
    Date date1 = new Date();
    Date date2 = new Date(date1.getTime() - 1000L);

    String aggregation1Id = tempEntity.newPolicyViolationAggregation(applicationId, date1, MONTH).getId();
    tempEntity.newPolicyViolationAggregation(applicationId, date2, WEEK);

    PolicyViolationAggregation retrievedAggregation = dao
        .getMostRecentByApplicationIdAndTimePeriod(applicationId, MONTH);

    assertThat(retrievedAggregation.getId(), is(aggregation1Id));
  }

  @Test
  public void testGetMostRecentByApplicationIdAndTimePeriod_NoAggregations() {
    String applicationId = "test-app-id";

    PolicyViolationAggregation retrievedAggregation = dao
        .getMostRecentByApplicationIdAndTimePeriod(applicationId, MONTH);

    assertThat(retrievedAggregation, is(nullValue()));
  }

  @Test
  public void testGetMostRecentByApplicationIdAndTimePeriod_FiltersTimePeriod() {
    String applicationId = "test-app-id";
    Date weekStartDate = new Date();
    Date monthStartDate = new Date(weekStartDate.getTime() - 1000L);
  
    // add data for both time periods
    String aggregationMonthId = tempEntity.newPolicyViolationAggregation(applicationId, monthStartDate, MONTH).getId();
    String aggregationWeekId = tempEntity.newPolicyViolationAggregation(applicationId, weekStartDate, WEEK).getId();

    PolicyViolationAggregation retrievedAggregation = dao
        .getMostRecentByApplicationIdAndTimePeriod(applicationId, MONTH);

    assertThat(retrievedAggregation.getId(), is(aggregationMonthId));
    assertThat(retrievedAggregation.getTimePeriod(), is(MONTH));
    assertThat(retrievedAggregation.getTimePeriodStart(), is(monthStartDate));
    
    retrievedAggregation = dao.getMostRecentByApplicationIdAndTimePeriod(applicationId, WEEK);

    assertThat(retrievedAggregation.getId(), is(aggregationWeekId));
    assertThat(retrievedAggregation.getTimePeriod(), is(WEEK));
    assertThat(retrievedAggregation.getTimePeriodStart(), is(weekStartDate));
  }

  @Test
  public void testGetMttrMonthlyAverages() {
    LocalDate today = new LocalDate();
    LocalDate beginningOfMonthLastYear = minusTimePeriod(today, MONTH, 12);

    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    List<MttrMonth> results = dao.getMttrMonthlyAverages(applicationIds, false);

    MttrMonth[] expectedResults = {
        new MttrMonth(toDate(beginningOfMonthLastYear), 1000L, 3000L, 2000L, 2000L, 1, 1, 2, 1),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 1)), 2500L, 2500L, null, null, 2, 1, 0, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 2)), null, null, null, null, 0, 0, 0, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 3)), 1000L, 1000L, null, null, 5, 3, 0, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 4)), null, null, null, null, 0, 0, 0, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 5)), 5000L, null, null, 5000L, 2, 0, 0, 1),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 6)), 16000L, 16000L, 16000L, 16000L, 3, 1, 2, 52),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 7)), null, null, null, null, 0, 0, 0, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 8)), 5000L, 10000L, 15000L, 20000L, 1, 1, 1, 3),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 9)), 50000L, 25000L, null, null, 3, 3, 0, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 10)), null, null, 5000L, null, 0, 0, 1, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 11)), 7000L, 8000L, 9000L, 10000L, 4, 4, 4, 4)
    };

    // check that only the most recent 12 months are included
    assertThat(results.size(), is(12));
    for (int i = 0; i < 12; i++) {
      assertMttrMonth(results.get(i), expectedResults[i]);
    }
  }

  @Test
  public void testGetMttrMonthlyAverages_includeLatestData() {
    LocalDate today = new LocalDate();
    LocalDate beginningOfMonthLastYear = minusTimePeriod(today, MONTH, 12);

    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    List<MttrMonth> results = dao.getMttrMonthlyAverages(applicationIds, true);

    MttrMonth[] expectedResults = {
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 1)), 2500L, 2500L, null, null, 2, 1, 0, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 2)), null, null, null, null, 0, 0, 0, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 3)), 1000L, 1000L, null, null, 5, 3, 0, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 4)), null, null, null, null, 0, 0, 0, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 5)), 5000L, null, null, 5000L, 2, 0, 0, 1),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 6)), 16000L, 16000L, 16000L, 16000L, 3, 1, 2, 52),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 7)), null, null, null, null, 0, 0, 0, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 8)), 5000L, 10000L, 15000L, 20000L, 1, 1, 1, 3),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 9)), 50000L, 25000L, null, null, 3, 3, 0, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 10)), null, null, 5000L, null, 0, 0, 1, 0),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 11)), 7000L, 8000L, 9000L, 10000L, 4, 4, 4, 4),
        new MttrMonth(toDate(plusMonths(beginningOfMonthLastYear, 12)), 4000L, 5000L, 6000L, 7000L, 2, 2, 2, 2)
    };

    // check that only the most recent 12 months are included, including current month
    assertThat(results.size(), is(12));
    for (int i = 0; i < 12; i++) {
      assertMttrMonth(results.get(i), expectedResults[i]);
    }
  }

  @Test
  public void testGetMttrMonthlyAverages_NoAggregations() {
    Set<String> applicationIds = new HashSet<>();
    applicationIds.add("1");
    applicationIds.add("2");

    List<MttrMonth> results = dao.getMttrMonthlyAverages(applicationIds, false);

    assertThat(results, hasSize(0));
  }

  @Test
  public void testGetMttrMonthlyAverages_EmptyApplicationIdSet() {
    List<MttrMonth> results = dao.getMttrMonthlyAverages(new HashSet<String>(), false);

    assertThat(results, hasSize(0));
  }

  @Test
  public void testGetMonthlyAverages_multipleAggregationForMultipleMonths() {
    LocalDate today = new LocalDate();
    LocalDate aggregationStart = minusTimePeriod(today, MONTH, 12);

    Application testApp1 = tempEntity.newApplication(Organization.ROOT_ORGANIZATION_ID);
    Application testApp2 = tempEntity.newApplication(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newPolicyViolationAggregation(testApp1.getId(), aggregationStart, MONTH, //
        asList(1, 2, 3, 4), //
        asList(2, 3, 4, 5), //
        asList(3, 4, 5, 6), //
        asList(4, 5, 6, 7), //
        1);
    tempEntity.newPolicyViolationAggregation(testApp2.getId(), aggregationStart, MONTH, //
        asList(3, 4, 5, 6), //
        asList(4, 5, 6, 7), //
        asList(5, 6, 7, 8), //
        asList(6, 7, 8, 9), //
        2);
    tempEntity
        .newPolicyViolationAggregation(testApp1.getId(), plusTimePeriod(aggregationStart, MONTH, 1), MONTH, //
            asList(2, 3, 4, 5), //
            asList(3, 4, 5, 6), //
            asList(4, 5, 6, 7), //
            asList(5, 6, 7, 8), //
            3);
    tempEntity
        .newPolicyViolationAggregation(testApp2.getId(), plusTimePeriod(aggregationStart, MONTH, 1), MONTH, //
            asList(4, 5, 6, 7), //
            asList(5, 6, 7, 8), //
            asList(6, 7, 8, 9), //
            asList(7, 8, 9, 10), //
            4);
    // a time period without evaluations should not affect averages
    tempEntity
        .newPolicyViolationAggregation(testApp1.getId(), plusTimePeriod(aggregationStart, MONTH, 2), MONTH, //
            asList(2, 2, 2, 2), //
            asList(2, 2, 2, 2), //
            asList(2, 2, 2, 2), //
            asList(2, 2, 2, 2), //
            1);
    tempEntity
        .newPolicyViolationAggregation(testApp2.getId(), plusTimePeriod(aggregationStart, MONTH, 2), MONTH, //
            asList(0, 0, 0, 0), //
            asList(0, 0, 0, 0), //
            asList(0, 0, 0, 0), //
            asList(0, 0, 0, 0), //
            0);
    
    // Add some week data
    tempEntity
        .newPolicyViolationAggregation(testApp1.getId(), plusTimePeriod(aggregationStart, WEEK, 1), WEEK, //
            asList(5, 4, 3, 2), //
            asList(6, 5, 4, 3), //
            asList(7, 6, 5, 4), //
            asList(8, 7, 6, 5), //
            3);
    tempEntity
        .newPolicyViolationAggregation(testApp2.getId(), plusTimePeriod(aggregationStart, WEEK, 1), WEEK, //
            asList(7, 6, 5, 4), //
            asList(8, 7, 6, 5), //
            asList(9, 8, 7, 6), //
            asList(10, 9, 8, 7), //
            4);

    List<AverageMonth> results = dao.getMonthlyAverages(getApplicationIds(testApp1, testApp2), false);

    assertThat(results, hasSize(3));
    assertThat(results.get(0).timePeriodStart, is(aggregationStart.toDate()));
    assertThat(results.get(0).evaluationCount, is(3));
    assertAverages(results.get(0).security, 2, 3, 4, 5);
    assertAverages(results.get(0).license, 3, 4, 5, 6);
    assertAverages(results.get(0).quality, 4, 5, 6, 7);
    assertAverages(results.get(0).other, 5, 6, 7, 8);

    assertThat(results.get(1).timePeriodStart, is(plusTimePeriod(aggregationStart, MONTH, 1).toDate()));
    assertThat(results.get(1).evaluationCount, is(7));
    assertAverages(results.get(1).security, 3, 4, 5, 6);
    assertAverages(results.get(1).license, 4, 5, 6, 7);
    assertAverages(results.get(1).quality, 5, 6, 7, 8);
    assertAverages(results.get(1).other, 6, 7, 8, 9);

    assertThat(results.get(2).timePeriodStart, is(plusTimePeriod(aggregationStart, MONTH, 2).toDate()));
    assertThat(results.get(2).evaluationCount, is(1));
    assertAverages(results.get(2).security, 2, 2, 2, 2);
    assertAverages(results.get(2).license, 2, 2, 2, 2);
    assertAverages(results.get(2).quality, 2, 2, 2, 2);
    assertAverages(results.get(2).other, 2, 2, 2, 2);
  }

  @Test
  public void testGetMonthlyAverages_onlyLast12AggregationsCounted() {
    LocalDate today = new LocalDate();
    LocalDate aggregationStart = minusTimePeriod(today, MONTH, 12);

    Application testApp = tempEntity.newApplication(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newPolicyViolationAggregation(testApp.getId(), minusTimePeriod(aggregationStart, MONTH, 1), MONTH, //
        asList(2, 3, 4, 5), //
        asList(3, 4, 5, 6), //
        asList(4, 5, 6, 7), //
        asList(5, 6, 7, 8), //
        5);
    for (int i = 0; i < 12; i++) {
      tempEntity.newPolicyViolationAggregation(testApp.getId(), plusTimePeriod(aggregationStart, MONTH, i), MONTH, //
          asList(1, 2, 3, 4), //
          asList(2, 3, 4, 5), //
          asList(3, 4, 5, 6), //
          asList(4, 5, 6, 7), //
          1);
    }
    List<AverageMonth> results = dao.getMonthlyAverages(getApplicationIds(testApp), false);

    assertThat(results, hasSize(12));
    for (int i = 0; i < 12; i++) {
      AverageMonth month = results.get(i);
      assertThat(month.timePeriodStart, is(plusTimePeriod(aggregationStart, MONTH, i).toDate()));
      assertThat(month.evaluationCount, is(1));
      assertAverages(month.security, 1, 2, 3, 4);
      assertAverages(month.license, 2, 3, 4, 5);
      assertAverages(month.quality, 3, 4, 5, 6);
      assertAverages(month.other, 4, 5, 6, 7);
    }
  }

  @Test
  public void testGetMonthlyAverages_onlyLast12AggregationsCounted_includingCurrentMonthForLatestData() {
    LocalDate today = new LocalDate();
    // Note the aggregation start date is 1 time period later than in the test above.
    LocalDate weekAggregationStart = minusTimePeriod(today, WEEK, 11);
    LocalDate monthAggregationStart = minusTimePeriod(today, MONTH, 11);

    Application testApp = tempEntity.newApplication(Organization.ROOT_ORGANIZATION_ID);
    // add data for both time periods
    tempEntity
        .newPolicyViolationAggregation(testApp.getId(), minusTimePeriod(monthAggregationStart, MONTH, 1), MONTH, //
            asList(2, 3, 4, 5), //
            asList(3, 4, 5, 6), //
            asList(4, 5, 6, 7), //
            asList(5, 6, 7, 8), //
            5);
    for (int i = 0; i < 12; i++) {
      tempEntity
          .newPolicyViolationAggregation(testApp.getId(), plusTimePeriod(monthAggregationStart, MONTH, i), MONTH, //
              asList(1, 2, 3, 4), //
              asList(2, 3, 4, 5), //
              asList(3, 4, 5, 6), //
              asList(4, 5, 6, 7), //
              1);
    }
    tempEntity
        .newPolicyViolationAggregation(testApp.getId(), minusTimePeriod(weekAggregationStart, WEEK, 1), WEEK, //
            asList(4, 5, 6, 7), //
            asList(5, 6, 7, 8), //
            asList(6, 7, 8, 9), //
            asList(7, 8, 9, 10), //
            5);
    for (int i = 0; i < 12; i++) {
      tempEntity.newPolicyViolationAggregation(testApp.getId(), plusTimePeriod(weekAggregationStart, WEEK, i), WEEK, //
          asList(3, 4, 5, 6), //
          asList(4, 5, 6, 7), //
          asList(5, 6, 7, 8), //
          asList(6, 7, 8, 9), //
          1);
    }
    List<AverageMonth> results = dao.getMonthlyAverages(getApplicationIds(testApp), true);

    assertThat(results, hasSize(12));
    for (int i = 0; i < 12; i++) {
      AverageMonth month = results.get(i);
      assertThat(month.timePeriodStart, is(plusTimePeriod(monthAggregationStart, MONTH, i).toDate()));
      assertThat(month.evaluationCount, is(1));
      assertAverages(month.security, 1, 2, 3, 4);
      assertAverages(month.license, 2, 3, 4, 5);
      assertAverages(month.quality, 3, 4, 5, 6);
      assertAverages(month.other, 4, 5, 6, 7);
    }
  }

  @Test
  public void testGetMonthlyAverages_NoAggregations() {
    Set<String> applicationIds = new HashSet<>();
    applicationIds.add("1");
    applicationIds.add("2");

    List<AverageMonth> results = dao.getMonthlyAverages(applicationIds, false);

    assertThat(results, hasSize(0));
  }

  @Test
  public void testGetMonthlyAverages_EmptyApplicationIdSet() {
    List<AverageMonth> monthlyAverages = dao.getMonthlyAverages(new HashSet<String>(), false);

    assertThat(monthlyAverages, hasSize(0));
  }

  @Test
  public void testGetActiveApplicationCount() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    int result = dao.getActiveApplicationCount(applicationIds, false);

    assertThat(result, is(4));
  }

  @Test
  public void testGetActiveApplicationCount_EmptyApplicationIdSet() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    int result = dao.getActiveApplicationCount(new HashSet<String>(), false);

    assertThat(result, is(0));
  }

  @Test
  public void testGetApplicationCountsByThreatByApplicationIds() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper
        .createApplicationCountAggregationHistory(tempEntity);

    ApplicationCountsByThreat result = dao.getApplicationCountsByThreatByApplicationIds(applicationIds, false);

    assertThat(result.countAnyThreat, is(4));
    assertThat(result.countAnyCriticalThreat, is(3));
    assertThat(result.countSecurityThreat, is(4));
    assertThat(result.countSecurityCriticalThreat, is(2));
    assertThat(result.countLicenseThreat, is(3));
    assertThat(result.countLicenseCriticalThreat, is(1));
    assertThat(result.countQualityThreat, is(1));
    assertThat(result.countQualityCriticalThreat, is(0));
    assertThat(result.countOtherThreat, is(1));
    assertThat(result.countOtherCriticalThreat, is(1));
  }

  @Test
  public void testGetApplicationCountsByThreatByApplicationIds_IncludeLatestData() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper
        .createApplicationCountAggregationHistory(tempEntity);

    ApplicationCountsByThreat result = dao.getApplicationCountsByThreatByApplicationIds(applicationIds, true);

    assertThat(result.countAnyThreat, is(4));
    assertThat(result.countAnyCriticalThreat, is(4));
    assertThat(result.countSecurityThreat, is(4));
    assertThat(result.countSecurityCriticalThreat, is(2));
    assertThat(result.countLicenseThreat, is(4));
    assertThat(result.countLicenseCriticalThreat, is(1));
    assertThat(result.countQualityThreat, is(3));
    assertThat(result.countQualityCriticalThreat, is(0));
    assertThat(result.countOtherThreat, is(3));
    assertThat(result.countOtherCriticalThreat, is(3));
  }

  @Test
  public void testGetApplicationCountsByThreatByApplicationIds_EmptyApplicationIdSet() {
    PolicyViolationAggregationDataHelper.createApplicationCountAggregationHistory(tempEntity);

    ApplicationCountsByThreat result = dao.getApplicationCountsByThreatByApplicationIds(new HashSet<String>(), false);

    assertThat(result.countAnyThreat, is(0));
    assertThat(result.countAnyCriticalThreat, is(0));
    assertThat(result.countSecurityThreat, is(0));
    assertThat(result.countSecurityCriticalThreat, is(0));
    assertThat(result.countLicenseThreat, is(0));
    assertThat(result.countLicenseCriticalThreat, is(0));
    assertThat(result.countQualityThreat, is(0));
    assertThat(result.countQualityCriticalThreat, is(0));
    assertThat(result.countOtherThreat, is(0));
    assertThat(result.countOtherCriticalThreat, is(0));
  }

  @Test
  public void testGetViolationCountsByApplicationIds() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    List<ViolationCountPeriod> actualList = dao.getViolationCountsByApplicationIds(applicationIds, false);

    LocalDate beginningOfWeek12WeeksAgo = new LocalDate().withDayOfWeek(1).minusWeeks(12);

    List<ViolationCountPeriod> expectedList = Arrays.asList(
        new ViolationCountPeriod(toDate(beginningOfWeek12WeeksAgo),
            discovered().security(0, 0, 0, 0).license(0, 0, 0, 2).quality(1, 1, 0, 2).other(0, 0, 0, 6).asMap(),
            fixed().security(2, 0, 1, 0).license(0, 1, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 1, 0).other(0, 0, 0, 1).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek12WeeksAgo.plusWeeks(1)),
            discovered().security(0, 0, 0, 0).license(0, 0, 0, 2).quality(0, 0, 0, 4).other(1, 3, 2, 6).asMap(),
            fixed().security(1, 0, 0, 0).license(0, 1, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(1, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek12WeeksAgo.plusWeeks(2)),
            discovered().security(1, 0, 0, 0).license(1, 0, 0, 1).quality(0, 0, 0, 3).other(0, 0, 0, 3).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(1, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek12WeeksAgo.plusWeeks(3)),
            discovered().security(1, 0, 5, 0).license(1, 2, 0, 3).quality(0, 3, 0, 6).other(0, 0, 0, 6).asMap(),
            fixed().security(3, 1, 0, 0).license(0, 1, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 1, 0, 0).license(0, 0, 0, 0).quality(1, 1, 0, 0).other(1, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek12WeeksAgo.plusWeeks(4)),
            discovered().security(0, 0, 0, 0).license(1, 1, 1, 1).quality(1, 0, 0, 3).other(0, 1, 1, 0).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek12WeeksAgo.plusWeeks(5)),
            discovered().security(2, 1, 3, 0).license(0, 0, 0, 3).quality(1, 2, 0, 6).other(1, 7, 1, 9).asMap(),
            fixed().security(0, 0, 0, 1).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(2, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek12WeeksAgo.plusWeeks(6)),
            discovered().security(0, 3, 0, 0).license(0, 0, 0, 3).quality(2, 3, 4, 3).other(3, 0, 0, 12).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 1, 0, 0).other(3, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 2, 52).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek12WeeksAgo.plusWeeks(7)),
            discovered().security(0, 0, 0, 0).license(0, 1, 1, 2).quality(1, 0, 1, 4).other(2, 0, 0, 10).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek12WeeksAgo.plusWeeks(8)),
            discovered().security(1, 1, 0, 0).license(0, 2, 0, 2).quality(0, 1, 1, 2).other(0, 2, 0, 4).asMap(),
            fixed().security(0, 0, 0, 1).license(0, 1, 1, 1).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 1).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek12WeeksAgo.plusWeeks(9)),
            discovered().security(1, 0, 3, 0).license(1, 1, 4, 2).quality(0, 2, 0, 8).other(0, 2, 0, 10).asMap(),
            fixed().security(1, 2, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(1, 2, 0, 0).other(3, 1, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek12WeeksAgo.plusWeeks(10)),
            discovered().security(0, 0, 1, 0).license(0, 0, 2, 1).quality(1, 0, 0, 2).other(0, 0, 0, 3).asMap(),
            fixed().security(0, 0, 1, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek12WeeksAgo.plusWeeks(11)),
            discovered().security(0, 0, 8, 0).license(0, 0, 0, 4).quality(3, 0, 1, 4).other(0, 0, 0, 12).asMap(),
            fixed().security(1, 1, 0, 0).license(0, 0, 0, 0).quality(2, 2, 1, 1).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 2, 2).license(0, 0, 0, 0).quality(1, 1, 1, 1).other(0, 0, 0, 0).asMap())
    );

    assertViolationCountHistory(expectedList, actualList);
  }

  @Test
  public void testGetViolationCountsByApplicationIds_IncludeLatestData() {
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    List<ViolationCountPeriod> actualList = dao.getViolationCountsByApplicationIds(applicationIds, true);

    LocalDate beginningOfWeek11WeeksAgo = new LocalDate().withDayOfWeek(1).minusWeeks(11);

    List<ViolationCountPeriod> expectedList = Arrays.asList(
        new ViolationCountPeriod(toDate(beginningOfWeek11WeeksAgo),
            discovered().security(0, 0, 0, 0).license(0, 0, 0, 2).quality(0, 0, 0, 4).other(1, 3, 2, 6).asMap(),
            fixed().security(1, 0, 0, 0).license(0, 1, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(1, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek11WeeksAgo.plusWeeks(1)),
            discovered().security(1, 0, 0, 0).license(1, 0, 0, 1).quality(0, 0, 0, 3).other(0, 0, 0, 3).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(1, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek11WeeksAgo.plusWeeks(2)),
            discovered().security(1, 0, 5, 0).license(1, 2, 0, 3).quality(0, 3, 0, 6).other(0, 0, 0, 6).asMap(),
            fixed().security(3, 1, 0, 0).license(0, 1, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 1, 0, 0).license(0, 0, 0, 0).quality(1, 1, 0, 0).other(1, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek11WeeksAgo.plusWeeks(3)),
            discovered().security(0, 0, 0, 0).license(1, 1, 1, 1).quality(1, 0, 0, 3).other(0, 1, 1, 0).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek11WeeksAgo.plusWeeks(4)),
            discovered().security(2, 1, 3, 0).license(0, 0, 0, 3).quality(1, 2, 0, 6).other(1, 7, 1, 9).asMap(),
            fixed().security(0, 0, 0, 1).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(2, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek11WeeksAgo.plusWeeks(5)),
            discovered().security(0, 3, 0, 0).license(0, 0, 0, 3).quality(2, 3, 4, 3).other(3, 0, 0, 12).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 1, 0, 0).other(3, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 2, 52).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek11WeeksAgo.plusWeeks(6)),
            discovered().security(0, 0, 0, 0).license(0, 1, 1, 2).quality(1, 0, 1, 4).other(2, 0, 0, 10).asMap(),
            fixed().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek11WeeksAgo.plusWeeks(7)),
            discovered().security(1, 1, 0, 0).license(0, 2, 0, 2).quality(0, 1, 1, 2).other(0, 2, 0, 4).asMap(),
            fixed().security(0, 0, 0, 1).license(0, 1, 1, 1).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 1).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek11WeeksAgo.plusWeeks(8)),
            discovered().security(1, 0, 3, 0).license(1, 1, 4, 2).quality(0, 2, 0, 8).other(0, 2, 0, 10).asMap(),
            fixed().security(1, 2, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(1, 2, 0, 0).other(3, 1, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek11WeeksAgo.plusWeeks(9)),
            discovered().security(0, 0, 1, 0).license(0, 0, 2, 1).quality(1, 0, 0, 2).other(0, 0, 0, 3).asMap(),
            fixed().security(0, 0, 1, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek11WeeksAgo.plusWeeks(10)),
            discovered().security(0, 0, 8, 0).license(0, 0, 0, 4).quality(3, 0, 1, 4).other(0, 0, 0, 12).asMap(),
            fixed().security(1, 1, 0, 0).license(0, 0, 0, 0).quality(2, 2, 1, 1).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 2, 2).license(0, 0, 0, 0).quality(1, 1, 1, 1).other(0, 0, 0, 0).asMap()),
        new ViolationCountPeriod(toDate(beginningOfWeek11WeeksAgo.plusWeeks(11)),
            discovered().security(4, 6, 8, 10).license(4, 6, 8, 10).quality(4, 6, 8, 10).other(4, 6, 8, 10).asMap(),
            fixed().security(2, 2, 0, 0).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap(),
            waived().security(0, 0, 2, 2).license(0, 0, 0, 0).quality(0, 0, 0, 0).other(0, 0, 0, 0).asMap())
    );

    assertViolationCountHistory(expectedList, actualList);
  }

  @Test
  public void testGetViolationCountsByThreatByApplicationIds_EmptyApplicationIdSet() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    List<ViolationCountPeriod> actualList = dao.getViolationCountsByApplicationIds(new HashSet<>(), true);

    assertEquals(Collections.emptyList(), actualList);
  }

  @Test
  public void testGetTotalOpenViolationsByApplicationIds() {
    LocalDate beginningOfWeek12WeeksAgo = new LocalDate().withDayOfWeek(1).minusWeeks(12);
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    List<OpenViolationCountsWeek> actualList = dao
        .getOpenViolationsCountsByApplicationIds(applicationIds, false);

    List<OpenViolationCountsWeek> expectedList = Arrays.asList(
        new OpenViolationCountsWeek(toDate(beginningOfWeek12WeeksAgo), categoryCounts(2, 4, 6, 8)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek12WeeksAgo.plusWeeks(1)), categoryCounts(2, 4, 6, 8)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek12WeeksAgo.plusWeeks(2)), categoryCounts(2, 4, 6, 8)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek12WeeksAgo.plusWeeks(3)), categoryCounts(3, 6, 9, 12)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek12WeeksAgo.plusWeeks(4)), categoryCounts(3, 6, 9, 12)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek12WeeksAgo.plusWeeks(5)), categoryCounts(3, 6, 9, 12)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek12WeeksAgo.plusWeeks(6)), categoryCounts(3, 6, 9, 12)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek12WeeksAgo.plusWeeks(7)), categoryCounts(3, 6, 9, 12)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek12WeeksAgo.plusWeeks(8)), categoryCounts(4, 8, 12, 16)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek12WeeksAgo.plusWeeks(9)), categoryCounts(4, 8, 12, 16)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek12WeeksAgo.plusWeeks(10)), categoryCounts(4, 8, 12, 16)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek12WeeksAgo.plusWeeks(11)), categoryCounts(4, 8, 12, 16))
    );

    assertOpenViolationCountsWeekHistory(actualList, expectedList);
  }

  @Test
  public void testGetTotalOpenViolationsByApplicationIds_IncludeLatestData() {
    LocalDate beginningOfWeek11WeeksAgo = new LocalDate().withDayOfWeek(1).minusWeeks(11);
    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    List<OpenViolationCountsWeek> actualList = dao
        .getOpenViolationsCountsByApplicationIds(applicationIds, true);

    List<OpenViolationCountsWeek> expectedList = Arrays.asList(
        new OpenViolationCountsWeek(toDate(beginningOfWeek11WeeksAgo), categoryCounts(2, 4, 6, 8)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek11WeeksAgo.plusWeeks(1)), categoryCounts(2, 4, 6, 8)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek11WeeksAgo.plusWeeks(2)), categoryCounts(3, 6, 9, 12)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek11WeeksAgo.plusWeeks(3)), categoryCounts(3, 6, 9, 12)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek11WeeksAgo.plusWeeks(4)), categoryCounts(3, 6, 9, 12)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek11WeeksAgo.plusWeeks(5)), categoryCounts(3, 6, 9, 12)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek11WeeksAgo.plusWeeks(6)), categoryCounts(3, 6, 9, 12)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek11WeeksAgo.plusWeeks(7)), categoryCounts(4, 8, 12, 16)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek11WeeksAgo.plusWeeks(8)), categoryCounts(4, 8, 12, 16)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek11WeeksAgo.plusWeeks(9)), categoryCounts(4, 8, 12, 16)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek11WeeksAgo.plusWeeks(10)), categoryCounts(4, 8, 12, 16)),
        new OpenViolationCountsWeek(toDate(beginningOfWeek11WeeksAgo.plusWeeks(11)), categoryCounts(2, 4, 6, 8))
    );

    assertOpenViolationCountsWeekHistory(actualList, expectedList);
  }

  @Test
  public void testGetTotalOpenViolationsByApplicationIds_EmptyApplicationIdSet() {
    PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    List<OpenViolationCountsWeek> actualList = dao
        .getOpenViolationsCountsByApplicationIds(new HashSet<>(), false);

    assertEquals(Collections.emptyList(), actualList);
  }

  private Map<PolicyThreatCategory, Integer> categoryCounts(int security, int license, int quality, int other) {
    Map<PolicyThreatCategory, Integer> result = new EnumMap<>(PolicyThreatCategory.class);
    result.put(SECURITY, security);
    result.put(LICENSE, license);
    result.put(QUALITY, quality);
    result.put(OTHER, other);
    return result;
  }

  private void assertViolationCountHistory(List<ViolationCountPeriod> expectedList,
                                           List<ViolationCountPeriod> actualList)
  {
    assertThat(actualList, hasSize(expectedList.size()));
    for (int i = 0; i < expectedList.size(); i++) {
      assertThat(i + " discovered", actualList.get(i).discoveredCounts, is(expectedList.get(i).discoveredCounts));
      assertThat(i + " fixed", actualList.get(i).fixedCounts, is(expectedList.get(i).fixedCounts));
      assertThat(i + " waived", actualList.get(i).waivedCounts, is(expectedList.get(i).waivedCounts));
      assertThat(actualList.get(i).periodStart, is(expectedList.get(i).periodStart));
    }
  }

  private void assertOpenViolationCountsWeekHistory(List<OpenViolationCountsWeek> expectedList,
                                                    List<OpenViolationCountsWeek> actualList)
  {
    assertThat(actualList, hasSize(expectedList.size()));
    for (int i = 0; i < expectedList.size(); i++) {
      OpenViolationCountsWeek expected = expectedList.get(i);
      OpenViolationCountsWeek actual = actualList.get(i);
      assertThat(i + " week start", actual.weekStart, is(expected.weekStart));
      assertThat(i + " security", actual.openViolationCounts.get(SECURITY), 
          is(expected.openViolationCounts.get(SECURITY)));
      assertThat(i + " license", actual.openViolationCounts.get(LICENSE), 
          is(expected.openViolationCounts.get(LICENSE)));
      assertThat(i + " quality", actual.openViolationCounts.get(QUALITY), 
          is(expected.openViolationCounts.get(QUALITY)));
      assertThat(i + " other", actual.openViolationCounts.get(OTHER), is(expected.openViolationCounts.get(OTHER)));
    }
  }

  private void assertAverages(AverageThreatCategoryMonth actual, double low, double moderate, double severe, double critical) {
    assertThat(actual.averageDiscoveredLowThreat, closeTo(low, TOLERANCE));
    assertThat(actual.averageDiscoveredModerateThreat, closeTo(moderate, TOLERANCE));
    assertThat(actual.averageDiscoveredSevereThreat, closeTo(severe, TOLERANCE));
    assertThat(actual.averageDiscoveredCriticalThreat, closeTo(critical, TOLERANCE));
  }

  private Set<String> getApplicationIds(Application... testApps) {
    HashSet<String> results = new HashSet<>();
    for (Application app : testApps) {
      results.add(app.getId());
    }
    return results;
  }

  private void assertMttrMonth(final MttrMonth actual, final MttrMonth expected) {
    assertThat(actual.monthStart, is(expected.monthStart));
    assertThat(actual.mttrLowThreat, is(expected.mttrLowThreat));
    assertThat(actual.mttrModerateThreat, is(expected.mttrModerateThreat));
    assertThat(actual.mttrSevereThreat, is(expected.mttrSevereThreat));
    assertThat(actual.mttrCriticalThreat, is(expected.mttrCriticalThreat));
    assertThat(actual.resolvedCountLowThreat, is(expected.resolvedCountLowThreat));
    assertThat(actual.resolvedCountModerateThreat, is(expected.resolvedCountModerateThreat));
    assertThat(actual.resolvedCountSevereThreat, is(expected.resolvedCountSevereThreat));
    assertThat(actual.resolvedCountCriticalThreat, is(expected.resolvedCountCriticalThreat));
  }

  private static Date toDate(LocalDate date) {
    return date.toDateTimeAtStartOfDay().toDate();
  }

  public static LocalDate plusMonths(LocalDate dateTime, int timePeriods) {
    return plusTimePeriod(dateTime, MONTH, timePeriods);
  }
}
