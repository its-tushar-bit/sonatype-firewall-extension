/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.sisu.BeanEntry;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

@Named
@Singleton
public class SisuAwareJobFactory
    implements JobFactory
{
  private final Iterable<BeanEntry<Named, Job>> entries;

  @Inject
  public SisuAwareJobFactory(Iterable<BeanEntry<Named, Job>> entries) {
    this.entries = entries;
  }

  @Override
  public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
    Class<? extends Job> type = bundle.getJobDetail().getJobClass();

    BeanEntry<Named, Job> beanEntry = locate(type);
    if (beanEntry == null) {
      throw new SchedulerException("Missing job component for type: " + type.getName());
    }

    return beanEntry.getProvider().get();
  }

  private BeanEntry<Named, Job> locate(Class<? extends Job> jobType) {
    for (BeanEntry<Named, Job> jobEntry : entries) {
      if (jobEntry.getImplementationClass().equals(jobType)) {
        return jobEntry;
      }
    }
    return null;
  }
}
