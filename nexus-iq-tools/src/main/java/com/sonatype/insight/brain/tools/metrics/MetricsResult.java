/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.metrics;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsResult
{
  private final Map<String, Object> metricsValues;

  private static final Logger log = LoggerFactory.getLogger(MetricsResult.class);

  public MetricsResult(Map<String, Object> metricsValues) {
    this.metricsValues = metricsValues;
  }

  private Map<String, Object> getGauges() {
    return (Map<String, Object>) metricsValues.get("gauges");
  }

  public <T> Optional<T> getGaugeValue(String gaugeName, Class<T> type) {
    Optional<T> gaugeValue = Optional.empty();
    Map<String, Object> gauges = getGauges();
    if (gauges == null) {
      log.error("No gauges configured");
      return gaugeValue;
    }
    Map<?, ?> specificGauge = (Map) gauges.get(gaugeName);
    if (specificGauge != null) {
      Object specificValue = specificGauge.get("value");
      // smaller values will be returned from Jackson parsing as Integers and when larger as Longs
      if (type.equals(Long.class)) {
        gaugeValue = Optional.of(type.cast(((Number) specificValue).longValue()));
      }
      else {
        gaugeValue = Optional.of(type.cast(specificValue));
      }
    }
    return gaugeValue;
  }
}
