/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

/**
 * Decides whether a failed continuous monitoring job should be retried (CLM-40039, Section 7).
 * <p>
 * Selective retry over blanket retry: only well-known transient causes (network blips, DB
 * connection pool timeouts, JDBC's transient set) are retried. All other exceptions are treated
 * as permanent and the row is deleted with a WARN log — better than the legacy SBOM CM behaviour
 * (no retry counter at all) because it both bounds retries on transient failures and avoids
 * pile-up on bug-induced failures.
 * <p>
 * Maximum retry count is configurable; default 3 per design Decision C.
 */
public interface RetryPolicy
{
  /** Whether {@code throwable} represents a transient condition worth retrying. */
  boolean isRetryable(Throwable throwable);

  /**
   * Maximum total number of attempts (initial + retries) before the row is deleted as exhausted.
   * <p>
   * <b>Lifecycle:</b> a freshly-enqueued row has {@code retryCount=0}. After each failure the
   * consumer computes {@code currentAttempt = retryCount + 1} (the count of the attempt that
   * just failed); if {@code currentAttempt >= maxRetries()} the row is dropped, otherwise the
   * counter is incremented and the row returns to PENDING.
   * <p>
   * Walking {@code maxRetries() == 3}: 1st failure → currentAttempt=1, 1≥3 false, retry; 2nd
   * failure → currentAttempt=2, 2≥3 false, retry; 3rd failure → currentAttempt=3, 3≥3 true,
   * drop. So {@code maxRetries() == 3} yields exactly 3 total attempts (initial plus up to 2
   * retries) and the drop happens <em>on the 3rd failure</em>, not before. {@code maxRetries() == 1}
   * disables retries — drops on the 1st failure (single attempt). {@code maxRetries() <= 0} also
   * drops on the 1st failure (currentAttempt=1, 1≥0 always true).
   * <p>
   * The operator-facing property name {@code maxContinuousMonitoringRetries} should be read as
   * "maximum total attempts" to match this contract.
   */
  int maxRetries();
}
