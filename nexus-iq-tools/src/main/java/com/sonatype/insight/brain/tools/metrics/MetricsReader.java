/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.metrics;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetricsReader
{
  public static MetricsResult getMetricsResult(String metricsJson) throws Exception {
    return new MetricsResult(getMetricsMap(metricsJson));
  }

  private static Map<String, Object> getMetricsMap(String metricsJson) throws Exception {
    return new ObjectMapper().readValue(metricsJson, new TypeReference<HashMap<String, Object>>() { });
  }
}
