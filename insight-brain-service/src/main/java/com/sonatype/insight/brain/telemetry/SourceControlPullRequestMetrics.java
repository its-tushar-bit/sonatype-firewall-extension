/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.nexus.iq.manager.PullRequestResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collect runtime metrics for results of all PRs run since application startup.
 * Results can also be cleared along with querying for aggregations, explicitly to support collecting daily metrics.
 */
@Named
@Singleton
public class SourceControlPullRequestMetrics
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlPullRequestMetrics.class);

  private final Map<String, List<PullRequestResult>> pullRequestResultMap = new ConcurrentHashMap<>();

  public void addResult(String applicationId, PullRequestResult pullRequestResult) {
    pullRequestResultMap.merge(applicationId, Collections.singletonList(pullRequestResult), (existing, adding) ->
        Stream.of(existing, adding)
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));
  }

  /**
   * Compute statistics of Pull Requests across all applications since last call to this method.
   * Results are cleared after computation.
   */
  public AggregatedPRStats computeStatsAndReset() {
    AggregatedPRStats stats = computeStats();
    log.debug("Since last metrics calculation: {}", stats);
    pullRequestResultMap.clear();
    return stats;
  }

  private AggregatedPRStats computeStats() {
    long totalTime = 0;
    long totalSuccessfulPRs = 0;
    long totalPossiblePRs = 0;
    List<ApplicationPRStats> applicationPRStats = new ArrayList<>();
    for (Entry<String, List<PullRequestResult>> entry : pullRequestResultMap.entrySet()) {
      long timeSpent = entry.getValue().stream().map(PullRequestResult::getTotalTime)
          .mapToLong(Long::longValue).sum();
      totalTime += timeSpent;
      long successfulPRs = entry.getValue().stream().filter(PullRequestResult::isSuccessful).count();
      totalSuccessfulPRs += successfulPRs;
      int possiblePRs = entry.getValue().size();
      totalPossiblePRs += possiblePRs;
      applicationPRStats.add(new ApplicationPRStats(entry.getKey(), timeSpent, successfulPRs, possiblePRs));
    }
    return new AggregatedPRStats(totalTime, totalSuccessfulPRs, totalPossiblePRs, applicationPRStats);
  }

  static class AggregatedPRStats
  {
    private final long totalTime;

    private final long successfulPRs;

    private final long totalSuggestedPRs;

    private final List<ApplicationPRStats> applicationPRStats;

    AggregatedPRStats(
        final long totalTime,
        final long successfulPRs,
        final long totalSuggestedPRs,
        final List<ApplicationPRStats> applicationPRStats)
    {
      this.totalTime = totalTime;
      this.successfulPRs = successfulPRs;
      this.totalSuggestedPRs = totalSuggestedPRs;
      this.applicationPRStats = applicationPRStats;
    }

    public long getTotalTime() {
      return totalTime;
    }

    public long getSuccessfulPRs() {
      return successfulPRs;
    }

    public long getTotalSuggestedPRs() {
      return totalSuggestedPRs;
    }

    public List<ApplicationPRStats> getApplicationPRStats() {
      return applicationPRStats;
    }

    @Override
    public String toString() {
      return "AggregatedPRStats{" +
          "totalTime=" + totalTime +
          ", successfulPRs=" + successfulPRs +
          ", totalSuggestedPRs=" + totalSuggestedPRs +
          ", applicationPRStats=" + applicationPRStats +
          '}';
    }
  }

  static class ApplicationPRStats
  {
    private final String applicationId;

    private final long totalTime;

    private final long successfulPRs;

    private final long totalSuggestedPRs;

    ApplicationPRStats(
        final String applicationId,
        final long totalTime,
        final long successfulPRs,
        final long totalSuggestedPRs)
    {
      this.applicationId = applicationId;
      this.totalTime = totalTime;
      this.successfulPRs = successfulPRs;
      this.totalSuggestedPRs = totalSuggestedPRs;
    }

    public String getApplicationId() {
      return applicationId;
    }

    public long getTotalTime() {
      return totalTime;
    }

    public long getSuccessfulPRs() {
      return successfulPRs;
    }

    public long getTotalSuggestedPRs() {
      return totalSuggestedPRs;
    }

    @Override
    public String toString() {
      return "AppResults{" +
          "applicationId='" + applicationId + '\'' +
          ", totalTime=" + totalTime +
          ", successfulPRs=" + successfulPRs +
          ", totalSuggestedPRs=" + totalSuggestedPRs +
          '}';
    }
  }
}
