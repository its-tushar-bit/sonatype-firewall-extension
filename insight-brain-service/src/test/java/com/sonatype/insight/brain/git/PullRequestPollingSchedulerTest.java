/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class PullRequestPollingSchedulerTest
    extends AbstractComponentTest
{
  @Mock
  private PullRequestPollingService pullRequestPollingServiceMock;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private InsightConfig insightConfig;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Inject
  private PullRequestPollingScheduler pullRequestPollingScheduler;

  @Override
  public void configure(Binder binder) {
    binder.bind(PullRequestPollingService.class).toInstance(pullRequestPollingServiceMock);
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    super.configure(binder);
  }

  @Test
  public void testStart_FeatureDisabled() throws Exception {
    setFeatureFlagEnabled(false);
    assertThat(insightConfig.isFeatureEnabled(Feature.PR_COMMENTING)).isFalse();

    pullRequestPollingScheduler.start();

    verifyNoInteractions(taskSchedulerMock);
  }

  @Test
  public void testStart_FeatureEnabled() throws Exception {
    setFeatureFlagEnabled(true);
    assertThat(insightConfig.isFeatureEnabled(Feature.PR_COMMENTING)).isTrue();

    pullRequestPollingScheduler.start();

    verify(taskSchedulerMock).schedulePeriodicTask(PullRequestPollingScheduler.class, PullRequestPollingScheduler.NAME,
        Duration.ofSeconds(PullRequestPollingScheduler.PULL_REQUEST_MONITORING_INTERVAL_SECONDS));
  }

  @Test
  public void testExecute() {
    PullRequestPollingScheduler pullRequestPollingSchedulerSpy = spy(pullRequestPollingScheduler);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(pullRequestPollingSchedulerSpy).monitorPullRequestsForCommenting();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      pullRequestPollingSchedulerSpy.execute(mock(JobExecutionContext.class));
    }

    verify(pullRequestPollingSchedulerSpy).monitorPullRequestsForCommenting();
  }

  @Test
  public void testMonitorPullRequestsForCommenting_Unlicensed() {
    testProductLicense.setFeatures();
    assertThat(testProductLicense.hasFeature(LicensedFeature.AUTOMATION)).isFalse();

    pullRequestPollingScheduler.monitorPullRequestsForCommenting();

    verifyNoInteractions(pullRequestPollingServiceMock);
  }

  @Test
  public void testMonitorPullRequestsForCommenting_Licensed() throws Exception {
    assertThat(testProductLicense.hasFeature(LicensedFeature.AUTOMATION)).isTrue();

    pullRequestPollingScheduler.monitorPullRequestsForCommenting();

    verify(pullRequestPollingServiceMock).fetchAndSendPullRequestsForCommenting();
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(PullRequestPollingScheduler.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  private void setFeatureFlagEnabled(boolean featureFlagEnabled) {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.PR_COMMENTING.getFlag(), featureFlagEnabled);
    insightConfig.setFeatures(features);
  }
}
