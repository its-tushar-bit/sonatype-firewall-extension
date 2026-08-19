/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.spring.config.ScheduledConfiguration;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
public class MtiqScheduledConfiguration
{
  @Bean
  @Singleton
  @Named("jobFactory")
  @Primary
  public SpringBeanJobFactory springBeanJobFactory(ApplicationContext applicationContext) {
    ScheduledConfiguration.AutowiringSpringBeanJobFactory jobFactory =
        new ScheduledConfiguration.AutowiringSpringBeanJobFactory();
    jobFactory.setApplicationContext(applicationContext);
    return jobFactory;
  }
}
