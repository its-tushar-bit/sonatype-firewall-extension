/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

/**
 * This enum is used to define the order in which components should be shut down.
 * <br />
 * <br />
 * An item using an enum value earlier in the list (with a lower ordinal) will be shutdown before an item using an enum
 * value later in the list.
 */
public enum ShutdownPriority
{
  ACTIVE_REQUESTS,
  QUARTZ_SCHEDULERS,
  DEFAULT,
  SOURCE_CONTROL_EVENT_PROCESSOR,
  POLICY_EVALUATIONS,
  NOTIFICATIONS,
  ASYNC_EVENT_BUS,
  HEALTH_CHECK_SCHEDULER,
  TELEMETRY,
}
