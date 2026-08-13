/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import java.util.Map;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsConfigurationTest
{
  @Test
  public void shouldProvideSimpleMeterRegistryForSingleTenant() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.register(MetricsConfiguration.class);
      context.refresh();

      assertThat(context.getBeansOfType(MeterRegistry.class).values())
          .singleElement()
          .isInstanceOf(SimpleMeterRegistry.class);
    }
  }

  @Test
  public void shouldNotProvideMeterRegistryWhenMtiqEnabled() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.getEnvironment()
          .getPropertySources()
          .addFirst(
              new MapPropertySource("test", Map.of("sonatype.mtiq.enabled", "true")));
      context.register(MetricsConfiguration.class);
      context.refresh();

      assertThat(context.getBeansOfType(MeterRegistry.class)).isEmpty();
    }
  }

  // Exercises the real Boot metrics auto-configuration (absent from the bare-context test above) to prove
  // that declaring this explicit bean makes Boot's SimpleMetricsExportAutoConfiguration fallback back off
  // via @ConditionalOnMissingBean: exactly one MeterRegistry remains - our SimpleMeterRegistry - so
  // /actuator/metrics stays backed by it and the two never collide.
  @Test
  public void shouldProvideSingleSimpleMeterRegistryWithBootMetricsAutoConfig() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            MetricsAutoConfiguration.class,
            CompositeMeterRegistryAutoConfiguration.class,
            SimpleMetricsExportAutoConfiguration.class))
        .withUserConfiguration(MetricsConfiguration.class)
        .run(context -> assertThat(context.getBeansOfType(MeterRegistry.class).values())
            .singleElement()
            .isInstanceOf(SimpleMeterRegistry.class));
  }

}
