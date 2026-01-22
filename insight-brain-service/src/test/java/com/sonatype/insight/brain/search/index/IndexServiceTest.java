/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.time.Duration;
import java.util.Collections;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotAuthorizedException;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

public class IndexServiceTest
    extends AbstractComponentTest
{
  @Mock
  private SearchIndexClient searchIndexClientMock;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private IndexCreationScheduler mockIndexCreationScheduler;

  @Inject
  private IndexService indexService;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    binder.bind(IndexCreationScheduler.class).toInstance(mockIndexCreationScheduler);
    binder.bind(SearchIndexClient.class).toInstance(searchIndexClientMock);
    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(IndexService.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute() throws Exception {
    indexService.execute(null);

    verify(searchIndexClientMock).updateIndex();
  }

  @Test
  public void testRegister() {
    indexService.register();

    verify(taskSchedulerMock).schedulePeriodicTask(indexService, Duration.ofSeconds(3));
  }

  @Test
  public void testCreateIndexAsync_AdvancedConfigurationDisabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> indexService.createIndexAsync())
        .withMessage("advanced-search-configuration feature is disabled.");
  }

  @Test
  public void testCreateSearchIndexAsync() {
    indexService.createIndexAsync();

    verify(taskSchedulerMock).scheduleOneTimeTask(mockIndexCreationScheduler);
  }

  @Test
  public void testIsFullIndexTriggered() {
    indexService.isFullIndexTriggered();

    verify(taskSchedulerMock).isJobTriggered(mockIndexCreationScheduler, Collections.emptyMap());
  }
}
