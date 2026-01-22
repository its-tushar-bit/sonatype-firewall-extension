/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.component.ComponentHelper;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestResultDAO;
import com.sonatype.insight.brain.git.EnhancedPullRequestResult;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestResult;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.iq.manager.PullRequestResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collect runtime metrics for results of all PRs run since application startup. Results can also be cleared along with
 * querying for aggregations, explicitly to support collecting daily metrics.
 */
@Named
@Singleton
public class SourceControlPullRequestMetrics
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlPullRequestMetrics.class);

  private SourceControlPullRequestResultDAO sourceControlPullRequestResultDAO;

  private ComponentHelper componentHelper;

  @Inject
  public SourceControlPullRequestMetrics(SourceControlPullRequestResultDAO sourceControlPullRequestResultDAO,
                                         ComponentHelper componentHelper)
  {
    this.sourceControlPullRequestResultDAO = sourceControlPullRequestResultDAO;
    this.componentHelper = componentHelper;
  }

  public void addResult(String applicationId, EnhancedPullRequestResult pullRequestResult) {
    SourceControlPullRequestResult sourceControlPullRequestResult =
        new SourceControlPullRequestResult(applicationId, JsonUtils.writeUnformatted(pullRequestResult));
    sourceControlPullRequestResultDAO.insert(sourceControlPullRequestResult);
  }

  /**
   * Compute statistics of Pull Requests across all applications since last call to this method. Results are cleared
   * after computation.
   */
  public AggregatedPRStats computeStatsAndReset() {
    AggregatedPRStats stats = computeStats();
    log.debug("Since last metrics calculation: {}", stats);
    sourceControlPullRequestResultDAO.deleteAll();
    return stats;
  }

  /**
   * Retrieve recorded metrics for the given application within the last telemetry reporting window.
   */
  public List<EnhancedPullRequestResult> metricsForApplication(final String applicationId) {
    return sourceControlPullRequestResultDAO.getByApplicationId(applicationId).stream()
        .map(this::convert)
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(EnhancedPullRequestResult::getStartTime))
        .collect(Collectors.toList());
  }

  private AggregatedPRStats computeStats() {
    List<ApplicationPRStats> applicationPRStats = new ArrayList<>();
    Map<String, List<EnhancedPullRequestResult>> enhancedPullRequestResultMap = new HashMap<>();
    for (SourceControlPullRequestResult sourceControlPullRequestResult : sourceControlPullRequestResultDAO.getAll()) {
      EnhancedPullRequestResult enhancedPullRequestResult = convert(sourceControlPullRequestResult);
      if (enhancedPullRequestResult != null) {
        enhancedPullRequestResultMap.computeIfAbsent(sourceControlPullRequestResult.getApplicationId(),
            k -> new ArrayList<>()).add(enhancedPullRequestResult);
      }
    }
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

      List<EnhancedPullRequestResult> allGoldenResults = result.stream()
          .filter(pr -> componentHelper.isGoldenVersion(pr.getTarget(), entry.getKey()))
          .toList();

      long successfulGoldenPRs = allGoldenResults.stream()
          .map(EnhancedPullRequestResult::getTiming)
          .filter(PullRequestResult::isSuccessful)
          .count();

      long possibleGoldenPRs = allGoldenResults.size();

      int possiblePRs = result.size();

      long exceptionsRaised = result.stream().filter(EnhancedPullRequestResult::isExceptionThrown).count();

      applicationPRStats
          .add(new ApplicationPRStats(entry.getKey(), timeSpent, successfulPRs, possiblePRs, exceptionsRaised,
              successfulGoldenPRs, possibleGoldenPRs));
    }
    return new AggregatedPRStats(applicationPRStats);
  }

  private EnhancedPullRequestResult convert(SourceControlPullRequestResult sourceControlPullRequestResult) {
    try {
      return JsonUtils.parse(sourceControlPullRequestResult.getPullRequestResultJson(),
          EnhancedPullRequestResult.class);
    }
    catch (IOException e) {
      log.warn("Removing unparsable source control pull request result for application ID {} with json {} due to {}.",
          sourceControlPullRequestResult.getApplicationId(),
          sourceControlPullRequestResult.getPullRequestResultJson(),
          e.getMessage(),
          e
      );
      sourceControlPullRequestResultDAO.delete(sourceControlPullRequestResult);
      return null;
    }
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

    public long getSuccessfulGoldenPRs() {
      return getApplicationPRStats().stream().mapToLong(ApplicationPRStats::getSuccessfulGoldenPRs).sum();
    }

    public long getTotalSuggestedGoldenPRs() {
      return getApplicationPRStats().stream().mapToLong(ApplicationPRStats::getTotalSuggestedGoldenPRs).sum();
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
          ", successfulGoldenPRs=" + getSuccessfulGoldenPRs() +
          ", totalSuggestedGoldenPRs=" + getTotalSuggestedGoldenPRs() +
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

    private final long successfulGoldenPRs;

    private long totalSuggestedGoldenPRs;

    ApplicationPRStats(
        final String applicationId,
        final long totalTime,
        final long successfulPRs,
        final long totalSuggestedPRs,
        final long exceptionsRaised,
        final long successfulGoldenPRs,
        final long totalSuggestedGoldenPRs)
    {
      this.applicationId = applicationId;
      this.totalTime = totalTime;
      this.successfulPRs = successfulPRs;
      this.totalSuggestedPRs = totalSuggestedPRs;
      this.exceptionsRaised = exceptionsRaised;
      this.successfulGoldenPRs = successfulGoldenPRs;
      this.totalSuggestedGoldenPRs = totalSuggestedGoldenPRs;
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

    public long getSuccessfulGoldenPRs() {
      return successfulGoldenPRs;
    }

    public long getTotalSuggestedGoldenPRs() {
      return totalSuggestedGoldenPRs;
    }

    @Override
    public String toString() {
      return "ApplicationPRStats{" +
          "applicationId='" + applicationId + '\'' +
          ", totalTime=" + totalTime +
          ", successfulPRs=" + successfulPRs +
          ", totalSuggestedPRs=" + totalSuggestedPRs +
          ", exceptionsRaised=" + exceptionsRaised +
          ", successfulGoldenPRs=" + successfulGoldenPRs +
          '}';
    }
  }
}
