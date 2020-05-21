/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import javax.inject.Named;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

@Named
public class TestJob
    implements Job
{
  public static final String NAME = "TestJob";

  private static volatile boolean shouldThrowException;

  private static volatile boolean executionFinished;

  @Override
  public void execute(JobExecutionContext context) {
    try {
      if (shouldThrowException) {
        throw new RuntimeException(NAME + " exception");
      }
    }
    finally {
      executionFinished = true;
    }
  }

  public static void setShouldThrowException(boolean shouldThrowException) {
    TestJob.shouldThrowException = shouldThrowException;
  }

  public static boolean isExecutionFinished() {
    return executionFinished;
  }

  public static void reset() {
    shouldThrowException = false;
    executionFinished = false;
  }
}
