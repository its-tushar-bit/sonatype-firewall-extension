/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.metrics;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MetricsResultTest
{
  private static final String SQL_CALL_KEY = "jpaSqlCalls";

  private static final String SQL_CALL_VALUE = "3000000000";

  private static final String FREE_MEMORY_SIZE = "1842";

  private static final String FILE_DESCRIPTORS_USED = "0.64453125";

  private static final String FILE_DESCRIPTORS_USED_KEY = "fileDescriptorsUsed (%)";

  private static final String FREE_MEMORY_SIZE_KEY = "freePhysicalMemorySize (MB)";

  private static final String METRICS_STRING = "{ \"version\": \"4.0.0\", \"gauges\": { " +
      "\"committedVirtualMemorySize (GB)\": { \"value\": 9.78082275390625  }, " +
      "\"" + FILE_DESCRIPTORS_USED_KEY + "\": { \"value\":  " + FILE_DESCRIPTORS_USED + " }, " +
      "\"" + FREE_MEMORY_SIZE_KEY + "\": { \"value\": " + FREE_MEMORY_SIZE + " }, " +
      "\"freeSwapSpaceSize (MB)\": { \"value\": 1605.5 }, " + "\"" + SQL_CALL_KEY + "\": { \"value\": " +
      SQL_CALL_VALUE + " } } }";

  @Test
  public void testGetLongGaugeValue() throws Exception {
    MetricsResult metricsResult = MetricsReader.getMetricsResult(METRICS_STRING);
    assertThat(metricsResult.getGaugeValue(SQL_CALL_KEY, Long.class).get(), equalTo(Long.valueOf(SQL_CALL_VALUE)));
  }

  @Test
  public void testGetDoubleGaugeValue() throws Exception {
    MetricsResult metricsResult = MetricsReader.getMetricsResult(METRICS_STRING);
    assertThat(metricsResult.getGaugeValue(FILE_DESCRIPTORS_USED_KEY, Double.class).get(),
        equalTo(Double.valueOf(FILE_DESCRIPTORS_USED)));
  }

  @Test
  public void testGetIntegerGaugeValue() throws Exception {
    MetricsResult metricsResult = MetricsReader.getMetricsResult(METRICS_STRING);
    assertThat(metricsResult.getGaugeValue(FREE_MEMORY_SIZE_KEY, Integer.class).get(),
        equalTo(Integer.valueOf(FREE_MEMORY_SIZE)));
  }
}
