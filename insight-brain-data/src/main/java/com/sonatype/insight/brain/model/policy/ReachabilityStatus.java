/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.stream.Stream;

public enum ReachabilityStatus
{
  REACHABLE("reachable"),
  NON_REACHABLE("non-reachable"),
  UNKNOWN("unknown");

  private final String name;

  ReachabilityStatus(String name) {
    this.name = name;
  }

  public static ReachabilityStatus fromString(String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }

    for (ReachabilityStatus status : values()) {
      if (name.equals(status.name)) {
        return status;
      }
    }

    throw new IllegalArgumentException("Unrecognized reachability status with name: " + name);
  }

  public static ReachabilityStatus fromBoolean(Boolean isReachable) {
    if (isReachable == null) {
      return UNKNOWN;
    }
    return isReachable ? REACHABLE : NON_REACHABLE;
  }

  public Boolean toBoolean() {
    return switch (this) {
      case REACHABLE -> true;
      case NON_REACHABLE -> false;
      case UNKNOWN -> null;
    };
  }

  /**
   * Merge two ReachabilityStatuses into an overall status according to the following logic: - if either status is
   * REACHABLE, the result is REACHABLE - otherwise, if either status is UNKNOWN, the result is UNKNOWN - otherwise, if
   * either status is null, the result is UNKNOWN - otherwise, both statuses are NON_REACHABLE, and the result is
   * NON_REACHABLE
   */
  public static ReachabilityStatus combine(
      final ReachabilityStatus a,
      final ReachabilityStatus b)
  {
    if (a == REACHABLE || b == REACHABLE) {
      return REACHABLE;
    }
    else if (a == UNKNOWN || b == UNKNOWN) {
      return UNKNOWN;
    }
    else if (a == null || b == null) {
      return UNKNOWN;
    }
    else {
      return NON_REACHABLE;
    }
  }

  public static ReachabilityStatus combine(final Stream<ReachabilityStatus> reachabilityStatuses) {
    return reachabilityStatuses
        .map(reachabilityStatus -> reachabilityStatus == null ? UNKNOWN : reachabilityStatus)
        .reduce(ReachabilityStatus::combine)
        .orElse(UNKNOWN);
  }

  public String getId() {
    return name();
  }

  public String getName() {
    return name;
  }
}
