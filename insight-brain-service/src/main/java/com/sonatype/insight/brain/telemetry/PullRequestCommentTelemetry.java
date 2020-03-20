/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

/**
 * @since 1.88
 */
public class PullRequestCommentTelemetry
{
  public static final String PULL_REQUEST_COMMENT_TELEMETRY = "pull_request_comment_telemetry";

  public static final String ACTION_CREATED = "created";

  public static final String ACTION_UPDATED = "updated";

  public String id;

  public String action;

  public PullRequestCommentTelemetry() {
    // for deserialization
  }

  public PullRequestCommentTelemetry(String id, String action) {
    this.id = id;
    this.action = action;
  }
}
