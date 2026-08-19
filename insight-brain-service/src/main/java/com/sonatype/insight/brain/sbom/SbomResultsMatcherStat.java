/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

public class SbomResultsMatcherStat
{
  public String candidateId;

  public Float purlMatchScore;

  public Float hashMatchScore;

  public Float coordMatchScore;

  public SbomResultsMatcherStat() {
    // no-op
  }

  public SbomResultsMatcherStat(
      final String candidateId,
      final Float purlMatchScore,
      final Float hashMatchScore,
      final Float coordMatchScore)
  {

    this.candidateId = candidateId;
    this.purlMatchScore = purlMatchScore;
    this.hashMatchScore = hashMatchScore;
    this.coordMatchScore = coordMatchScore;
  }
}
