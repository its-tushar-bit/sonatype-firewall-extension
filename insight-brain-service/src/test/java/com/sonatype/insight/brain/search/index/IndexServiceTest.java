/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;

public class IndexServiceTest
    extends AbstractComponentTest
{
  @Mock
  private SearchIndexClient searchIndexClientMock;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Inject
  private IndexCreationScheduler indexCreationScheduler;

  @Inject
  private IndexService indexService;

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

    verify(taskSchedulerMock).scheduleOneTimeTask(indexCreationScheduler);
  }

  @Test
  public void testIsFullIndexTriggered() {
    indexService.isFullIndexTriggered();

    verify(taskSchedulerMock).isJobTriggered(indexCreationScheduler, Collections.emptyMap());
  }
}
