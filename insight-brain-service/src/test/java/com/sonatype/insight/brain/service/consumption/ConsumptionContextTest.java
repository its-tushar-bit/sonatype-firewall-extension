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

  @Test
  public void suppressVrCascadeScope_setsFlagWhileOpen_restoresOnClose() {
    ConsumptionContext.set("org-1", "pro", "ui");
    assertThat(ConsumptionContext.get().isSuppressVrCascade()).isFalse();

    try (var ignored = ConsumptionContext.suppressVrCascadeScope()) {
      assertThat(ConsumptionContext.get().isSuppressVrCascade()).isTrue();
    }

    assertThat(ConsumptionContext.get().isSuppressVrCascade()).isFalse();
  }

  @Test
  public void suppressVrCascadeScope_restoresOnException() {
    ConsumptionContext.set("org-1", "pro", "ui");

    try {
      try (var ignored = ConsumptionContext.suppressVrCascadeScope()) {
        assertThat(ConsumptionContext.get().isSuppressVrCascade()).isTrue();
        throw new RuntimeException("boom");
      }
    }
    catch (RuntimeException expected) {
      // expected
    }

    assertThat(ConsumptionContext.get().isSuppressVrCascade()).isFalse();
  }

  @Test
  public void suppressVrCascadeScope_preservesNestedTrue() {
    ConsumptionContext.set("org-1", "pro", "ui");
    ConsumptionContext.get().setSuppressVrCascade(true);

    try (var outer = ConsumptionContext.suppressVrCascadeScope()) {
      try (var inner = ConsumptionContext.suppressVrCascadeScope()) {
        assertThat(ConsumptionContext.get().isSuppressVrCascade()).isTrue();
      }
      assertThat(ConsumptionContext.get().isSuppressVrCascade()).isTrue();
    }

    assertThat(ConsumptionContext.get().isSuppressVrCascade()).isTrue();
  }

  @Test
  public void suppressVrCascadeScope_handlesNullContext() {
    assertThat(ConsumptionContext.get()).isNull();

    try (var ignored = ConsumptionContext.suppressVrCascadeScope()) {
      assertThat(ConsumptionContext.get()).isNull();
    }
  }

  @Test
  public void snapshot_doesNotCarrySuppressVrCascade() {
    // Guards MTIQ tenant isolation: a future engineer adding the field to Snapshot would
    // silently leak the flag onto background-job threads via restore().
    ConsumptionContext.set("org-1", "pro", "ui");
    ConsumptionContext.get().setSuppressVrCascade(true);
    ConsumptionContext.Snapshot snap = ConsumptionContext.snapshot();
    ConsumptionContext.clear();

    ConsumptionContext.restore(snap);

    assertThat(ConsumptionContext.get().isSuppressVrCascade()).isFalse();
  }
}
