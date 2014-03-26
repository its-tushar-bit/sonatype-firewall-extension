/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

/**
 * @since 1.11
 */
 * The threat category for a policy is based on the conditions in the constraints of that policy.
 * 
 * @see Policy#getThreatCategory
 * 
 * @since 1.10
 */
public enum PolicyThreatCategory
{
  SECURITY("security"), LICENSE("license"), QUALITY("quality"), OTHER("other");

  private final String name;

  PolicyThreatCategory(String name) {
    this.name = name;
  }

  public static PolicyThreatCategory getByName(String name) {
    if (name == null) {
      return null;
    }

    for (PolicyThreatCategory status : values()) {
      if (name.equals(status.name)) {
        return status;
      }
    }

    throw new IllegalArgumentException("Unknown policy threat category with name: " + name);
  }

  public String getId() {
    return name();
  }

  public String getName() {
    return name;
  }
}
