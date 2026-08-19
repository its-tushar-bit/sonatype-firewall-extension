/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.EnumMap;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.utils.ThreatLevel;

/**
 * @since 1.51
 */
public class ViolationCountsDTO
{
  public String timePeriodName;

  public Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> discoveredCounts;

  public Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> waivedCounts;

  public Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> fixedCounts;

  public ViolationCountsDTO() {
    this.discoveredCounts = allZeroCounts();
    this.waivedCounts = allZeroCounts();
    this.fixedCounts = allZeroCounts();
  }

  public ViolationCountsDTO(
      String timePeriodName,
      Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> discoveredCounts,
      Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> fixedCounts,
      Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> waivedCounts)
  {
    this.timePeriodName = timePeriodName;
    this.discoveredCounts = discoveredCounts;
    this.waivedCounts = waivedCounts;
    this.fixedCounts = fixedCounts;
  }

  private Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> allZeroCounts() {
    EnumMap<PolicyThreatCategory, Map<ThreatLevel, Integer>> result = new EnumMap<>(PolicyThreatCategory.class);
    for (PolicyThreatCategory category : PolicyThreatCategory.values()) {
      result.put(category, new EnumMap<>(ThreatLevel.class));
      for (ThreatLevel level : ThreatLevel.values()) {
        result.get(category).put(level, 0);
      }
    }
    return result;
  }
}
