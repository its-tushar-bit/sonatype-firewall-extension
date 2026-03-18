/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.EnumIntegerTable;
import com.sonatype.insight.brain.model.successmetrics.TimePeriod;
import com.sonatype.insight.brain.utils.ThreatLevel;

import com.google.common.collect.Table;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.LocalDate;

import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.model.successmetrics.TimePeriod.MONTH;
import static com.sonatype.insight.brain.model.successmetrics.TimePeriod.WEEK;
import static com.sonatype.insight.brain.utils.ThreatLevel.CRITICAL;
import static com.sonatype.insight.brain.utils.ThreatLevel.LOW;
import static com.sonatype.insight.brain.utils.ThreatLevel.MODERATE;
import static com.sonatype.insight.brain.utils.ThreatLevel.SEVERE;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyViolationAggregationDataHelper
{
  // NOTE: the last id deliberately doesn't actually have any records, and the second to last has records with
  // all 0's for every month (ie, it is an inactive application)
  public static final String[] APPLICATION_IDS = {"1", "2", "3", "4", "5", "6"};

  public static final String ORG_ID = "PVADH_ORG_ID";

  public static Set<String> createAggregationHistory(TemporaryEntity tempEntity) {
    createMonthlyAggregationHistory(tempEntity);
    createWeeklyAggregationHistory(tempEntity);
    return new HashSet<>(Arrays.asList(APPLICATION_IDS));
  }

  /**
   * Create 13 months worth of PolicyViolationAggregation records and return a set that includes all application
   * ids that are present in the created data
   */
  private static void createMonthlyAggregationHistory(TemporaryEntity tempEntity) {
    LocalDate today = new LocalDate();

    LocalDate beginningOfMonth;

    // the beginning of the month that we are currently adding records to. Start 13 months ago so we can
    // test the logic that limits the returned data to just 12 months
    beginningOfMonth = minusTimePeriod(today, MONTH, 13);
    Date beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{5000, 6000, 7000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{40000}), //
        new DescriptiveStatistics(new double[]{2000, 2000}),
        discovered().get(), //
        fixed().security(1, 0, 1, 1).get(), //
        waived().license(2, 0, 0, 1).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{1000}), //
        new DescriptiveStatistics(new double[]{3000}), //
        new DescriptiveStatistics(new double[]{2000}), //
        new DescriptiveStatistics(new double[]{2000}), //
        discovered().license(0, 0, 0, 1).quality(0, 0, 0, 2).other(0, 0, 0, 4).get(), //
        fixed().security(1, 0, 0, 0).license(0, 1, 0, 0).get(), //
        waived().quality(0, 0, 1, 0).other(0, 0, 0, 1).get(), //
        openWithSampleData().get(), //
        4);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{2000}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(0, 0, 0, 1).quality(1, 1, 0, 0).other(0, 0, 0, 2).get(), //
        fixed().security(0, 0, 1, 0).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{2000, 3000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(0, 0, 0, 1).quality(0, 0, 0, 2).other(1, 1, 1, 3).get(), //
        fixed().security(1, 0, 0, 0).get(), //
        waived().security(1, 0, 0, 0).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{2500}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(0, 0, 0, 1).quality(0, 0, 0, 2).other(0, 2, 1, 3).get(), //
        fixed().license(0, 1, 0, 0).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(1, 0, 0, 0).license(1, 0, 0, 1).quality(0, 0, 0, 3).other(0, 0, 0, 3).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{300, 1700}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(1, 0, 1, 0).license(0, 1, 0, 0).quality(0, 3, 0, 2).other(0, 0, 0, 1).get(), //
        fixed().license(0, 1, 0, 0).get(), //
        waived().quality(0, 1, 0, 0).get(), //
        openWithSampleData().get(), //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{1000}), //
        new DescriptiveStatistics(new double[]{1000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(1, 1, 0, 0).quality(0, 0, 0, 4).other(0, 0, 0, 2).get(), //
        fixed().security(1, 0, 0, 0).get(), //
        waived().security(0, 1, 0, 0).get(), //
        openWithSampleData().get(), //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{1000, 1000, 1000, 1000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(0, 0, 4, 0).license(0, 0, 0, 3).other(0, 0, 0, 3).get(), //
        fixed().security(2, 0, 0, 0).get(), //
        waived().quality(1, 0, 0, 0).other(1, 0, 0, 0).get(), //
        openWithSampleData().get(), //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(1, 1, 1, 1).quality(1, 0, 0, 3).other(0, 1, 1, 0).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{5000}), //
        discovered().security(0, 0, 1, 0).license(0, 0, 0, 1).quality(1, 0, 0, 4).other(1, 4, 1, 3).get(), //
        fixed().security(0, 0, 0, 1).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{5000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(0, 0, 2, 0).license(0, 0, 0, 1).quality(0, 0, 0, 2).other(0, 3, 0, 3).get(), //
        fixed().get(), //
        waived().other(1, 0, 0, 0).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{5000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(2, 1, 0, 0).license(0, 0, 0, 1).quality(0, 2, 0, 0).other(0, 0, 0, 3).get(), //
        fixed().get(), //
        waived().other(1, 0, 0, 0).get(), //
        openWithSampleData().get(), //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{8000, 16000, 24000}), //
        new DescriptiveStatistics(new double[]{16000}), //
        new DescriptiveStatistics(new double[]{8000, 24000}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(0, 1, 0, 0).license(0, 0, 0, 1).quality(1, 1, 1, 1).other(3, 0, 0, 2).get(), //
        fixed().other(3, 0, 0, 0).quality(0, 1, 0, 0).get(), //
        waived().license(0, 0, 2, 0).get(), //
        openWithSampleData().get(), //
        1000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{
          16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000,
          16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000,
          16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000, 16000,
          16000, 16000, 16000, 16000, 16000, 16000, 16000
        }), //
        discovered().security(0, 2, 0, 0).quality(0, 0, 0, 2).other(0, 0, 0, 3).get(), //
        fixed().get(), //
        waived().license(0, 0, 0, 52).get(), //
        openWithSampleData().get(), //
        2000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(0, 0, 0, 2).quality(1, 2, 3, 0).other(0, 0, 0, 7).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        3000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(0, 0, 0, 1).quality(0, 0, 0, 4).other(2, 0, 0, 5).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(0, 1, 1, 1).quality(1, 0, 1, 0).other(0, 0, 0, 5).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{5000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{10000}), //
        discovered().security(0, 1, 0, 0).license(0, 1, 0, 1).quality(0, 0, 1, 1).other(0, 2, 0, 0).get(), //
        fixed().other(1, 0, 0, 0).get(), //
        waived().security(0, 0, 0, 1).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{15000}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(1, 0, 0, 0).license(0, 1, 0, 1).quality(0, 1, 0, 1).other(0, 0, 0, 4).get(), //
        fixed().license(0, 0, 1, 0).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{10000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{20000}), //
        discovered().get(), //
        fixed().license(0, 1, 0, 1).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{30000}), //
        discovered().get(), //
        fixed().security(0, 0, 0, 1).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{50000}), //
        new DescriptiveStatistics(new double[]{12500, 37500}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(1, 0, 1, 0).license(0, 0, 3, 1).quality(0, 2, 0, 4).other(0, 1, 0, 6).get(), //
        fixed().get(), //
        waived().quality(1, 2, 0, 0).get(), //
        openWithSampleData().get(), //
        4);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{50000, 50000}), //
        new DescriptiveStatistics(new double[]{25000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(0, 0, 2, 0).license(1, 1, 1, 1).quality(0, 0, 0, 4).other(0, 1, 0, 4).get(), //
        fixed().security(0, 1, 0, 0).get(), //
        waived().other(2, 0, 0, 0).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(0, 0, 1, 0).license(0, 0, 2, 1).quality(1, 0, 0, 2).other(0, 0, 0, 3).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{5000}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().security(0, 0, 1, 0).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    // add some data for current month to test option for including latest data
    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{1000}), //
        new DescriptiveStatistics(new double[]{2000}), //
        new DescriptiveStatistics(new double[]{3000}), //
        new DescriptiveStatistics(new double[]{4000}), //
        discovered().security(0, 0, 2, 0).quality(3, 0, 0, 0).other(0, 0, 0, 2).get(), //
        fixed().quality(1, 1, 1, 1).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{5000}), //
        new DescriptiveStatistics(new double[]{6000}), //
        new DescriptiveStatistics(new double[]{7000}), //
        new DescriptiveStatistics(new double[]{8000}), //
        discovered().security(0, 0, 2, 0).other(0, 0, 0, 3).get(), //
        fixed().get(), //
        waived().quality(1, 1, 1, 1).get(), //
        openWithSampleData().get(), //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{9000}), //
        new DescriptiveStatistics(new double[]{10000}), //
        new DescriptiveStatistics(new double[]{11000}), //
        new DescriptiveStatistics(new double[]{12000}), //
        discovered().security(0, 0, 2, 0).license(0, 0, 0, 3).quality(0, 0, 1, 0).other(0, 0, 0, 5).get(), //
        fixed().security(1, 1, 0, 0).get(), //
        waived().security(0, 0, 1, 1).get(), //
        openWithSampleData().get(), //
        3);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, MONTH, //
        new DescriptiveStatistics(new double[]{13000}), //
        new DescriptiveStatistics(new double[]{14000}), //
        new DescriptiveStatistics(new double[]{15000}), //
        new DescriptiveStatistics(new double[]{16000}), //
        discovered().security(0, 0, 2, 0).license(0, 0, 0, 1).quality(0, 0, 0, 4).other(0, 0, 0, 2).get(), //
        fixed().quality(1, 1, 0, 0).get(), //
        waived().security(0, 0, 1, 1).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    // add some data for current month to test option for including latest data
    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    Date now = new Date();
    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, now, MONTH, //
        new DescriptiveStatistics(new double[]{3000}), //
        new DescriptiveStatistics(new double[]{4000}), //
        new DescriptiveStatistics(new double[]{5000}), //
        new DescriptiveStatistics(new double[]{6000}), //
        discovered().security(1, 2, 3, 4).license(1, 2, 3, 4).quality(1, 2, 3, 4).other(1, 2, 3, 4).get(), //
        fixed().security(1, 1, 0, 0).get(), //
        waived().security(0, 0, 1, 1).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, now, MONTH, //
        new DescriptiveStatistics(new double[]{5000}), //
        new DescriptiveStatistics(new double[]{6000}), //
        new DescriptiveStatistics(new double[]{7000}), //
        new DescriptiveStatistics(new double[]{8000}), //
        discovered().security(3, 4, 5, 6).license(3, 4, 5, 6).quality(3, 4, 5, 6).other(3, 4, 5, 6).get(), //
        fixed().security(1, 1, 0, 0).get(), //
        waived().security(0, 0, 1, 1).get(), //
        openWithSampleData().get(), //
        3);

    // sanity check
    assertThat(beginningOfMonth).isEqualTo(today.withDayOfMonth(1));

    Organization org = tempEntity.newOrganizationWithSpecificId(ORG_ID);
    for (String appId : APPLICATION_IDS) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId + "-publicId", org.getId());
    }
  }

  /**
   * Create 13 weeks worth of PolicyViolationAggregation records and return a set that includes all application
   * ids that are present in the created data
   */
  private static void createWeeklyAggregationHistory(TemporaryEntity tempEntity) {
    LocalDate today = new LocalDate();

    LocalDate beginningOfWeek;

    // the beginning of the week that we are currently adding records to. Start 13 weeks ago so we can
    // test the logic that limits the returned data to just 12 weeks
    beginningOfWeek = minusTimePeriod(today, WEEK, 13);
    Date beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{10000, 12000, 14000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{80000}), //
        new DescriptiveStatistics(new double[]{4000, 4000}),
        discovered().get(), //
        fixed().security(1, 0, 1, 1).get(), //
        waived().license(2, 0, 0, 1).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{2000}), //
        new DescriptiveStatistics(new double[]{4000}), //
        new DescriptiveStatistics(new double[]{3000}), //
        new DescriptiveStatistics(new double[]{3000}), //
        discovered().license(0, 0, 0, 1).quality(0, 0, 0, 2).other(0, 0, 0, 4).get(), //
        fixed().security(2, 0, 0, 0).license(0, 1, 0, 0).get(), //
        waived().quality(0, 0, 1, 0).other(0, 0, 0, 1).get(), //
        openWithSampleData().get(), //
        4);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{3000}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(0, 0, 0, 1).quality(1, 1, 0, 0).other(0, 0, 0, 2).get(), //
        fixed().security(0, 0, 1, 0).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{4000, 6000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(0, 0, 0, 1).quality(0, 0, 0, 2).other(1, 1, 1, 3).get(), //
        fixed().security(1, 0, 0, 0).get(), //
        waived().security(1, 0, 0, 0).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{5000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(0, 0, 0, 1).quality(0, 0, 0, 2).other(0, 2, 1, 3).get(), //
        fixed().license(0, 1, 0, 0).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(1, 0, 0, 0).license(1, 0, 0, 1).quality(0, 0, 0, 3).other(0, 0, 0, 3).get(), //
        fixed().get(), //
        waived().security(1, 0, 0, 0).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{300, 1700}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(1, 0, 1, 0).license(0, 1, 0, 0).quality(0, 3, 0, 2).other(0, 0, 0, 1).get(), //
        fixed().license(0, 1, 0, 0).get(), //
        waived().quality(0, 1, 0, 0).get(), //
        openWithSampleData().get(), //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{1000}), //
        new DescriptiveStatistics(new double[]{4000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(1, 1, 0, 0).quality(0, 0, 0, 4).other(0, 0, 0, 2).get(), //
        fixed().security(1, 0, 0, 0).get(), //
        waived().security(0, 1, 0, 0).get(), //
        openWithSampleData().get(), //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{1000, 1000, 1000, 1000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(0, 0, 4, 0).license(0, 0, 0, 3).other(0, 0, 0, 3).get(), //
        fixed().security(2, 1, 0, 0).get(), //
        waived().quality(1, 0, 0, 0).other(1, 0, 0, 0).get(), //
        openWithSampleData().get(), //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(1, 1, 1, 1).quality(1, 0, 0, 3).other(0, 1, 1, 0).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{3000}), //
        discovered().security(0, 0, 1, 0).license(0, 0, 0, 1).quality(1, 0, 0, 4).other(1, 4, 1, 3).get(), //
        fixed().security(0, 0, 0, 1).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{3000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(0, 0, 2, 0).license(0, 0, 0, 1).quality(0, 0, 0, 2).other(0, 3, 0, 3).get(), //
        fixed().get(), //
        waived().other(1, 0, 0, 0).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{3000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(2, 1, 0, 0).license(0, 0, 0, 1).quality(0, 2, 0, 0).other(0, 0, 0, 3).get(), //
        fixed().get(), //
        waived().other(1, 0, 0, 0).get(), //
        openWithSampleData().get(), //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{4000, 8000, 12000}), //
        new DescriptiveStatistics(new double[]{8000}), //
        new DescriptiveStatistics(new double[]{4000, 12000}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(0, 1, 0, 0).license(0, 0, 0, 1).quality(1, 1, 1, 1).other(3, 0, 0, 2).get(), //
        fixed().other(3, 0, 0, 0).quality(0, 1, 0, 0).get(), //
        waived().license(0, 0, 2, 0).get(), //
        openWithSampleData().get(), //
        1000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{
          8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000,
          8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000,
          8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000, 8000,
          8000, 8000, 8000, 8000, 8000, 8000, 8000
        }), //
        discovered().security(0, 2, 0, 0).quality(0, 0, 0, 2).other(0, 0, 0, 3).get(), //
        fixed().get(), //
        waived().license(0, 0, 0, 52).get(), //
        openWithSampleData().get(), //
        2000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(0, 0, 0, 2).quality(1, 2, 3, 0).other(0, 0, 0, 7).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        3000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(0, 0, 0, 1).quality(0, 0, 0, 4).other(2, 0, 0, 5).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().license(0, 1, 1, 1).quality(1, 0, 1, 0).other(0, 0, 0, 5).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{10000}), //
        discovered().security(0, 1, 0, 0).license(0, 1, 0, 1).quality(0, 0, 1, 1).other(0, 2, 0, 0).get(), //
        fixed().other(0, 0, 0, 0).get(), //
        waived().security(0, 0, 0, 1).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{15000}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(1, 0, 0, 0).license(0, 1, 0, 1).quality(0, 1, 0, 1).other(0, 0, 0, 4).get(), //
        fixed().license(0, 0, 1, 0).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{10000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{20000}), //
        discovered().get(), //
        fixed().license(0, 1, 0, 1).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{30000}), //
        discovered().get(), //
        fixed().security(0, 0, 0, 1).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{25000}), //
        new DescriptiveStatistics(new double[]{12500, 37500}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(1, 0, 1, 0).license(0, 0, 3, 1).quality(0, 2, 0, 4).other(0, 1, 0, 6).get(), //
        fixed().get(), //
        waived().quality(1, 2, 0, 0).get(), //
        openWithSampleData().get(), //
        4);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{25000, 25000}), //
        new DescriptiveStatistics(new double[]{25000}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(0, 0, 2, 0).license(1, 1, 1, 1).quality(0, 0, 0, 4).other(0, 1, 0, 4).get(), //
        fixed().security(1, 2, 0, 0).get(), //
        waived().other(3, 1, 0, 0).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().security(0, 0, 1, 0).license(0, 0, 2, 1).quality(1, 0, 0, 2).other(0, 0, 0, 3).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{2500}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().security(0, 0, 1, 0).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        new DescriptiveStatistics(new double[]{}), //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    // add some data for current week to test option for including latest data
    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{2000}), //
        new DescriptiveStatistics(new double[]{3000}), //
        new DescriptiveStatistics(new double[]{4000}), //
        new DescriptiveStatistics(new double[]{5000}), //
        discovered().security(0, 0, 2, 0).quality(3, 0, 0, 0).other(0, 0, 0, 2).get(), //
        fixed().quality(1, 1, 1, 1).get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{6000}), //
        new DescriptiveStatistics(new double[]{7000}), //
        new DescriptiveStatistics(new double[]{8000}), //
        new DescriptiveStatistics(new double[]{9000}), //
        discovered().security(0, 0, 2, 0).other(0, 0, 0, 3).get(), //
        fixed().get(), //
        waived().quality(1, 1, 1, 1).get(), //
        openWithSampleData().get(), //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{10000}), //
        new DescriptiveStatistics(new double[]{11000}), //
        new DescriptiveStatistics(new double[]{12000}), //
        new DescriptiveStatistics(new double[]{13000}), //
        discovered().security(0, 0, 2, 0).license(0, 0, 0, 3).quality(0, 0, 1, 0).other(0, 0, 0, 5).get(), //
        fixed().security(1, 1, 0, 0).get(), //
        waived().security(0, 0, 1, 1).get(), //
        openWithSampleData().get(), //
        3);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfWeekDate, WEEK, //
        new DescriptiveStatistics(new double[]{14000}), //
        new DescriptiveStatistics(new double[]{15000}), //
        new DescriptiveStatistics(new double[]{16000}), //
        new DescriptiveStatistics(new double[]{17000}), //
        discovered().security(0, 0, 2, 0).license(0, 0, 0, 1).quality(0, 0, 0, 4).other(0, 0, 0, 2).get(), //
        fixed().quality(1, 1, 0, 0).get(), //
        waived().security(0, 0, 1, 1).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    // add some data for current week to test option for including latest data
    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    Date now = new Date();
    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, now, WEEK, //
        new DescriptiveStatistics(new double[]{4000}), //
        new DescriptiveStatistics(new double[]{5000}), //
        new DescriptiveStatistics(new double[]{6000}), //
        new DescriptiveStatistics(new double[]{7000}), //
        discovered().security(1, 2, 3, 4).license(1, 2, 3, 4).quality(1, 2, 3, 4).other(1, 2, 3, 4).get(), //
        fixed().security(1, 1, 0, 0).get(), //
        waived().security(0, 0, 1, 1).get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, now, WEEK, //
        new DescriptiveStatistics(new double[]{6000}), //
        new DescriptiveStatistics(new double[]{7000}), //
        new DescriptiveStatistics(new double[]{8000}), //
        new DescriptiveStatistics(new double[]{9000}), //
        discovered().security(3, 4, 5, 6).license(3, 4, 5, 6).quality(3, 4, 5, 6).other(3, 4, 5, 6).get(), //
        fixed().security(1, 1, 0, 0).get(), //
        waived().security(0, 0, 1, 1).get(), //
        openWithSampleData().get(), //
        3);

    // sanity check
    assertThat(beginningOfWeek).isEqualTo(today.withDayOfWeek(1));
  }

  public static Set<String> createApplicationCountAggregationHistory(TemporaryEntity tempEntity) {
    Organization org = tempEntity.newOrganizationWithSpecificId(ORG_ID);
    for (String appId : APPLICATION_IDS) {
      tempEntity.newApplicationWithSpecificId(appId, "app-" + appId, appId, org.getId());
    }

    createMonthlyApplicationCountAggregationHistory(tempEntity);
    createWeeklyApplicationCountAggregationHistory(tempEntity);
    return new HashSet<>(Arrays.asList(APPLICATION_IDS));
  }

  /**
   * Create 13 months worth of PolicyViolationAggregation records and return a set that includes all application ids
   * that are present in the created data. This data, unlike the data above, has different applications varying
   * in whether they have different types of threats, and is therefore more suitable for testing the application count
   * parts of the Success Metrics charts
   */
  private static void createMonthlyApplicationCountAggregationHistory(TemporaryEntity tempEntity) {
    LocalDate today = new LocalDate();

    LocalDate beginningOfMonth;

    // the beginning of the month that we are currently adding records to. Start 13 months ago so we can
    // test the logic that limits the returned data to just 12 months
    beginningOfMonth = minusTimePeriod(today, MONTH, 13);
    Date beginningOfMonthDate = toDate(beginningOfMonth);

    DescriptiveStatistics emptyStats = new DescriptiveStatistics();

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(0, 0, 0, 1).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(1, 0, 0, 0).get(), //
        fixed().get(), //
        waived().get(), //
        open().get(), //
        5);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(0, 5, 0, 0).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(0, 0, 0, 1).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(0, 0, 0, 1).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        3);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        2000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        3000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().license(0, 0, 0, 1).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(0, 0, 3, 0).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        4);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().license(0, 3, 0, 1).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().license(0, 3, 0, 0).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().quality(0, 0, 7, 0).other(0, 0, 0, 8).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        3);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfMonthDate, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().license(0, 3, 0, 0).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        4);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfMonthDate, MONTH);

    // add some data for current month to test option for including latest data
    beginningOfMonth = plusTimePeriod(beginningOfMonth, MONTH, 1);
    beginningOfMonthDate = toDate(beginningOfMonth);

    Date now = new Date();
    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfMonthDate, now, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(1, 0, 0, 0).license(0, 2, 0, 0).quality(0, 0, 3, 0).other(0, 0, 0, 4).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfMonthDate, now, MONTH, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(5, 0, 0, 0).license(0, 6, 0, 0).quality(0, 0, 7, 0).other(0, 0, 0, 8).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        3);

    // sanity check
    assertThat(beginningOfMonth).isEqualTo(withDayOfTimePeriod(today, MONTH, 1));
  }

  /**
   * Create 13 weeks worth of PolicyViolationAggregation records and return a set that includes all application ids
   * that are present in the created data. This data, unlike the data above, has different applications varying
   * in whether they have different types of threats, and is therefore more suitable for testing the application count
   * parts of the Success Metrics charts
   */
  private static void createWeeklyApplicationCountAggregationHistory(TemporaryEntity tempEntity) {
    LocalDate today = new LocalDate();

    LocalDate beginningOfWeek;

    // the beginning of the week that we are currently adding records to. Start 13 weeks ago so we can
    // test the logic that limits the returned data to just 12 weeks
    beginningOfWeek = minusTimePeriod(today, WEEK, 13);
    Date beginningOfWeekDate = toDate(beginningOfWeek);

    DescriptiveStatistics emptyStats = new DescriptiveStatistics();

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(0, 0, 0, 1).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(1, 0, 0, 0).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        5);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().quality(1, 0, 0, 0).other(0, 0, 0, 1).security(0, 5, 0, 0).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(0, 0, 0, 1).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        10);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(0, 0, 0, 1).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(0, 0, 0, 1).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        3);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        2000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        3000);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().license(0, 0, 0, 1).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(0, 0, 3, 0).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        4);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().license(0, 3, 0, 1).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().license(0, 3, 0, 0).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().quality(0, 0, 7, 0).other(0, 0, 0, 8).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        0);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        2);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[2], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        3);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[3], beginningOfWeekDate, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().license(1, 0, 0, 1).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        4);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[4], beginningOfWeekDate, WEEK);

    // add some data for current week to test option for including latest data
    beginningOfWeek = plusTimePeriod(beginningOfWeek, WEEK, 1);
    beginningOfWeekDate = toDate(beginningOfWeek);

    Date now = new Date();
    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[0], beginningOfWeekDate, now, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(1, 0, 0, 0).license(0, 2, 0, 0).quality(0, 0, 3, 0).other(0, 0, 0, 4).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        1);

    tempEntity.newPolicyViolationAggregation(APPLICATION_IDS[1], beginningOfWeekDate, now, WEEK, //
        emptyStats, emptyStats, emptyStats, emptyStats, //
        discovered().security(5, 0, 0, 0).license(0, 6, 0, 0).quality(0, 0, 7, 1).other(0, 0, 0, 8).get(), //
        fixed().get(), //
        waived().get(), //
        openWithSampleData().get(), //
        3);

    // sanity check
    assertThat(beginningOfWeek).isEqualTo(withDayOfTimePeriod(today, WEEK, 1));
  }

  // These methods exist to improve readability of client code.
  public static CountsBuilder discovered() {
    return new CountsBuilder();
  }

  public static CountsBuilder fixed() {
    return new CountsBuilder();
  }

  public static CountsBuilder waived() {
    return new CountsBuilder();
  }

  public static CountsBuilder open() {
    return new CountsBuilder();
  }

  public static CountsBuilder openWithSampleData() {
    return new CountsBuilder().security(1, 0, 1, 1).license(0, 3, 2, 1).quality(5, 0, 0, 0).other(0, 3, 0, 2);
  }

  public static class CountsBuilder
  {
    private final Table<PolicyThreatCategory, ThreatLevel, Integer> table = new EnumIntegerTable<>(
        PolicyThreatCategory.class, ThreatLevel.class);

    private CountsBuilder put(
        PolicyThreatCategory category,
        Integer countLow,
        Integer countModerate,
        Integer countSevere,
        Integer countCritical)
    {
      this.table.put(category, LOW, countLow);
      this.table.put(category, MODERATE, countModerate);
      this.table.put(category, SEVERE, countSevere);
      this.table.put(category, CRITICAL, countCritical);
      return this;
    }

    public CountsBuilder security(Integer countLow, Integer countModerate, Integer countSevere, Integer countCritical) {
      return put(SECURITY, countLow, countModerate, countSevere, countCritical);
    }

    public CountsBuilder license(Integer countLow, Integer countModerate, Integer countSevere, Integer countCritical) {
      return put(LICENSE, countLow, countModerate, countSevere, countCritical);
    }

    public CountsBuilder quality(Integer countLow, Integer countModerate, Integer countSevere, Integer countCritical) {
      return put(QUALITY, countLow, countModerate, countSevere, countCritical);
    }

    public CountsBuilder other(Integer countLow, Integer countModerate, Integer countSevere, Integer countCritical) {
      return put(OTHER, countLow, countModerate, countSevere, countCritical);
    }

    public Table<PolicyThreatCategory, ThreatLevel, Integer> get() {
      return this.table;
    }

    public Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> asMap() {
      return this.table.rowMap();
    }
  }

  public static OpenCountsBuilder openCounts(
      int countSecurity,
      int countLicense,
      int countQuality,
      int countOther)
  {
    return new OpenCountsBuilder(countSecurity, countLicense, countQuality, countOther);
  }

  public static class OpenCountsBuilder
  {
    private final Map<PolicyThreatCategory, Integer> map;

    private OpenCountsBuilder(
        Integer countSecurity,
        Integer countLicense,
        Integer countQuality,
        Integer countOther)
    {
      map = new EnumMap<>(PolicyThreatCategory.class);

      map.put(SECURITY, countSecurity);
      map.put(LICENSE, countLicense);
      map.put(QUALITY, countQuality);
      map.put(OTHER, countOther);
    }

    public Map<PolicyThreatCategory, Integer> get() {
      return this.map;
    }
  }

  private static Date toDate(LocalDate date) {
    return date.toDateTimeAtStartOfDay().toDate();
  }

  public static LocalDate withDayOfTimePeriod(LocalDate dateTime, TimePeriod timePeriod, int dayOf) {
    return dateTime.withField(timePeriod.getDateTimeFieldType(), dayOf);
  }

  /**
   * This method sets the dateTime to the beginning of specified timePeriod and adds the number of timePeriods
   * requested.
   *
   * @param dateTime The dateTime to be added to.
   * @param timePeriod The timePeriod type to be added.
   * @param timePeriods The number of timePeriods to be added.
   */
  public static LocalDate plusTimePeriod(LocalDate dateTime, TimePeriod timePeriod, int timePeriods) {
    return dateTime.withField(timePeriod.getDateTimeFieldType(), 1).plus(timePeriod.getPeriod(timePeriods));
  }

  /**
   * This method sets the dateTime to the beginning of specified timePeriod and subtracts the number of timePeriods
   * requested.
   *
   * @param dateTime The dateTime to be added to.
   * @param timePeriod The timePeriod type to be added.
   * @param timePeriods The number of timePeriods to be added.
   */
  public static LocalDate minusTimePeriod(LocalDate dateTime, TimePeriod timePeriod, int timePeriods) {
    return dateTime.withField(timePeriod.getDateTimeFieldType(), 1).minus(timePeriod.getPeriod(timePeriods));
  }
}
