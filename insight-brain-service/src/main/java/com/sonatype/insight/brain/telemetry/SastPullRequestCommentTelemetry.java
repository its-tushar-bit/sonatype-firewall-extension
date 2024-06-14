/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.telemetry;

public class SastPullRequestCommentTelemetry
{
  public static final String SAST_PULL_REQUEST_COMMENT_TELEMETRY = "sast_pull_request_comment_telemetry";

  public static final String ACTION_CREATED = "created";

  public static final String ACTION_UPDATED = "updated";

  public String applicationId;

  public int prNumber;

  public Long commentId;

  public String action;

  public String provider;

  public SastPullRequestCommentTelemetry() {
    // for deserialization
  }

  public SastPullRequestCommentTelemetry(final String applicationId, final int prNumber, final String provider) {
    this.applicationId = applicationId;
    this.prNumber = prNumber;
    this.provider = provider;
  }
}
