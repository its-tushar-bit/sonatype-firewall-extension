/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

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
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class SourceControlEventProcessingSchedulerTest
    extends AbstractComponentTest
{
  @Inject
  SourceControlEventProcessingScheduler sourceControlEventProcessingScheduler;

  @Mock
  SourceControlEventService sourceControlEventServiceMock;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private TestProductLicense testProductLicense;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(SourceControlEventService.class).toInstance(sourceControlEventServiceMock);
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    super.configure(binder);
  }

  @Test
  public void testStart_FeatureDisabled() throws Exception {
    setFeatureFlagEnabled(false);
    assertThat(insightConfig.isFeatureEnabled(Feature.PR_COMMENTING)).isFalse();

    sourceControlEventProcessingScheduler.start();

    verifyNoInteractions(taskSchedulerMock);
  }

  @Test
  public void testStart_FeatureEnabled() throws Exception {
    setFeatureFlagEnabled(true);
    assertThat(insightConfig.isFeatureEnabled(Feature.PR_COMMENTING)).isTrue();

    sourceControlEventProcessingScheduler.start();

    verify(taskSchedulerMock).schedulePeriodicTask(SourceControlEventProcessingScheduler.class,
        SourceControlEventProcessingScheduler.NAME,
        Duration.ofSeconds(SourceControlEventProcessingScheduler.SOURCE_CONTROL_EVENT_PROCESSING_INTERVAL_SECONDS));
  }

  @Test
  public void testExecute() {
    SourceControlEventProcessingScheduler sourceControlEventProcessingSchedulerSpy =
        spy(sourceControlEventProcessingScheduler);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(sourceControlEventProcessingSchedulerSpy).processSourceControlEvents();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      sourceControlEventProcessingSchedulerSpy.execute(mock(JobExecutionContext.class));
    }

    verify(sourceControlEventProcessingSchedulerSpy).processSourceControlEvents();
  }

  @Test
  public void testMonitorPullRequestsForCommenting_Unlicensed() {
    testProductLicense.setFeatures();
    assertThat(testProductLicense.hasFeature(LicensedFeature.AUTOMATION)).isFalse();

    sourceControlEventProcessingScheduler.processSourceControlEvents();

    verifyNoInteractions(sourceControlEventServiceMock);
  }

  @Test
  public void testMonitorPullRequestsForCommenting_Licensed() throws Exception {
    assertThat(testProductLicense.hasFeature(LicensedFeature.AUTOMATION)).isTrue();

    sourceControlEventProcessingScheduler.processSourceControlEvents();

    verify(sourceControlEventServiceMock).processEvents();
  }

  private void setFeatureFlagEnabled(boolean featureFlagEnabled) {
    Map<String, Boolean> features = new HashMap<>();
    features.put(Feature.PR_COMMENTING.getFlag(), featureFlagEnabled);
    insightConfig.setFeatures(features);
  }
}
