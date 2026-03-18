/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit rule to reduce the delay for actual Quartz job registration in {@link QuartzJobSchedulingService}. The
 * production value there is 3 seconds, but this is far too long to use in tests. So this rule will reduce the wait time
 * to 10 milliseconds and additionally restore the original value after the tests have run.
 * <p>
 * NOTE: See {@link #waitForRealSchedulingToComplete(QuartzJobSchedulingService)}. If your test needs to verify real
 * Quartz values, you can use this to ensure the jobs have actually been scheduled.
 */
public class QuartzJobSchedulingServiceRule
    extends ExternalResource
{
  private static final Logger log = LoggerFactory.getLogger(QuartzJobSchedulingServiceRule.class);

  private static final long TEST_WAIT_DELAY = 10L;

  @Override
  protected void before() throws Throwable {
    log.info("Setting 10ms quartz scheduling wait delay for tests");
    QuartzJobSchedulingService.DELAY_MILLIS = TEST_WAIT_DELAY;
  }

  @Override
  protected void after() {
    log.info("Restoring regular quartz scheduling wait delay after tests");
    QuartzJobSchedulingService.DELAY_MILLIS = QuartzJobSchedulingService.DEFAULT_DELAY_MILLIS;
  }

  /**
   * This rule reduces the wait delay for the actual jobs to be scheduled to 10ms so that tests aren't slowed too much.
   * Some tests need to wait for the jobs to actually be scheduled by Quartz. This method will need to be called in
   * those tests that need to test/verify anything against a real Quartz job.
   */
  public void waitForRealSchedulingToComplete(final QuartzJobSchedulingService quartzJobSchedulingService) {
    log.info("Waiting for up to 1s to let jobs actually be scheduled with Quartz");
    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .until(() -> !quartzJobSchedulingService.areJobsPending());
  }
}
