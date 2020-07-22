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

import com.sonatype.insight.brain.git.EnhancedPullRequestResult;
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

  private final Map<String, List<EnhancedPullRequestResult>> enhancedPullRequestResultMap = new ConcurrentHashMap<>();

  public void addResult(String applicationId, EnhancedPullRequestResult pullRequestResult) {
    enhancedPullRequestResultMap
        .merge(applicationId, Collections.singletonList(pullRequestResult), (existing, adding) ->
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
    enhancedPullRequestResultMap.clear();
    return stats;
  }

  /**
   * Retrieve recorded metrics for the given application within the last telemetry reporting window.
   */
  public List<EnhancedPullRequestResult> metricsForApplication(final String applicationId) {
    return enhancedPullRequestResultMap.getOrDefault(applicationId, Collections.emptyList());
  }

  private AggregatedPRStats computeStats() {
    List<ApplicationPRStats> applicationPRStats = new ArrayList<>();
    for (Entry<String, List<EnhancedPullRequestResult>> entry : enhancedPullRequestResultMap.entrySet()) {
      List<EnhancedPullRequestResult> result = entry.getValue();
      long timeSpent = result.stream()
          .map(EnhancedPullRequestResult::getTiming)
          .map(PullRequestResult::getTotalTime)
          .mapToLong(Long::longValue)
          .sum();
      
      long successfulPRs = result.stream()
          .map(EnhancedPullRequestResult::getTiming)
          .filter(PullRequestResult::isSuccessful)
          .count();
      
      int possiblePRs = result.size();
      
      long exceptionsRaised = result.stream().filter(EnhancedPullRequestResult::isExceptionThrown).count();

      applicationPRStats
          .add(new ApplicationPRStats(entry.getKey(), timeSpent, successfulPRs, possiblePRs, exceptionsRaised));
    }
    return new AggregatedPRStats(applicationPRStats);
  }

  static class AggregatedPRStats
  {
    private final List<ApplicationPRStats> applicationPRStats;

    AggregatedPRStats(final List<ApplicationPRStats> applicationPRStats) {
      this.applicationPRStats = applicationPRStats;
    }

    public long getTotalTime() {
      return getApplicationPRStats().stream().mapToLong(ApplicationPRStats::getTotalTime).sum();
    }

    public long getSuccessfulPRs() {
      return getApplicationPRStats().stream().mapToLong(ApplicationPRStats::getSuccessfulPRs).sum();
    }

    public long getTotalSuggestedPRs() {
      return getApplicationPRStats().stream().mapToLong(ApplicationPRStats::getTotalSuggestedPRs).sum();
    }
    
    public long getTotalRaisedExceptions() {
      return getApplicationPRStats().stream().mapToLong(ApplicationPRStats::getExceptionsRaised).sum();
    }

    public List<ApplicationPRStats> getApplicationPRStats() {
      return applicationPRStats;
    }

    @Override
    public String toString() {
      return "AggregatedPRStats{" +
          "totalTime=" + getTotalTime() +
          ", successfulPRs=" + getSuccessfulPRs() +
          ", totalSuggestedPRs=" + getTotalSuggestedPRs() +
          ", totalExceptionsRaised=" + getTotalRaisedExceptions() +
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
    
    private final long exceptionsRaised;

    ApplicationPRStats(
        final String applicationId,
        final long totalTime,
        final long successfulPRs,
        final long totalSuggestedPRs,
        final long exceptionsRaised)
    {
      this.applicationId = applicationId;
      this.totalTime = totalTime;
      this.successfulPRs = successfulPRs;
      this.totalSuggestedPRs = totalSuggestedPRs;
      this.exceptionsRaised = exceptionsRaised;
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

    public long getExceptionsRaised() {
      return exceptionsRaised;
    }

    @Override
    public String toString() {
      return "ApplicationPRStats{" +
          "applicationId='" + applicationId + '\'' +
          ", totalTime=" + totalTime +
          ", successfulPRs=" + successfulPRs +
          ", totalSuggestedPRs=" + totalSuggestedPRs +
          ", exceptionsRaised=" + exceptionsRaised +
          '}';
    }
  }
}
