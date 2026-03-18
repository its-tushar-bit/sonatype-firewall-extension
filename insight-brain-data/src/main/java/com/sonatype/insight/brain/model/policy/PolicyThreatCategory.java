/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.SortedSet;

/**
 * The threat category for a policy is based on the conditions in the constraints of that policy.
 *
 * @see Policy#getThreatCategory
 *
 * @since 1.11
 */
public enum PolicyThreatCategory
{
  // The order the policy threat categories are declared here is used when the threat category for a policy is
  // determined. Changing the order here changes the category for policies.
  SECURITY("security"),
  LICENSE("license"),
  QUALITY("quality"),
  OTHER("other");

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

  public static PolicyThreatCategory getCategory(SortedSet<PolicyThreatCategory> policyThreatCategories) {
    if (policyThreatCategories.isEmpty()) {
      return OTHER;
    }
    return policyThreatCategories.first();
  }

  public String getId() {
    return name();
  }

  public String getName() {
    return name;
  }
}
