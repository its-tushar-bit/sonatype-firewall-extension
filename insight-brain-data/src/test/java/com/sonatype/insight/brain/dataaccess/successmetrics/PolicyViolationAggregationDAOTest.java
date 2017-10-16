/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.ApplicationCountsByThreat;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.AverageMonth;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.AverageThreatCategoryMonth;
import com.sonatype.insight.brain.dataaccess.successmetrics.PolicyViolationAggregationDAO.MttrMonth;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationAggregation;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.LocalDate;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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

    DescriptiveStatistics mttrLowThreatStats = new DescriptiveStatistics();
    DescriptiveStatistics mttrModerateThreatStats = new DescriptiveStatistics();
    DescriptiveStatistics mttrSevereThreatStats = new DescriptiveStatistics();
    DescriptiveStatistics mttrCriticalThreatStats = new DescriptiveStatistics();

    mttrLowThreatStats.addValue(1);
    mttrLowThreatStats.addValue(2);
    mttrLowThreatStats.addValue(3);

    mttrModerateThreatStats.addValue(8);

    mttrSevereThreatStats.addValue(50);
    mttrSevereThreatStats.addValue(35);
    mttrSevereThreatStats.addValue(47);

    PolicyViolationAggregation aggregation = new PolicyViolationAggregation(applicationId, timePeriodStart,
        mttrLowThreatStats, mttrModerateThreatStats, mttrSevereThreatStats, mttrCriticalThreatStats);

    // create
    assertThat(aggregation.getId(), is(nullValue()));
    dao.insert(aggregation);
    assertThat(aggregation.getId(), is(notNullValue()));

    // read
    aggregation = dao.getById(aggregation.getId());
    assertThat(aggregation, is(notNullValue()));
    assertThat(aggregation.getApplicationId(), is(applicationId));
    assertThat(aggregation.getTimePeriodStart(), is(timePeriodStart));
    assertThat(aggregation.getMttrLowThreat(), is(2L));
    assertThat(aggregation.getMttrModerateThreat(), is(8L));
    assertThat(aggregation.getMttrSevereThreat(), is(44L));
    assertThat(aggregation.getMttrCriticalThreat(), is(nullValue()));
    assertThat(aggregation.getResolvedCountLowThreat(), is(3));
    assertThat(aggregation.getResolvedCountModerateThreat(), is(1));
    assertThat(aggregation.getResolvedCountSevereThreat(), is(3));
    assertThat(aggregation.getResolvedCountCriticalThreat(), is(0));

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
  public void testGetMostRecentByApplicationId() {
    String applicationId = "test-app-id";
    Date date1 = new Date();
    Date date2 = new Date(date1.getTime() - 1000L);

    String aggregation1Id = tempEntity.newPolicyViolationAggregation(applicationId, date1).getId();
    tempEntity.newPolicyViolationAggregation(applicationId, date2);

    PolicyViolationAggregation retrievedAggregation = dao.getMostRecentByApplicationId(applicationId);

    assertThat(retrievedAggregation.getId(), is(aggregation1Id));
  }

  @Test
  public void testGetMostRecentByApplicationId_NoAggregations() {
    String applicationId = "test-app-id";

    PolicyViolationAggregation retrievedAggregation = dao.getMostRecentByApplicationId(applicationId);

    assertThat(retrievedAggregation, is(nullValue()));
  }

  @Test
  public void testGetMttrMonthlyAverages() {
    LocalDate today = new LocalDate();
    LocalDate beginningOfMonthLastYear = today.withDayOfMonth(1).minusYears(1);

    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    List<MttrMonth> results = dao.getMttrMonthlyAverages(applicationIds, false);

    MttrMonth[] expectedResults = {
        new MttrMonth(toDate(beginningOfMonthLastYear), 1000L, 3000L, 2000L, 2000L, 1, 1, 2, 1),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(1)), 2500L, 2500L, null, null, 2, 1, 0, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(2)), null, null, null, null, 0, 0, 0, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(3)), 1000L, 1000L, null, null, 5, 3, 0, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(4)), null, null, null, null, 0, 0, 0, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(5)), 5000L, null, null, 5000L, 2, 0, 0, 1),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(6)), 16000L, 16000L, 16000L, 16000L, 3, 1, 2, 52),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(7)), null, null, null, null, 0, 0, 0, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(8)), 5000L, 10000L, 15000L, 20000L, 1, 1, 1, 3),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(9)), 50000L, 25000L, null, null, 3, 3, 0, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(10)), null, null, 5000L, null, 0, 0, 1, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(11)), 7000L, 8000L, 9000L, 10000L, 4, 4, 4, 4) };

    // check that only the most recent 12 months are included
    assertThat(results.size(), is(12));
    for (int i = 0; i < 12; i++) {
      assertMttrMonth(results.get(i), expectedResults[i]);
    }
  }

  @Test
  public void testGetMttrMonthlyAverages_includeLatestData() {
    LocalDate today = new LocalDate();
    LocalDate beginningOfMonthLastYear = today.withDayOfMonth(1).minusYears(1);

    Set<String> applicationIds = PolicyViolationAggregationDataHelper.createAggregationHistory(tempEntity);

    List<MttrMonth> results = dao.getMttrMonthlyAverages(applicationIds, true);

    MttrMonth[] expectedResults = {
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(1)), 2500L, 2500L, null, null, 2, 1, 0, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(2)), null, null, null, null, 0, 0, 0, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(3)), 1000L, 1000L, null, null, 5, 3, 0, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(4)), null, null, null, null, 0, 0, 0, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(5)), 5000L, null, null, 5000L, 2, 0, 0, 1),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(6)), 16000L, 16000L, 16000L, 16000L, 3, 1, 2, 52),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(7)), null, null, null, null, 0, 0, 0, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(8)), 5000L, 10000L, 15000L, 20000L, 1, 1, 1, 3),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(9)), 50000L, 25000L, null, null, 3, 3, 0, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(10)), null, null, 5000L, null, 0, 0, 1, 0),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(11)), 7000L, 8000L, 9000L, 10000L, 4, 4, 4, 4),
        new MttrMonth(toDate(beginningOfMonthLastYear.plusMonths(12)), 4000L, 5000L, 6000L, 7000L, 2, 2, 2, 2)
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
    LocalDate aggregationStart = today.withDayOfMonth(1).minusYears(1);

    Application testApp1 = tempEntity.newApplication(Organization.ROOT_ORGANIZATION_ID);
    Application testApp2 = tempEntity.newApplication(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newPolicyViolationAggregation(testApp1.getId(), aggregationStart, //
        asList(1, 2, 3, 4), //
        asList(2, 3, 4, 5), //
        asList(3, 4, 5, 6), //
        asList(4, 5, 6, 7), //
        1);
    tempEntity.newPolicyViolationAggregation(testApp2.getId(), aggregationStart, //
        asList(3, 4, 5, 6), //
        asList(4, 5, 6, 7), //
        asList(5, 6, 7, 8), //
        asList(6, 7, 8, 9), //
        2);
    tempEntity.newPolicyViolationAggregation(testApp1.getId(), aggregationStart.plusMonths(1), //
        asList(2, 3, 4, 5), //
        asList(3, 4, 5, 6), //
        asList(4, 5, 6, 7), //
        asList(5, 6, 7, 8), //
        3);
    tempEntity.newPolicyViolationAggregation(testApp2.getId(), aggregationStart.plusMonths(1), //
        asList(4, 5, 6, 7), //
        asList(5, 6, 7, 8), //
        asList(6, 7, 8, 9), //
        asList(7, 8, 9, 10), //
        4);
    // a month without evaluations should not affect averages
    tempEntity.newPolicyViolationAggregation(testApp1.getId(), aggregationStart.plusMonths(2), //
        asList(2, 2, 2, 2), //
        asList(2, 2, 2, 2), //
        asList(2, 2, 2, 2), //
        asList(2, 2, 2, 2), //
        1);
    tempEntity.newPolicyViolationAggregation(testApp2.getId(), aggregationStart.plusMonths(2), //
        asList(0, 0, 0, 0), //
        asList(0, 0, 0, 0), //
        asList(0, 0, 0, 0), //
        asList(0, 0, 0, 0), //
        0);

    List<AverageMonth> results = dao.getMonthlyAverages(getApplicationIds(testApp1, testApp2), false);

    assertThat(results, hasSize(3));
    assertThat(results.get(0).timePeriodStart, is(aggregationStart.toDate()));
    assertThat(results.get(0).evaluationCount, is(3));
    assertAverages(results.get(0).security, 2, 3, 4, 5);
    assertAverages(results.get(0).license, 3, 4, 5, 6);
    assertAverages(results.get(0).quality, 4, 5, 6, 7);
    assertAverages(results.get(0).other, 5, 6, 7, 8);

    assertThat(results.get(1).timePeriodStart, is(aggregationStart.plusMonths(1).toDate()));
    assertThat(results.get(1).evaluationCount, is(7));
    assertAverages(results.get(1).security, 3, 4, 5, 6);
    assertAverages(results.get(1).license, 4, 5, 6, 7);
    assertAverages(results.get(1).quality, 5, 6, 7, 8);
    assertAverages(results.get(1).other, 6, 7, 8, 9);

    assertThat(results.get(2).timePeriodStart, is(aggregationStart.plusMonths(2).toDate()));
    assertThat(results.get(2).evaluationCount, is(1));
    assertAverages(results.get(2).security, 2, 2, 2, 2);
    assertAverages(results.get(2).license, 2, 2, 2, 2);
    assertAverages(results.get(2).quality, 2, 2, 2, 2);
    assertAverages(results.get(2).other, 2, 2, 2, 2);
  }

  @Test
  public void testGetMonthlyAverages_onlyLast12AggregationsCounted() {
    LocalDate today = new LocalDate();
    LocalDate aggregationStart = today.withDayOfMonth(1).minusYears(1);

    Application testApp = tempEntity.newApplication(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newPolicyViolationAggregation(testApp.getId(), aggregationStart.minusMonths(1), //
        asList(2, 3, 4, 5), //
        asList(3, 4, 5, 6), //
        asList(4, 5, 6, 7), //
        asList(5, 6, 7, 8), //
        5);
    for (int i = 0; i < 12; i++) {
      tempEntity.newPolicyViolationAggregation(testApp.getId(), aggregationStart.plusMonths(i), //
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
      assertThat(month.timePeriodStart, is(aggregationStart.plusMonths(i).toDate()));
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
    // Note the aggregation start date is 1 month later than in the test above.
    LocalDate aggregationStart = today.withDayOfMonth(1).minusMonths(11);

    Application testApp = tempEntity.newApplication(Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newPolicyViolationAggregation(testApp.getId(), aggregationStart.minusMonths(1), //
        asList(2, 3, 4, 5), //
        asList(3, 4, 5, 6), //
        asList(4, 5, 6, 7), //
        asList(5, 6, 7, 8), //
        5);
    for (int i = 0; i < 12; i++) {
      tempEntity.newPolicyViolationAggregation(testApp.getId(), aggregationStart.plusMonths(i), //
          asList(1, 2, 3, 4), //
          asList(2, 3, 4, 5), //
          asList(3, 4, 5, 6), //
          asList(4, 5, 6, 7), //
          1);
    }
    List<AverageMonth> results = dao.getMonthlyAverages(getApplicationIds(testApp), true);

    assertThat(results, hasSize(12));
    for (int i = 0; i < 12; i++) {
      AverageMonth month = results.get(i);
      assertThat(month.timePeriodStart, is(aggregationStart.plusMonths(i).toDate()));
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
}
