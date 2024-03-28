/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;

public class ThirdPartySbomMetadataSummaryListDTO
{
  private int totalResultsCount;

  private List<ThirdPartySbomMetadataSummaryDTO> results;

  public int getTotalResultsCount() {
    return totalResultsCount;
  }

  public void setTotalResultsCount(int totalResultsCount) {
    this.totalResultsCount = totalResultsCount;
  }

  public List<ThirdPartySbomMetadataSummaryDTO> getResults() {
    return results;
  }

  public void setResults(List<ThirdPartySbomMetadataSummaryDTO> results) {
    this.results = results;
  }
}
