/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;

public class ApiReportResultsDTO
    extends ApiApplicationReportDTOV2
{
  public String policyEvaluationId;

  public String scanId;

  public boolean isReevaluation;

  public boolean isForMonitoring;

  public String commitHash;

  public PolicyEvaluationResult policyEvaluationResult;

  public ApiReportResultsDTO() {
  }

  public ApiReportResultsDTO(
      final PolicyEvaluation policyEvaluation,
      final PolicyEvaluationResult policyEvaluationResult)
  {
    this.applicationId = policyEvaluation.getApplicationId();
    this.stage = policyEvaluation.getStageTypeId();
    this.scanId = policyEvaluation.getScanId();
    this.isReevaluation = policyEvaluation.isReevaluation();
    this.isForMonitoring = policyEvaluation.isForMonitoring();
    this.policyEvaluationId = policyEvaluation.getId();
    this.evaluationDate = policyEvaluation.getTime();
    this.commitHash = policyEvaluation.getCommitHash();
    this.policyEvaluationResult = policyEvaluationResult;
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
        '}';
  }
}
