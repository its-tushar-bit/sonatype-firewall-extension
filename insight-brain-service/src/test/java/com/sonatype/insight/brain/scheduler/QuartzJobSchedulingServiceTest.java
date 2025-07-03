/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.test.LogOutput;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QuartzJobSchedulingServiceTest
{
  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule
  public LogOutput logOutput = new LogOutput(QuartzJobSchedulingService.class);

  @Rule
  public QuartzJobSchedulingServiceRule quartzJobSchedulingServiceRule = new QuartzJobSchedulingServiceRule();

  @Mock
  private Scheduler mockQuartzScheduler;

  @Mock
  private JobLogger mockJobLogger;

  @Captor
  ArgumentCaptor<Map<JobDetail, Set<? extends Trigger>>> mapCaptor;

  private QuartzJobSchedulingService underTest;

  @Before
  public void setup() throws SchedulerException {
    underTest = new QuartzJobSchedulingService();
    when(mockQuartzScheduler.getSchedulerName()).thenReturn("testScheduler");
  }

  @Test
  public void testScheduleTask_SingleJob() throws Exception {
    // Given
    JobDetail jobDetail = createJobDetail("testJob", "testGroup");
    Set<Trigger> triggers = Set.of(createTrigger("testTrigger", "testGroup"));

    // When
    underTest.scheduleTask(mockQuartzScheduler, jobDetail, triggers, mockJobLogger);
    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(underTest);

    // Then
    verifyJobs(List.of(Pair.of(jobDetail, triggers)));

    // Verify log output
    verify(mockJobLogger).log();
    assertThat(logOutput).atDebugLevel()
        .contains("Adding job testGroup.testJob")
        .contains("Scheduling 1 jobs on scheduler");
  }

  @Test
  public void testScheduleTask_MultipleJobs() throws Exception {
    // Given
    JobDetail jobDetail1 = createJobDetail("testJob1", "testGroup");
    Set<Trigger> triggers1 = Set.of(createTrigger("testTrigger1", "testGroup"));

    JobDetail jobDetail2 = createJobDetail("testJob2", "testGroup");
    Set<Trigger> triggers2 = Set.of(createTrigger("testTrigger2", "testGroup"));

    // When
    underTest.scheduleTask(mockQuartzScheduler, jobDetail1, triggers1, mockJobLogger);
    underTest.scheduleTask(mockQuartzScheduler, jobDetail2, triggers2, mockJobLogger);
    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(underTest);

    // Then
    verifyJobs(List.of(Pair.of(jobDetail1, triggers1), Pair.of(jobDetail2, triggers2)));

    // Verify log output
    verify(mockJobLogger, times(2)).log();
    assertThat(logOutput).atDebugLevel()
        .contains("Adding job testGroup.testJob1")
        .contains("Adding job testGroup.testJob2")
        .contains("Scheduling 2 jobs on scheduler");
  }

  @Test
  public void testScheduleTask_Exception() throws Exception {
    // Given
    JobDetail jobDetail = createJobDetail("testJob", "testGroup");
    Set<Trigger> triggers = Set.of(createTrigger("testTrigger", "testGroup"));

    // Make scheduler.scheduleJobs throw an exception
    doThrow(new SchedulerException("Test exception")).when(mockQuartzScheduler).scheduleJobs(anyMap(), anyBoolean());

    // When
    underTest.scheduleTask(mockQuartzScheduler, jobDetail, triggers, mockJobLogger);
    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(underTest);

    // Then
    verify(mockQuartzScheduler).scheduleJobs(anyMap(), eq(true));
    // JobLogger should not be run if there's an exception
    verify(mockJobLogger, times(1)).log();
    assertThat(logOutput).atErrorLevel()
        .contains("Error scheduling jobs");
  }

  @Test
  public void testScheduleTask_MultipleSchedulers() throws Exception {
    // Given
    Scheduler scheduler1 = mock(Scheduler.class);
    when(scheduler1.getSchedulerName()).thenReturn("scheduler1");

    Scheduler scheduler2 = mock(Scheduler.class);
    when(scheduler2.getSchedulerName()).thenReturn("scheduler2");

    JobDetail jobDetail1 = createJobDetail("testJob1", "testGroup");
    Set<Trigger> triggers1 = Set.of(createTrigger("testTrigger1", "testGroup"));

    JobDetail jobDetail2 = createJobDetail("testJob2", "testGroup");
    Set<Trigger> triggers2 = Set.of(createTrigger("testTrigger2", "testGroup"));

    // When
    underTest.scheduleTask(scheduler1, jobDetail1, triggers1, mockJobLogger);
    underTest.scheduleTask(scheduler2, jobDetail2, triggers2, mockJobLogger);
    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(underTest);

    // Then
    verify(scheduler1).scheduleJobs(anyMap(), eq(true));
    verify(scheduler2).scheduleJobs(anyMap(), eq(true));
    verify(mockJobLogger, times(2)).log();

    // Verify log output
    assertThat(logOutput).atDebugLevel()
        .contains("Adding job testGroup.testJob1")
        .contains("Adding job testGroup.testJob2")
        .contains("Scheduling 1 jobs on scheduler scheduler1")
        .contains("Scheduling 1 jobs on scheduler scheduler2");
  }

  @Test
  public void testScheduleTask_StaggeredJobs() throws Exception {
    // Given
    JobDetail jobDetail1 = createJobDetail("testJob1", "testGroup");
    Set<Trigger> triggers1 = Set.of(createTrigger("testTrigger1", "testGroup"));

    JobDetail jobDetail2 = createJobDetail("testJob2", "testGroup");
    Set<Trigger> triggers2 = Set.of(createTrigger("testTrigger2", "testGroup"));

    // When
    underTest.scheduleTask(mockQuartzScheduler, jobDetail1, triggers1, mockJobLogger);
    underTest.scheduleTask(mockQuartzScheduler, jobDetail2, triggers2, mockJobLogger);
    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(underTest);

    // Then
    verifyJobs(List.of(Pair.of(jobDetail1, triggers1), Pair.of(jobDetail2, triggers2)));

    // Verify log output
    verify(mockJobLogger, times(2)).log();
    assertThat(logOutput).atDebugLevel()
        .contains("Adding job testGroup.testJob1")
        .contains("Adding job testGroup.testJob2")
        .contains("Scheduling 2 jobs on scheduler");

    reset(mockQuartzScheduler, mockJobLogger);

    JobDetail jobDetail3 = createJobDetail("testJob3", "testGroup");
    Set<Trigger> triggers3 = Set.of(createTrigger("testTrigger3", "testGroup"));

    JobDetail jobDetail4 = createJobDetail("testJob4", "testGroup");
    Set<Trigger> triggers4 = Set.of(createTrigger("testTrigger4", "testGroup"));

    underTest.scheduleTask(mockQuartzScheduler, jobDetail3, triggers3, mockJobLogger);
    underTest.scheduleTask(mockQuartzScheduler, jobDetail4, triggers4, mockJobLogger);
    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(underTest);

    // Then
    verifyJobs(List.of(Pair.of(jobDetail3, triggers3), Pair.of(jobDetail4, triggers4)));

    // Verify log output
    verify(mockJobLogger, times(2)).log();
    assertThat(logOutput).atDebugLevel()
        .contains("Adding job testGroup.testJob3")
        .contains("Adding job testGroup.testJob4")
        .contains("Scheduling 2 jobs on scheduler");
  }

  @Test
  public void testScheduleTask_Unschedule() throws Exception {
    // Given
    JobDetail jobDetail1 = createJobDetail("testJob1", "testGroup");
    Set<Trigger> triggers1 = Set.of(createTrigger("testTrigger1", "testGroup"));

    // When
    underTest.scheduleTask(mockQuartzScheduler, jobDetail1, triggers1, mockJobLogger);
    underTest.unscheduleTask(mockQuartzScheduler, JobKey.jobKey("testJob1", "testGroup"));
    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(underTest);

    // Then
    // Verify log output
    verify(mockJobLogger, times(0)).log();
    assertThat(logOutput).atDebugLevel()
        .contains("Adding job testGroup.testJob1. Total pending tenant job count: 1")
        .contains("Removing job testGroup.testJob1. Total pending tenant job count: 0")
        .doesNotContain("Scheduling 1 jobs on scheduler");
  }

  @SuppressWarnings("unchecked")
  private void verifyJobs(
      List<Pair<JobDetail, Set<Trigger>>> jobsWithTriggers) throws SchedulerException
  {
    verify(mockQuartzScheduler).scheduleJobs(mapCaptor.capture(), eq(true));
    Map<JobDetail, Set<? extends Trigger>> capturedArgs = mapCaptor.getValue();

    assertThat(capturedArgs).hasSameSizeAs(jobsWithTriggers);

    for (Pair<JobDetail, Set<Trigger>> jobsWithTrigger : jobsWithTriggers) {
      JobDetail jobDetail = jobsWithTrigger.getLeft();
      Set<Trigger> triggers = jobsWithTrigger.getRight();

      assertThat(capturedArgs).containsKey(jobDetail);

      Set<? extends Trigger> argumentTriggers = capturedArgs.get(jobDetail);
      // The quartz method takes a Set of type `? extends Trigger`, but the assertion requires `Set<Trigger>`
      assertThat((Set<Trigger>) argumentTriggers).containsExactlyInAnyOrderElementsOf(triggers);
    }
  }

  private JobDetail createJobDetail(String name, String group) {
    JobDetailImpl jobDetail = new JobDetailImpl();
    jobDetail.setName(name);
    jobDetail.setGroup(group);
    return jobDetail;
  }

  private Trigger createTrigger(String name, String group) {
    SimpleTriggerImpl trigger = new SimpleTriggerImpl();
    trigger.setName(name);
    trigger.setGroup(group);
    return trigger;
  }
}
