/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ClusterLock;

import com.google.common.annotations.VisibleForTesting;

/**
 * At this moment all SCM operations are pinned to a single IQ instance.  In the future the plan is to distribute
 * this work across multiple instances in a clustered environment.
 *
 * As such, this class is pretty simple to start with but will expand based on how the work is to be distributed.
 *   - initially we could pin an IQ instance to a particular user/token
 *   - eventually we want to leverage the full capabilities of clustered IQ and let any instance pick up any work
 *     that doesn't conflict with the work another instance is doing
 */
@Named
@Singleton
public class SourceControlInstanceManager
{
  private static final String SOURCE_CONTROL_ACCESS_LOCK = "source-control-access";

  private final ClusterLock sourceControlAccessLock = new ClusterLock(SOURCE_CONTROL_ACCESS_LOCK);

  public boolean canPoll() {
    return sourceControlAccessLock.tryLock();
  }

  public boolean canProcessEvents() {
    return sourceControlAccessLock.tryLock();
  }

  @VisibleForTesting
  void releaseInstance() {
    sourceControlAccessLock.unlock();
  }
}
