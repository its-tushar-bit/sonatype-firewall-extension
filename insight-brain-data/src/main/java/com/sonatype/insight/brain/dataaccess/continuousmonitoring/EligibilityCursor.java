/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.continuousmonitoring;

import java.util.Date;
import java.util.Objects;

/**
 * Keyset-pagination cursor over the CM eligibility result set (CLM-41005). Identifies the last row
 * the producer consumed in the current cycle by its (time, repository_component_id) tuple — the
 * same tuple the result set is ordered by, so the next page's predicate is a strict
 * "less-than-this-tuple" comparison. {@code null} signifies "first page".
 * <p>
 * <strong>Immutability:</strong> {@link java.util.Date} is mutable, so the canonical constructor
 * defensively copies the incoming reference and the {@link #time()} accessor returns a fresh
 * {@code Date} on every call. Callers may freely mutate either the input or returned instance
 * without corrupting the cursor's internal state. (Records guarantee referential immutability of
 * components but not value immutability of mutable types; this is the standard workaround for
 * {@code java.util.Date} fields in records.)
 */
public record EligibilityCursor(Date time, String repositoryComponentId)
{
  public EligibilityCursor {
    Objects.requireNonNull(time, "time");
    Objects.requireNonNull(repositoryComponentId, "repositoryComponentId");
    if (repositoryComponentId.isEmpty()) {
      throw new IllegalArgumentException("repositoryComponentId must not be empty");
    }
    // Defensive copy on construction so a caller mutating the source Date cannot corrupt the
    // cursor's snapshot (see PR #16434 review thread r3466119506).
    time = new Date(time.getTime());
  }

  /**
   * Returns a defensive copy so callers cannot mutate the cursor's internal {@code Date} via
   * the returned reference.
   */
  @Override
  public Date time() {
    return new Date(time.getTime());
  }

  /**
   * Stable string form for log lines. Format: {@code <epochMillis> ':' <restOfStringVerbatim>}
   * — a decimal {@code time.getTime()}, a single literal colon, and {@code repositoryComponentId}
   * appended verbatim. {@link #decode(String)} splits on the FIRST colon only, so any colons
   * inside the component id are preserved on the round trip.
   */
  public String encode() {
    return time.getTime() + ":" + repositoryComponentId;
  }

  /**
   * Returns the canonical encoded form so cursors render consistently in logs, stack traces,
   * and debugger views without requiring explicit {@link #encode()} calls.
   */
  @Override
  public String toString() {
    return encode();
  }

  /**
   * Inverse of {@link #encode}.
   *
   * @throws IllegalArgumentException if the string is not in the expected format
   */
  public static EligibilityCursor decode(final String s) {
    Objects.requireNonNull(s, "s");
    int sep = s.indexOf(':');
    if (sep <= 0 || sep == s.length() - 1) {
      throw new IllegalArgumentException("malformed cursor: " + s);
    }
    long epochMillis;
    try {
      epochMillis = Long.parseLong(s.substring(0, sep));
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("malformed cursor (epochMillis): " + s, e);
    }
    return new EligibilityCursor(new Date(epochMillis), s.substring(sep + 1));
  }
}
