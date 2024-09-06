/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

public enum ReachabilityStatus
{
  REACHABLE("reachable"), NON_REACHABLE("non-reachable");

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

    throw new IllegalArgumentException("Unknown reachability status with name: " + name);
  }

  public String getId() {
    return name();
  }

  public String getName() {
    return name;
  }
}
