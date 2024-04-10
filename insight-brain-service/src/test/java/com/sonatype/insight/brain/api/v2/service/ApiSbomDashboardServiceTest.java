/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;

import javax.inject.Inject;

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
    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);

    newSbomMetadataBuilder(daoFactory).withCreatedAt(now).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneYearAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(sixMonthsAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoMonthsAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneMonthAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneWeekAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus("PENDING").build();

    SbomsAnalyzedMetricsDTO result = service.getSbomsAnalyzedMetrics();
    assertThat(result).isNotNull();
    assertThat(result.getTotal()).isEqualTo(7);
    assertThat(result.getThreshold()).isEqualTo(productLicense.getMaxSboms().longValue());
  }

  @Test
  public void testGetSbomsAnalyzedMetrics_NoSbomsAnalyzed() {
    SbomsAnalyzedMetricsDTO result = service.getSbomsAnalyzedMetrics();
    assertThat(result).isNotNull();
    assertThat(result.getTotal()).isZero();
    assertThat(result.getThreshold()).isEqualTo(productLicense.getMaxSboms().longValue());
  }
}
