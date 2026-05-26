/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.scheduler.QuartzConcurrencyListener;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.index.IndexCreationScheduler;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import jakarta.inject.Inject;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import org.junit.Test;
import org.mockito.Mock;

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
  private TestProductLicense testProductLicense;

  @Inject
  private PopulateSearchIndexTask task;

  @Test
  public void testExecute_AdvancedSearchConfigurationDisabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> task.execute())
        .withMessage("advanced-search-configuration feature is disabled.");

    verifyNoInteractions(mockTaskScheduler);
  }

  @Test
  public void testExecute_ProductLicenseInvalid() {
    testProductLicense.clear();

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(
        () -> task.execute());

    verifyNoInteractions(mockTaskScheduler);
  }

  @Test
  public void testExecute_SchedulesTaskWithDefaultMaxConcurrent() throws Exception {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    when(mockConfiguration.getMaxConcurrentTenantIndexCreation()).thenReturn(5);

    task.execute();

    Map<String, String> expectedJobDataMap = Map.of(
        QuartzConcurrencyListener.MAX_CONCURRENT, "5");
    verify(mockTaskScheduler).scheduleOneTimeTask(eq(mockIndexCreationScheduler), eq(expectedJobDataMap));
  }

  @Test
  public void testExecute_SchedulesTaskWithCustomMaxConcurrent() throws Exception {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    when(mockConfiguration.getMaxConcurrentTenantIndexCreation()).thenReturn(10);

    task.execute();

    Map<String, String> expectedJobDataMap = Map.of(
        QuartzConcurrencyListener.MAX_CONCURRENT, "10");
    verify(mockTaskScheduler).scheduleOneTimeTask(eq(mockIndexCreationScheduler), eq(expectedJobDataMap));
  }

  @Test
  public void testExecute_SchedulesTaskWithZeroMaxConcurrent() throws Exception {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    when(mockConfiguration.getMaxConcurrentTenantIndexCreation()).thenReturn(0);

    task.execute();

    Map<String, String> expectedJobDataMap = Map.of(
        QuartzConcurrencyListener.MAX_CONCURRENT, "0");
    verify(mockTaskScheduler).scheduleOneTimeTask(eq(mockIndexCreationScheduler), eq(expectedJobDataMap));
  }

  @Test
  public void testExecute_IgnoresInputParameters() throws Exception {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(true);
    when(mockConfiguration.getMaxConcurrentTenantIndexCreation()).thenReturn(5);

    task.execute(Map.of("param1", java.util.List.of("value1"), "param2", java.util.List.of("value2")),
        new PrintWriter(OutputStream.nullOutputStream()));

    Map<String, String> expectedJobDataMap = Map.of(
        QuartzConcurrencyListener.MAX_CONCURRENT, "5");
    verify(mockTaskScheduler).scheduleOneTimeTask(eq(mockIndexCreationScheduler), eq(expectedJobDataMap));
  }
}
