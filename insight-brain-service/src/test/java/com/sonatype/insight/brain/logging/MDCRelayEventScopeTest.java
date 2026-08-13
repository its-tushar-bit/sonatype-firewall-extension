/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

public class MDCRelayEventScopeTest
{
  @AfterEach
  public void clearMdc() {
    MDC.remove(MDCRelayEventScope.RELAY_EVENT_ID);
  }

  @Test
  public void scopeSetsAndClearsKey() {
    assertThat(MDC.get(MDCRelayEventScope.RELAY_EVENT_ID)).isNull();

    try (MDCRelayEventScope scope = MDCRelayEventScope.forEventId("evt-1")) {
      assertThat(MDC.get(MDCRelayEventScope.RELAY_EVENT_ID)).isEqualTo("evt-1");
    }

    assertThat(MDC.get(MDCRelayEventScope.RELAY_EVENT_ID)).isNull();
  }

  @Test
  public void nullEventIdRemovesKeyAndRestoresOnClose() {
    MDC.put(MDCRelayEventScope.RELAY_EVENT_ID, "outer");

    try (MDCRelayEventScope scope = MDCRelayEventScope.forEventId(null)) {
      assertThat(MDC.get(MDCRelayEventScope.RELAY_EVENT_ID)).isNull();
    }

    assertThat(MDC.get(MDCRelayEventScope.RELAY_EVENT_ID)).isEqualTo("outer");
  }

  @Test
  public void nestedScopesRestorePreviousValueOnClose() {
    try (MDCRelayEventScope outer = MDCRelayEventScope.forEventId("outer")) {
      assertThat(MDC.get(MDCRelayEventScope.RELAY_EVENT_ID)).isEqualTo("outer");
      try (MDCRelayEventScope inner = MDCRelayEventScope.forEventId("inner")) {
        assertThat(MDC.get(MDCRelayEventScope.RELAY_EVENT_ID)).isEqualTo("inner");
      }
      assertThat(MDC.get(MDCRelayEventScope.RELAY_EVENT_ID)).isEqualTo("outer");
    }

    assertThat(MDC.get(MDCRelayEventScope.RELAY_EVENT_ID)).isNull();
  }

  @Test
  public void closeWithNoPriorValueRemovesKey() {
    try (MDCRelayEventScope scope = MDCRelayEventScope.forEventId("evt-1")) {
      assertThat(MDC.get(MDCRelayEventScope.RELAY_EVENT_ID)).isEqualTo("evt-1");
    }

    assertThat(MDC.get(MDCRelayEventScope.RELAY_EVENT_ID)).isNull();
  }
}
