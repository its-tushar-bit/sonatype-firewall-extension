/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.AggregatedPRStats;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.ApplicationPRStats;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.telemetry.SourceControlMetricsTelemetryCollector.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

public class SourceControlMetricsTelemetryCollectorTest extends AbstractComponentTest
{
  @Mock
  private SourceControlDAO sourceControlDAO;

  private final SourceControlPullRequestDAO sourceControlPullRequestDAO = new SourceControlPullRequestDAO();

  @Mock
  private ApplicationDAO applicationDAO;
  
  @Mock
  private SourceControlPullRequestMetrics metrics;
  
  private SourceControlMetricsTelemetryCollector collector;

  @Before
  public void setup() {
    collector =
        new SourceControlMetricsTelemetryCollector(sourceControlDAO, sourceControlPullRequestDAO, applicationDAO,
            metrics);
  }

  @Test
  public void test_collectData_emptyLists() {
    when(sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled()).thenReturn(new ArrayList<>());
    when(sourceControlDAO.getByApplication()).thenReturn(new ArrayList<>());
    when(applicationDAO.getAll()).thenReturn(new ArrayList<>());
    when(metrics.computeStatsAndReset()).thenReturn(new AggregatedPRStats(Collections.emptyList()));

    assertThat(collector.collectData().getAttributes())
        .isNotEmpty()
        .hasSize(13)
        .containsOnly(entry(TOTAL_SC_WITH_REMEDIATION_PRS_ENABLED, "0"),
            entry(TOTAL_APPLICATION_SC_ENTRIES, "0"),
            entry(TOTAL_APPLICATIONS, "0"),
            entry(TOTAL_SC_PR_TIME_SPENT, "0"),
            entry(TOTAL_SC_PRS_CREATED, "0"),
            entry(TOTAL_SC_PRS_SUGGESTED, "0"),
            entry(TOTAL_SC_APPLICATIONS_WITH_PRS, "0"),
            entry(TOTAL_SC_EXCEPTIONS_RAISED, "0"),
            entry(TOTAL_PULL_REQUESTS_UPDATED_1_W_TO_1_M_AGO, 0),
            entry(TOTAL_PULL_REQUESTS_UPDATED_1_M_TO_2_M_AGO, 0),
            entry(TOTAL_PULL_REQUESTS_UPDATED_2_M_TO_3_M_AGO, 0),
            entry(TOTAL_PULL_REQUESTS_UPDATED_3_M_TO_6_M_AGO, 0),
            entry(TOTAL_PULL_REQUESTS_UPDATED_6_M_AGO_OR_EARLIER, 0));
  }

  @Test
  public void test_collectData_fullLists() {
    when(sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled())
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

    setupPrBranchTestData();

    assertThat(collector.collectData().getAttributes())
        .isNotEmpty()
        .hasSize(13)
        .containsOnly(entry(TOTAL_SC_WITH_REMEDIATION_PRS_ENABLED, "2"),
            entry(TOTAL_APPLICATION_SC_ENTRIES, "3"),
            entry(TOTAL_APPLICATIONS, "4"),
            entry(TOTAL_SC_PR_TIME_SPENT, "1"),
            entry(TOTAL_SC_PRS_CREATED, "2"),
            entry(TOTAL_SC_PRS_SUGGESTED, "3"),
            entry(TOTAL_SC_APPLICATIONS_WITH_PRS, "1"),
            entry(TOTAL_SC_EXCEPTIONS_RAISED, "1"),
            entry(TOTAL_PULL_REQUESTS_UPDATED_1_W_TO_1_M_AGO, 1),
            entry(TOTAL_PULL_REQUESTS_UPDATED_1_M_TO_2_M_AGO, 2),
            entry(TOTAL_PULL_REQUESTS_UPDATED_2_M_TO_3_M_AGO, 1),
            entry(TOTAL_PULL_REQUESTS_UPDATED_3_M_TO_6_M_AGO, 2),
            entry(TOTAL_PULL_REQUESTS_UPDATED_6_M_AGO_OR_EARLIER, 1));
  }

  private void setupPrBranchTestData() {
    // add 1 record with update time older than 1 week
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -10);
    Date updateTime = calendar.getTime();
    tempEntity.newSourceControlPullRequest("repoUrl", 1, "sha", "b-1", new Date(), new Date(),
        updateTime);

    // add 2 records with update time older than 1 month
    calendar.add(Calendar.DATE, -25);
    updateTime = calendar.getTime();
    tempEntity.newSourceControlPullRequest("repoUrl", 2, "sha", "b-2", new Date(), new Date(),
        updateTime);
    tempEntity.newSourceControlPullRequest("repoUrl", 3, "sha", "b-3", new Date(), new Date(),
        updateTime);

    // add 1 record with update time older than 2 months
    calendar.add(Calendar.MONTH, -1);
    updateTime = calendar.getTime();
    tempEntity.newSourceControlPullRequest("repoUrl", 4, "sha", "b-4", new Date(), new Date(),
        updateTime);

    // add 2 records with update time older than 3 month
    calendar.add(Calendar.MONTH, -1);
    updateTime = calendar.getTime();
    tempEntity.newSourceControlPullRequest("repoUrl", 5, "sha", "b-5", new Date(), new Date(),
        updateTime);
    tempEntity.newSourceControlPullRequest("repoUrl", 6, "sha", "b-6", new Date(), new Date(),
        updateTime);

    // add 1 record with update time older than 6 months
    calendar.add(Calendar.MONTH, -4);
    updateTime = calendar.getTime();
    tempEntity.newSourceControlPullRequest("repoUrl", 7, "sha", "b-7", new Date(), new Date(),
        updateTime);
  }
}
