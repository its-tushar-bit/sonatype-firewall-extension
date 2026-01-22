/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

import jakarta.inject.Named;

import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.JobExecutionContext;

@Named
public class TestJob
    implements InsightJob
{
  public static final String NAME = "TestJob";

  private static volatile boolean shouldThrowException;

  private static final AtomicInteger executions = new AtomicInteger();

  private static final ConcurrentMap<Integer, Map<String, Object>> jobParamsByExecution = new ConcurrentHashMap<>();

  private static IntUnaryOperator durations;

  @Override
  public void execute(JobExecutionContext context) {
    int execution = executions.getAndIncrement();
    jobParamsByExecution.put(execution, context.getMergedJobDataMap());
    if (durations != null) {
      int duration = durations.applyAsInt(execution);
      if (duration > 0) {
        for (long start = System.currentTimeMillis(); System.currentTimeMillis() - start < duration; ) {
          Thread.yield();
        }
      }
    }
    if (shouldThrowException) {
      throw new RuntimeException(NAME + " exception");
    }
  }

  public static void setShouldThrowException(boolean shouldThrowException) {
    TestJob.shouldThrowException = shouldThrowException;
  }

  public static void setDurations(IntUnaryOperator durations) {
    TestJob.durations = durations;
  }

  public static int getExecutions() {
    return executions.get();
  }

  public static Map<String, Object> getJobParameters(int execution) {
    return jobParamsByExecution.get(execution);
  }

  public static void reset() {
    shouldThrowException = false;
    executions.set(0);
    jobParamsByExecution.clear();
    durations = null;
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
