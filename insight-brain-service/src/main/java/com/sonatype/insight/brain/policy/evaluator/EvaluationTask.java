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
  private static final int NEXT_POLLING_INTERVAL_IN_SECONDS = 5;

  protected PolicyEvaluationPollingResult makeCopy(PolicyEvaluationPollingResult from) {
    PolicyEvaluationPollingResult result = new PolicyEvaluationPollingResult();
    result.setStatus(from.getStatus());
    result.setSubStatus(from.getSubStatus());
    result.setReason(from.getReason());
    result.setResult(from.getResult());
    result.setScanReceipt(from.getScanReceipt());
    result.setNextPollingIntervalInSeconds(from.getNextPollingIntervalInSeconds());
    return result;
  }

  /**
   * Retrieve the interval (in seconds) before checking again if a result is available.
   *
   * @return value of {@link EvaluationTask#NEXT_POLLING_INTERVAL_IN_SECONDS}
   *         if {@code disablePollingIntervalForTesting} is {@code true}
   */
  public static int getNextPollingInterval(boolean disablePollingIntervalForTesting) {
    return disablePollingIntervalForTesting ? 1 : NEXT_POLLING_INTERVAL_IN_SECONDS;
  }
}
