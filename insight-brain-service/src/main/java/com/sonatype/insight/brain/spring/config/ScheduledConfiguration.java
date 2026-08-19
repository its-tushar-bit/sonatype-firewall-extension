/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.QuartzConcurrencyListener;
import com.sonatype.insight.brain.scheduler.QuartzJobSchedulingService;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.QuartzTriggerListener;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import jakarta.inject.Named;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * Quartz scheduler configuration.
 * Enables dependency injection in Quartz jobs.
 */
@Configuration
public class ScheduledConfiguration
{

  @Bean
  @Named("jobFactory")
  @Primary
  public SpringBeanJobFactory springBeanJobFactory(ApplicationContext applicationContext) {
    AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
    jobFactory.setApplicationContext(applicationContext);
    return jobFactory;
  }

  @Bean
  public QuartzJobStoreTX quartzJobStoreTX(
      ProductLicense productLicense,
      InsightConfig insightConfig,
      OperationalDataStore operationalDataStore) throws Exception
  {
    return new QuartzJobStoreTX(productLicense, insightConfig, operationalDataStore);
  }

  @Bean
  public QuartzTriggerListener quartzTriggerListener() {
    return new QuartzTriggerListener();
  }

  @Bean
  public QuartzConcurrencyListener quartzConcurrencyListener(QuartzJobStoreTX quartzJobStoreTX) {
    return new QuartzConcurrencyListener(quartzJobStoreTX);
  }

  @Bean
  public QuartzJobSchedulingService quartzJobSchedulingService() {
    return new QuartzJobSchedulingService();
  }

  @Bean
  public TaskScheduler taskScheduler(
      QuartzJobStoreTX quartzJobStoreTX,
      SpringBeanJobFactory springBeanJobFactory,
      @Value("${scheduler.name:" + TaskScheduler.DEFAULT_SCHEDULER_NAME + "}") String schedulerName,
      QuartzTriggerListener quartzTriggerListener,
      QuartzConcurrencyListener quartzConcurrencyListener,
      ShutdownHandler shutdownHandler,
      QuartzJobSchedulingService quartzJobSchedulingService)
  {
    return new TaskScheduler(
        quartzJobStoreTX,
        springBeanJobFactory,
        schedulerName,
        quartzTriggerListener,
        quartzConcurrencyListener,
        shutdownHandler,
        quartzJobSchedulingService);
  }

  /**
   * JobFactory that autowires Quartz job instances.
   * Tries to get the job from Spring's bean factory first, falling back to
   * creating a new instance and autowiring it.
   */
  public static class AutowiringSpringBeanJobFactory
      extends SpringBeanJobFactory
      implements ApplicationContextAware
  {

    private transient ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
      Class<?> jobClass = bundle.getJobDetail().getJobClass();

      try {
        return applicationContext.getBean(jobClass);
      }
      catch (NoSuchBeanDefinitionException e) {
        // Fall through to bean-name lookup.
      }

      try {
        return applicationContext.getBean(getBeanName(jobClass));
      }
      catch (NoSuchBeanDefinitionException e) {
        Object job = super.createJobInstance(bundle);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(job);
        return job;
      }
    }

    private String getBeanName(Class<?> jobClass) {
      String beanName = jobClass.getSimpleName();
      return Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
    }
  }
}
