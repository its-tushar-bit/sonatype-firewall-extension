/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;

/**
 * Abstract implementation of {@link Runnable} to share commonly used functions to process a Policy Evaluation.
 *
 * @since 1.98
 */
public abstract class EvaluationTask
    implements Runnable
{
  protected PolicyEvaluationPollingResult makeCopy(PolicyEvaluationPollingResult from) {
    PolicyEvaluationPollingResult result = new PolicyEvaluationPollingResult();
    result.setStatus(from.getStatus());
    result.setReason(from.getReason());
    result.setResult(from.getResult());
    result.setScanReceipt(from.getScanReceipt());
    result.setNextPollingIntervalInSeconds(from.getNextPollingIntervalInSeconds());
    return result;
  }
}
