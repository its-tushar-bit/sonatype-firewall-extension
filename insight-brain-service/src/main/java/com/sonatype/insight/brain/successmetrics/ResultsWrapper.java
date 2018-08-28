/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.util.Map;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.utils.ThreatLevel;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

class ResultsWrapper {

  final MttrStats mttrStats;

  int evaluationCount;

  final Table<PolicyThreatCategory, ThreatLevel, Integer> waivedCounts;

  final Table<PolicyThreatCategory, ThreatLevel, Integer> discoveredCounts;

  final Table<PolicyThreatCategory, ThreatLevel, Integer> fixedCounts;

  final Map<PolicyThreatCategory, Integer> openCounts;

  public ResultsWrapper(Map<PolicyThreatCategory, Integer> openCounts) {
    mttrStats = new MttrStats();
    evaluationCount = 0;
    discoveredCounts = allZeroCounts();
    fixedCounts = allZeroCounts();
    waivedCounts = allZeroCounts();
    this.openCounts = openCounts;
  }

  public ResultsWrapper(MttrStats mttrStats,
                        int evaluationCount,
                        Table<PolicyThreatCategory, ThreatLevel, Integer> discoveredCounts,
                        Table<PolicyThreatCategory, ThreatLevel, Integer> fixedCounts,
                        Table<PolicyThreatCategory, ThreatLevel, Integer> waivedCounts,
                        Map<PolicyThreatCategory, Integer> openCounts)
  {
    this.mttrStats = mttrStats;
    this.evaluationCount = evaluationCount;
    this.discoveredCounts = discoveredCounts;
    this.fixedCounts = fixedCounts;
    this.waivedCounts = waivedCounts;
    this.openCounts = openCounts;
  }

  private static Table<PolicyThreatCategory, ThreatLevel, Integer> allZeroCounts() {
    Table<PolicyThreatCategory, ThreatLevel, Integer> result = HashBasedTable.create();
    for (PolicyThreatCategory category : PolicyThreatCategory.values()) {
      for (ThreatLevel level : ThreatLevel.values()) {
        result.put(category, level, 0);
      }
    }
    return result;
  }

}
