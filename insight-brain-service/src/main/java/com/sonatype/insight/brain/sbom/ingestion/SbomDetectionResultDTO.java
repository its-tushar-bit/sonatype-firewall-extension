/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.util.List;

import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.sbom.utils.SbomSummary;
import com.sonatype.insight.brain.thirdparty.SbomScanType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class SbomDetectionResultDTO
{
  private String requestId;

  private SbomSummary sbomSummary;

  private String errorMessage;

  private List<String> validationErrors;

  private SbomScanType scanType;

  @JsonInclude(Include.NON_EMPTY)
  private Boolean isValid;

  @JsonInclude(Include.NON_EMPTY)
  private Boolean isValidationErrorIgnorable;

  public SbomDetectionResultDTO() {
    // jackson
  }

  public SbomDetectionResultDTO(SbomRequestIdElements idElements, SbomDetectionResult sbomDetectionResult) {
    this(idElements.encodeRequestId(), sbomDetectionResult.summary, sbomDetectionResult.errorMessage,
        sbomDetectionResult.validationErrors, idElements.getScanType(), sbomDetectionResult.isValidationErrorIgnorable,
        sbomDetectionResult.isValid);
  }

  public SbomDetectionResultDTO(String requestId, SbomScanType scanType, SbomDetectionResult sbomDetectionResult) {
    this(requestId, sbomDetectionResult.summary, sbomDetectionResult.errorMessage, sbomDetectionResult.validationErrors,
        scanType, sbomDetectionResult.isValidationErrorIgnorable, sbomDetectionResult.isValid);
  }

  public SbomDetectionResultDTO(
      String requestId,
      SbomSummary sbomSummary,
      String errorMessage,
      List<String> validationErrors,
      SbomScanType scanType,
      Boolean isValidationErrorIgnorable,
      Boolean isValid)
  {
    this.requestId = requestId;
    this.sbomSummary = sbomSummary;
    this.errorMessage = errorMessage;
    this.validationErrors = validationErrors;
    this.scanType = scanType;
    this.isValidationErrorIgnorable = isValidationErrorIgnorable;
    this.isValid = isValid;
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

  public List<String> getValidationErrors() {
    return validationErrors;
  }

  public void setValidationErrors(List<String> validationErrors) {
    this.validationErrors = validationErrors;
  }

  public SbomScanType getScanType() {
    return scanType;
  }

  public void setScanType(SbomScanType scanType) {
    this.scanType = scanType;
  }

  public Boolean getIsValid() {
    return isValid;
  }

  public void setIsValid(Boolean isValid) {
    this.isValid = isValid;
  }

  public Boolean getIsValidationErrorIgnorable() {
    return isValidationErrorIgnorable;
  }

  public void setIsValidationErrorIgnorable(Boolean isValidationErrorIgnorable) {
    this.isValidationErrorIgnorable = isValidationErrorIgnorable;
  }
}
