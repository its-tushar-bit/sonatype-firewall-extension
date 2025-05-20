/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.AggregatedPRStats;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.ApplicationPRStats;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;

import static com.sonatype.insight.brain.telemetry.SourceControlMetricsTelemetryCollector.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SourceControlMetricsTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private SourceControlPullRequestDAO sourceControlPullRequestDAO;

  @Mock
  private SourceControlDAO sourceControlDAO;

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
    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    when(mockContext.getPreviousFireTime()).thenReturn(new Date());

    when(sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled()).thenReturn(new ArrayList<>());
    when(sourceControlDAO.getByApplication()).thenReturn(new ArrayList<>());
    when(applicationDAO.getAll()).thenReturn(new ArrayList<>());
    when(metrics.computeStatsAndReset()).thenReturn(new AggregatedPRStats(Collections.emptyList()));

    assertThat(collector.collectData(mockContext).getAttributes())
        .isNotEmpty()
        .hasSize(23)
        .containsOnly(entry(TOTAL_SC_WITH_REMEDIATION_PRS_ENABLED, "0"),
            entry(TOTAL_APPLICATION_SC_ENTRIES, "0"),
            entry(TOTAL_APPLICATIONS, "0"),
            entry(TOTAL_SC_PR_TIME_SPENT, "0"),
            entry(TOTAL_SC_PRS_CREATED, "0"),
            entry(TOTAL_SC_PRS_SUGGESTED, "0"),
            entry(TOTAL_SC_APPLICATIONS_WITH_PRS, "0"),
            entry(TOTAL_SC_EXCEPTIONS_RAISED, "0"),
            entry(TOTAL_SC_GOLDEN_PRS_CREATED, "0"),
            entry(TOTAL_SC_GOLDEN_PRS_SUGGESTED, "0"),
            entry(TOTAL_PULL_REQUESTS_UPDATED_1_W_TO_1_M_AGO, 0),
            entry(TOTAL_PULL_REQUESTS_UPDATED_1_M_TO_2_M_AGO, 0),
            entry(TOTAL_PULL_REQUESTS_UPDATED_2_M_TO_3_M_AGO, 0),
            entry(TOTAL_PULL_REQUESTS_UPDATED_3_M_TO_6_M_AGO, 0),
            entry(TOTAL_PULL_REQUESTS_UPDATED_6_M_AGO_OR_EARLIER, 0),
            entry(TOTAL_SC_AUTOMATIC_PRS_CREATED, 0),
            entry(TOTAL_SC_MANUAL_PRS_CREATED, 0),
            entry(TOTAL_SC_AUTOMATIC_PRS_CLOSED, 0),
            entry(TOTAL_SC_MANUAL_PRS_CLOSED, 0),
            entry(TOTAL_SC_AUTOMATIC_PRS_MERGED, 0),
            entry(TOTAL_SC_MANUAL_PRS_MERGED, 0),
            entry(TOTAL_SC_AUTOMATIC_PRS_MISSING, 0),
            entry(TOTAL_SC_MANUAL_PRS_MISSING, 0));
  }

  @Test
  public void test_collectData_fullLists() {
    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    when(mockContext.getPreviousFireTime()).thenReturn(new Date());

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
        .thenReturn(new AggregatedPRStats(Collections.singletonList(new ApplicationPRStats("foo", 1, 2, 3, 1,
            5, 6))));

    setupPrBranchTestData();

    assertThat(collector.collectData(mockContext).getAttributes())
        .isNotEmpty()
        .hasSize(23)
        .containsOnly(entry(TOTAL_SC_WITH_REMEDIATION_PRS_ENABLED, "2"),
            entry(TOTAL_APPLICATION_SC_ENTRIES, "3"),
            entry(TOTAL_APPLICATIONS, "4"),
            entry(TOTAL_SC_PR_TIME_SPENT, "1"),
            entry(TOTAL_SC_PRS_CREATED, "2"),
            entry(TOTAL_SC_PRS_SUGGESTED, "3"),
            entry(TOTAL_SC_APPLICATIONS_WITH_PRS, "1"),
            entry(TOTAL_SC_EXCEPTIONS_RAISED, "1"),
            entry(TOTAL_SC_GOLDEN_PRS_CREATED, "5"),
            entry(TOTAL_SC_GOLDEN_PRS_SUGGESTED, "6"),
            entry(TOTAL_PULL_REQUESTS_UPDATED_1_W_TO_1_M_AGO, 1),
            entry(TOTAL_PULL_REQUESTS_UPDATED_1_M_TO_2_M_AGO, 2),
            entry(TOTAL_PULL_REQUESTS_UPDATED_2_M_TO_3_M_AGO, 1),
            entry(TOTAL_PULL_REQUESTS_UPDATED_3_M_TO_6_M_AGO, 2),
            entry(TOTAL_PULL_REQUESTS_UPDATED_6_M_AGO_OR_EARLIER, 1),
            entry(TOTAL_SC_AUTOMATIC_PRS_CREATED, 0),
            entry(TOTAL_SC_MANUAL_PRS_CREATED, 0),
            entry(TOTAL_SC_AUTOMATIC_PRS_CLOSED, 0),
            entry(TOTAL_SC_MANUAL_PRS_CLOSED, 0),
            entry(TOTAL_SC_AUTOMATIC_PRS_MERGED, 0),
            entry(TOTAL_SC_MANUAL_PRS_MERGED, 0),
            entry(TOTAL_SC_AUTOMATIC_PRS_MISSING, 0),
            entry(TOTAL_SC_MANUAL_PRS_MISSING, 0));
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(collector.isClusterTelemetry()).isTrue();
  }

  @Test
  public void testCollectAutoAndManualPRStats() {
    // Create a timestamp for "previous collection time", one day ago
    Date now = new Date();
    Date previousFireTime = Date.from(now.toInstant().minus(1, ChronoUnit.DAYS));

    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    when(mockContext.getPreviousFireTime()).thenReturn(previousFireTime);

    // Mock the other DAO responses to avoid NPEs
    when(sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled()).thenReturn(new ArrayList<>());
    when(sourceControlDAO.getByApplication()).thenReturn(new ArrayList<>());
    when(applicationDAO.getAll()).thenReturn(new ArrayList<>());
    when(metrics.computeStatsAndReset()).thenReturn(new AggregatedPRStats(Collections.emptyList()));

    // Create test PRs with different states and sources
    setupAutoManualPRTestData(previousFireTime);

    // Collect telemetry data
    TelemetryData result = collector.collectData(mockContext);

    // Verify the Auto/Manual PR stats
    assertThat(result.getAttributes())

        // Note: includes merged and closed PRs that have createTimes since the previous fire time
        .containsEntry(TOTAL_SC_AUTOMATIC_PRS_CREATED, 6)
        .containsEntry(TOTAL_SC_MANUAL_PRS_CREATED, 4)

        .containsEntry(TOTAL_SC_AUTOMATIC_PRS_CLOSED, 1)
        .containsEntry(TOTAL_SC_MANUAL_PRS_CLOSED, 2)
        .containsEntry(TOTAL_SC_AUTOMATIC_PRS_MERGED, 2)
        .containsEntry(TOTAL_SC_MANUAL_PRS_MERGED, 1)
        .containsEntry(TOTAL_SC_AUTOMATIC_PRS_MISSING, 1)
        .containsEntry(TOTAL_SC_MANUAL_PRS_MISSING, 0);
  }

  @Test
  public void testNonOpenPRsAreDeletedAfterCollection() {
    // Create a timestamp for "previous collection time", one day ago
    Date now = new Date();
    Date previousFireTime = Date.from(now.toInstant().minus(1, ChronoUnit.DAYS));

    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    when(mockContext.getPreviousFireTime()).thenReturn(previousFireTime);

    // Mock the other DAO responses to avoid NPEs
    when(sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled()).thenReturn(new ArrayList<>());
    when(sourceControlDAO.getByApplication()).thenReturn(new ArrayList<>());
    when(applicationDAO.getAll()).thenReturn(new ArrayList<>());
    when(metrics.computeStatsAndReset()).thenReturn(new AggregatedPRStats(Collections.emptyList()));

    // Create test PRs with different states and sources
    setupAutoManualPRTestData(previousFireTime);

    // Get IDs of the non-OPEN PRs that should be deleted after collection
    var nonOpenPrIds = List.of(4, 5, 6, 7, 8, 9, 10);

    // Verify that the PRs exist before collection
    for (var id : nonOpenPrIds) {
      SourceControlPullRequest pr = sourceControlPullRequestDAO.getByRepositoryUrlAndPullRequestId("testRepoUrl", id);
      assertThat(pr).isNotNull();
    }

    // Collect telemetry data
    collector.collectData(mockContext);

    // Verify that the non-OPEN PRs are deleted after collection
    for (var id : nonOpenPrIds) {
      SourceControlPullRequest pr = sourceControlPullRequestDAO.getByRepositoryUrlAndPullRequestId("testRepoUrl", id);
      assertThat(pr).isNull();
    }

    // Verify that OPEN PRs still exist
    for (int id = 1; id <= 3; id++) {
      SourceControlPullRequest pr = sourceControlPullRequestDAO.getByRepositoryUrlAndPullRequestId("testRepoUrl", id);
      assertThat(pr).isNotNull();
    }
    for (int id = 11; id <= 13; id++) {
      SourceControlPullRequest pr = sourceControlPullRequestDAO.getByRepositoryUrlAndPullRequestId("testRepoUrl", id);
      assertThat(pr).isNotNull();
    }
  }

  private void setupAutoManualPRTestData(Date previousTime) {
    Date now = new Date();

    // Create OPEN PRs created after the previous collection time
    // 2 AUTOMATIC OPEN PRs
    createSourceControlPullRequest(1, PullRequestState.OPEN, PullRequestSource.AUTOMATIC, now);
    createSourceControlPullRequest(2, PullRequestState.OPEN, PullRequestSource.AUTOMATIC, now);

    // 1 MANUAL OPEN PR
    createSourceControlPullRequest(3, PullRequestState.OPEN, PullRequestSource.MANUAL, now);

    // Create non-OPEN PRs
    // 1 AUTOMATIC CLOSED PR
    createSourceControlPullRequest(4, PullRequestState.CLOSED, PullRequestSource.AUTOMATIC, now);

    // 2 MANUAL CLOSED PRs
    createSourceControlPullRequest(5, PullRequestState.CLOSED, PullRequestSource.MANUAL, now);
    createSourceControlPullRequest(6, PullRequestState.CLOSED, PullRequestSource.MANUAL, now);

    // 2 AUTOMATIC MERGED PRs
    createSourceControlPullRequest(7, PullRequestState.MERGED, PullRequestSource.AUTOMATIC, now);
    createSourceControlPullRequest(8, PullRequestState.MERGED, PullRequestSource.AUTOMATIC, now);

    // 1 MANUAL MERGED PR
    createSourceControlPullRequest(9, PullRequestState.MERGED, PullRequestSource.MANUAL, now);

    // 1 AUTOMATIC MISSING PR
    createSourceControlPullRequest(10, PullRequestState.MISSING, PullRequestSource.AUTOMATIC, now);

    // Create some PRs with dates before the cutoff to ensure they aren't counted
    Date oldDate = Date.from(previousTime.toInstant().minus(1, ChronoUnit.DAYS));

    // These should not be counted in the stats because they were created before the previous fire time
    createSourceControlPullRequest(11, PullRequestState.OPEN, PullRequestSource.AUTOMATIC, oldDate);
    createSourceControlPullRequest(12, PullRequestState.OPEN, PullRequestSource.MANUAL, oldDate);

    // This should not be counted because it has an EXTERNAL source
    createSourceControlPullRequest(13, PullRequestState.OPEN, PullRequestSource.EXTERNAL, now);
  }

  private SourceControlPullRequest createSourceControlPullRequest(
      int id,
      PullRequestState state,
      PullRequestSource source,
      Date createTime)
  {
    return tempEntity.newSourceControlPullRequest(
        "testRepoUrl",
        id,
        "sha",
        "b-sha",
        "branch" + id,
        "main",
        createTime,
        new Date(),
        new Date(),
        state,
        source
    );
  }

  private void setupPrBranchTestData() {
    // add 1 record with update time older than 1 week
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -10);
    Date updateTime = calendar.getTime();
    tempEntity.newSourceControlPullRequest("repoUrl", 1, "sha", "b-sha", "b-1", "bb",
        new Date(), new Date(), updateTime);

    // add 2 records with update time older than 1 month
    calendar.add(Calendar.DATE, -25);
    updateTime = calendar.getTime();
    tempEntity.newSourceControlPullRequest("repoUrl", 2, "sha", "b-sha", "b-2", "bb",
        new Date(), new Date(), updateTime);
    tempEntity.newSourceControlPullRequest("repoUrl", 3, "sha", "b-sha", "b-3", "bb",
        new Date(), new Date(), updateTime);

    // add 1 record with update time older than 2 months
    calendar.add(Calendar.MONTH, -1);
    updateTime = calendar.getTime();
    tempEntity.newSourceControlPullRequest("repoUrl", 4, "sha", "b-sha", "b-4", "bb",
        new Date(), new Date(), updateTime);

    // add 2 records with update time older than 3 month
    calendar.add(Calendar.MONTH, -1);
    updateTime = calendar.getTime();
    tempEntity.newSourceControlPullRequest("repoUrl", 5, "sha", "b-sha", "b-5", "bb",
        new Date(), new Date(), updateTime);
    tempEntity.newSourceControlPullRequest("repoUrl", 6, "sha", "b-sha", "b-6", "bb",
        new Date(), new Date(), updateTime);

    // add 1 record with update time older than 6 months
    calendar.add(Calendar.MONTH, -4);
    updateTime = calendar.getTime();
    tempEntity.newSourceControlPullRequest("repoUrl", 7, "sha", "b-sha", "b-7", "bb",
        new Date(), new Date(), updateTime);
  }
}
