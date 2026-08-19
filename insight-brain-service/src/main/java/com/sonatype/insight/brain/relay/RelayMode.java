/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import com.sonatype.insight.brain.model.relay.RelayConfiguration;

import org.apache.commons.lang3.StringUtils;

/**
 * Tags a relay registration as either PAT-based or GitHub App-based. Used to discriminate
 * the secondary dedup tuple in {@code relay_event_log} so events from a different mode are
 * not over-matched after a customer migrates between modes.
 *
 * <p>
 * Stored as a plain {@code varchar(16)} string ("pat" or "github-app") rather than a JPA
 * enum so the DAO/jOOQ path stays trivial. {@link #fromConfiguration(RelayConfiguration)}
 * derives the mode from the locally persisted configuration row: PAT mode iff
 * {@code webhook_url} is populated (the relay returns a customer-facing URL only on PAT
 * registration), GitHub App mode iff blank.
 */
public final class RelayMode
{
  /** PAT-based registration. The relay returned a per-customer webhook URL. */
  public static final String MODE_PAT = "pat";

  /** GitHub App-based registration. Routed by installation id; no per-customer webhook URL. */
  public static final String MODE_GITHUB_APP = "github-app";

  private RelayMode() {
    // constants holder
  }

  /**
   * Derives the relay mode from a persisted {@link RelayConfiguration} row. Returns
   * {@code null} when the configuration is null so callers can decide whether to skip
   * the dedup write or default — this method does not invent a mode for missing data.
   */
  public static String fromConfiguration(RelayConfiguration configuration) {
    if (configuration == null) {
      return null;
    }
    return StringUtils.isBlank(configuration.getWebhookUrl()) ? MODE_GITHUB_APP : MODE_PAT;
  }
}
