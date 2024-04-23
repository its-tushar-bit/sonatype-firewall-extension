/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.api.v2.dto.SbomsAnalyzedMetricsDTO;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import static com.sonatype.insight.brain.utils.SbomMetadataBuilder.newSbomMetadataBuilder;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiSbomDashboardServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiSbomDashboardService service;

  @Inject
  private ProductLicense productLicense;

  @Test
  public void testGetSbomsAnalyzedMetrics() {
    insertSbomData();
    SbomsAnalyzedMetricsDTO result = service.getSbomsAnalyzedMetrics();
    assertThat(result).isNotNull();
    assertThat(result.getTotal()).isEqualTo(8);
    assertThat(result.getThreshold()).isEqualTo(productLicense.getMaxSboms().longValue());
  }

  @Test
  public void testGetSbomsAnalyzedMetrics_NoSbomsAnalyzed() {
    SbomsAnalyzedMetricsDTO result = service.getSbomsAnalyzedMetrics();
    assertThat(result).isNotNull();
    assertThat(result.getTotal()).isZero();
    assertThat(result.getThreshold()).isEqualTo(productLicense.getMaxSboms().longValue());
  }

  @Test
  public void testGetSbomsHistoryMetrics() {
    insertSbomData();
    ApiSbomApplicationsHistoryMetricDTO result = service.getApplicationsHistoryMetric();
    assertThat(result).isNotNull();
    assertThat(result.totalScannedApplications).isEqualTo(8);
    assertThat(result.applicationsUpdatedLastYear).isEqualTo(7);
    assertThat(result.applicationsUpdatedLastMonth).isEqualTo(4);
    assertThat(result.applicationsUpdatedLastWeek).isEqualTo(3);
  }

  @Test
  public void testGetSbomsHistoryMetrics_NotMetrics() {
    ApiSbomApplicationsHistoryMetricDTO result = service.getApplicationsHistoryMetric();
    assertThat(result).isNotNull();
    assertThat(result.totalScannedApplications).isZero();
    assertThat(result.applicationsUpdatedLastYear).isZero();
    assertThat(result.applicationsUpdatedLastMonth).isZero();
    assertThat(result.applicationsUpdatedLastWeek).isZero();
  }

  private void insertSbomData() {
    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date twoYearsAgo = DateUtils.addYears(now, -2);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);

    newSbomMetadataBuilder(daoFactory).withCreatedAt(now).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneYearAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoYearsAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(sixMonthsAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoMonthsAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneMonthAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneWeekAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus("PENDING").build();
  }
}
