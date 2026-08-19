/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.api.admin.service.TenantService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MultiTenantDefaultBranchMonitorExecutorTest
{
  private MultiTenantDefaultBranchMonitorExecutor underTest;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private SourceControlDAO mockSourceControlDAO;

  @Mock
  private SourceControlEventPublisher mockEventPublisher;

  @Mock
  private TenantService mockTenantService;

  private final List<String> allTenants = Arrays.asList("slug1", "slug2", "slug3", "slug4");

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(mockTenantService.getTenantSlug()).thenReturn("slug1");
    lenient().when(mockTenantService.getAllTenantsNames()).thenReturn(allTenants);
    underTest = new MultiTenantDefaultBranchMonitorExecutor(mockTaskScheduler, mockSourceControlDAO, mockEventPublisher,
        mockTenantService);
  }

  @Test
  public void whenASingleTenant_ShouldBeStartOfDay() {
    LocalDateTime localDateTime = underTest.calculateStartTime("slug", Collections.singletonList("slug"));

    assertThat(localDateTime.getHour()).isEqualTo(0);
    assertThat(localDateTime.getMinute()).isEqualTo(0);
    assertThat(localDateTime.getSecond()).isEqualTo(0);
  }

  @Test
  public void whenMultipleTenants_ShouldBeEquallySpaced() {
    LocalDateTime localDateTime = underTest.calculateStartTime("slug1", allTenants);
    verifySlugStartTime(LocalDateTime.from(localDateTime), 0, 0, 0);

    localDateTime = underTest.calculateStartTime("slug2", allTenants);
    verifySlugStartTime(LocalDateTime.from(localDateTime), 6, 0, 0);

    localDateTime = underTest.calculateStartTime("slug3", allTenants);
    verifySlugStartTime(LocalDateTime.from(localDateTime), 12, 0, 0);

    localDateTime = underTest.calculateStartTime("slug4", allTenants);
    verifySlugStartTime(LocalDateTime.from(localDateTime), 18, 0, 0);
  }

  @Test
  public void whenTenantDoesNotExist_ScheduleNotStarted() {
    when(mockTenantService.getTenantSlug()).thenReturn("does not exist");

    underTest.schedule(mock(InsightJob.class));

    verify(mockTaskScheduler, never()).scheduleOneTimeTask(any(InsightJob.class), any(LocalTime.class));
  }

  @Test
  public void whenTenantExists_ScheduleStarted() {
    underTest.schedule(mock(InsightJob.class));

    verify(mockTaskScheduler).scheduleOneTimeTask(any(InsightJob.class), any(LocalDateTime.class));
  }

  @Test
  public void performScan_shouldScheduleJobAgain() {
    underTest.performScan(mock(InsightJob.class));

    // Job is rescheduled
    verify(mockTaskScheduler).scheduleOneTimeTask(any(InsightJob.class), any(LocalDateTime.class));
  }

  @Test
  public void rescheduleJobWillAlterStartTimeIfTenantsChange() {
    ArgumentCaptor<LocalDateTime> argument = ArgumentCaptor.forClass(LocalDateTime.class);

    when(mockTenantService.getTenantSlug()).thenReturn("slug2");

    underTest.performScan(mock(InsightJob.class));
    verify(mockTaskScheduler).scheduleOneTimeTask(any(), argument.capture());
    verifySlugStartTime(argument.getValue(), 6, 0, 0);

    when(mockTenantService.getAllTenantsNames()).thenReturn(
        Arrays.asList("slug1", "slug2", "slug3", "slug4", "slug5"));

    underTest.performScan(mock(InsightJob.class));
    verify(mockTaskScheduler, times(2)).scheduleOneTimeTask(any(), argument.capture());
    verifySlugStartTime(argument.getValue(), 4, 48, 0);
  }

  private void verifySlugStartTime(final LocalDateTime localDateTime, int hour, int minutes, int seconds) {
    assertThat(localDateTime.getHour()).isEqualTo(hour);
    assertThat(localDateTime.getMinute()).isEqualTo(minutes);
    assertThat(localDateTime.getSecond()).isEqualTo(seconds);
  }
}
