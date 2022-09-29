/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.AggregatedPRStats;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

@Named
@Singleton
public class SourceControlMetricsTelemetryCollector
    implements TelemetryCollector
{
  public static final String TOTAL_SC_WITH_REMEDIATION_PRS_ENABLED = "total_source_control_entries_with_pr_enabled";

  public static final String TOTAL_APPLICATION_SC_ENTRIES = "total_source_control_applications";

  public static final String TOTAL_APPLICATIONS = "total_applications";

  public static final String TOTAL_SC_PR_TIME_SPENT = "total_daily_source_control_pull_request_time_ms";

  public static final String TOTAL_SC_PRS_CREATED = "total_daily_source_control_pull_requests_created";

  public static final String TOTAL_SC_PRS_SUGGESTED =
      "total_daily_source_control_pull_requests_suggested_for_remediation";

  public static final String TOTAL_SC_APPLICATIONS_WITH_PRS =
      "total_daily_source_control_pull_requests_applications_with_prs";

  public static final String TOTAL_SC_EXCEPTIONS_RAISED = "total_daily_source_control_pull_requests_exceptions_raised";

  public static final String TOTAL_PULL_REQUESTS_UPDATED_1_W_TO_1_M_AGO = "total_pull_requests_updated_1w_to_1m_ago";

  public static final String TOTAL_PULL_REQUESTS_UPDATED_1_M_TO_2_M_AGO = "total_pull_requests_updated_1m_to_2m_ago";

  public static final String TOTAL_PULL_REQUESTS_UPDATED_2_M_TO_3_M_AGO = "total_pull_requests_updated_2m_to_3m_ago";

  public static final String TOTAL_PULL_REQUESTS_UPDATED_3_M_TO_6_M_AGO = "total_pull_requests_updated_3m_to_6m_ago";

  public static final String TOTAL_PULL_REQUESTS_UPDATED_6_M_AGO_OR_EARLIER =
      "total_pull_requests_updated_6m_ago_or_earlier";

  private final SourceControlDAO sourceControlDAO;

  private final SourceControlPullRequestDAO sourceControlPullRequestDAO;

  private final ApplicationDAO applicationDAO;

  private final SourceControlPullRequestMetrics metrics;

  @Inject
  public SourceControlMetricsTelemetryCollector(
      SourceControlDAO sourceControlDAO,
      SourceControlPullRequestDAO sourceControlPullRequestDAO,
      ApplicationDAO applicationDAO,
      SourceControlPullRequestMetrics metrics)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.sourceControlPullRequestDAO = sourceControlPullRequestDAO;
    this.applicationDAO = applicationDAO;
    this.metrics = metrics;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_METRICS);
    Map<String, Object> attributes = telemetryData.getAttributes();

    attributes
        .put(TOTAL_SC_WITH_REMEDIATION_PRS_ENABLED,
            String.valueOf(sourceControlDAO.getApplicationsWithRemediationPullRequestsEnabled().size()));
    attributes.put(TOTAL_APPLICATION_SC_ENTRIES, String.valueOf(sourceControlDAO.getByApplication().size()));
    attributes.put(TOTAL_APPLICATIONS, String.valueOf(applicationDAO.getAll().size()));

    AggregatedPRStats aggregatedPRStats = metrics.computeStatsAndReset();
    attributes.put(TOTAL_SC_PR_TIME_SPENT, String.valueOf(aggregatedPRStats.getTotalTime()));
    attributes.put(TOTAL_SC_PRS_CREATED, String.valueOf(aggregatedPRStats.getSuccessfulPRs()));
    attributes.put(TOTAL_SC_PRS_SUGGESTED, String.valueOf(aggregatedPRStats.getTotalSuggestedPRs()));
    attributes.put(TOTAL_SC_APPLICATIONS_WITH_PRS, String.valueOf(aggregatedPRStats.getApplicationPRStats().size()));
    attributes.put(TOTAL_SC_EXCEPTIONS_RAISED, String.valueOf(aggregatedPRStats.getTotalRaisedExceptions()));

    collectStaleBranchStats(attributes);

    return telemetryData;
  }

  private void collectStaleBranchStats(Map<String, Object> attributes) {
    // compute cutoff dates
    Date now = new Date();
    Date oneWeekAgo = getPastDate(Calendar.DATE, -7, now);
    Date oneMonthAgo = getPastDate(Calendar.MONTH, -1, now);
    Date twoMonthsAgo = getPastDate(Calendar.MONTH, -2, now);
    Date threeMonthsAgo = getPastDate(Calendar.MONTH, -3, now);
    Date sixMonthsAgo = getPastDate(Calendar.MONTH, -6, now);

    // populate telemetry attributes
    attributes.put(TOTAL_PULL_REQUESTS_UPDATED_1_W_TO_1_M_AGO,
        sourceControlPullRequestDAO.getCountByUpdateTimeRange(oneMonthAgo, oneWeekAgo));
    attributes.put(TOTAL_PULL_REQUESTS_UPDATED_1_M_TO_2_M_AGO,
        sourceControlPullRequestDAO.getCountByUpdateTimeRange(twoMonthsAgo, oneMonthAgo));
    attributes.put(TOTAL_PULL_REQUESTS_UPDATED_2_M_TO_3_M_AGO,
        sourceControlPullRequestDAO.getCountByUpdateTimeRange(threeMonthsAgo, twoMonthsAgo));
    attributes.put(TOTAL_PULL_REQUESTS_UPDATED_3_M_TO_6_M_AGO,
        sourceControlPullRequestDAO.getCountByUpdateTimeRange(sixMonthsAgo, threeMonthsAgo));
    attributes.put(TOTAL_PULL_REQUESTS_UPDATED_6_M_AGO_OR_EARLIER,
        sourceControlPullRequestDAO.getCountByUpdateTimeRange(null, sixMonthsAgo));
  }

  private Date getPastDate(int type, int delta, Date origin) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(origin);
    calendar.add(type, delta);
    return calendar.getTime();
  }

  @Override
  public boolean isClusterTelemetry() {
    return false;
  }
}
