/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.nexus.scm.api.access.control.ExclusiveAccessRequestTimeoutException;

import com.google.common.collect.ImmutableList;
import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EventProcessingErrorRetryRuleTest
{
  @Test
  public void testShouldRetry_unknownHost() {
    EventProcessingErrorRetryRule rule = new EventProcessingErrorRetryRule();

    assertThat(rule.shouldRetry(createEvent(), new UnknownHostException())).isTrue();
    assertThat(rule.shouldRetry(createEvent(), new RuntimeException(new UnknownHostException()))).isTrue();
  }

  @Test
  public void testShouldRetry_socketTimeout() {
    EventProcessingErrorRetryRule rule = new EventProcessingErrorRetryRule();

    assertThat(rule.shouldRetry(createEvent(), new SocketTimeoutException())).isTrue();
    assertThat(rule.shouldRetry(createEvent(), new RuntimeException(new SocketTimeoutException()))).isTrue();
  }

  @Test
  public void testShouldRetry_badGateway() {
    EventProcessingErrorRetryRule rule = new EventProcessingErrorRetryRule();

    assertThat(rule.shouldRetry(createEvent(), new HttpResponseException(0, "foo Bad Gateway bar"))).isTrue();
    assertThat(
        rule.shouldRetry(createEvent(), new RuntimeException(new HttpResponseException(0, "foo Bad Gateway bar"))))
            .isTrue();

    assertThat(rule.shouldRetry(createEvent(), new HttpResponseException(0, "test"))).isFalse();
  }

  @Test
  public void testShouldRetry_exclusiveAccessRequestTimeout() {
    EventProcessingErrorRetryRule rule = new EventProcessingErrorRetryRule();

    assertThat(rule.shouldRetry(createEvent(), new ExclusiveAccessRequestTimeoutException("testing"))).isTrue();
    assertThat(
        rule.shouldRetry(createEvent(), new RuntimeException(new ExclusiveAccessRequestTimeoutException("testing"))))
            .isTrue();
  }

  @Test
  public void testShouldRetry_forAbuseDetection() {
    EventProcessingErrorRetryRule rule = new EventProcessingErrorRetryRule();

    assertThat(rule.shouldRetry(createEvent(), new Exception("blah blah abuse detection blah blah"))).isTrue();
  }

  @Test
  public void testShouldRetry_generalExceptions() {
    EventProcessingErrorRetryRule rule = new EventProcessingErrorRetryRule();

    assertThat(rule.shouldRetry(createEvent(), new Exception("some message"))).isFalse();
    assertThat(rule.shouldRetry(createEvent(), new Exception())).isFalse();
  }

  @Test
  public void testShouldRetry_missingData() {
    EventProcessingErrorRetryRule rule = new EventProcessingErrorRetryRule();

    assertThat(rule.shouldRetry(null, null)).isFalse();
    assertThat(rule.shouldRetry(createEvent(), null)).isFalse();
    assertThat(rule.shouldRetry(null, new UnknownHostException())).isFalse();
  }

  @Test
  public void testShouldRetry_retriesExceededAndReset() {
    EventProcessingErrorRetryRule rule = new EventProcessingErrorRetryRule();
    List<Exception> retryExceptions = ImmutableList.of(
        new UnknownHostException(),
        new ExclusiveAccessRequestTimeoutException("for testing"),
        new Exception("blah abuse detection blah"));
    retryExceptions.forEach(e -> {
      SourceControlEvent event = createEvent();
      SourceControlEvent event2 = createEvent();
      // exhaust retries
      for (int i = 0; i < EventProcessingErrorRetryRule.EVENT_PROCESSING_RETRY_COUNT; i++) {
        assertThat(rule.shouldRetry(event, e)).isTrue();
      }
      // next check should fail
      assertThat(rule.shouldRetry(event, e)).isFalse();

      // verify that the retries are tracked by application - check different event for different application
      assertThat(rule.shouldRetry(event2, e)).isTrue();

      // simulate event2 processed successfully but didn't affect the retry characteristics for the original event's
      // application
      rule.onEventProcessed(event2);
      assertThat(rule.shouldRetry(event, e)).isFalse();

      // simulate event processed successfully and check retry status
      rule.onEventProcessed(event);
      assertThat(rule.shouldRetry(event, e)).isTrue();
    });
  }

  private SourceControlEvent createEvent() {
    return new SourceControlEvent().forStatusUpdate().setApplicationId(UUID.randomUUID().toString());
  }
}
