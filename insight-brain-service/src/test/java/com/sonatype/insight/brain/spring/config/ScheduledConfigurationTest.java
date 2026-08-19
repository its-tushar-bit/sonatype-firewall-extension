/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

public class ScheduledConfigurationTest
{
  @Test
  public void shouldReturnSpringManagedJobWhenFoundByNameAfterTypeLookupMiss() throws Exception {
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
    ManagedJob managedJob = new ManagedJob();
    ExposedAutowiringSpringBeanJobFactory jobFactory = new ExposedAutowiringSpringBeanJobFactory();
    jobFactory.setApplicationContext(applicationContext);

    when(applicationContext.getBean(ManagedJob.class)).thenThrow(new NoSuchBeanDefinitionException(ManagedJob.class));
    when(applicationContext.getBean("managedJob")).thenReturn(managedJob);
    when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(beanFactory);

    Object createdJob = jobFactory.create(createBundle(ManagedJob.class));

    assertThat(createdJob).isSameAs(managedJob);
    verify(beanFactory, never()).autowireBean(any());
  }

  @Test
  public void shouldAutowireNewJobWhenNoSpringBeanExists() throws Exception {
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
    ExposedAutowiringSpringBeanJobFactory jobFactory = new ExposedAutowiringSpringBeanJobFactory();
    jobFactory.setApplicationContext(applicationContext);

    when(applicationContext.getBean(UnmanagedJob.class))
        .thenThrow(new NoSuchBeanDefinitionException(UnmanagedJob.class));
    when(applicationContext.getBean("unmanagedJob")).thenThrow(new NoSuchBeanDefinitionException("unmanagedJob"));
    when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(beanFactory);

    Object createdJob = jobFactory.create(createBundle(UnmanagedJob.class));

    assertThat(createdJob).isInstanceOf(UnmanagedJob.class);
    verify(beanFactory).autowireBean(createdJob);
  }

  @Test
  public void shouldPropagateBeanCreationFailureInsteadOfFallingBack() throws Exception {
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
    ExposedAutowiringSpringBeanJobFactory jobFactory = new ExposedAutowiringSpringBeanJobFactory();
    jobFactory.setApplicationContext(applicationContext);

    when(applicationContext.getBean(FailingNamedJob.class))
        .thenThrow(new NoSuchBeanDefinitionException(FailingNamedJob.class));
    when(applicationContext.getBean("failingNamedJob"))
        .thenThrow(new BeanCreationException("failingNamedJob", "boom"));
    when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(beanFactory);

    assertThatThrownBy(() -> jobFactory.create(createBundle(FailingNamedJob.class)))
        .isInstanceOf(BeanCreationException.class)
        .hasMessageContaining("failingNamedJob");
    verify(beanFactory, never()).autowireBean(any());
  }

  private TriggerFiredBundle createBundle(Class<? extends Job> jobClass) {
    JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobClass.getSimpleName(), "test").build();
    return new TriggerFiredBundle(jobDetail, new SimpleTriggerImpl(), null, false,
        new Date(), new Date(), new Date(), new Date());
  }

  private static class ExposedAutowiringSpringBeanJobFactory
      extends ScheduledConfiguration.AutowiringSpringBeanJobFactory
  {
    Object create(TriggerFiredBundle bundle) throws Exception {
      return super.createJobInstance(bundle);
    }
  }

  public static class ManagedJob
      implements Job
  {
    @Override
    public void execute(JobExecutionContext context) {
      // noop
    }
  }

  public static class UnmanagedJob
      implements Job
  {
    @Override
    public void execute(JobExecutionContext context) {
      // noop
    }
  }

  public static class FailingNamedJob
      implements Job
  {
    @Override
    public void execute(JobExecutionContext context) {
      // noop
    }
  }
}
