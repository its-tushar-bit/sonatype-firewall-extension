/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.SystemConfigurationPropertyCacheInvalidationJob;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration test verifying that enable/disable feature calls trigger cross-node cache invalidation.
 */
@ContextConfiguration(classes = ApiConfigFeaturesServiceCrossNodeTest.TestConfig.class)
public class ApiConfigFeaturesServiceCrossNodeTest
    extends AbstractComponentTest
{
  @Inject
  private ApiConfigFeaturesService service;

  @Inject
  @Named("testTaskScheduler")
  private TaskScheduler mockTaskScheduler;

  @Inject
  private SystemConfigurationPropertyCacheInvalidationJob cacheInvalidationJob;

  @Inject
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @TestConfiguration
  static class TestConfig
  {
    @Bean(name = "testTaskScheduler")
    @Primary
    public TaskScheduler testTaskScheduler() {
      return mock(TaskScheduler.class);
    }
  }

  @After
  public void resetFeatureState() {
    // Delete any row written by setEnabled(), restoring the default (absent = enabled) state
    systemConfigurationPropertyDAO.set(
        SystemConfigurationPropertyFeature.CODE_INSIGHTS.getPropertyName(), null);
  }

  @Test
  public void testEnableFeature_triggersCrossNodeInvalidation() {
    // Disable the feature first so we can enable it
    SystemConfigurationPropertyFeature.CODE_INSIGHTS.setEnabled(false);

    service.enableFeatureNoAuthz("codeInsights");

    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(cacheInvalidationJob);
  }

  @Test
  public void testDisableFeature_triggersCrossNodeInvalidation() {
    // Ensure the feature is enabled so we can disable it
    SystemConfigurationPropertyFeature.CODE_INSIGHTS.setEnabled(true);

    service.disableFeatureNoAuthz("codeInsights");

    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(cacheInvalidationJob);
  }
}
