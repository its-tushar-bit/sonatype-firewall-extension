/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestDAO;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource;
import com.sonatype.insight.brain.model.sourcecontrol.PullRequestState;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequest;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.AggregatedPRStats;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.Tables;
import org.quartz.JobExecutionContext;

import static com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource.AUTOMATIC;
import static com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource.AUTOMATIC_INNER_SOURCE;
import static com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource.MANUAL;
import static com.sonatype.insight.brain.model.sourcecontrol.PullRequestSource.MANUAL_INNER_SOURCE;
import static com.sonatype.insight.brain.model.sourcecontrol.PullRequestState.AUTO_CLOSED;
import static com.sonatype.insight.brain.model.sourcecontrol.PullRequestState.CLOSED;
import static com.sonatype.insight.brain.model.sourcecontrol.PullRequestState.MERGED;
import static com.sonatype.insight.brain.model.sourcecontrol.PullRequestState.MISSING;
import static com.sonatype.insight.brain.model.sourcecontrol.PullRequestState.OPEN;

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

  public static final String TOTAL_SC_MANUAL_PRS_CREATED = "total_daily_source_control_manual_pull_requests_created";

  public static final String TOTAL_SC_AUTOMATIC_PRS_CREATED =
      "total_daily_source_control_automatic_pull_requests_created";

  public static final String TOTAL_SC_MANUAL_PRS_CLOSED = "total_daily_source_control_manual_pull_requests_closed";

  public static final String TOTAL_SC_AUTOMATIC_PRS_CLOSED =
      "total_daily_source_control_automatic_pull_requests_closed";

  public static final String TOTAL_SC_AUTOMATIC_PRS_AUTO_CLOSED =
      "total_daily_source_control_automatic_pull_requests_auto_closed";

  public static final String TOTAL_SC_MANUAL_PRS_MERGED = "total_daily_source_control_manual_pull_requests_merged";

  public static final String TOTAL_SC_AUTOMATIC_PRS_MERGED =
      "total_daily_source_control_automatic_pull_requests_merged";

  public static final String TOTAL_SC_MANUAL_PRS_MISSING = "total_daily_source_control_manual_pull_requests_missing";

  public static final String TOTAL_SC_AUTOMATIC_PRS_MISSING =
      "total_daily_source_control_automatic_pull_requests_missing";

  public static final String TOTAL_SC_INNER_SOURCE_MANUAL_PRS_CREATED =
      "total_daily_source_control_innersource_manual_pull_requests_created";

  public static final String TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_CREATED =
      "total_daily_source_control_innersource_automatic_pull_requests_created";

  public static final String TOTAL_SC_INNER_SOURCE_MANUAL_PRS_CLOSED =
      "total_daily_source_control_innersource_manual_pull_requests_closed";

  public static final String TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_CLOSED =
      "total_daily_source_control_innersource_automatic_pull_requests_closed";

  public static final String TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_AUTO_CLOSED =
      "total_daily_source_control_innersource_automatic_pull_requests_auto_closed";

  public static final String TOTAL_SC_INNER_SOURCE_MANUAL_PRS_MERGED =
      "total_daily_source_control_innersource_manual_pull_requests_merged";

  public static final String TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_MERGED =
      "total_daily_source_control_innersource_automatic_pull_requests_merged";

  public static final String TOTAL_SC_INNER_SOURCE_MANUAL_PRS_MISSING =
      "total_daily_source_control_innersource_manual_pull_requests_missing";

  public static final String TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_MISSING =
      "total_daily_source_control_innersource_automatic_pull_requests_missing";

  public static final String TOTAL_SC_GOLDEN_PRS_CREATED = "total_daily_source_control_golden_pull_requests_created";

  public static final String TOTAL_SC_PRS_SUGGESTED =
      "total_daily_source_control_pull_requests_suggested_for_remediation";

  public static final String TOTAL_SC_GOLDEN_PRS_SUGGESTED =
      "total_daily_source_control_golden_pull_requests_suggested_for_remediation";

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
  public TelemetryData collectData(JobExecutionContext jobExecutionContext) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_METRICS);
    Map<String, Object> attributes = telemetryData.getAttributes();
    Date previousCollectionTime = jobExecutionContext.getPreviousFireTime();

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
    attributes.put(TOTAL_SC_GOLDEN_PRS_CREATED, String.valueOf(aggregatedPRStats.getSuccessfulGoldenPRs()));
    attributes.put(TOTAL_SC_GOLDEN_PRS_SUGGESTED, String.valueOf(aggregatedPRStats.getTotalSuggestedGoldenPRs()));

    collectStaleBranchStats(attributes);
    collectAutoAndManualPRStats(attributes, previousCollectionTime);

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
        sourceControlPullRequestDAO.getExternalCountByUpdateTimeRange(oneMonthAgo, oneWeekAgo));
    attributes.put(TOTAL_PULL_REQUESTS_UPDATED_1_M_TO_2_M_AGO,
        sourceControlPullRequestDAO.getExternalCountByUpdateTimeRange(twoMonthsAgo, oneMonthAgo));
    attributes.put(TOTAL_PULL_REQUESTS_UPDATED_2_M_TO_3_M_AGO,
        sourceControlPullRequestDAO.getExternalCountByUpdateTimeRange(threeMonthsAgo, twoMonthsAgo));
    attributes.put(TOTAL_PULL_REQUESTS_UPDATED_3_M_TO_6_M_AGO,
        sourceControlPullRequestDAO.getExternalCountByUpdateTimeRange(sixMonthsAgo, threeMonthsAgo));
    attributes.put(TOTAL_PULL_REQUESTS_UPDATED_6_M_AGO_OR_EARLIER,
        sourceControlPullRequestDAO.getExternalCountByUpdateTimeRange(null, sixMonthsAgo));
  }

  /**
   * Collect stats on the numbers of manual and auto PRs that were opened, closed, and merged since the last telemetry
   * run. These are derived from SourceControlPullRequest records, not SourceControlPullRequestResult records.
   */
  private void collectAutoAndManualPRStats(
      Map<String, Object> attributes,
      Date previousCollectionTime)
  {
    var openPRsSinceLastCollection =
        sourceControlPullRequestDAO.getInternalCreatedSince(previousCollectionTime);

    // non-open PRs all get deleted at the end of metrics collection, so these are implicitly only the ones since
    // last time
    var nonOpenPRs = sourceControlPullRequestDAO.getByStatesAndSources(
        EnumSet.of(AUTO_CLOSED, CLOSED, MERGED, MISSING),
        EnumSet.of(AUTOMATIC, AUTOMATIC_INNER_SOURCE, MANUAL, MANUAL_INNER_SOURCE)
    );

    var openPRsBySource = openPRsSinceLastCollection.stream()
        .collect(Collectors.groupingBy(SourceControlPullRequest::getSource));

    // a table with a list of non-open PRs for each source and state
    var nonOpenPRsBySourceAndState = nonOpenPRs.stream()
        .collect(Tables.toTable(
            SourceControlPullRequest::getSource,
            SourceControlPullRequest::getState,
            List::of,
            (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).toList(),
            () -> ArrayTable.create(
                EnumSet.allOf(PullRequestSource.class),
                EnumSet.allOf(PullRequestState.class)
            )
        ));

    BiFunction<PullRequestSource, PullRequestState, Integer> getCount = (source, state) -> {
      var list = state == PullRequestState.OPEN ?
          openPRsBySource.get(source) :
          nonOpenPRsBySourceAndState.get(source, state);

      return list == null ? 0 : list.size();
    };

    attributes.put(TOTAL_SC_AUTOMATIC_PRS_CREATED,
        getCount.apply(AUTOMATIC, OPEN) + getCount.apply(AUTOMATIC_INNER_SOURCE, OPEN));
    attributes.put(TOTAL_SC_MANUAL_PRS_CREATED,
        getCount.apply(MANUAL, OPEN) + getCount.apply(MANUAL_INNER_SOURCE, OPEN));
    attributes.put(TOTAL_SC_AUTOMATIC_PRS_CLOSED,
        getCount.apply(AUTOMATIC, CLOSED) + getCount.apply(AUTOMATIC_INNER_SOURCE, CLOSED));
    attributes.put(TOTAL_SC_AUTOMATIC_PRS_AUTO_CLOSED,
        getCount.apply(AUTOMATIC, AUTO_CLOSED) + getCount.apply(AUTOMATIC_INNER_SOURCE, AUTO_CLOSED));
    attributes.put(TOTAL_SC_MANUAL_PRS_CLOSED,
        getCount.apply(MANUAL, CLOSED) + getCount.apply(MANUAL_INNER_SOURCE, CLOSED));
    attributes.put(TOTAL_SC_AUTOMATIC_PRS_MERGED,
        getCount.apply(AUTOMATIC, MERGED) + getCount.apply(AUTOMATIC_INNER_SOURCE, MERGED));
    attributes.put(TOTAL_SC_MANUAL_PRS_MERGED,
        getCount.apply(MANUAL, MERGED) + getCount.apply(MANUAL_INNER_SOURCE, MERGED));
    attributes.put(TOTAL_SC_AUTOMATIC_PRS_MISSING,
        getCount.apply(AUTOMATIC, MISSING) + getCount.apply(AUTOMATIC_INNER_SOURCE, MISSING));
    attributes.put(TOTAL_SC_MANUAL_PRS_MISSING,
        getCount.apply(MANUAL, MISSING) + getCount.apply(MANUAL_INNER_SOURCE, MISSING));

    attributes.put(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_CREATED, getCount.apply(AUTOMATIC_INNER_SOURCE, OPEN));
    attributes.put(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_CREATED, getCount.apply(MANUAL_INNER_SOURCE, OPEN));
    attributes.put(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_CLOSED, getCount.apply(AUTOMATIC_INNER_SOURCE, CLOSED));
    attributes.put(
        TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_AUTO_CLOSED,
        getCount.apply(AUTOMATIC_INNER_SOURCE, AUTO_CLOSED)
    );
    attributes.put(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_CLOSED, getCount.apply(MANUAL_INNER_SOURCE, CLOSED));
    attributes.put(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_MERGED, getCount.apply(AUTOMATIC_INNER_SOURCE, MERGED));
    attributes.put(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_MERGED, getCount.apply(MANUAL_INNER_SOURCE, MERGED));
    attributes.put(TOTAL_SC_INNER_SOURCE_AUTOMATIC_PRS_MISSING, getCount.apply(AUTOMATIC_INNER_SOURCE, MISSING));
    attributes.put(TOTAL_SC_INNER_SOURCE_MANUAL_PRS_MISSING, getCount.apply(MANUAL_INNER_SOURCE, MISSING));

    // delete all non-open PRs that were recorded in telemetry
    nonOpenPRsBySourceAndState.values().stream()
      .filter(Objects::nonNull)
      .flatMap(List::stream)
      .forEach(sourceControlPullRequestDAO::delete);
  }

  private Date getPastDate(int type, int delta, Date origin) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(origin);
    calendar.add(type, delta);
    return calendar.getTime();
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }
}
