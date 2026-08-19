/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.DriverDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StdJDBCDelegateUtils
{
  private static final Logger log = LoggerFactory.getLogger(StdJDBCDelegateUtils.class);

  // Visible for testing
  static final long ORPHANED_MILLIS = 600000;

  private StdJDBCDelegateUtils() {
    throw new UnsupportedOperationException();
  }

  static List<TriggerKey> selectTriggerToAcquire(
      DriverDelegate driverDelegate,
      Connection connection,
      List<TriggerKey> triggerKeys,
      String instanceId)
  {
    try {
      List<TriggerKey> triggersToAcquire = new ArrayList<>();
      for (TriggerKey triggerKey : triggerKeys) {
        Trigger trigger = driverDelegate.selectTrigger(connection, triggerKey);
        if (trigger != null && !isTriggerToBeAcquiredByOtherNode(trigger, instanceId)) {
          triggersToAcquire.add(triggerKey);
        }
      }
      return triggersToAcquire;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isTriggerToBeAcquiredByOtherNode(Trigger trigger, String instanceId) {
    String nodeId = trigger.getJobDataMap().getString(TaskScheduler.QUARTZ_NODE_ID);
    if (nodeId == null || nodeId.equals(instanceId)) {
      return false;
    }
    Date nextFireTime = trigger.getNextFireTime();
    if (nextFireTime != null && System.currentTimeMillis() - nextFireTime.getTime() > ORPHANED_MILLIS) {
      log.debug("Grabbing orphaned non-local trigger {}", trigger.getKey());
      return false;
    }
    log.trace("Ignoring non-local trigger {}", trigger.getKey());
    return true;
  }
}
