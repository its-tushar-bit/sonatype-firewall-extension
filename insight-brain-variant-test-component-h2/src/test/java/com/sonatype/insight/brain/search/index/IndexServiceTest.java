/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotAuthorizedException;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;

@ComponentH2Test
public class IndexServiceTest
    extends AbstractComponentH2Test
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

  @Test
  public void testCancelFullRebuild() {
    when(searchIndexClientMock.isFullRebuildInProgress()).thenReturn(true);

    indexService.cancelFullRebuild();

    verify(searchIndexClientMock).cancelFullRebuild();
  }

  /**
   * A cancel outlives this call — a rebuild that is only scheduled reads it when its task starts — so one accepted
   * while nothing is building would stay armed and abort whichever rebuild ran next.
   */
  @Test
  public void testCancelFullRebuild_IsANoOpWhenNothingIsBuilding() {
    when(taskSchedulerMock.isJobTriggered(indexCreationScheduler, Collections.emptyMap())).thenReturn(false);
    when(searchIndexClientMock.isFullRebuildInProgress()).thenReturn(false);

    indexService.cancelFullRebuild();

    verify(searchIndexClientMock, never()).cancelFullRebuild();
  }

  /**
   * A rebuild that is scheduled but not yet started is cancellable: that gap is where an operator hitting cancel
   * straight after starting a rebuild lands.
   */
  @Test
  public void testCancelFullRebuild_CancelsARebuildThatIsScheduledButNotYetRunning() {
    when(taskSchedulerMock.isJobTriggered(indexCreationScheduler, Collections.emptyMap())).thenReturn(true);

    indexService.cancelFullRebuild();

    verify(searchIndexClientMock).cancelFullRebuild();
  }

  /**
   * Turning the feature off while a rebuild is running must not strand it. Cancel is the only way to stop one, so it
   * stays reachable on permission alone.
   */
  @Test
  public void testCancelFullRebuild_StillCancelsWhenAdvancedConfigurationDisabled() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.setEnabled(false);
    when(searchIndexClientMock.isFullRebuildInProgress()).thenReturn(true);

    indexService.cancelFullRebuild();

    verify(searchIndexClientMock).cancelFullRebuild();
  }

  @Test
  public void testIsFullRebuildInProgress_DelegatesToClientWhenNotScheduled() {
    when(taskSchedulerMock.isJobTriggered(indexCreationScheduler, Collections.emptyMap())).thenReturn(false);
    when(searchIndexClientMock.isFullRebuildInProgress()).thenReturn(true);

    assertThat(indexService.isFullRebuildInProgress()).isTrue();
  }
}
