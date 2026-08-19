/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.opentelemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the OTel span guard pattern used throughout the codebase:
 *
 * <pre>
 *   Span otelSpan = Span.current();
 *   if (otelSpan.getSpanContext().isValid()) {
 *     otelSpan.setAttribute(...);
 *   }
 * </pre>
 *
 * Ensures that when no OTel agent is active (no valid span), the guard prevents attribute setting,
 * and when a valid span exists, the guard allows it through.
 */
public class OtelSpanGuardTest
{
  @Test
  public void spanCurrentWithoutActiveSpan_hasInvalidContext() {
    // When no OTel agent is active, Span.current() returns a no-op span with invalid context
    Span span = Span.current();

    assertThat(span.getSpanContext().isValid()).isFalse();
  }

  @Test
  public void spanCurrentWithActiveSpan_hasValidContext() {
    // Simulate an active span (as the OTel agent would create)
    SpanContext validContext = SpanContext.create(
        "0123456789abcdef0123456789abcdef",
        "0123456789abcdef",
        TraceFlags.getSampled(),
        TraceState.getDefault());
    Span validSpan = Span.wrap(validContext);

    try (Scope ignored = Context.current().with(validSpan).makeCurrent()) {
      Span current = Span.current();

      assertThat(current.getSpanContext().isValid()).isTrue();
    }
  }

  @Test
  public void guardPattern_skipsWhenNoActiveSpan() {
    // Verifies the guard pattern used in TenantManager, IndexCreationScheduler, IndexService
    Span otelSpan = Span.current();
    boolean guardPassed = otelSpan.getSpanContext().isValid();

    assertThat(guardPassed).isFalse();
  }

  @Test
  public void guardPattern_proceedsWhenActiveSpan() {
    SpanContext validContext = SpanContext.create(
        "0123456789abcdef0123456789abcdef",
        "0123456789abcdef",
        TraceFlags.getSampled(),
        TraceState.getDefault());
    Span validSpan = Span.wrap(validContext);

    try (Scope ignored = Context.current().with(validSpan).makeCurrent()) {
      Span otelSpan = Span.current();
      boolean guardPassed = otelSpan.getSpanContext().isValid();

      assertThat(guardPassed).isTrue();
    }
  }
}
