/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.75
 */
@JsonInclude(Include.NON_NULL)
public class ApiThirdPartyScanResultDTO
{
  public String policyAction;

  public String reportHtmlUrl;

  public String reportPdfUrl;

  public String reportDataUrl;

  public String embeddableReportHtmlUrl;

  public boolean isError;

  public String errorMessage;

  public ApiEvaluationResultCounterDTO componentsAffected;

  public ApiEvaluationResultCounterDTO openPolicyViolations;

  /**
   * @deprecated use legacyViolations
   */
  @Deprecated
  public Integer grandfatheredPolicyViolations;

  public Integer legacyViolations;

  public ApiThirdPartyScanResultDTO() {
    // noop
  }

  public ApiThirdPartyScanResultDTO(
      String policyAction,
      String reportHtmlUrl,
      String reportPdfUrl,
      String reportDataUrl,
      String embeddableReportHtmlUrl,
      ApiEvaluationResultCounterDTO componentsAffected,
      ApiEvaluationResultCounterDTO openPolicyViolations,
      Integer legacyViolations)
  {
    this.policyAction = policyAction;
    this.reportHtmlUrl = reportHtmlUrl;
    this.reportPdfUrl = reportPdfUrl;
    this.reportDataUrl = reportDataUrl;
    this.embeddableReportHtmlUrl = embeddableReportHtmlUrl;
    this.componentsAffected = componentsAffected;
    this.openPolicyViolations = openPolicyViolations;
    this.grandfatheredPolicyViolations = legacyViolations;
    this.legacyViolations = legacyViolations;
  }

  public ApiThirdPartyScanResultDTO(String errorMessage) {
    this.isError = true;
    this.errorMessage = errorMessage;
  }
}
