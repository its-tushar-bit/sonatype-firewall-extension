/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class PolicyEvaluationPollingResultDTO
{
  public PolicyEvaluationStatus status;

  @JsonInclude(Include.NON_NULL)
  public PolicyEvaluationSubStatus subStatus;

  public String reason;

  public PolicyEvaluationResult result;

  public ScanReceipt scanReceipt;

  public String statusId;

  public int nextPollingIntervalInSeconds;
}
