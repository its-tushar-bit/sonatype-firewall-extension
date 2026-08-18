/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import static org.assertj.core.api.Assertions.assertThat;

public class JobLoggerTest
{
  @RegisterExtension
  public LogOutput logOutput = new LogOutput(JobLogger.class);

  @Test
  public void testDailyJobLogger() {
    // Given
    JobKey jobKey = new JobKey("testJob", "testGroup");
    Date nextExecutionTime = new Date();
    InsightJob insightJob = new TestInsightJob();
    Function<InsightJob, Date> nextExecutionTimeFunction = job -> nextExecutionTime;

    // When
    JobLogger jobLogger = JobLogger.daily(nextExecutionTimeFunction, insightJob, jobKey);
    jobLogger.log();

    // Then
    assertThat(logOutput).atDebugLevel()
        .contains("Scheduling daily task for job TestJob, jobKey 'testGroup.testJob', next execution time: "
            + nextExecutionTime);
  }

  @Test
  public void testOneTimeJobLogger() {
    // Given
    JobKey jobKey = new JobKey("testJob", "testGroup");
    Date nextExecutionTime = new Date();
    InsightJob insightJob = new TestInsightJob();
    Function<InsightJob, Date> nextExecutionTimeFunction = job -> nextExecutionTime;

    // When
    JobLogger jobLogger = JobLogger.oneTime(nextExecutionTimeFunction, insightJob, jobKey);
    jobLogger.log();

    // Then
    assertThat(logOutput).atDebugLevel()
        .contains("Scheduling one-time task for job TestJob, jobKey 'testGroup.testJob', next execution time: "
            + nextExecutionTime);
  }

  @Test
  public void testPeriodicJobLogger() {
    // Given
    JobKey jobKey = new JobKey("testJob", "testGroup");
    Date nextExecutionTime = new Date();
    Duration interval = Duration.ofMinutes(5);
    InsightJob insightJob = new TestInsightJob();
    Function<InsightJob, Date> nextExecutionTimeFunction = job -> nextExecutionTime;

    // When
    JobLogger jobLogger = JobLogger.periodic(nextExecutionTimeFunction, insightJob, jobKey, interval);
    jobLogger.log();

    // Then
    assertThat(logOutput).atDebugLevel()
        .contains("Scheduling periodic task for job TestJob, jobKey 'testGroup.testJob', to run every "
            + interval + ", next execution time " + nextExecutionTime);
  }

  @Test
  public void testOnOtherNodesJobLogger() {
    // Given
    JobKey jobKey = new JobKey("testJob", "testGroup");
    Set<String> otherNodes = new HashSet<>();
    otherNodes.add("node1");
    otherNodes.add("node2");
    InsightJob insightJob = new TestInsightJob();

    // When
    JobLogger jobLogger = JobLogger.onOtherNodes(insightJob, jobKey, otherNodes);
    jobLogger.log();

    // Then
    assertThat(logOutput).atDebugLevel()
        .contains("Scheduling one-time task for job TestJob, jobKey 'testGroup.testJob', to run on "
            + otherNodes + " nodes.");
  }

  /**
   * Simple implementation of InsightJob for testing
   */
  private static class TestInsightJob
      implements InsightJob
  {
    @Override
    public String getJobName() {
      return "TestJob";
    }

    @Override
    public void execute(JobExecutionContext context) {
      // Not used in tests
    }
  }
}
