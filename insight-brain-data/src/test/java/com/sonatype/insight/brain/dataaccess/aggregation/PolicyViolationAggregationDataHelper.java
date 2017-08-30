/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.aggregation;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PolicyViolationAggregationDataHelper
{
  // NOTE: the last id deliberately doesn't actually have any records, and the second to last has records with
  // all 0's for every month (ie, it is an inactive application)
  public static final String[] APPLICATION_IDS = { "1", "2", "3", "4", "5", "6" };

  /**
   * Create 13 months worth of PolicyViolationAggregation records and return a set that includes all application ids
   * that are present in the created data
   */
  public static Set<String> createAggregationHistory(TemporaryEntity tempEntity) {
    LocalDate today = new LocalDate();

    // the beginning of the month that we are currently adding records to. Start 13 months ago so we can test
    // the logic that limits the returned data to just 12 months
    LocalDate beginningOfMonth = today.withDayOfMonth(1).minusMonths(13);
    Date beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 5000, 6000, 7000 }), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] { 40000 }), //
        new DescriptiveStatistics(new double[] { 2000, 2000 }));

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 1000 }), //
        new DescriptiveStatistics(new double[] { 3000 }), //
        new DescriptiveStatistics(new double[] { 2000 }), //
        new DescriptiveStatistics(new double[] { 2000 }), //
        1, 2, 3, 4, //
        2, 3, 4, 5, //
        3, 4, 5, 6, //
        4, 5, 6, 7, //
        5);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] { 2000 }), //
        new DescriptiveStatistics(new double[] {}), //
        3, 4, 5, 6, //
        4, 5, 6, 7, //
        5, 6, 7, 8, //
        6, 7, 8, 9, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 2000, 3000 }), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        1, 1, 1, 1, //
        2, 2, 2, 2, //
        3, 3, 3, 3, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] { 2500 }), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        2, 2, 2, 2, //
        3, 3, 3, 3, //
        4, 4, 4, 4, //
        5, 5, 5, 5, //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] { 300, 1700 }), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        15, 15, 15, 15, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 1000 }), //
        new DescriptiveStatistics(new double[] { 1000 }), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        15, 15, 15, 15, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 1000, 1000, 1000, 1000 }), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        15, 15, 15, 15, //
        0, 0, 0, 0, //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] { 5000 }), //
        6, 0, 0, 0, //
        6, 0, 0, 0, //
        6, 0, 0, 0, //
        6, 0, 0, 0, //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 5000 }), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        3, 0, 0, 0, //
        3, 0, 0, 0, //
        3, 0, 0, 0, //
        3, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 5000 }), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        9, 0, 0, 0, //
        9, 0, 0, 0, //
        9, 0, 0, 0, //
        9, 0, 0, 0, //
        3);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 8000, 16000, 24000 }), //
        new DescriptiveStatistics(new double[] { 16000 }), //
        new DescriptiveStatistics(new double[] { 8000, 24000 }), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {
            16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000,
            16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000,
            16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000,
            16000, 16000, 16000, 16000, 16000, 16000, 16000
        }), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        2000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        3000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 5000 }), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] { 10000 }), //
        0, 0, 0, 4, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] { 15000 }), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 4, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] { 10000 }), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] { 20000 }), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 4, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] { 30000 }), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 4, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 50000 }), //
        new DescriptiveStatistics(new double[] { 12500, 37500 }), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        4, 4, 4, 4, //
        4, 4, 4, 4, //
        4, 4, 4, 4, //
        4, 4, 4, 4, //
        4);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 50000, 50000 }), //
        new DescriptiveStatistics(new double[] { 25000 }), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] { 5000 }), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        new DescriptiveStatistics(new double[] {}), //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 1000 }), //
        new DescriptiveStatistics(new double[] { 2000 }), //
        new DescriptiveStatistics(new double[] { 3000 }), //
        new DescriptiveStatistics(new double[] { 4000 }), //
        1, 2, 3, 4, //
        1, 2, 3, 4, //
        1, 2, 3, 4, //
        1, 2, 3, 4, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 5000 }), //
        new DescriptiveStatistics(new double[] { 6000 }), //
        new DescriptiveStatistics(new double[] { 7000 }), //
        new DescriptiveStatistics(new double[] { 8000 }), //
        2, 3, 4, 5, //
        2, 3, 4, 5, //
        2, 3, 4, 5, //
        2, 3, 4, 5, //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 9000 }), //
        new DescriptiveStatistics(new double[] { 10000 }), //
        new DescriptiveStatistics(new double[] { 11000 }), //
        new DescriptiveStatistics(new double[] { 12000 }), //
        3, 4, 5, 6, //
        3, 4, 5, 6, //
        3, 4, 5, 6, //
        3, 4, 5, 6, //
        3);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, //
        new DescriptiveStatistics(new double[] { 13000 }), //
        new DescriptiveStatistics(new double[] { 14000 }), //
        new DescriptiveStatistics(new double[] { 15000 }), //
        new DescriptiveStatistics(new double[] { 16000 }), //
        4, 5, 6, 7, //
        4, 5, 6, 7, //
        4, 5, 6, 7, //
        4, 5, 6, 7, //
        4);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    // sanity check
    assertThat(beginningOfMonth.plusMonths(1), is(today.withDayOfMonth(1)));

    return new HashSet<>(Arrays.asList(APPLICATION_IDS));
  }

  /**
   * Create 13 months worth of PolicyViolationAggregation records and return a set that includes all application ids
   * that are present in the created data. This data, unlike the data above, has different applications varying
   * in whether they have different types of threats, and is therefore more suitable for testing the application count
   * parts of the Success Metrics charts
   */
  public static Set<String> createApplicationCountAggregationHistory(TemporaryEntity tempEntity) {
    LocalDate today = new LocalDate();

    // the beginning of the month that we are currently adding records to. Start 13 months ago so we can test
    // the logic that limits the returned data to just 12 months
    LocalDate beginningOfMonth = today.withDayOfMonth(1).minusMonths(13);
    Date beginningOfMonthDate = toDate(beginningOfMonth);
    DescriptiveStatistics emptyStats = new DescriptiveStatistics();

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 1, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        1, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        5);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 5, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 1, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 1, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        3);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        2000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        3000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 1, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 3, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        4);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 3, 0, 1, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 3, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 7, 0, //
        0, 0, 0, 8, //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    beginningOfMonth = beginningOfMonth.plusMonths(1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        3);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        0, 0, 0, 0, //
        0, 3, 0, 0, //
        0, 0, 0, 0, //
        0, 0, 0, 0, //
        4);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate);

    // sanity check
    assertThat(beginningOfMonth.plusMonths(1), is(today.withDayOfMonth(1)));

    return new HashSet<>(Arrays.asList(APPLICATION_IDS));
  }

  private static Date toDate(LocalDate date) {
    return date.toDateTimeAtStartOfDay().toDate();
  }
}
