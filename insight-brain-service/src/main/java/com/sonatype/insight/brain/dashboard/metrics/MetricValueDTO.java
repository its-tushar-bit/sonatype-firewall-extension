/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single dashboard metric value (CLM-40927). Immutable once built so a response value can't be
 * aliased/mutated after construction; {@code breakdown} is defensively copied and unmodifiable.
 */
public class MetricValueDTO
{
  public final long total;

  public final Map<String, Long> breakdown;

  public final String source;

  @JsonCreator
  public MetricValueDTO(
      @JsonProperty("total") long total,
      @JsonProperty("breakdown") Map<String, Long> breakdown,
      @JsonProperty("source") String source)
  {
    this.total = total;
    this.breakdown = breakdown == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(breakdown));
    this.source = source;
  }
}
