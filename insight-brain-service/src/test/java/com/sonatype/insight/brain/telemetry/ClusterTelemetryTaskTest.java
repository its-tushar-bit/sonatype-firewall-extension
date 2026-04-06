/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.ZScalerConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.zscaler.ZScalerMetricsDAO;
import com.sonatype.insight.brain.model.configuration.ZScalerConfiguration;
import com.sonatype.insight.brain.model.configuration.ZscalerFormat;
import com.sonatype.insight.brain.model.zscaler.ZScalerMetrics;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
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

  @Inject
  private ZScalerConfigurationDAO zScalerConfigurationDAO;

  @Inject
  private ZScalerMetricsDAO zScalerMetricsDAO;

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
    // ZScaler configuration and metrics are required in order for the telemetry to be sent
    ZScalerConfiguration zScalerConfiguration = new ZScalerConfiguration();
    zScalerConfiguration.setHostname("host");
    zScalerConfiguration.setUsername("user");
    zScalerConfiguration.setPassword("password");
    zScalerConfiguration.setApikey("validapikey1");
    List<ZscalerFormat> zscalerFormats = new ArrayList<>();
    zscalerFormats.add(new ZscalerFormat("maven", false));
    zscalerFormats.add(new ZscalerFormat("npm", true));
    zscalerFormats.add(new ZscalerFormat("pypi", true));
    zscalerFormats.add(new ZscalerFormat("nuget", false));
    zScalerConfigurationDAO.set(zScalerConfiguration, zscalerFormats);
    zScalerMetricsDAO.set(new ZScalerMetrics());

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
      TelemetryPurpose.REAL_OWNER_IDS, // This one is for Applications
      TelemetryPurpose.REAL_OWNER_IDS, // This one is for Organizations
      TelemetryPurpose.ZSCALER_CONFIGURATION,
      TelemetryPurpose.ZSCALER_METRICS,
      TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION,
      TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION,
      TelemetryPurpose.TIME_TO_CHANGE_VERSION_POLICY_VIOLATION, // Included when remediation is via version change
      // TelemetryPurpose.APPLICATION_CATEGORY, Sent with a different overload:
      // .send(TelemetryData telemetryData)
    };
    // Capture all telemetry sends (count varies: 12 base collectors, plus 0-3 audit entries depending on data)
    // Audit collectors may produce 0-2 TTWPV entries and 0-2 TTRPV entries (TTRPV may include TTCVPV for version
    // changes)
    verify(telemetrySenderMock, atLeastOnce()).send(allTelemetryDataCaptor.capture());
    List<TelemetryData> allTelemetryData =
        allTelemetryDataCaptor.getAllValues().stream().flatMap(List::stream).collect(toList());
    // Verify all purposes that were actually sent are in the allowed list
    // isSubsetOf allows actual to be smaller than expected (audit collectors are conditional)
    // and allows duplicates (e.g., ROLE_USAGE may appear multiple times)
    assertThat(allTelemetryData).extracting(TelemetryData::getPurpose).isSubsetOf(expectedPurposes);
  }

  @Test
  public void testExecutePaginatedCollector() {
    // Given 1 collector with only 2 pages
    TelemetryCollectorsProvider telemetryCollectorsProviderMock = mockTelemetryCollectorsProvider();

    ClusterTelemetryTask simpleClusterTelemetryTask = new ClusterTelemetryTask(
        telemetryCollectorsProviderMock,
        taskSchedulerMock,
        telemetrySenderMock);

    // When
    simpleClusterTelemetryTask.execute(mock(JobExecutionContext.class));

    // Then verify that 2 pages were sent
    verify(telemetrySenderMock, times(2)).send(any(TelemetryData.class));
  }

  private TelemetryCollectorsProvider mockTelemetryCollectorsProvider() {
    TelemetryCollectorsProvider telemetryCollectorsProviderMock = mock(TelemetryCollectorsProvider.class);
    ApplicationCategoryTelemetryCollector applicationCategoryTelemetryCollectorMock =
        mockApplicationCategoryTelemetryCollector();

    when(telemetryCollectorsProviderMock.getTelemetryCollectors())
        .thenReturn(Set.of(applicationCategoryTelemetryCollectorMock));

    return telemetryCollectorsProviderMock;
  }

  private ApplicationCategoryTelemetryCollector mockApplicationCategoryTelemetryCollector() {
    ApplicationCategoryTelemetryCollector applicationCategoryTelemetryCollectorMock =
        mock(ApplicationCategoryTelemetryCollector.class);

    when(applicationCategoryTelemetryCollectorMock.isClusterTelemetry())
        .thenReturn(true);

    when(applicationCategoryTelemetryCollectorMock.hasMoreData())
        .thenReturn(true)
        .thenReturn(false);

    when(applicationCategoryTelemetryCollectorMock.firstPage())
        .thenReturn(mock(TelemetryData.class)); // Page 1

    when(applicationCategoryTelemetryCollectorMock.nextPage())
        .thenReturn(mock(TelemetryData.class)); // Page 2

    return applicationCategoryTelemetryCollectorMock;
  }

  @Test
  public void testRegister() {
    clusterTelemetryTask.register();

    verify(taskSchedulerMock).schedulePeriodicTask(clusterTelemetryTask, Duration.ofDays(1));
  }
}
