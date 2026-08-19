/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import jakarta.inject.Named;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class QuartzTriggerListener
    extends TriggerListenerSupport
{
  private static final Logger log = LoggerFactory.getLogger(QuartzTriggerListener.class);

  static final String QUARTZ_VETO = "quartz.veto";

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
    if (trigger.getJobDataMap().getBoolean(QUARTZ_VETO)) {
      log.debug("Vetoing {}", trigger.getKey());
      return true;
    }
    return false;
  }
}
