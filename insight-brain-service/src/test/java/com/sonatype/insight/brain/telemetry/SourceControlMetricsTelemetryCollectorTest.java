/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.AggregatedPRStats;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.ApplicationPRStats;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.telemetry.SourceControlMetricsTelemetryCollector.TOTAL_APPLICATIONS;
import static com.sonatype.insight.brain.telemetry.SourceControlMetricsTelemetryCollector.TOTAL_APPLICATION_SC_ENTRIES;
import static com.sonatype.insight.brain.telemetry.SourceControlMetricsTelemetryCollector.TOTAL_SC_APPLICATIONS_WITH_PRS;
import static com.sonatype.insight.brain.telemetry.SourceControlMetricsTelemetryCollector.TOTAL_SC_EXCEPTIONS_RAISED;
import static com.sonatype.insight.brain.telemetry.SourceControlMetricsTelemetryCollector.TOTAL_SC_PRS_CREATED;
import static com.sonatype.insight.brain.telemetry.SourceControlMetricsTelemetryCollector.TOTAL_SC_PRS_SUGGESTED;
import static com.sonatype.insight.brain.telemetry.SourceControlMetricsTelemetryCollector.TOTAL_SC_PR_TIME_SPENT;
import static com.sonatype.insight.brain.telemetry.SourceControlMetricsTelemetryCollector.TOTAL_SC_WITH_PR_ENABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

public class SourceControlMetricsTelemetryCollectorTest extends AbstractComponentTest
{
  @Mock
  private SourceControlDAO sourceControlDAO;

  @Mock
  private ApplicationDAO applicationDAO;
  
  @Mock
  private SourceControlPullRequestMetrics metrics;
  
  private SourceControlMetricsTelemetryCollector collector;

  @Before
  public void setup() {
    collector = new SourceControlMetricsTelemetryCollector(sourceControlDAO, applicationDAO, metrics);
  }

  @Test
  public void test_collectData_emptyLists() {
    when(sourceControlDAO.getApplicationsWithPullReqsEnabled()).thenReturn(new ArrayList<>());
    when(sourceControlDAO.getByApplication()).thenReturn(new ArrayList<>());
    when(applicationDAO.getAll()).thenReturn(new ArrayList<>());
    when(metrics.computeStatsAndReset()).thenReturn(new AggregatedPRStats(Collections.emptyList()));

    assertThat(collector.collectData().getAttributes())
        .isNotEmpty()
        .hasSize(8)
        .containsOnly(entry(TOTAL_SC_WITH_PR_ENABLED, "0"),
            entry(TOTAL_APPLICATION_SC_ENTRIES, "0"),
            entry(TOTAL_APPLICATIONS, "0"),
            entry(TOTAL_SC_PR_TIME_SPENT, "0"),
            entry(TOTAL_SC_PRS_CREATED, "0"),
            entry(TOTAL_SC_PRS_SUGGESTED, "0"),
            entry(TOTAL_SC_APPLICATIONS_WITH_PRS, "0"),
            entry(TOTAL_SC_EXCEPTIONS_RAISED, "0"));
  }

  @Test
  public void test_collectData_fullLists() {
    when(sourceControlDAO.getApplicationsWithPullReqsEnabled())
        .thenReturn(Arrays.asList(
            new SourceControl.Builder().build(),
            new SourceControl.Builder().build()
        ));
    when(sourceControlDAO.getByApplication())
        .thenReturn(Arrays.asList(
            new SourceControl.Builder().build(),
            new SourceControl.Builder().build(),
            new SourceControl.Builder().build()
        ));
    when(applicationDAO.getAll()).thenReturn(Arrays.asList(
        new Application(), new Application(), new Application(), new Application()
    ));
    when(metrics.computeStatsAndReset())
        .thenReturn(new AggregatedPRStats(Collections.singletonList(new ApplicationPRStats("foo", 1, 2, 3, 1))));

    assertThat(collector.collectData().getAttributes())
        .isNotEmpty()
        .hasSize(8)
        .containsOnly(entry(TOTAL_SC_WITH_PR_ENABLED, "2"),
            entry(TOTAL_APPLICATION_SC_ENTRIES, "3"),
            entry(TOTAL_APPLICATIONS, "4"),
            entry(TOTAL_SC_PR_TIME_SPENT, "1"),
            entry(TOTAL_SC_PRS_CREATED, "2"),
            entry(TOTAL_SC_PRS_SUGGESTED, "3"),
            entry(TOTAL_SC_APPLICATIONS_WITH_PRS, "1"),
            entry(TOTAL_SC_EXCEPTIONS_RAISED, "1"));
  }
}
