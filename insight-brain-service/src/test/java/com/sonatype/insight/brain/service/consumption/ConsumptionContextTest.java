/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConsumptionContextTest
{
  @After
  public void clearContext() {
    ConsumptionContext.clear();
  }

  @Test
  public void sessionId_isNullByDefault() {
    ConsumptionContext.set("org-1", "pro", "ui");
    assertThat(ConsumptionContext.get().getSessionId()).isNull();
  }

  @Test
  public void sessionId_canBeSetAndRead() {
    ConsumptionContext.set("org-1", "pro", "ui");
    ConsumptionContext.get().setSessionId("sess-abc123");
    assertThat(ConsumptionContext.get().getSessionId()).isEqualTo("sess-abc123");
  }
}
