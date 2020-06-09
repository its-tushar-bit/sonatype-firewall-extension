/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.utils.ISODateSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ApiPolicyEvaluationDTO
{
  public String id;

  public String applicationId;

  public String stageTypeId;

  public String scanId;

  public boolean isReevaluation;

  public boolean isForMonitoring;

  public String commitHash;

  public boolean isReportAvailable;

  @JsonSerialize(using = ISODateSerializer.class)
  public Date time;

  public PolicyEvaluationResult policyEvaluationResult;

  public ApiPolicyEvaluationDTO() {
  }

  public ApiPolicyEvaluationDTO(
      final PolicyEvaluation policyEvaluation,
      final PolicyEvaluationResult policyEvaluationResult,
      final boolean isReportAvailable)
  {
    this.applicationId = policyEvaluation.getApplicationId();
    this.stageTypeId = policyEvaluation.getStageTypeId();
    this.scanId = policyEvaluation.getScanId();
    this.isReevaluation = policyEvaluation.isReevaluation();
    this.isForMonitoring = policyEvaluation.isForMonitoring();
    this.id = policyEvaluation.getId();
    this.time = policyEvaluation.getTime();
    this.commitHash = policyEvaluation.getCommitHash();
    this.policyEvaluationResult = policyEvaluationResult;
    this.isReportAvailable = isReportAvailable;
  }

  @Override
  public String toString() {
    return "PolicyEvaluation{" + //
        "id='" + id + '\'' + //
        ", applicationId='" + applicationId + '\'' + //
        ", stageTypeId='" + stageTypeId + '\'' + //
        ", scanId='" + scanId + '\'' + //
        ", isReevaluation=" + isReevaluation + //
        ", isForMonitoring=" + isForMonitoring + //
        ", time=" + time + " (" + (time == null ? "" : time.getTime()) + ")" + //
        ", commitHash='" + commitHash + '\'' + //
        '}';
  }
}
