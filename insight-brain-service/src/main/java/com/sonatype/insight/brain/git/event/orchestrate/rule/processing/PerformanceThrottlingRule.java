/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.time.LocalDateTime;
import java.util.Date;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this rule is to throttle event processing if the performance of processing the simplest and highest
 * priority API event (commit status update) falls below a certain threshold.
 */
public class PerformanceThrottlingRule
    implements EventProcessedListener
{
  private static final Logger log = LoggerFactory.getLogger(PerformanceThrottlingRule.class);

  private static final int TIMING_CUTOFF_MS = 500;

  @VisibleForTesting
  static final int EVENT_SUSPENSION_SECONDS = 3;

  private static final int ACCEPTABLE_DURATION_WINDOW_MULTIPLICATION_FACTOR = 10;

  private static final LocalDateTime IN_THE_PAST = LocalDateTime.now().minusSeconds(1);

  private long minCommitStatusTimingMs = 2_000;

  private LocalDateTime eventSuspensionExpirationTime = IN_THE_PAST;

  public boolean canPushEvents() {
    boolean result = !LocalDateTime.now().isBefore(eventSuspensionExpirationTime);
    log.trace("Can push event = {}", result);
    return result;
  }

  @Override
  public void onEventProcessed(SourceControlEvent event) {
    if (event.getEventType().equals(SourceControlEvent.STATUS_UPDATE_EVENT)) {
      // assume processing time is ok unless proven otherwise
      eventSuspensionExpirationTime = IN_THE_PAST;

      long eventDurationMs = getEventDurationMs(event);

      // tweak the min timing down if it falls below our initial setting but is above the absolute minimum
      if (0 < eventDurationMs && eventDurationMs < TIMING_CUTOFF_MS) {
        minCommitStatusTimingMs = TIMING_CUTOFF_MS;
        log.trace("Commit status event duration canary time adjusted to minimum");
      }
      else if (TIMING_CUTOFF_MS < eventDurationMs && eventDurationMs < minCommitStatusTimingMs) {
        minCommitStatusTimingMs = eventDurationMs;
        log.trace("Commit status event duration canary time adjusted to {} seconds",
            durationToTimeStr(eventDurationMs));
      }
      else if (eventDurationMs > getAcceptableDurationMilliseconds()) {
        eventSuspensionExpirationTime = LocalDateTime.now().plusSeconds(EVENT_SUSPENSION_SECONDS);
        log.debug("Commit status event duration of {} seconds exceeded canary limit of {} seconds.  " +
            "Suspending event processing for {} seconds", durationToTimeStr(eventDurationMs),
            getAcceptableDurationSeconds(), EVENT_SUSPENSION_SECONDS);
      }
      else {
        log.trace("Commit status event duration of {} seconds within limits", durationToTimeStr(eventDurationMs));
      }
    }
  }

  /**
   * take our best commit status time and multiple by the pre-determined factor to get the acceptable duration window
   */
  @VisibleForTesting
  long getAcceptableDurationMilliseconds() {
    return minCommitStatusTimingMs * ACCEPTABLE_DURATION_WINDOW_MULTIPLICATION_FACTOR;
  }

  @VisibleForTesting
  long getAcceptableDurationSeconds() {
    return getAcceptableDurationMilliseconds() / 1_000;
  }

  @VisibleForTesting
  long getMinCommitStatusTimingMs() {
    return minCommitStatusTimingMs;
  }

  private long getEventDurationMs(SourceControlEvent event) {
    Date eventCompleteTime = event.getCompleteTime();
    if (eventCompleteTime == null) {
      eventCompleteTime = new Date();
    }
    return eventCompleteTime.getTime() - event.getCreateTime().getTime();
  }

  private String durationToTimeStr(long durationInMs) {
    return String.format("%3.1f", durationInMs / 1_000.0);
  }
}
