/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import java.util.List;

public class SbomComponentListDTO
{
  private int totalResultsCount;

  private List<SbomComponentDTO> results;

  public SbomComponentListDTO() {
    // For Jackson
  }

  public int getTotalResultsCount() {
    return totalResultsCount;
  }

  public void setTotalResultsCount(int totalResultsCount) {
    this.totalResultsCount = totalResultsCount;
  }

  public List<SbomComponentDTO> getResults() {
    return results;
  }

  public void setResults(List<SbomComponentDTO> results) {
    this.results = results;
  }
}
