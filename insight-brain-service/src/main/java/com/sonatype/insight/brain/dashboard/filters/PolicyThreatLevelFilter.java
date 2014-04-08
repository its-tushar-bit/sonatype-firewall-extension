/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.base.Predicate;
import org.codehaus.plexus.util.StringUtils;

public class PolicyThreatLevelFilter
    implements Predicate<PolicyViolation>
{

  private int minPolicyThreatLevel = Integer.MIN_VALUE;
  private int maxPolicyThreatLevel = Integer.MAX_VALUE;

  public PolicyThreatLevelFilter() {
    // No argument constructor for convenience.
  }

  /**
   * @param range A range of Integers in the format 'min,max' or ',max' or 'min,'.
   */
  public PolicyThreatLevelFilter(String range) {
    if (StringUtils.isBlank(range)) {
      throw new BadRequestException("Unable to parse policy threat range from empty or null range.");
    }

    // Ensure that if a comma is present there will always be two elements.
    String ensureRange = range + " ";
    String[] integers = ensureRange.split(",");

    try {
      if (integers.length <= 1 || integers.length > 2) {
        throw new BadRequestException("Unable to parse policy threat range from " + range
            + ". Expected format is 'min,max' or ',max' or 'min,'.");
      }

      initialize(parseInt(integers[0].trim()), parseInt(integers[1].trim()));
    }
    catch (NumberFormatException e) {
      throw new BadRequestException("Unable to parse policy threat range from " + range + ".", e);
    }
  }

  public PolicyThreatLevelFilter(Integer min, Integer max) {
    initialize(min, max);
  }

  @Override
  public boolean apply(PolicyViolation input) {
    return (input != null) ? (minPolicyThreatLevel <= input.getThreatLevel() && input.getThreatLevel() <= maxPolicyThreatLevel)
        : false;
  }

  private void initialize(Integer min, Integer max) {
    if (min != null) {
      minPolicyThreatLevel = min.intValue();
    }

    if (max != null) {
      maxPolicyThreatLevel = max.intValue();
    }

    if (minPolicyThreatLevel > maxPolicyThreatLevel) {
      throw new BadRequestException("Minimum policy threat level should not exceed maximum policy threat level.");
    }
  }

  private Integer parseInt(String integer) {
    if (StringUtils.isBlank(integer)) {
      return null;
    }

    return Integer.parseInt(integer);
  }
}
