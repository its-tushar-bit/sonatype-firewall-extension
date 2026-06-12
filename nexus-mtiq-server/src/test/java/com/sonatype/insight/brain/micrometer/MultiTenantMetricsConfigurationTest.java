/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.micrometer;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.common.metering.MeteredThreadPoolExecutor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MultiTenantMetricsConfigurationTest
{
  @After
  public void resetStaticMeterRegistry() {
    MeteredThreadPoolExecutor.injectMeterRegistry(null);
  }

  @Test
  public void shouldExposeProviderMeterRegistryWhenMtiqEnabled() {
    MeterRegistry providedRegistry = new SimpleMeterRegistry();
    MultiTenantMeterRegistryProvider provider = mock(MultiTenantMeterRegistryProvider.class);
    when(provider.get()).thenReturn(providedRegistry);

    try (AnnotationConfigApplicationContext context = newMtiqContext(provider)) {
      assertThat(context.getBean(MeterRegistry.class)).isSameAs(providedRegistry);
    }
  }

  @Test
  public void shouldInjectMeterRegistryIntoMeteredThreadPoolExecutor() {
    MeterRegistry providedRegistry = new SimpleMeterRegistry();
    MultiTenantMeterRegistryProvider provider = mock(MultiTenantMeterRegistryProvider.class);
    when(provider.get()).thenReturn(providedRegistry);

    try (AnnotationConfigApplicationContext context = newMtiqContext(provider)) {
      MeteredThreadPoolExecutor executor = new MeteredThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy(),
          Tags.of("kind", "test", "name", "test"));
      try {
        assertThat(providedRegistry.find("executor.active").tags("kind", "test", "name", "test").gauge())
            .isNotNull();
      }
      finally {
        executor.shutdownNow();
      }
    }
  }

  // When the provider has no exporter (null), the config still supplies an in-memory SimpleMeterRegistry
  // and injects it into the static field, so an implicit-constructor executor registers into it.
  @Test
  public void shouldFallBackToSimpleMeterRegistryWhenProviderReturnsNull() {
    MultiTenantMeterRegistryProvider provider = mock(MultiTenantMeterRegistryProvider.class);
    when(provider.get()).thenReturn(null);

    try (AnnotationConfigApplicationContext context = newMtiqContext(provider)) {
      assertThat(context.getBeansOfType(MeterRegistry.class).values())
          .singleElement()
          .isInstanceOf(SimpleMeterRegistry.class);

      MeterRegistry fallbackRegistry = context.getBean(MeterRegistry.class);
      MeteredThreadPoolExecutor executor = new MeteredThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy(),
          Tags.of("kind", "test", "name", "test"));
      try {
        assertThat(fallbackRegistry.find("executor.active").tags("kind", "test", "name", "test").gauge())
            .isNotNull();
      }
      finally {
        executor.shutdownNow();
      }
    }
  }

  @Test
  public void shouldClearStaticMeterRegistryOnContextClose() {
    MeterRegistry providedRegistry = new SimpleMeterRegistry();
    MultiTenantMeterRegistryProvider provider = mock(MultiTenantMeterRegistryProvider.class);
    when(provider.get()).thenReturn(providedRegistry);

    AnnotationConfigApplicationContext context = newMtiqContext(provider);
    context.close();

    // @PreDestroy ran injectMeterRegistry(null); an implicit-constructor executor created afterwards
    // therefore registers nothing in the previously-provided registry.
    MeteredThreadPoolExecutor executor = new MeteredThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy(),
        Tags.of("kind", "test", "name", "test"));
    try {
      assertThat(providedRegistry.find("executor.active").tags("kind", "test", "name", "test").gauge()).isNull();
    }
    finally {
      executor.shutdownNow();
    }
  }

  // Exercises the real Boot metrics auto-configuration (absent from the bare-context tests above):
  // with no exporter the context must still start with exactly one MeterRegistry (our fallback
  // SimpleMeterRegistry), so the Actuator metrics endpoint - which requires a MeterRegistry - resolves
  // instead of failing context startup, and Boot's own fallback does not also register a duplicate.
  @Test
  public void shouldStartWithSingleSimpleMeterRegistryWhenProviderReturnsNull() {
    MultiTenantMeterRegistryProvider provider = mock(MultiTenantMeterRegistryProvider.class);
    when(provider.get()).thenReturn(null);

    new ApplicationContextRunner()
        .withPropertyValues("sonatype.mtiq.enabled=true")
        .withBean(MultiTenantMeterRegistryProvider.class, () -> provider)
        .withConfiguration(AutoConfigurations.of(
            MetricsAutoConfiguration.class,
            CompositeMeterRegistryAutoConfiguration.class,
            SimpleMetricsExportAutoConfiguration.class))
        .withUserConfiguration(MultiTenantMetricsConfiguration.class)
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(MeterRegistry.class);
          assertThat(context.getBean(MeterRegistry.class)).isInstanceOf(SimpleMeterRegistry.class);
        });
  }

  @Test
  public void shouldNotActivateForSingleTenant() {
    MultiTenantMeterRegistryProvider provider = mock(MultiTenantMeterRegistryProvider.class);

    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.registerBean(MultiTenantMeterRegistryProvider.class, () -> provider);
      context.register(MultiTenantMetricsConfiguration.class);
      context.refresh();

      assertThat(context.getBeansOfType(MeterRegistry.class)).isEmpty();
    }
  }

  private static AnnotationConfigApplicationContext newMtiqContext(final MultiTenantMeterRegistryProvider provider) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getEnvironment()
        .getPropertySources()
        .addFirst(
            new MapPropertySource("test", Map.of("sonatype.mtiq.enabled", "true")));
    context.registerBean(MultiTenantMeterRegistryProvider.class, () -> provider);
    context.register(MultiTenantMetricsConfiguration.class);
    context.refresh();
    return context;
  }
}
