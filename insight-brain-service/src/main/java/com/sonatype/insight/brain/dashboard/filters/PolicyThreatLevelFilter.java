/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.filters;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.sonatype.insight.error.exception.BadRequestException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

public class PolicyThreatLevelFilter
    implements Predicate<Integer>
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

  @JsonCreator
  public PolicyThreatLevelFilter(
      @JsonProperty("minPolicyThreatLevel") final Integer min,
      @JsonProperty("maxPolicyThreatLevel") final Integer max)
  {
    initialize(min, max);
  }

  public int getMinPolicyThreatLevel() {
    return minPolicyThreatLevel;
  }

  public int getMaxPolicyThreatLevel() {
    return maxPolicyThreatLevel;
  }

  /**
   * Threat ranges to apply as SQL {@code BETWEEN} predicates (OR'd when more than one).
   * Subclasses that OR multiple buckets override this so loaders do not collapse to the envelope.
   */
  public List<Map.Entry<Integer, Integer>> sqlThreatLevelRanges() {
    return List.of(Map.entry(minPolicyThreatLevel, maxPolicyThreatLevel));
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

  @Override
  public boolean test(Integer threatLevel) {
    return threatLevel != null && minPolicyThreatLevel <= threatLevel && threatLevel <= maxPolicyThreatLevel;
  }
}
