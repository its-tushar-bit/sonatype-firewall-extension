/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.Injector;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * A Guice-aware JobFactory that uses Guice's Injector to instantiate Quartz Job classes.
 * This replaces the SisuAwareJobFactory after removing Sisu dependency.
 */
@Named
@Singleton
public class GuiceAwareJobFactory
    implements JobFactory
{
  private final Injector injector;

  @Inject
  public GuiceAwareJobFactory(Injector injector) {
    this.injector = injector;
  }

  @Override
  public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
    Class<? extends Job> type = bundle.getJobDetail().getJobClass();

    try {
      // Use Guice to get or create an instance of the Job class
      return injector.getInstance(type);
    }
    catch (Exception e) {
      throw new SchedulerException("Failed to instantiate job class: " + type.getName(), e);
    }
  }
}
