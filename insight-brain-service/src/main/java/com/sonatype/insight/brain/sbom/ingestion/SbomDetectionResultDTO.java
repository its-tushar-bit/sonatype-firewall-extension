/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import com.sonatype.insight.brain.sbom.utils.SbomSummary;

public class SbomDetectionResultDTO
{
  private String requestId;

  private SbomSummary sbomSummary;

  private String errorMessage;

  public SbomDetectionResultDTO() {
    // jackson
  }

  public SbomDetectionResultDTO(String requestId, SbomSummary sbomSummary, String errorMessage) {
    this.requestId = requestId;
    this.sbomSummary = sbomSummary;
    this.errorMessage = errorMessage;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public SbomSummary getSbomSummary() {
    return sbomSummary;
  }

  public void setSbomSummary(SbomSummary sbomSummary) {
    this.sbomSummary = sbomSummary;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
