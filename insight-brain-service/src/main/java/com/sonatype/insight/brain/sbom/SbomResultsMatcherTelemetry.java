/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import java.util.LinkedList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.apache.commons.lang3.ObjectUtils;

public class SbomResultsMatcherTelemetry
{
  public static final String ATTRIBUTE_NAME = "sbom_results_matcher_stats";

  private List<SbomResultsMatcherStat> matchStats = new LinkedList<>();

  private SbomResultsMatcherStat winnerStat;

  public void addMatchStat(
      ComponentIdentifier id,
      Float purlMatchScore,
      Float hashMatchScore,
      Float coordMatchScore,
      boolean winner)
  {
    if (ObjectUtils.allNotNull(id, purlMatchScore, hashMatchScore, coordMatchScore)) {
      SbomResultsMatcherStat stat =
          new SbomResultsMatcherStat(obfuscate(id), purlMatchScore, hashMatchScore, coordMatchScore);
      matchStats.add(stat);
      if (winner) {
        winnerStat = stat;
      }
    }
  }

  public List<SbomResultsMatcherStat> getMatchStats() {
    return matchStats;
  }

  public void setMatchStats(final List<SbomResultsMatcherStat> matchStats) {
    this.matchStats = matchStats;
  }

  public SbomResultsMatcherStat getWinnerStat() {
    return winnerStat;
  }

  public void setWinnerStat(final SbomResultsMatcherStat stat) {
    this.winnerStat = stat;
  }

  private String obfuscate(ComponentIdentifier id) {
    return id.toSyntheticHash();
  }
}
