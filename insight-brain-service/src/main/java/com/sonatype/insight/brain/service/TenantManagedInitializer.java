/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.lifecycle.Managed;
import com.sonatype.insight.brain.scheduler.TaskScheduler;

/**
 * Responsible for setting up (on startup) and tearing down (on shutdown) tenant managed beans.
 */
public interface TenantManagedInitializer
    extends Managed
{
  public static final int PRIORITY = TaskScheduler.TASK_SCHEDULER_BEAN_PRIORITY - 1;
}
