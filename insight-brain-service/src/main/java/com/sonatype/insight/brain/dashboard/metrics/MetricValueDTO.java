/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single dashboard metric value (CLM-40927). Immutable once built so a response value can't be
 * aliased/mutated after construction; {@code breakdown} is defensively copied and unmodifiable.
 */
public class MetricValueDTO
{
  public static final String UNSUPPORTED_FILTER_COMBINATION = "UNSUPPORTED_FILTER_COMBINATION";

  public static final String METRIC_UNAVAILABLE = "METRIC_UNAVAILABLE";

  /**
   * Aggregate count for the metric. Null only when {@link #errorCode} is set (e.g.
   * {@link #UNSUPPORTED_FILTER_COMBINATION}); callers must not auto-unbox as a primitive
   * {@code long} without checking {@link #errorCode} first.
   */
  public final Long total;

  public final Map<String, Long> breakdown;

  public final String source;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public final String errorCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public final List<String> unsupportedDimensions;

  public MetricValueDTO(long total, Map<String, Long> breakdown, String source) {
    this(total, breakdown, source, null, null);
  }

  @JsonCreator
  public MetricValueDTO(
      @JsonProperty("total") Long total,
      @JsonProperty("breakdown") Map<String, Long> breakdown,
      @JsonProperty("source") String source,
      @JsonProperty("errorCode") String errorCode,
      @JsonProperty("unsupportedDimensions") List<String> unsupportedDimensions)
  {
    this.total = total;
    this.breakdown = breakdown == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(breakdown));
    this.source = source;
    this.errorCode = errorCode;
    this.unsupportedDimensions =
        unsupportedDimensions == null ? null : List.copyOf(unsupportedDimensions);
  }

  public static MetricValueDTO unsupported(List<String> unsupportedDimensions) {
    return new MetricValueDTO(
        null,
        null,
        null,
        UNSUPPORTED_FILTER_COMBINATION,
        unsupportedDimensions);
  }

  public static MetricValueDTO unavailable(final String source) {
    return new MetricValueDTO(null, null, source, METRIC_UNAVAILABLE, null);
  }
}
