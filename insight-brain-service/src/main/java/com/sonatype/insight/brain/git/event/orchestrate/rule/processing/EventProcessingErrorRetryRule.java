/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.common.exception.ExceptionHelper;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.nexus.scm.api.access.control.ExclusiveAccessRequestTimeoutException;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * events with certain errors during processing should be retried; this rule defines those errors and the number
 * of retries to apply; other rules may enforce processing suspension periods (delays) between the retries
 */
public class EventProcessingErrorRetryRule
    implements EventProcessedListener
{
  private static final Logger log = LoggerFactory.getLogger(EventProcessingErrorRetryRule.class);

  @VisibleForTesting
  static final int EVENT_PROCESSING_RETRY_COUNT = 2;

  // keep track of the retry count for application that had an error
  private final Map<String, Integer> eventsInError = new HashMap<>();

  @Override
  public void onEventProcessed(SourceControlEvent event) {
    eventsInError.remove(getEventInErrorKey(event));
  }

  /**
   * shouldRetry makes the assumption that the caller is invoking this method because an error has already occurred
   */
  public boolean shouldRetry(SourceControlEvent event, Exception e) {
    if (null == event || null == e) {
      return false;
    }
    boolean shouldRetry = isRetryableException(e) && isBelowRetryThreshold(event);
    log.debug("Should retry event '{}' of type '{}' for application {} = {}", event.getId(), event.getEventType(),
        event.getApplicationId(), shouldRetry);

    return shouldRetry;
  }

  private boolean isBelowRetryThreshold(SourceControlEvent event) {
    int retryCount = eventsInError.merge(getEventInErrorKey(event), 1, Integer::sum);
    return retryCount <= EVENT_PROCESSING_RETRY_COUNT;
  }

  private boolean isRetryableException(Exception e) {
    return ExceptionHelper.hasCauseOrSuppressedOfType(e, UnknownHostException.class)
        || ExceptionHelper.hasCauseOrSuppressedOfType(e, SocketTimeoutException.class)
        || (ExceptionHelper.hasCauseOrSuppressedOfType(e, HttpResponseException.class)
            && ExceptionUtils.getStackTrace(e).contains("Bad Gateway"))
        || ExceptionHelper.hasCauseOrSuppressedOfType(e, ExclusiveAccessRequestTimeoutException.class)
        || (!isBlank(e.getMessage()) && e.getMessage().contains("abuse detection"));
  }

  private String getEventInErrorKey(SourceControlEvent event) {
    return event.getApplicationId();
  }
}
