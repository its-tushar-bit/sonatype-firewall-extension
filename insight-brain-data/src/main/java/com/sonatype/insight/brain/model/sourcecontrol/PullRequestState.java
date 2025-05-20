/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

// Note that this is separate from com.sonatype.nexus.scm.api.model.PullRequestState
// to ensure that the db does not directly depend on an enum that may evolve separately
public enum PullRequestState
{
  CLOSED,
  LOCKED,
  MERGED,
  OPEN,
  MISSING; // for when a previously-seen PR can no longer be found in SCM

  public static PullRequestState fromSCMState(
      final com.sonatype.nexus.scm.api.model.PullRequestState pullRequestState)
  {
    if (pullRequestState == null) {
      return null;
    }
    return switch (pullRequestState) {
      case CLOSED -> CLOSED;
      case LOCKED -> LOCKED;
      case OPEN -> OPEN;
      case MERGED -> MERGED;
      default -> null;
    };
  }
}
