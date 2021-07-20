/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.net.UnknownHostException;

import com.sonatype.nexus.scm.api.access.control.ExclusiveAccessRequestTimeoutException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EventProcessingErrorRetryRuleTest
{
  @Test
  public void testShouldRetry() {
    EventProcessingErrorRetryRule rule = new EventProcessingErrorRetryRule();

    assertThat(rule.shouldRetry(new UnknownHostException())).isTrue();
    assertThat(rule.shouldRetry(new ExclusiveAccessRequestTimeoutException("testing"))).isTrue();
    assertThat(rule.shouldRetry(new Exception("blah blah abuse detection blah blah"))).isTrue();

    assertThat(rule.shouldRetry(null)).isFalse();
    assertThat(rule.shouldRetry(new Exception("some message"))).isFalse();
    assertThat(rule.shouldRetry(new Exception())).isFalse();
  }
}
