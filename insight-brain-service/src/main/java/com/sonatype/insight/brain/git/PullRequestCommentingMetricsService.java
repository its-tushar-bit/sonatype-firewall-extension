/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class PullRequestCommentingMetricsService
{
  public void recordEvent(boolean commentCreated) {
    // todo - INT-2490 will implement this
  }
}
