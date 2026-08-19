/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

public class CpeResultsTelemetry
{
  public static final String ATTRIBUTE_NAME = "cpe_results_metrics";

  private int candidateFormatsCount;

  private int reportComponentTotal;

  private int cpeMatchedComponentCount;

  private int cpeMatchedVulnerabilityCount;

  private int cpeUnMatchedVulnerabilityCount;

  public int getCandidateFormatsCount() {
    return candidateFormatsCount;
  }

  public int getReportComponentTotal() {
    return reportComponentTotal;
  }

  public int getCpeMatchedComponentCount() {
    return cpeMatchedComponentCount;
  }

  public int getCpeMatchedVulnerabilityCount() {
    return cpeMatchedVulnerabilityCount;
  }

  public int getCpeUnMatchedVulnerabilityCount() {
    return cpeUnMatchedVulnerabilityCount;
  }

  public void incrementCandidateFormatsCount() {
    this.candidateFormatsCount++;
  }

  public void incrementReportComponentTotal() {
    this.reportComponentTotal++;
  }

  public void incrementCpeMatchedComponentCount() {
    this.cpeMatchedComponentCount++;
  }

  public void incrementCpeMatchedVulnerabilityCount() {
    this.cpeMatchedVulnerabilityCount++;
  }

  public void incrementCpeUnMatchedVulnerabilityCount() {
    this.cpeUnMatchedVulnerabilityCount++;
  }
}
