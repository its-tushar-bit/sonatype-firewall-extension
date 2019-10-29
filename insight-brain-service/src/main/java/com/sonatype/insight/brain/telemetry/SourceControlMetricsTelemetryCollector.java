/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.AggregatedPRStats;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

@Named
@Singleton
public class SourceControlMetricsTelemetryCollector
    implements TelemetryCollector
{
  public static final String TOTAL_SC_WITH_PR_ENABLED = "total_source_control_entries_with_pr_enabled";

  public static final String TOTAL_APPLICATION_SC_ENTRIES = "total_source_control_applications";

  public static final String TOTAL_APPLICATIONS = "total_applications";
  
  public static final String TOTAL_SC_PR_TIME_SPENT = "total_daily_source_control_pull_request_time_ms";
  
  public static final String TOTAL_SC_PRS_CREATED = "total_daily_source_control_pull_requests_created";

  public static final String TOTAL_SC_PRS_SUGGESTED =
      "total_daily_source_control_pull_requests_suggested_for_remediation";

  public static final String TOTAL_SC_APPLICATIONS_WITH_PRS =
      "total_daily_source_control_pull_requests_applications_with_prs";

  private final SourceControlDAO sourceControlDAO;

  private final ApplicationDAO applicationDAO;

  private final SourceControlPullRequestMetrics metrics;

  @Inject
  public SourceControlMetricsTelemetryCollector(
      SourceControlDAO sourceControlDAO,
      ApplicationDAO applicationDAO,
      SourceControlPullRequestMetrics metrics)
  {
    this.sourceControlDAO = sourceControlDAO;
    this.applicationDAO = applicationDAO;
    this.metrics = metrics;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_METRICS);
    Map<String, Object> attributes = telemetryData.getAttributes();

    attributes
        .put(TOTAL_SC_WITH_PR_ENABLED, String.valueOf(sourceControlDAO.getApplicationsWithPullReqsEnabled().size()));
    attributes.put(TOTAL_APPLICATION_SC_ENTRIES, String.valueOf(sourceControlDAO.getByApplication().size()));
    attributes.put(TOTAL_APPLICATIONS, String.valueOf(applicationDAO.getAll().size()));

    AggregatedPRStats aggregatedPRStats = metrics.computeStatsAndReset();
    attributes.put(TOTAL_SC_PR_TIME_SPENT, String.valueOf(aggregatedPRStats.getTotalTime()));
    attributes.put(TOTAL_SC_PRS_CREATED, String.valueOf(aggregatedPRStats.getSuccessfulPRs()));
    attributes.put(TOTAL_SC_PRS_SUGGESTED, String.valueOf(aggregatedPRStats.getTotalSuggestedPRs()));
    attributes.put(TOTAL_SC_APPLICATIONS_WITH_PRS, String.valueOf(aggregatedPRStats.getApplicationPRStats().size()));

    return telemetryData;
  }
}
