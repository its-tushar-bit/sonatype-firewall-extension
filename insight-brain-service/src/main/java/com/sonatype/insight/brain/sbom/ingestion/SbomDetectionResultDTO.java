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
  private SbomSummary sbomSummary;

  private String errorMessage;

  private List<String> validationErrors;

  private SbomScanType scanType;

  /**
   * The version under which the SBOM was saved in the database. Will often be the same as summary.applicationVersion,
   * but may be different in case that field is blank or a new version had to be used to avoid a conflict.
   */
  private String savedVersion;

  @JsonInclude(Include.NON_EMPTY)
  private Boolean isValid;

  @JsonInclude(Include.NON_EMPTY)
  private Boolean isValidationErrorIgnorable;

  public SbomDetectionResultDTO() {
    // jackson
  }

  public SbomDetectionResultDTO(SbomScanType scanType, SbomDetectionResult sbomDetectionResult) {
    this(scanType, sbomDetectionResult, null);
  }

  public SbomDetectionResultDTO(SbomScanType scanType, SbomDetectionResult sbomDetectionResult, String savedVersion) {
    this.sbomSummary = sbomDetectionResult.summary;
    this.errorMessage = sbomDetectionResult.errorMessage;
    this.validationErrors = sbomDetectionResult.validationErrors;
    this.scanType = scanType;
    this.isValidationErrorIgnorable = sbomDetectionResult.isValidationErrorIgnorable;
    this.isValid = sbomDetectionResult.isValid;
    this.savedVersion = savedVersion;
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

  public String getSavedVersion() {
    return savedVersion;
  }

  public void setSavedVersion(String savedVersion) {
    this.savedVersion = savedVersion;
  }
}
