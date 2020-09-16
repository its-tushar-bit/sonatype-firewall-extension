/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

/**
 * Abstract implementation of {@link PolicyEvaluateService} to share commonly used functions to
 * process a Policy Evaluation.
 *
 * @since 1.98
 */
public abstract class AbstractPolicyEvaluateService
    implements PolicyEvaluateService
{
  private static final int NEXT_POLLING_INTERVAL_IN_SECONDS = 5;

  public boolean disablePollingIntervalForTesting = false;

  /**
   * Retrieve the interval of many seconds before checking again if a result is available.
   *
   * @return value of {@link #NEXT_POLLING_INTERVAL_IN_SECONDS} or 1
   * if {@link #disablePollingIntervalForTesting} is true
   */
  protected int getNextPollingInterval() {
    return disablePollingIntervalForTesting ? 1 : NEXT_POLLING_INTERVAL_IN_SECONDS;
  }
}
