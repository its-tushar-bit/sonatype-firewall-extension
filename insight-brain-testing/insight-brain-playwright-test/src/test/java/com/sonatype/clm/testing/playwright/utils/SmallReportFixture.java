/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.utils;

/**
 * Single source of truth for facts about the {@code small-report} canned scan fixture used by
 * the {@code PolicyEvaluation*PlaywrightTest} classes. Values here must stay in sync with
 * {@code src/test/resources/canned-reports/small-report/bom.json}; the JSON test-data files
 * ({@code policy-evaluation-match-state.json}, {@code policy-evaluation-security-severity.json})
 * mirror these values for the data-driven tests.
 *
 * <p>
 * Threat levels are intentionally NOT defined here — they are seed-time parameters chosen by
 * each test, not facts about the fixture.
 *
 * <h2>Fixture change notes for cross-test reviewers</h2>
 * <ul>
 * <li>{@code small-report/security.json} carries {@code extension="jar"} +
 * {@code classifier=""} on every entry's {@code componentIdentifier.coordinates}. These
 * fields are required for {@code ComponentIdentifier} matching at policy-evaluation time,
 * so {@code SecurityVulnerabilitySeverity} policies fire against the canned report — see
 * {@code PolicyEvaluationSecurityVulnerabilityPlaywrightTest}.</li>
 * <li>{@code ReportListPlaywrightTest} uses {@code small-report} as its STAGE scan but only
 * asserts on the BUILD-column chiclets (which are sourced from {@code large-report}), so
 * that test is unaffected by stage-side violations introduced by the security.json shape.
 * Any future Stage-Release-column assertions added there must account for this.</li>
 * </ul>
 */
public final class SmallReportFixture
{
  /** Classpath dir holding the canned scan report. */
  public static final String CANNED_REPORT_DIR = "/canned-reports/small-report";

  /** Count of {@code MatchState=exact} components in {@code bom.json}. */
  public static final int EXACT_COMPONENT_COUNT = 12;

  /** Count of {@code MatchState=unknown} components in {@code bom.json}. */
  public static final int UNKNOWN_COMPONENT_COUNT = 1;

  /** Distinguished exact-match component used by row-presence assertions. */
  public static final String COMPONENT_JETTY = "jetty";

  /** Second distinguished exact-match component used by row-presence assertions. */
  public static final String COMPONENT_GERONIMO = "geronimo-security";

  private SmallReportFixture() {
  }
}
