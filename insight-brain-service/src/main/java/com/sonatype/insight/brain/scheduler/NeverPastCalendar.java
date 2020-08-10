/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import org.quartz.impl.calendar.BaseCalendar;

/**
 * Custom Quartz calender that excludes any timestamp of the past. Especially for tasks with a small period (in
 * particular smaller than the misfire threshold) that a single task execution could potentially exceed, this helps to
 * avoid rapid task firing due to the task's next fire time trying to catch up with the present.
 */
class NeverPastCalendar
    extends BaseCalendar
{
  private static final long serialVersionUID = 2840252013101843910L;

  static final String CALENDAR_NAME = "never-past";

  private long getStartOfPresent() {
    // NOTE: We need to have some leeway to ensure newly scheduled tasks don't get their start time excluded just
    // because OperableTrigger.computeFirstFireTime() got called a few CPU cycles too late, delaying the first execution
    // by an entire task period.
    return System.currentTimeMillis() - 1500;
  }

  @Override
  public boolean isTimeIncluded(long timeStamp) {
    if (timeStamp < getStartOfPresent()) {
      return false;
    }
    return super.isTimeIncluded(timeStamp);
  }

  @Override
  public long getNextIncludedTime(long timeStamp) {
    return Math.max(super.getNextIncludedTime(timeStamp), getStartOfPresent());
  }
}
