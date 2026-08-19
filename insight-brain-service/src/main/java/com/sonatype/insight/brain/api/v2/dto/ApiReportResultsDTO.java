/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;

public class ApiReportResultsDTO
    extends ApiApplicationReportDTOV2
{
  public String policyEvaluationId;

  public String scanId;

  public boolean isReevaluation;

  public boolean isForMonitoring;

  public String commitHash;

  /**
   * @since 1.168
   */
  public String scanTriggerType;

  public String scanTriggerTypeDisplayName;

  public Boolean scanTriggerInternal;

  public String scannerVersion;

  public PolicyEvaluationResult policyEvaluationResult;

  public ApiReportResultsDTO() {
  }

  public ApiReportResultsDTO(
      final PolicyEvaluation policyEvaluation,
      final PolicyEvaluationResult policyEvaluationResult,
      final String scannerVersion)
  {
    this.applicationId = policyEvaluation.getOwnerId();
    this.stage = policyEvaluation.getStageTypeId();
    this.scanId = policyEvaluation.getScanId();
    this.isReevaluation = policyEvaluation.isReevaluation();
    this.isForMonitoring = policyEvaluation.isForMonitoring();
    this.policyEvaluationId = policyEvaluation.getId();
    this.evaluationDate = policyEvaluation.getTime();
    this.commitHash = policyEvaluation.getCommitHash();
    ScanTriggerType scanTriggerType = policyEvaluation.getScanTriggerType();
    if (scanTriggerType != null) {
      this.scanTriggerType = scanTriggerType.getId();
      this.scanTriggerTypeDisplayName = scanTriggerType.getDisplayName();
      this.scanTriggerInternal = scanTriggerType.isInternal();
    }
    this.policyEvaluationResult = policyEvaluationResult;
    this.scannerVersion = scannerVersion;
  }

  @Override
  public String toString() {
    return "PolicyEvaluation{" + //
        "policyEvaluationId='" + policyEvaluationId + '\'' + //
        ", applicationId='" + applicationId + '\'' + //
        ", stageTypeId='" + stage + '\'' + //
        ", scanId='" + scanId + '\'' + //
        ", isReevaluation=" + isReevaluation + //
        ", isForMonitoring=" + isForMonitoring + //
        ", reportDate=" + evaluationDate + " (" + (evaluationDate == null ? "" : evaluationDate.getTime()) + ")" + //
        ", commitHash='" + commitHash + '\'' + //
        ", scanTriggerType='" + scanTriggerType + //
        ", scanTriggerTypeDisplayName='" + scanTriggerTypeDisplayName + //
        ", scanTriggerInternal='" + scanTriggerInternal + //
        ", scannerVersion='" + scannerVersion + //
        '}';
  }
}
