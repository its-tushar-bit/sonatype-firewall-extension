/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.service.AbstractComponentAuditTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Audit counterpart of {@link AbstractComponentH2Test}: the Jupiter Spring wiring
 * ({@code @ExtendWith(SpringExtension.class)}) for reused-context H2 component audit tests, kept off the shared
 * {@code SpringInjectedTest}/{@code AbstractComponentAuditTest} chain so the JUnit-4 (vintage) service tests stay
 * vintage-only. Converted component audit tests extend this instead of {@code AbstractComponentAuditTest} and carry
 * {@code @ComponentH2Test}. The audit-log capture helpers ({@code getLogOutput}, {@code assertAuditLog}, …) are
 * inherited unchanged from {@code AbstractComponentAuditTest}.
 *
 * <p>
 * Audit records are captured by attaching a logback appender to the process-global audit logger
 * ({@code com.sonatype.insight.audit}), which is shared by every class in the reused {@code @ComponentH2Test}
 * context — the reason audit tests were previously classified as reuse-incompatible (see CLM-45554). Isolation here
 * is per test, not process-global: the inherited {@code LogOutput} is an {@code ExternalResource} field, so
 * {@link ComponentTestDbHarnessExtension} attaches a fresh appender before each test and detaches it after, and
 * {@link #clearCapturedAuditRecords()} empties it at the start of each test. Assertions go through
 * {@code awaitLogEntries}, which blocks until the test's own record is captured and filters by audit domain + type,
 * so a test normally observes only the records it produced. Audit emission can be asynchronous, so a delayed
 * straggler of the same domain and type from an earlier test could in principle still land after the clear; prefer
 * the {@code >=}-based {@code awaitLogEntries}/{@code assertAuditLog} helpers over asserting exact global counts.
 */
@ExtendWith(SpringExtension.class)
public abstract class AbstractComponentH2AuditTest
    extends AbstractComponentAuditTest
{
  /**
   * Reset the shared audit-log appender before each test. Runs after {@link ComponentTestDbHarnessExtension} has
   * attached the (per-test) appender in its {@code beforeEach}, so each test observes only the audit records it
   * produces itself.
   */
  @BeforeEach
  public void clearCapturedAuditRecords() {
    getLogOutput().clear();
  }
}
