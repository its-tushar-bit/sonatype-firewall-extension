/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.EnumMap;
import java.util.Map;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.utils.ThreatLevel;

class DiscoveredStats
{
  final Map<PolicyThreatCategory, Map<ThreatLevel, Integer>> threatCategoryToThreatLevelToCounts = new EnumMap<>(
      PolicyThreatCategory.class);

  int evaluationCount = 0;

  public DiscoveredStats() {
    for (PolicyThreatCategory threatCategory : PolicyThreatCategory.values()) {
      EnumMap<ThreatLevel, Integer> threatLevelToCount = new EnumMap<>(ThreatLevel.class);
      threatCategoryToThreatLevelToCounts.put(threatCategory, threatLevelToCount);
      for (ThreatLevel threatLevel : ThreatLevel.values()) {
        threatLevelToCount.put(threatLevel, 0);
      }
    }
  }

  void addViolation(PolicyViolation violation) {
    ThreatLevel threatLevel = ThreatLevel.from(violation.getThreatLevel());
    PolicyThreatCategory threatCategory = violation.getThreatCategory();
    threatCategoryToThreatLevelToCounts.get(threatCategory)
        .put(threatLevel, getCount(threatCategory, threatLevel) + 1);
  }

  int getCount(PolicyThreatCategory threatCategory, ThreatLevel threatLevel) {
    return threatCategoryToThreatLevelToCounts.get(threatCategory).get(threatLevel);
  }

  public void incrementEvaluationCount() {
    evaluationCount++;
  }

  public int getEvaluationCount() {
    return evaluationCount;
  }
}
