/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.githubapp;

/**
 * Health of a GitHub App's link to the SCM webhook relay.
 *
 * <p>
 * Stored on {@code github_app.relay_link_state} as a plain {@code varchar(16)} string so the
 * jOOQ/JPA path stays trivial. Transitions:
 * <ul>
 * <li>{@link #UNREGISTERED} -&gt; {@link #OK} on first successful auto-registration.</li>
 * <li>{@link #UNREGISTERED} -&gt; {@link #ERROR} when auto-registration fails and retry budget remains.</li>
 * <li>{@link #ERROR} -&gt; {@link #OK} when a polling-cycle retry succeeds.</li>
 * <li>{@link #ERROR} -&gt; {@link #FAILED} when the per-row attempt counter reaches {@link #MAX_ATTEMPTS}.</li>
 * <li>{@link #FAILED} -&gt; {@link #ERROR} every hour by the slow sweep task (counter reset to 0).</li>
 * </ul>
 *
 * <p>
 * The retry budget and sweep cadence are deliberately hard-coded for v1; see the design doc.
 */
public final class RelayLinkState
{
  /** Last attempt succeeded; relay is routing webhooks for this installation. */
  public static final String OK = "OK";

  /** Never attempted (e.g. App created with the relay feature gate off, or from a flow without a webhook secret). */
  public static final String UNREGISTERED = "UNREGISTERED";

  /** Last attempt failed; retry budget remaining. The polling-cycle pre-flight retries these. */
  public static final String ERROR = "ERROR";

  /**
   * Retry budget exhausted (attempts &gt;= {@link #MAX_ATTEMPTS}); the slow sweep flips this back to {@link #ERROR}
   * hourly.
   */
  public static final String FAILED = "FAILED";

  /** Per-row attempt cap before transitioning to {@link #FAILED}. */
  public static final int MAX_ATTEMPTS = 10;

  private RelayLinkState() {
    // constants holder
  }
}
