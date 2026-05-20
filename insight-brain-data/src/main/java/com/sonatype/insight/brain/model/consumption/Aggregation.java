/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.consumption;

import java.util.Locale;
import java.util.Optional;

/**
 * Supported aggregation levels for consumption history queries.
 *
 * @since 1.204
 */
public enum Aggregation
{
  DAILY("daily", 30),
  WEEKLY("weekly", 12),
  MONTHLY("monthly", 12);

  private final String value;

  private final int defaultLimit;

  Aggregation(String value, int defaultLimit) {
    this.value = value;
    this.defaultLimit = defaultLimit;
  }

  public String getValue() {
    return value;
  }

  public int getDefaultLimit() {
    return defaultLimit;
  }

  /**
   * JAX-RS-compatible factory: case-insensitive parse to enum, throws on invalid input.
   * Picked up automatically by JAX-RS for {@code @QueryParam} of type {@code Aggregation},
   * yielding HTTP 400 on bad values via Dropwizard's {@code ParamExceptionMapper}.
   */
  public static Aggregation fromString(String input) {
    return parseOptional(input)
        .orElseThrow(() -> new IllegalArgumentException(
            "Invalid aggregation value: " + input + ". Must be one of: daily, weekly, monthly"));
  }

  public static Optional<Aggregation> parseOptional(String input) {
    if (input == null) {
      return Optional.empty();
    }
    String normalized = input.toLowerCase(Locale.ROOT);
    for (Aggregation a : values()) {
      if (a.value.equals(normalized)) {
        return Optional.of(a);
      }
    }
    return Optional.empty();
  }
}
