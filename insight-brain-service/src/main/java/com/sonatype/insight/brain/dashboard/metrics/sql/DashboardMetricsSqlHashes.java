/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;

final class DashboardMetricsSqlHashes
{
  private DashboardMetricsSqlHashes() {
  }

  static String sha256Hex(final String normalized) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8)));
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }

  static String normalizeIds(@Nullable final Set<String> values) {
    if (values == null || values.isEmpty()) {
      return "";
    }
    return values.stream()
        .sorted()
        .map(value -> value.length() + ":" + value)
        .collect(Collectors.joining(","));
  }
}
