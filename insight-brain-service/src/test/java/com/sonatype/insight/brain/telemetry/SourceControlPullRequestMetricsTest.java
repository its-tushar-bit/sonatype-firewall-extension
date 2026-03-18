/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ComponentHelper;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestResultDAO;
import com.sonatype.insight.brain.git.EnhancedPullRequestResult;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.AggregatedPRStats;
import com.sonatype.insight.brain.telemetry.SourceControlPullRequestMetrics.ApplicationPRStats;
import com.sonatype.nexus.iq.manager.PullRequestResult;

import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlPullRequestMetricsTest
    extends AbstractComponentTest
{
  private static final ComponentIdentifier MAVEN_COORDINATES =
      ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0");

  @Mock
  private ComponentHelper mockComponentHelper;

  @Inject
  private SourceControlPullRequestMetrics metrics;

  @Inject
  private SourceControlPullRequestResultDAO sourceControlPullRequestResultDAO;

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
    metrics = new SourceControlPullRequestMetrics(sourceControlPullRequestResultDAO, mockComponentHelper);

    PullRequestResult success = new PullRequestResult();
    success.setCheckoutTime(1L);
    success.setRemediationTime(1L);
    success.setPushTime(1L);
    success.setPullRequestCreationTime(1L);
    success.setSuccessful(true);
    EnhancedPullRequestResult enhancedSuccess = new EnhancedPullRequestResult(success, new Date(),
        ComponentIdentifier.createMavenCoordinates("foo", "bar", "1.0"),
        "Bump bar to 1.1", false);
    Application foo = tempEntity.newApplicationWithParent();
    Application bar = tempEntity.newApplicationWithParent();
    metrics.addResult(foo.getId(), enhancedSuccess);

    PullRequestResult failure = new PullRequestResult();
    failure.setCheckoutTime(1L);
    failure.setRemediationTime(1L);
    failure.setPushTime(1L);
    failure.setSuccessful(false);
    EnhancedPullRequestResult enhancedFailure = new EnhancedPullRequestResult(failure, new Date(),
        MAVEN_COORDINATES, "Bump bar to 1.1", true);
    metrics.addResult(foo.getId(), enhancedFailure);

    PullRequestResult app2Success = new PullRequestResult();
    app2Success.setCheckoutTime(1L);
    app2Success.setRemediationTime(1L);
    app2Success.setPushTime(1L);
    app2Success.setPullRequestCreationTime(1L);
    app2Success.setSuccessful(true);
    EnhancedPullRequestResult app2EnhancedSuccess = new EnhancedPullRequestResult(app2Success, new Date(),
        MAVEN_COORDINATES, "Bump bar to 1.1", false);
    metrics.addResult(bar.getId(), app2EnhancedSuccess);

    AggregatedPRStats stats = metrics.computeStatsAndReset();
    assertThat(stats.getTotalTime()).isEqualTo(11L);
    assertThat(stats.getTotalSuggestedPRs()).isEqualTo(3L);
    assertThat(stats.getSuccessfulPRs()).isEqualTo(2L);
    assertThat(stats.getApplicationPRStats()).hasSize(2);
    assertThat(
        stats.getApplicationPRStats().stream().map(ApplicationPRStats::getApplicationId).collect(Collectors.toList()))
            .contains(foo.getId(), bar.getId());
    assertThat(stats.getTotalRaisedExceptions()).isEqualTo(1);

    stats.getApplicationPRStats().forEach(applicationPRStats -> {
      if (applicationPRStats.getApplicationId().equals(foo.getId())) {
        assertThat(applicationPRStats.getSuccessfulPRs()).isEqualTo(1);
        assertThat(applicationPRStats.getTotalSuggestedPRs()).isEqualTo(2);
        assertThat(applicationPRStats.getTotalTime()).isEqualTo(7);
        assertThat(applicationPRStats.getExceptionsRaised()).isEqualTo(1);
      }
      else {
        assertThat(applicationPRStats.getApplicationId().equals(bar.getId()));
        assertThat(applicationPRStats.getSuccessfulPRs()).isEqualTo(1);
        assertThat(applicationPRStats.getTotalSuggestedPRs()).isEqualTo(1);
        assertThat(applicationPRStats.getTotalTime()).isEqualTo(4);
        assertThat(applicationPRStats.getExceptionsRaised()).isEqualTo(0);
      }
    });

    // ensure that stats are cleared when dumped
    AggregatedPRStats cleared = metrics.computeStatsAndReset();
    assertThat(cleared.getTotalTime()).isEqualTo(0L);
    assertThat(cleared.getTotalSuggestedPRs()).isEqualTo(0L);
    assertThat(cleared.getSuccessfulPRs()).isEqualTo(0L);
    assertThat(cleared.getApplicationPRStats()).isEmpty();
  }

  @Test
  public void test_metricsForApplication() {
    // given: an application with available metrics
    PullRequestResult success = new PullRequestResult();
    success.setCheckoutTime(1L);
    success.setRemediationTime(1L);
    success.setPushTime(1L);
    success.setPullRequestCreationTime(1L);
    success.setSuccessful(true);
    Date start = new Date(System.currentTimeMillis() - 1000);
    EnhancedPullRequestResult enhancedSuccess = new EnhancedPullRequestResult(success, start,
        MAVEN_COORDINATES, "Bump bar to 1.1", false);
    String applicationId = tempEntity.newApplicationWithParent().getId();
    metrics.addResult(applicationId, enhancedSuccess);

    PullRequestResult fail = new PullRequestResult();
    fail.setCheckoutTime(1L);
    fail.setRemediationTime(1L);
    fail.setPushTime(1L);
    fail.setPullRequestCreationTime(1L);
    fail.setSuccessful(false);
    Date failStart = new Date();
    EnhancedPullRequestResult enhancedFail = new EnhancedPullRequestResult(success, failStart,
        MAVEN_COORDINATES, "Bump bar to 1.2", true);
    metrics.addResult(applicationId, enhancedFail);

    // when: we request metrics for that application
    List<EnhancedPullRequestResult> results = metrics.metricsForApplication(applicationId);

    // then: results are returned as expected
    assertThat(results).hasSize(2);
    assertThat(results.get(0)).extracting(EnhancedPullRequestResult::getTarget).isEqualTo(MAVEN_COORDINATES);
    assertThat(results.get(0)).extracting(EnhancedPullRequestResult::getStartTime).isEqualTo(start);
    assertThat(results.get(0)).extracting(EnhancedPullRequestResult::getTitle).isEqualTo("Bump bar to 1.1");
    assertThat(results.get(0)).extracting(EnhancedPullRequestResult::getReasoning)
        .isEqualTo(
            "A pull request was successfully created to remediate policy violations related to \"foo : bar : 1.0\".");

    assertThat(results.get(1)).extracting(EnhancedPullRequestResult::getTarget).isEqualTo(MAVEN_COORDINATES);
    assertThat(results.get(1)).extracting(EnhancedPullRequestResult::getStartTime).isEqualTo(failStart);
    assertThat(results.get(1)).extracting(EnhancedPullRequestResult::getTitle).isEqualTo("Bump bar to 1.2");
    assertThat(results.get(1)).extracting(EnhancedPullRequestResult::getReasoning)
        .isEqualTo(
            "An error happened trying to create this PR, look in server logs for more information.");
  }

  @Test
  public void test_metricsForApplication_doesNotExist() {
    // given: an application without available metrics
    // when: we request metrics for that application
    List<EnhancedPullRequestResult> results = metrics.metricsForApplication("foo");

    // then: results are empty as expected
    assertThat(results).isEmpty();
  }

  @Test
  public void testMetricsForApplication_Unparsable() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControlPullRequestResult(application.getId(), "{\"startTime\": true}");

    List<EnhancedPullRequestResult> results = metrics.metricsForApplication(application.getId());

    assertThat(results).isEmpty();
    assertThat(sourceControlPullRequestResultDAO.getAll()).isEmpty();
  }

  @Test
  public void testMetricsForApplication_hasBothPRTypes() {
    // an application with both automated and manual PRs
    PullRequestResult automatedPr = new PullRequestResult();
    automatedPr.setSuccessful(true);
    EnhancedPullRequestResult automatedResult = new EnhancedPullRequestResult(automatedPr, new Date(),
        MAVEN_COORDINATES, "Automated PR", false, false);

    PullRequestResult manualPr = new PullRequestResult();
    manualPr.setSuccessful(true);
    EnhancedPullRequestResult manualResult = new EnhancedPullRequestResult(manualPr, new Date(),
        MAVEN_COORDINATES, "Manual PR", false, true);

    String applicationId = tempEntity.newApplicationWithParent().getId();
    metrics.addResult(applicationId, automatedResult);
    metrics.addResult(applicationId, manualResult);

    List<EnhancedPullRequestResult> results = metrics.metricsForApplication(applicationId);

    assertThat(results).hasSize(2);
    assertThat(results.get(0).getTitle()).isEqualTo("Automated PR");
    assertThat(results.get(0).isManualPR()).isFalse();
    assertThat(results.get(1).getTitle()).isEqualTo("Manual PR");
    assertThat(results.get(1).isManualPR()).isTrue();
  }

  @Test
  public void testComputeStats_Unparsable() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControlPullRequestResult(application.getId(), "{\"startTime\": true}");

    AggregatedPRStats aggregatedPRStats = metrics.computeStatsAndReset();

    assertThat(aggregatedPRStats).isNotNull();
    assertThat(sourceControlPullRequestResultDAO.getAll()).isEmpty();
  }
}
