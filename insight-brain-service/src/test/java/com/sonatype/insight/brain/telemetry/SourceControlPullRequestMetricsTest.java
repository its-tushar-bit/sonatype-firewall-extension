/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Date;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.git.EnhancedPullRequestResult;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.AggregatedPRStats;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.ApplicationPRStats;
import com.sonatype.nexus.iq.manager.PullRequestResult;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlPullRequestMetricsTest
{
  private SourceControlPullRequestMetrics metrics = new SourceControlPullRequestMetrics();
  
  @Test
  public void test_computeStatsAndReset_noPRS() {
    AggregatedPRStats stats = metrics.computeStatsAndReset();
    assertThat(stats.getApplicationPRStats()).isEmpty();
    assertThat(stats.getTotalTime()).isEqualTo(0);
    assertThat(stats.getSuccessfulPRs()).isEqualTo(0);
    assertThat(stats.getTotalSuggestedPRs()).isEqualTo(0);
  }
  
  @Test
  public void test_computeStatsAndReset_withPrs() {
    PullRequestResult success = new PullRequestResult();
    success.setCheckoutTime(1L);
    success.setRemediationTime(1L);
    success.setPushTime(1L);
    success.setPullRequestCreationTime(1L);
    success.setSuccessful(true);
    EnhancedPullRequestResult enhancedSuccess = new EnhancedPullRequestResult(success, new Date(),
        ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0"),
        "Bump bar to 1.1", false);
    metrics.addResult("foo", enhancedSuccess);
    
    PullRequestResult failure = new PullRequestResult();
    failure.setCheckoutTime(1L);
    failure.setRemediationTime(1L);
    failure.setPushTime(1L);    
    failure.setSuccessful(false);
    EnhancedPullRequestResult enhancedFailure = new EnhancedPullRequestResult(failure, new Date(),
        ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0"),
        "Bump bar to 1.1", true);
    metrics.addResult("foo", enhancedFailure);

    PullRequestResult app2Success = new PullRequestResult();
    app2Success.setCheckoutTime(1L);
    app2Success.setRemediationTime(1L);
    app2Success.setPushTime(1L);
    app2Success.setPullRequestCreationTime(1L);
    app2Success.setSuccessful(true);
    EnhancedPullRequestResult app2EnhancedSuccess = new EnhancedPullRequestResult(app2Success, new Date(),
        ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0"),
        "Bump bar to 1.1", false);
    metrics.addResult("bar", app2EnhancedSuccess);

    AggregatedPRStats stats = metrics.computeStatsAndReset();
    assertThat(stats.getTotalTime()).isEqualTo(11L);
    assertThat(stats.getTotalSuggestedPRs()).isEqualTo(3L);
    assertThat(stats.getSuccessfulPRs()).isEqualTo(2L);
    assertThat(stats.getApplicationPRStats()).hasSize(2);
    assertThat(
        stats.getApplicationPRStats().stream().map(ApplicationPRStats::getApplicationId).collect(Collectors.toList()))
        .contains("foo", "bar");
    assertThat(stats.getTotalRaisedExceptions()).isEqualTo(1);

    stats.getApplicationPRStats().forEach(applicationPRStats -> {
      if (applicationPRStats.getApplicationId().equals("foo")) {
        assertThat(applicationPRStats.getSuccessfulPRs()).isEqualTo(1);
        assertThat(applicationPRStats.getTotalSuggestedPRs()).isEqualTo(2);
        assertThat(applicationPRStats.getTotalTime()).isEqualTo(7);
        assertThat(applicationPRStats.getExceptionsRaised()).isEqualTo(1);
      }
      else {
        assertThat(applicationPRStats.getApplicationId().equals("bar"));
        assertThat(applicationPRStats.getSuccessfulPRs()).isEqualTo(1);
        assertThat(applicationPRStats.getTotalSuggestedPRs()).isEqualTo(1);
        assertThat(applicationPRStats.getTotalTime()).isEqualTo(4);
        assertThat(applicationPRStats.getExceptionsRaised()).isEqualTo(0);
      }
    });

    //ensure that stats are cleared when dumped
    AggregatedPRStats cleared = metrics.computeStatsAndReset();
    assertThat(cleared.getTotalTime()).isEqualTo(0L);
    assertThat(cleared.getTotalSuggestedPRs()).isEqualTo(0L);
    assertThat(cleared.getSuccessfulPRs()).isEqualTo(0L);
    assertThat(cleared.getApplicationPRStats()).isEmpty();
  }
}
