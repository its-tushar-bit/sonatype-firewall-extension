/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.UUID;

import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.concurrent.PerpetualLockManager;

import com.google.common.annotations.VisibleForTesting;

/**
 * At this moment all SCM operations are pinned to a single IQ instance.  In the future the plan is to distribute
 * this work across multiple instances in a clustered environment.
 *
 * As such, this class is pretty simple to start with but will expand based on how the work is to be distributed.
 * - initially we could pin an IQ instance to a particular user/token
 * - eventually we want to leverage the full capabilities of clustered IQ and let any instance pick up any work
 *   that doesn't conflict with the work another instance is doing
 */
@Named
@Singleton
public class SourceControlInstanceManager
{
  private static final String SOURCE_CONTROL_ACCESS_LOCK = "source-control-access-c78943f1";

  private static final long POLLING_LOCK_EXPIRATION_SECONDS = 75;

  private static final long EVENT_PROCESSING_LOCK_EXPIRATION_SECONDS = 25;

  // non-static for testing purposes
  private final String sourceControlInstanceId;

  public SourceControlInstanceManager() {
    sourceControlInstanceId = UUID.randomUUID().toString();
  }

  private final PerpetualLockManager perpetualLockManager = new PerpetualLockManager();

  public boolean canPoll() {
    return perpetualLockManager
        .tryAcquireLock(SOURCE_CONTROL_ACCESS_LOCK, sourceControlInstanceId, POLLING_LOCK_EXPIRATION_SECONDS);
  }

  public boolean canProcessEvents() {
    return perpetualLockManager.tryAcquireLock(SOURCE_CONTROL_ACCESS_LOCK, sourceControlInstanceId,
        EVENT_PROCESSING_LOCK_EXPIRATION_SECONDS);
  }

  @VisibleForTesting
  void releaseInstance() {
    perpetualLockManager.releasePerpetualLock(SOURCE_CONTROL_ACCESS_LOCK, sourceControlInstanceId);
  }

  public String getSourceControlInstanceId() {
    return sourceControlInstanceId;
  }
}
