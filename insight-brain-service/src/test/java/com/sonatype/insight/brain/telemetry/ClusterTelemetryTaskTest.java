/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClusterTelemetryTaskTest
    extends AbstractComponentTest
{
  @Inject
  private ClusterTelemetryTask clusterTelemetryTask;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private QuartzJobStoreTX quartzJobStoreTXMock;

  @Captor
  private ArgumentCaptor<List<TelemetryData>> allTelemetryDataCaptor;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bind(QuartzJobStoreTX.class).toInstance(quartzJobStoreTXMock);
    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(ClusterTelemetryTask.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute() throws Exception {
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(telemetrySenderMock).send(anyList());
    when(quartzJobStoreTXMock.getSchedulerStateRecords()).thenReturn(Collections.nCopies(2, null));

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      clusterTelemetryTask.execute(mock(JobExecutionContext.class));
    }

    TelemetryPurpose[] expectedPurposes = { //
                                            TelemetryPurpose.DATABASE, //
                                            TelemetryPurpose.HIERARCHY_METRICS, //
                                            TelemetryPurpose.POLICY_STATUS_OVERRIDE, //
                                            TelemetryPurpose.REALM, //
                                            TelemetryPurpose.ROLE_USAGE, //
                                            TelemetryPurpose.REPOSITORY_CONFIGURATION, //
                                            TelemetryPurpose.SOURCE_CONTROL_METRICS, //
                                            TelemetryPurpose.CLUSTER_USAGE, //
    };
    verify(telemetrySenderMock, times(expectedPurposes.length)).send(allTelemetryDataCaptor.capture());
    List<TelemetryData> allTelemetryData =
        allTelemetryDataCaptor.getAllValues().stream().flatMap(List::stream).collect(toList());
    assertThat(allTelemetryData).extracting(TelemetryData::getPurpose).containsOnly(expectedPurposes);
  }

  @Test
  public void testRegister() {
    clusterTelemetryTask.register();

    verify(taskSchedulerMock)
        .schedulePeriodicTask(ClusterTelemetryTask.class, ClusterTelemetryTask.NAME, Duration.ofDays(1));
  }
}
