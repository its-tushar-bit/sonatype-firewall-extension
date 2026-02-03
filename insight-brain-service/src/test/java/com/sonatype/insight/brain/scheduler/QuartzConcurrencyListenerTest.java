/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QuartzConcurrencyListenerTest
{
  private QuartzConcurrencyListener listener;

  @Mock
  private Trigger mockTrigger;

  @Mock
  private JobExecutionContext mockContext;

  @Mock
  private JobDetail mockJobDetail;

  @Mock
  private Scheduler mockScheduler;

  private JobDataMap jobDataMap;

  private JobKey jobKey;

  private TriggerKey triggerKey;

  @Before
  public void setUp() {
    listener = new QuartzConcurrencyListener();
    jobDataMap = new JobDataMap();
    jobKey = JobKey.jobKey("TestJob", "TestGroup");
    triggerKey = TriggerKey.triggerKey("TestTrigger", "TestGroup");

    when(mockContext.getJobDetail()).thenReturn(mockJobDetail);
    when(mockContext.getTrigger()).thenReturn(mockTrigger);
    when(mockContext.getScheduler()).thenReturn(mockScheduler);
    when(mockTrigger.getJobDataMap()).thenReturn(jobDataMap);
    when(mockJobDetail.getKey()).thenReturn(jobKey);
    when(mockTrigger.getKey()).thenReturn(triggerKey);

    // Use thenAnswer to avoid generic type issues with TriggerBuilder
    when(mockTrigger.getTriggerBuilder()).thenAnswer(invocation ->
        TriggerBuilder.newTrigger().withIdentity(triggerKey));
  }

  @Test
  public void testGetName() {
    assertThat(listener.getName()).isEqualTo("QuartzConcurrencyListener");
  }

  @Test
  public void testVetoJobExecution_NoMaxConcurrentInJobDataMap_AllowsExecution() throws Exception {
    // Given - jobDataMap does not contain MAX_CONCURRENT key

    // When
    boolean veto = listener.vetoJobExecution(mockTrigger, mockContext);

    // Then
    assertThat(veto).isFalse();
    verify(mockScheduler, never()).rescheduleJob(any(), any());
  }

  @Test
  public void testVetoJobExecution_RunningCountBelowLimit_AllowsExecution() throws Exception {
    // Given
    jobDataMap.put(QuartzConcurrencyListener.MAX_CONCURRENT, 2);
    when(mockScheduler.getCurrentlyExecutingJobs()).thenReturn(Collections.emptyList());

    // When
    boolean veto = listener.vetoJobExecution(mockTrigger, mockContext);

    // Then
    assertThat(veto).isFalse();
    verify(mockScheduler, never()).rescheduleJob(any(), any());
  }

  @Test
  public void testVetoJobExecution_RunningCountAtLimit_VetoesExecution() throws Exception {
    // Given
    jobDataMap.put(QuartzConcurrencyListener.MAX_CONCURRENT, 1);

    // Mock one currently executing job with the same name
    JobExecutionContext runningContext = mock(JobExecutionContext.class);
    JobDetail runningJobDetail = mock(JobDetail.class);
    when(runningContext.getJobDetail()).thenReturn(runningJobDetail);
    when(runningJobDetail.getKey()).thenReturn(jobKey);

    List<JobExecutionContext> executingJobs = new ArrayList<>();
    executingJobs.add(runningContext);
    when(mockScheduler.getCurrentlyExecutingJobs()).thenReturn(executingJobs);

    // When
    boolean veto = listener.vetoJobExecution(mockTrigger, mockContext);

    // Then
    assertThat(veto).isTrue();
    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
    verify(mockScheduler).rescheduleJob(eq(triggerKey), triggerCaptor.capture());

    // Verify the new trigger has a delayed start time
    Trigger rescheduledTrigger = triggerCaptor.getValue();
    assertThat(rescheduledTrigger.getStartTime()).isAfter(new Date());
  }

  @Test
  public void testVetoJobExecution_RunningCountAboveLimit_VetoesExecution() throws Exception {
    // Given
    jobDataMap.put(QuartzConcurrencyListener.MAX_CONCURRENT, 1);

    // Mock two currently executing jobs with the same name
    JobExecutionContext runningContext1 = mock(JobExecutionContext.class);
    JobDetail runningJobDetail1 = mock(JobDetail.class);
    when(runningContext1.getJobDetail()).thenReturn(runningJobDetail1);
    when(runningJobDetail1.getKey()).thenReturn(jobKey);

    JobExecutionContext runningContext2 = mock(JobExecutionContext.class);
    JobDetail runningJobDetail2 = mock(JobDetail.class);
    when(runningContext2.getJobDetail()).thenReturn(runningJobDetail2);
    when(runningJobDetail2.getKey()).thenReturn(jobKey);

    List<JobExecutionContext> executingJobs = new ArrayList<>();
    executingJobs.add(runningContext1);
    executingJobs.add(runningContext2);
    when(mockScheduler.getCurrentlyExecutingJobs()).thenReturn(executingJobs);

    // When
    boolean veto = listener.vetoJobExecution(mockTrigger, mockContext);

    // Then
    assertThat(veto).isTrue();
    verify(mockScheduler).rescheduleJob(eq(triggerKey), any(Trigger.class));
  }

  @Test
  public void testVetoJobExecution_UsesDefaultQueueDelayWhenNotSpecified() throws Exception {
    // Given
    jobDataMap.put(QuartzConcurrencyListener.MAX_CONCURRENT, 1);

    // Mock one currently executing job
    JobExecutionContext runningContext = mock(JobExecutionContext.class);
    JobDetail runningJobDetail = mock(JobDetail.class);
    when(runningContext.getJobDetail()).thenReturn(runningJobDetail);
    when(runningJobDetail.getKey()).thenReturn(jobKey);

    List<JobExecutionContext> executingJobs = new ArrayList<>();
    executingJobs.add(runningContext);
    when(mockScheduler.getCurrentlyExecutingJobs()).thenReturn(executingJobs);

    long beforeTime = System.currentTimeMillis();

    // When
    listener.vetoJobExecution(mockTrigger, mockContext);

    // Then
    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
    verify(mockScheduler).rescheduleJob(eq(triggerKey), triggerCaptor.capture());

    Trigger rescheduledTrigger = triggerCaptor.getValue();
    long delayMs = rescheduledTrigger.getStartTime().getTime() - beforeTime;
    int expectedDelayMs = (int) Duration.ofMinutes(1).toMillis();
    // Allow 100ms tolerance for test execution time
    assertThat(delayMs).isBetween((long) expectedDelayMs - 100, (long) expectedDelayMs + 100);
  }

  @Test
  public void testVetoJobExecution_UsesCustomQueueDelay() throws Exception {
    // Given
    int customDelayMs = 5000;
    jobDataMap.put(QuartzConcurrencyListener.MAX_CONCURRENT, 1);
    jobDataMap.put(QuartzConcurrencyListener.QUEUE_DELAY_MS, customDelayMs);

    // Mock one currently executing job
    JobExecutionContext runningContext = mock(JobExecutionContext.class);
    JobDetail runningJobDetail = mock(JobDetail.class);
    when(runningContext.getJobDetail()).thenReturn(runningJobDetail);
    when(runningJobDetail.getKey()).thenReturn(jobKey);

    List<JobExecutionContext> executingJobs = new ArrayList<>();
    executingJobs.add(runningContext);
    when(mockScheduler.getCurrentlyExecutingJobs()).thenReturn(executingJobs);

    long beforeTime = System.currentTimeMillis();

    // When
    listener.vetoJobExecution(mockTrigger, mockContext);

    // Then
    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
    verify(mockScheduler).rescheduleJob(eq(triggerKey), triggerCaptor.capture());

    Trigger rescheduledTrigger = triggerCaptor.getValue();
    long delayMs = rescheduledTrigger.getStartTime().getTime() - beforeTime;
    // Allow 100ms tolerance for test execution time
    assertThat(delayMs).isBetween((long) customDelayMs - 100, (long) customDelayMs + 100);
  }

  @Test
  public void testVetoJobExecution_DifferentJobNames_DoesNotCount() throws Exception {
    // Given
    jobDataMap.put(QuartzConcurrencyListener.MAX_CONCURRENT, 1);

    // Mock one currently executing job with a DIFFERENT name
    JobExecutionContext runningContext = mock(JobExecutionContext.class);
    JobDetail runningJobDetail = mock(JobDetail.class);
    JobKey differentJobKey = JobKey.jobKey("DifferentJob", "TestGroup");
    when(runningContext.getJobDetail()).thenReturn(runningJobDetail);
    when(runningJobDetail.getKey()).thenReturn(differentJobKey);

    List<JobExecutionContext> executingJobs = new ArrayList<>();
    executingJobs.add(runningContext);
    when(mockScheduler.getCurrentlyExecutingJobs()).thenReturn(executingJobs);

    // When
    boolean veto = listener.vetoJobExecution(mockTrigger, mockContext);

    // Then - should not veto because the running job has a different name
    assertThat(veto).isFalse();
    verify(mockScheduler, never()).rescheduleJob(any(), any());
  }

  @Test
  public void testVetoJobExecution_ExceptionOccurs_ReturnsFalse() throws Exception {
    // Given
    jobDataMap.put(QuartzConcurrencyListener.MAX_CONCURRENT, 1);
    when(mockScheduler.getCurrentlyExecutingJobs()).thenThrow(new SchedulerException("Test exception"));

    // When
    boolean veto = listener.vetoJobExecution(mockTrigger, mockContext);

    // Then - should return false when exception occurs
    assertThat(veto).isFalse();
  }

  @Test
  public void testVetoJobExecution_ReschedulesWithCorrectJobDataMap() throws Exception {
    // Given
    jobDataMap.put(QuartzConcurrencyListener.MAX_CONCURRENT, 1);
    jobDataMap.put("customKey", "customValue");

    // Mock one currently executing job
    JobExecutionContext runningContext = mock(JobExecutionContext.class);
    JobDetail runningJobDetail = mock(JobDetail.class);
    when(runningContext.getJobDetail()).thenReturn(runningJobDetail);
    when(runningJobDetail.getKey()).thenReturn(jobKey);

    List<JobExecutionContext> executingJobs = new ArrayList<>();
    executingJobs.add(runningContext);
    when(mockScheduler.getCurrentlyExecutingJobs()).thenReturn(executingJobs);

    // When
    listener.vetoJobExecution(mockTrigger, mockContext);

    // Then
    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
    verify(mockScheduler).rescheduleJob(eq(triggerKey), triggerCaptor.capture());

    Trigger rescheduledTrigger = triggerCaptor.getValue();
    JobDataMap rescheduledJobDataMap = rescheduledTrigger.getJobDataMap();
    assertThat(rescheduledJobDataMap.getString("customKey")).isEqualTo("customValue");
    assertThat(rescheduledJobDataMap.getIntValue(QuartzConcurrencyListener.MAX_CONCURRENT)).isEqualTo(1);
  }
}
