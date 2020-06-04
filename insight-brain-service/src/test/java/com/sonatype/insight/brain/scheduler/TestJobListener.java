/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.util.concurrent.atomic.AtomicInteger;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;

public class TestJobListener
    extends JobListenerSupport
{
  private volatile JobExecutionContext jobExecutionContext;

  private volatile JobExecutionException jobExecutionException;

  private volatile AtomicInteger executions = new AtomicInteger();

  @Override
  public String getName() {
    return TestJobListener.class.getSimpleName();
  }

  @Override
  public void jobWasExecuted(JobExecutionContext jobExecutionContext, JobExecutionException jobExecutionException) {
    this.jobExecutionContext = jobExecutionContext;
    this.jobExecutionException = jobExecutionException;
    executions.incrementAndGet();
  }

  public JobExecutionContext getJobExecutionContext() {
    return jobExecutionContext;
  }

  public JobExecutionException getJobExecutionException() {
    return jobExecutionException;
  }

  public int getExecutions() {
    return executions.get();
  }
}
