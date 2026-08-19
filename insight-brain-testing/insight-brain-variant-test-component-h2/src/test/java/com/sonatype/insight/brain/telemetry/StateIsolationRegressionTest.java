/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuditTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

/**
 * Regression guard for the CLM-45583 isolation contract: the process-global audit-log capture and the
 * {@link AdvancedSearchTelemetryMetrics} singleton are reset per test, even though the {@code @ComponentH2Test}
 * Spring context (and therefore the logback logger hierarchy and that collector) is reused across every test and
 * class in this module.
 *
 * <p>
 * {@code step1} deliberately pollutes both state holders; {@code step2} — forced to run after {@code step1} by
 * {@link MethodOrderer.MethodName} — asserts it sees none of it. This directly exercises the per-test reset within a
 * class. Cross-class isolation is not ordered here (this module shards classes across forks, so a deterministic
 * "a later class observes an earlier class's pollution" probe cannot be guaranteed); it instead follows by
 * construction from the same two mechanisms this test verifies — each test gets a fresh {@code LogOutput} appender
 * (attached/detached per test by {@code ComponentTestDbHarnessExtension}) and
 * {@link AdvancedSearchTelemetryResetExtension} drains the collector before every test regardless of which class ran
 * previously.
 */
@ComponentH2Test
@ExtendWith(AdvancedSearchTelemetryResetExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class StateIsolationRegressionTest
    extends AbstractComponentH2AuditTest
{
  private static final String PROBE_DOMAIN = "clm-45583-regression";

  private static final String PROBE_LOGGER = AuditRecorder.toLoggerName(PROBE_DOMAIN);

  @Inject
  private AdvancedSearchTelemetryMetrics telemetryMetrics;

  @Test
  public void step1_pollutesGlobalAuditAndTelemetryState() {
    // Emit a record on the process-global audit logger and confirm this test captures exactly it.
    LoggerFactory.getLogger(PROBE_LOGGER).info("{\"domain\":\"" + PROBE_DOMAIN + "\",\"type\":\"probe\"}");
    assertThat(getLogOutput().getInfoMessages(PROBE_LOGGER)).hasSize(1);

    // Pollute the shared, non-transactional search-telemetry singleton without draining it.
    telemetryMetrics.addSearch(Set.of("clm-45583-field"));
  }

  @Test
  public void step2_observesCleanGlobalAuditAndTelemetryState() {
    // The audit appender is per test: step1's record must not have leaked into this test's capture.
    assertThat(getLogOutput().getInfoMessages(PROBE_LOGGER)).isEmpty();

    // AdvancedSearchTelemetryResetExtension drained the singleton before this test, so step1's
    // recorded search must not be visible here.
    assertThat(telemetryMetrics.computeStatsAndReset().getTotalSearches()).isEqualTo(0L);
  }
}
