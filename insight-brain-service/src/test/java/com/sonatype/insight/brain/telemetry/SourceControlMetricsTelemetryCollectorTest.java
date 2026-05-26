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
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.githubapp.GitHubAppDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.githubapp.GitHubApp;
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
import static org.mockito.ArgumentMatchers.any;
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

  @Mock
  private OrganizationDAO organizationDAO;

  @Mock
  private GitHubAppDAO gitHubAppDAO;

  @Mock
  private SourceControlEventDAO sourceControlEventDAO;

  private SourceControlMetricsTelemetryCollector collector;

  @Before
  public void setup() {
    collector =
        new SourceControlMetricsTelemetryCollector(sourceControlDAO, sourceControlPullRequestDAO, applicationDAO,
            metrics, organizationDAO, gitHubAppDAO, sourceControlEventDAO);
  }

  @Override
  protected void setUpTestLicenseThreatGroups() {
    // This test class uses mocked DAOs for telemetry inputs and does not need LTG fixture data.
  }

  @Test
  public void test_collectData_emptyLists() {
    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    when(mockContext.getPreviousFireTime()).thenReturn(new Date());

    when(sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled()).thenReturn(new ArrayList<>());
    when(sourceControlDAO.getByApplication()).thenReturn(new ArrayList<>());
    when(sourceControlDAO.getAll()).thenReturn(new ArrayList<>());
    when(applicationDAO.getAll()).thenReturn(new ArrayList<>());
    when(organizationDAO.getAll()).thenReturn(new ArrayList<>());
    when(gitHubAppDAO.getAll()).thenReturn(new ArrayList<>());
    when(sourceControlEventDAO.countSuccessfulPullRequestsByAuthenticationTypeSince(any()))
        .thenReturn(Collections.emptyMap());
    when(metrics.computeStatsAndReset()).thenReturn(new AggregatedPRStats(Collections.emptyList()));

    assertThat(collector.collectData(mockContext).getAttributes())
        .isNotEmpty()
        .hasSize(42)
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
            entry(TOTAL_SC_AUTOMATIC_PRS_AUTO_CLOSED, 0),
            entry(TOTAL_SC_MANUAL_PRS_CLOSED, 0),
            entry(TOTAL_SC_AUTOMATIC_PRS_MERGED, 0),
            entry(TOTAL_SC_MANUAL_PRS_MERGED, 0),
            entry(TOTAL_SC_AUTOMATIC_PRS_MISSING, 0),
            entry(TOTAL_SC_MANUAL_PRS_MISSING, 0),
            entry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_CREATED, 0),
            entry(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_CREATED, 0),
            entry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_CLOSED, 0),
            entry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_AUTO_CLOSED, 0),
            entry(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_CLOSED, 0),
            entry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_MERGED, 0),
            entry(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_MERGED, 0),
            entry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_MISSING, 0),
            entry(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_MISSING, 0),
            entry(TOTAL_SC_ORGS_WITH_AUTH_CONFIGURED, "0"),
            entry(TOTAL_SC_APPS_WITH_AUTH_CONFIGURED, "0"),
            entry(TOTAL_SC_ORGS_USING_PAT, "0"),
            entry(TOTAL_SC_ORGS_USING_GITHUB_APP, "0"),
            entry(TOTAL_SC_APPS_USING_PAT, "0"),
            entry(TOTAL_SC_APPS_USING_GITHUB_APP, "0"),
            entry(TOTAL_SC_GITHUB_APP_INSTALLATIONS, "0"),
            entry(TOTAL_DAILY_SC_PRS_USING_PAT, "0"),
            entry(TOTAL_DAILY_SC_PRS_USING_GITHUB_APP, "0"));
  }

  @Test
  public void test_collectData_fullLists() {
    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    when(mockContext.getPreviousFireTime()).thenReturn(new Date());

    when(sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled())
        .thenReturn(Arrays.asList(
            new SourceControl.Builder().build(),
            new SourceControl.Builder().build()));
    when(sourceControlDAO.getByApplication())
        .thenReturn(Arrays.asList(
            new SourceControl.Builder().build(),
            new SourceControl.Builder().build(),
            new SourceControl.Builder().build()));
    when(sourceControlDAO.getAll()).thenReturn(new ArrayList<>());
    when(applicationDAO.getAll()).thenReturn(Arrays.asList(
        new Application(), new Application(), new Application(), new Application()));
    when(organizationDAO.getAll()).thenReturn(new ArrayList<>());
    when(gitHubAppDAO.getAll()).thenReturn(new ArrayList<>());
    when(sourceControlEventDAO.countSuccessfulPullRequestsByAuthenticationTypeSince(any()))
        .thenReturn(Collections.emptyMap());
    when(metrics.computeStatsAndReset())
        .thenReturn(new AggregatedPRStats(Collections.singletonList(new ApplicationPRStats("foo", 1, 2, 3, 1,
            5, 6))));

    setupPrBranchTestData();

    assertThat(collector.collectData(mockContext).getAttributes())
        .isNotEmpty()
        .hasSize(42)
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
            entry(TOTAL_SC_AUTOMATIC_PRS_AUTO_CLOSED, 0),
            entry(TOTAL_SC_MANUAL_PRS_CLOSED, 0),
            entry(TOTAL_SC_AUTOMATIC_PRS_MERGED, 0),
            entry(TOTAL_SC_MANUAL_PRS_MERGED, 0),
            entry(TOTAL_SC_AUTOMATIC_PRS_MISSING, 0),
            entry(TOTAL_SC_MANUAL_PRS_MISSING, 0),
            entry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_CREATED, 0),
            entry(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_CREATED, 0),
            entry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_CLOSED, 0),
            entry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_AUTO_CLOSED, 0),
            entry(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_CLOSED, 0),
            entry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_MERGED, 0),
            entry(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_MERGED, 0),
            entry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_MISSING, 0),
            entry(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_MISSING, 0),
            entry(TOTAL_SC_ORGS_WITH_AUTH_CONFIGURED, "0"),
            entry(TOTAL_SC_APPS_WITH_AUTH_CONFIGURED, "0"),
            entry(TOTAL_SC_ORGS_USING_PAT, "0"),
            entry(TOTAL_SC_ORGS_USING_GITHUB_APP, "0"),
            entry(TOTAL_SC_APPS_USING_PAT, "0"),
            entry(TOTAL_SC_APPS_USING_GITHUB_APP, "0"),
            entry(TOTAL_SC_GITHUB_APP_INSTALLATIONS, "0"),
            entry(TOTAL_DAILY_SC_PRS_USING_PAT, "0"),
            entry(TOTAL_DAILY_SC_PRS_USING_GITHUB_APP, "0"));
  }

  @Test
  public void test_collectPullRequestExecutionStats_emitsPatAndGithubAppCounts() {
    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    when(mockContext.getPreviousFireTime()).thenReturn(new Date());

    java.util.Map<String, Long> byAuth = new java.util.HashMap<>();
    byAuth.put("PAT", 7L);
    byAuth.put("GITHUB_APP", 3L);
    when(sourceControlEventDAO.countSuccessfulPullRequestsByAuthenticationTypeSince(any())).thenReturn(byAuth);

    // Stub the rest of the collector's data sources so collectData runs end-to-end.
    when(sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled()).thenReturn(new ArrayList<>());
    when(sourceControlDAO.getByApplication()).thenReturn(new ArrayList<>());
    when(sourceControlDAO.getAll()).thenReturn(new ArrayList<>());
    when(applicationDAO.getAll()).thenReturn(new ArrayList<>());
    when(organizationDAO.getAll()).thenReturn(new ArrayList<>());
    when(gitHubAppDAO.getAll()).thenReturn(new ArrayList<>());
    when(metrics.computeStatsAndReset()).thenReturn(new AggregatedPRStats(Collections.emptyList()));

    assertThat(collector.collectData(mockContext).getAttributes())
        .contains(
            entry(TOTAL_DAILY_SC_PRS_USING_PAT, "7"),
            entry(TOTAL_DAILY_SC_PRS_USING_GITHUB_APP, "3"));
  }

  @Test
  public void test_collectPullRequestExecutionStats_firstFireWithNullPreviousFireTimeStillEmitsCounts() {
    // Quartz returns null for getPreviousFireTime() on the first run after install. The collector must still
    // produce non-zero counts when there is real PR activity in the database.
    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    when(mockContext.getPreviousFireTime()).thenReturn(null);

    java.util.Map<String, Long> byAuth = new java.util.HashMap<>();
    byAuth.put("PAT", 4L);
    byAuth.put("GITHUB_APP", 2L);
    when(sourceControlEventDAO.countSuccessfulPullRequestsByAuthenticationTypeSince(any())).thenReturn(byAuth);

    when(sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled()).thenReturn(new ArrayList<>());
    when(sourceControlDAO.getByApplication()).thenReturn(new ArrayList<>());
    when(sourceControlDAO.getAll()).thenReturn(new ArrayList<>());
    when(applicationDAO.getAll()).thenReturn(new ArrayList<>());
    when(organizationDAO.getAll()).thenReturn(new ArrayList<>());
    when(gitHubAppDAO.getAll()).thenReturn(new ArrayList<>());
    when(metrics.computeStatsAndReset()).thenReturn(new AggregatedPRStats(Collections.emptyList()));

    assertThat(collector.collectData(mockContext).getAttributes())
        .contains(
            entry(TOTAL_DAILY_SC_PRS_USING_PAT, "4"),
            entry(TOTAL_DAILY_SC_PRS_USING_GITHUB_APP, "2"));
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
        .containsEntry(TOTAL_SC_AUTOMATIC_PRS_CREATED, 14)
        .containsEntry(TOTAL_SC_MANUAL_PRS_CREATED, 8)
        .containsEntry(TOTAL_SC_AUTOMATIC_PRS_CLOSED, 2)
        .containsEntry(TOTAL_SC_AUTOMATIC_PRS_AUTO_CLOSED, 2)
        .containsEntry(TOTAL_SC_MANUAL_PRS_CLOSED, 4)
        .containsEntry(TOTAL_SC_AUTOMATIC_PRS_MERGED, 4)
        .containsEntry(TOTAL_SC_MANUAL_PRS_MERGED, 2)
        .containsEntry(TOTAL_SC_AUTOMATIC_PRS_MISSING, 2)
        .containsEntry(TOTAL_SC_MANUAL_PRS_MISSING, 0)

        .containsEntry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_CREATED, 7)
        .containsEntry(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_CREATED, 4)
        .containsEntry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_CLOSED, 1)
        .containsEntry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_AUTO_CLOSED, 1)
        .containsEntry(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_CLOSED, 2)
        .containsEntry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_MERGED, 2)
        .containsEntry(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_MERGED, 1)
        .containsEntry(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_MISSING, 1)
        .containsEntry(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_MISSING, 0);
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
    var nonOpenPrIds = List.of(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);

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
    for (int id = 1; id <= 6; id++) {
      SourceControlPullRequest pr = sourceControlPullRequestDAO.getByRepositoryUrlAndPullRequestId("testRepoUrl", id);
      assertThat(pr).isNotNull();
    }
    for (int id = 21; id <= 26; id++) {
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

    // 2 AUTOMATIC_INNER_SOURCE OPEN PRs
    createSourceControlPullRequest(3, PullRequestState.OPEN, PullRequestSource.AUTOMATIC_INNER_SOURCE, now);
    createSourceControlPullRequest(4, PullRequestState.OPEN, PullRequestSource.AUTOMATIC_INNER_SOURCE, now);

    // 1 MANUAL OPEN PR
    createSourceControlPullRequest(5, PullRequestState.OPEN, PullRequestSource.MANUAL, now);

    // 1 MANUAL_INNER_SOURCE OPEN PR
    createSourceControlPullRequest(6, PullRequestState.OPEN, PullRequestSource.MANUAL_INNER_SOURCE, now);

    // Create non-OPEN PRs
    // 1 AUTOMATIC CLOSED PR
    createSourceControlPullRequest(7, PullRequestState.CLOSED, PullRequestSource.AUTOMATIC, now);

    // 1 AUTOMATIC AUTO CLOSED PR
    createSourceControlPullRequest(27, PullRequestState.AUTO_CLOSED, PullRequestSource.AUTOMATIC, now);

    // 1 AUTOMATIC_INNER_SOURCE CLOSED PR
    createSourceControlPullRequest(8, PullRequestState.CLOSED, PullRequestSource.AUTOMATIC_INNER_SOURCE, now);

    // 1 AUTOMATIC_INNER_SOURCE AUTO CLOSED PR
    createSourceControlPullRequest(28, PullRequestState.AUTO_CLOSED, PullRequestSource.AUTOMATIC_INNER_SOURCE, now);

    // 2 MANUAL CLOSED PRs
    createSourceControlPullRequest(9, PullRequestState.CLOSED, PullRequestSource.MANUAL, now);
    createSourceControlPullRequest(10, PullRequestState.CLOSED, PullRequestSource.MANUAL, now);

    // 2 MANUAL_INNER_SOURCE CLOSED PRs
    createSourceControlPullRequest(11, PullRequestState.CLOSED, PullRequestSource.MANUAL_INNER_SOURCE, now);
    createSourceControlPullRequest(12, PullRequestState.CLOSED, PullRequestSource.MANUAL_INNER_SOURCE, now);

    // 2 AUTOMATIC MERGED PRs
    createSourceControlPullRequest(13, PullRequestState.MERGED, PullRequestSource.AUTOMATIC, now);
    createSourceControlPullRequest(14, PullRequestState.MERGED, PullRequestSource.AUTOMATIC, now);

    // 2 AUTOMATIC_INNER_SOURCE MERGED PRs
    createSourceControlPullRequest(15, PullRequestState.MERGED, PullRequestSource.AUTOMATIC_INNER_SOURCE, now);
    createSourceControlPullRequest(16, PullRequestState.MERGED, PullRequestSource.AUTOMATIC_INNER_SOURCE, now);

    // 1 MANUAL MERGED PR
    createSourceControlPullRequest(17, PullRequestState.MERGED, PullRequestSource.MANUAL, now);

    // 1 MANUAL_INNER_SOURCE MERGED PR
    createSourceControlPullRequest(18, PullRequestState.MERGED, PullRequestSource.MANUAL_INNER_SOURCE, now);

    // 1 AUTOMATIC MISSING PR
    createSourceControlPullRequest(19, PullRequestState.MISSING, PullRequestSource.AUTOMATIC, now);

    // 1 AUTOMATIC_INNER_SOURCE MISSING PR
    createSourceControlPullRequest(20, PullRequestState.MISSING, PullRequestSource.AUTOMATIC_INNER_SOURCE, now);

    // Create some PRs with dates before the cutoff to ensure they aren't counted
    Date oldDate = Date.from(previousTime.toInstant().minus(1, ChronoUnit.DAYS));

    // These should not be counted in the stats because they were created before the previous fire time
    createSourceControlPullRequest(21, PullRequestState.OPEN, PullRequestSource.AUTOMATIC, oldDate);
    createSourceControlPullRequest(22, PullRequestState.OPEN, PullRequestSource.AUTOMATIC_INNER_SOURCE, oldDate);
    createSourceControlPullRequest(23, PullRequestState.OPEN, PullRequestSource.MANUAL, oldDate);
    createSourceControlPullRequest(24, PullRequestState.OPEN, PullRequestSource.MANUAL_INNER_SOURCE, oldDate);

    // This should not be counted because it has an EXTERNAL source
    createSourceControlPullRequest(25, PullRequestState.OPEN, PullRequestSource.EXTERNAL, now);
    createSourceControlPullRequest(26, PullRequestState.OPEN, null, now);
  }

  private void createSourceControlPullRequest(
      int id,
      PullRequestState state,
      PullRequestSource source,
      Date createTime)
  {
    tempEntity.newSourceControlPullRequest(
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
        source);
  }

  @Test
  public void test_collectAuthenticationStats() {
    JobExecutionContext mockContext = mock(JobExecutionContext.class);
    when(mockContext.getPreviousFireTime()).thenReturn(new Date());

    // Create test data: 2 orgs, 3 apps
    Organization org1 = new Organization();
    org1.setId("org-1");
    Organization org2 = new Organization();
    org2.setId("org-2");

    // Org1 uses PAT, Org2 uses GitHub App
    SourceControl orgConfig1 = new SourceControl.Builder()
        .setOwnerId("org-1")
        .setAuthenticationType(SourceControl.AuthenticationType.PAT)
        .build();
    SourceControl orgConfig2 = new SourceControl.Builder()
        .setOwnerId("org-2")
        .setAuthenticationType(SourceControl.AuthenticationType.GITHUB_APP)
        .build();

    // App1 uses PAT, App2 uses GitHub App, App3 uses PAT
    SourceControl appConfig1 = new SourceControl.Builder()
        .setOwnerId("app-1")
        .setAuthenticationType(SourceControl.AuthenticationType.PAT)
        .build();
    SourceControl appConfig2 = new SourceControl.Builder()
        .setOwnerId("app-2")
        .setAuthenticationType(SourceControl.AuthenticationType.GITHUB_APP)
        .build();
    SourceControl appConfig3 = new SourceControl.Builder()
        .setOwnerId("app-3")
        .setAuthenticationType(SourceControl.AuthenticationType.PAT)
        .build();

    when(organizationDAO.getAll()).thenReturn(Arrays.asList(org1, org2));
    when(sourceControlDAO.getAll()).thenReturn(Arrays.asList(
        orgConfig1, orgConfig2, appConfig1, appConfig2, appConfig3));

    // Mock GitHubApp instances
    GitHubApp githubApp1 = new GitHubApp();
    GitHubApp githubApp2 = new GitHubApp();
    when(gitHubAppDAO.getAll()).thenReturn(Arrays.asList(githubApp1, githubApp2));

    // Mock other required DAOs
    when(sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled()).thenReturn(new ArrayList<>());
    when(sourceControlDAO.getByApplication()).thenReturn(new ArrayList<>());
    when(applicationDAO.getAll()).thenReturn(new ArrayList<>());
    when(metrics.computeStatsAndReset()).thenReturn(new AggregatedPRStats(Collections.emptyList()));

    TelemetryData telemetryData = collector.collectData(mockContext);

    // Verify authentication stats
    assertThat(telemetryData.getAttributes())
        .contains(
            entry(TOTAL_SC_ORGS_WITH_AUTH_CONFIGURED, "2"),
            entry(TOTAL_SC_APPS_WITH_AUTH_CONFIGURED, "3"),
            entry(TOTAL_SC_ORGS_USING_PAT, "1"),
            entry(TOTAL_SC_ORGS_USING_GITHUB_APP, "1"),
            entry(TOTAL_SC_APPS_USING_PAT, "2"),
            entry(TOTAL_SC_APPS_USING_GITHUB_APP, "1"),
            entry(TOTAL_SC_GITHUB_APP_INSTALLATIONS, "2"));
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
