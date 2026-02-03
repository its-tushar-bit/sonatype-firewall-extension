/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Collections;
import java.util.Map;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.scheduler.QuartzConcurrencyListener;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.index.IndexCreationScheduler;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.google.inject.Binder;
import jakarta.inject.Inject;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PopulateSearchIndexTaskTest
    extends AbstractComponentTest
{
  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private IndexCreationScheduler mockIndexCreationScheduler;

  @Mock
  private Configuration mockConfiguration;

  @Inject
  private PopulateSearchIndexTask populateSearchIndexTask;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    binder.bind(IndexCreationScheduler.class).toInstance(mockIndexCreationScheduler);
    binder.bind(Configuration.class).toInstance(mockConfiguration);
    super.configure(binder);
  }

  @Test
  public void testGetName() {
    assertThat(populateSearchIndexTask.getName()).isEqualTo("populateSearchIndex");
  }

  @Test
  public void testExecute_AdvancedSearchConfigurationDisabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> populateSearchIndexTask.execute(Collections.emptyMap(), null))
        .withMessage("advanced-search-configuration feature is disabled.");

    verifyNoInteractions(mockTaskScheduler);
  }

  @Test
  public void testExecute_SchedulesTaskWithDefaultMaxConcurrent() throws Exception {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    when(mockConfiguration.getMaxConcurrentTenantIndexCreation()).thenReturn(5);

    populateSearchIndexTask.execute(Collections.emptyMap(), null);

    Map<String, String> expectedJobDataMap = Map.of(
        QuartzConcurrencyListener.MAX_CONCURRENT, "5"
    );
    verify(mockTaskScheduler).scheduleOneTimeTask(eq(mockIndexCreationScheduler), eq(expectedJobDataMap));
  }

  @Test
  public void testExecute_SchedulesTaskWithCustomMaxConcurrent() throws Exception {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    when(mockConfiguration.getMaxConcurrentTenantIndexCreation()).thenReturn(10);

    populateSearchIndexTask.execute(Collections.emptyMap(), null);

    Map<String, String> expectedJobDataMap = Map.of(
        QuartzConcurrencyListener.MAX_CONCURRENT, "10"
    );
    verify(mockTaskScheduler).scheduleOneTimeTask(eq(mockIndexCreationScheduler), eq(expectedJobDataMap));
  }

  @Test
  public void testExecute_SchedulesTaskWithZeroMaxConcurrent() throws Exception {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    when(mockConfiguration.getMaxConcurrentTenantIndexCreation()).thenReturn(0);

    populateSearchIndexTask.execute(Collections.emptyMap(), null);

    Map<String, String> expectedJobDataMap = Map.of(
        QuartzConcurrencyListener.MAX_CONCURRENT, "0"
    );
    verify(mockTaskScheduler).scheduleOneTimeTask(eq(mockIndexCreationScheduler), eq(expectedJobDataMap));
  }

  @Test
  public void testExecute_IgnoresInputParameters() throws Exception {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    when(mockConfiguration.getMaxConcurrentTenantIndexCreation()).thenReturn(5);

    // Pass some input parameters that should be ignored
    Map<String, java.util.List<String>> inputParams = Map.of(
        "param1", java.util.List.of("value1"),
        "param2", java.util.List.of("value2")
    );

    populateSearchIndexTask.execute(inputParams, null);

    Map<String, String> expectedJobDataMap = Map.of(
        QuartzConcurrencyListener.MAX_CONCURRENT, "5"
    );
    verify(mockTaskScheduler).scheduleOneTimeTask(eq(mockIndexCreationScheduler), eq(expectedJobDataMap));
  }
}
