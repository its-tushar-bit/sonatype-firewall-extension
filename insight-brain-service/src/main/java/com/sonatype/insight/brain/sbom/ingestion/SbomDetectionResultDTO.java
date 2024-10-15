/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.util.List;

import com.sonatype.insight.brain.sbom.utils.SbomSummary;
import com.sonatype.insight.brain.thirdparty.SbomScanType;

public class SbomDetectionResultDTO
{
  private String requestId;

  private SbomSummary sbomSummary;

  private String errorMessage;

  private List<String> errors;

  private SbomScanType scanType;

  public SbomDetectionResultDTO() {
    // jackson
  }

  public SbomDetectionResultDTO(
      String requestId,
      SbomSummary sbomSummary,
      String errorMessage,
      List<String> errors,
      SbomScanType scanType)
  {
    this.requestId = requestId;
    this.sbomSummary = sbomSummary;
    this.errorMessage = errorMessage;
    this.errors = errors;
    this.scanType = scanType;
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

  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }

  public SbomScanType getScanType() {
    return scanType;
  }

  public void setScanType(final SbomScanType scanType) {
    this.scanType = scanType;
  }
}
