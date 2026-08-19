/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Comparator;

/**
 * Compares policy violations by threat level (descending), policy name, application name, coordinates, and then
 * hashes.
 *
 * @since 1.11.0
 */
public class PolicyViolationDTOComparator
    implements Comparator<PolicyViolationDTO>
{
  @Override
  public int compare(PolicyViolationDTO v1, PolicyViolationDTO v2) {
    int result = v2.threatLevel - v1.threatLevel;
    if (result != 0) {
      return result;
    }

    result = v1.policyName.compareToIgnoreCase(v2.policyName);
    if (result != 0) {
      return result;
    }

    result = v1.applicationName.compareToIgnoreCase(v2.applicationName);
    if (result != 0) {
      return result;
    }

    result = compareCoordinates(v1, v2);
    if (result != 0) {
      return result;
    }

    result = nullCheck(v1.hash, v2.hash);
    if (result != 0) {
      return result;
    }
    if (v1.hash != null) {
      result = v1.hash.compareToIgnoreCase(v2.hash);
    }
    return result;
  }

  private int compareCoordinates(PolicyViolationDTO v1, PolicyViolationDTO v2) {
    int result = nullCheck(v1.componentIdentifier, v2.componentIdentifier);
    if (result != 0) {
      return result;
    }
    if (v1.componentIdentifier == null) {
      return 0;
    }

    return v1.componentIdentifier.compareTo(v2.componentIdentifier);
  }

  /**
   * Null objects should be treated as infinitely large.
   */
  private int nullCheck(Object o1, Object o2) {
    if (o1 == null && o2 != null) {
      return 1;
    }
    else if (o1 != null && o2 == null) {
      return -1;
    }

    return 0;
  }
}
