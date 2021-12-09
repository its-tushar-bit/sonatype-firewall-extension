/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.git.VerifiableLoggingTestBase;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class SourceControlEventProcessingSchedulerTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private InsightConfig mockInsightConfig;

  @Mock
  private SourceControlEventService sourceControlEventService;

  @Mock
  private IqForScmLicenseChecker licenseChecker;

  public SourceControlEventProcessingSchedulerTest() {
    super(SourceControlEventProcessingScheduler.class);
  }

  @Test
  public void testSourceControlEventProcessingScheduler_startAndStop() throws Exception {
    // given: scheduler instance with valid product license
    final int delaySeconds = 2;
    final int intervalSeconds = 1;
    SourceControlEventProcessingScheduler scheduler =
        new SourceControlEventProcessingScheduler(sourceControlEventService, mockInsightConfig, licenseChecker,
            delaySeconds, intervalSeconds);
    when(licenseChecker.isIqForScmSupported()).thenReturn(true);
    when(mockInsightConfig.isFeatureEnabled(
        eq(Feature.ORCHESTRATED_EVENT_PROCESSING))).thenReturn(false);

    // when: start scheduler and wait (less than full initial delay)
    scheduler.start();
    Thread.sleep(1500);

    // then: no invocations of event processing yet
    verify(sourceControlEventService, never()).processEvents();
    assertThatLogMessagesEqual(
        info("Scheduled processing of source control events every 1 second(s) starting in 2 second(s)")
    );

    // when: wait 3 event processing cycles
    Thread.sleep(3000);

    // then: event service invoked 3 times
    verify(sourceControlEventService, times(3)).processEvents();
    assertThatLogMessagesEqual(
        info("Scheduled processing of source control events every 1 second(s) starting in 2 second(s)"),
        debug("Commencing source control event processing cycle"),
        debug("0 source control events submitted for execution"),
        debug("Source control event processing cycle complete"),
        debug("Commencing source control event processing cycle"),
        debug("0 source control events submitted for execution"),
        debug("Source control event processing cycle complete"),
        debug("Commencing source control event processing cycle"),
        debug("0 source control events submitted for execution"),
        debug("Source control event processing cycle complete")
    );

    // when: stop scheduler and wait (2 intervals)
    scheduler.stop();
    Thread.sleep(2000);

    // then: scheduler stopped and no new invocations of the event processing service
    verify(sourceControlEventService, times(3)).processEvents();
    assertThatLogMessagesEqual(
        info("Scheduled processing of source control events every 1 second(s) starting in 2 second(s)"),
        debug("Commencing source control event processing cycle"),
        debug("0 source control events submitted for execution"),
        debug("Source control event processing cycle complete"),
        debug("Commencing source control event processing cycle"),
        debug("0 source control events submitted for execution"),
        debug("Source control event processing cycle complete"),
        debug("Commencing source control event processing cycle"),
        debug("0 source control events submitted for execution"),
        debug("Source control event processing cycle complete"),
        info("Stopped source control event processing")
    );
  }

  @Test
  public void testSourceControlEventProcessingScheduler_exceptionInEventProcessing() throws Exception {
    // given: scheduler instance with polling service that throws IO exceptions
    final int delaySeconds = 1;
    final int intervalSeconds = 1;
    SourceControlEventProcessingScheduler scheduler =
        new SourceControlEventProcessingScheduler(sourceControlEventService, mockInsightConfig, licenseChecker,
            delaySeconds, intervalSeconds);
    doThrow(new RuntimeException("some runtime exception")).when(sourceControlEventService).processEvents();
    when(licenseChecker.isIqForScmSupported()).thenReturn(true);
    when(mockInsightConfig.isFeatureEnabled(eq(Feature.ORCHESTRATED_EVENT_PROCESSING))).thenReturn(false);

    // when: start scheduler, wait (delay + 1 interval)
    scheduler.start();
    Thread.sleep(1500);

    // then : expecting exception was thrown, caught and handled
    verify(sourceControlEventService, times(1)).processEvents();
    assertThatLogMessagesEqual(
        info("Scheduled processing of source control events every 1 second(s) starting in 1 second(s)"),
        debug("Commencing source control event processing cycle"),
        error("some runtime exception"),
        debug("Source control event processing cycle complete")
    );

    // when: throw runtime exception instead and wait another interval
    doThrow(new RuntimeException("some runtime exception")).when(sourceControlEventService).processEvents();
    Thread.sleep(1000);

    // then: polling still occurring and runtime exception was handled
    verify(sourceControlEventService, times(2)).processEvents();
    assertThatLogMessagesEqual(
        info("Scheduled processing of source control events every 1 second(s) starting in 1 second(s)"),
        debug("Commencing source control event processing cycle"),
        error("some runtime exception"),
        debug("Source control event processing cycle complete"),
        debug("Commencing source control event processing cycle"),
        error("some runtime exception"),
        debug("Source control event processing cycle complete")
    );

    // when: stop throwing and wait
    doReturn(0).when(sourceControlEventService).processEvents();
    Thread.sleep(1000);

    // then: polling still occurring
    verify(sourceControlEventService, times(3)).processEvents();
    assertThatLogMessagesEqual(
        info("Scheduled processing of source control events every 1 second(s) starting in 1 second(s)"),
        debug("Commencing source control event processing cycle"),
        error("some runtime exception"),
        debug("Source control event processing cycle complete"),
        debug("Commencing source control event processing cycle"),
        error("some runtime exception"),
        debug("Source control event processing cycle complete"),
        debug("Commencing source control event processing cycle"),
        debug("0 source control events submitted for execution"),
        debug("Source control event processing cycle complete")
    );

    // cleanup: stop the scheduler so as not to interfere with other tests
    scheduler.stop();
  }

  @Test
  public void testSourceControlEventProcessingScheduler_unlicensed() throws Exception {
    // given: valid scheduler instance but missing license feature
    final int delaySeconds = 1;
    final int intervalSeconds = 1;
    SourceControlEventProcessingScheduler scheduler =
        new SourceControlEventProcessingScheduler(sourceControlEventService, mockInsightConfig, licenseChecker,
            delaySeconds, intervalSeconds);
    when(mockInsightConfig.isFeatureEnabled(eq(Feature.ORCHESTRATED_EVENT_PROCESSING))).thenReturn(false);

    // when: start scheduler, wait (delay + 1 interval)
    scheduler.start();

    // then : scheduler is started, but it does nothing
    verify(sourceControlEventService, never()).processEvents();
    assertThatLogMessagesEqual(
        info("Scheduled processing of source control events every 1 second(s) starting in 1 second(s)")
    );

    // cleanup: stop the scheduler so as not to interfere with other tests
    scheduler.stop();
  }
}
