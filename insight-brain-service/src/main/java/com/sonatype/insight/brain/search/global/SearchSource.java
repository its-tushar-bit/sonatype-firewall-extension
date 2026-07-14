/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Origin of a Global Search row and value of the {@code ?source=} query parameter. Serializes as
 * lowercase {@code "local"} (tenant IQ index) or {@code "catalog"} (shared Sonatype catalog via the
 * Guide federation client). Exactly one source per request; no cross-source federation or
 * fall-through.
 */
public enum SearchSource
{
  LOCAL("local"),
  CATALOG("catalog");

  public static final SearchSource DEFAULT = LOCAL;

  private final String value;

  SearchSource(final String value) {
    this.value = value;
  }

  @JsonValue
  public String value() {
    return value;
  }

  /**
   * Case-insensitive; {@code null} or blank returns {@link #DEFAULT}. Throws
   * {@link IllegalArgumentException} on any other value.
   */
  public static SearchSource fromWireValue(final String raw) {
    if (raw == null) {
      return DEFAULT;
    }
    final String trimmed = raw.strip();
    if (trimmed.isEmpty()) {
      return DEFAULT;
    }
    final String normalized = trimmed.toLowerCase(Locale.ROOT);
    for (SearchSource s : values()) {
      if (s.value.equals(normalized)) {
        return s;
      }
    }
    // Do NOT echo attacker-controllable user input in the message.
    throw new IllegalArgumentException(
        "unknown search source; valid values are: " + LOCAL.value + ", " + CATALOG.value);
  }
}
