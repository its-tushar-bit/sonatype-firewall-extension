/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.service.DefaultApplicationLifecycle;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TelemetryStartupOrderingTest
{
  @Test
  public void onlyTelemetrySchedulerShouldDependOnApplicationLifecycle() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(DefaultApplicationLifecycle.class, TelemetrySender.class, DefaultTelemetryScheduler.class);

      assertThat(context.getBeanFactory().getBeanDefinition("telemetrySender").getDependsOn())
          .isNull();
      assertThat(context.getBeanFactory().getBeanDefinition("defaultTelemetryScheduler").getDependsOn())
          .contains("defaultApplicationLifecycle");
    }
  }
}
