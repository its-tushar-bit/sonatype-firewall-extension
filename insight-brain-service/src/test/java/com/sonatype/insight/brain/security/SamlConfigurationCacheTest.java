/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamlConfigurationCacheTest
{
  private final SamlConfigurationService samlConfigurationService = mock(SamlConfigurationService.class);

  private final TaskScheduler taskScheduler = mock(TaskScheduler.class);

  private final SamlConfigurationCache cache = new SamlConfigurationCache(samlConfigurationService, taskScheduler);

  @Test
  public void refreshCachesCurrentConfiguration() {
    SamlConfiguration configuration = new SamlConfiguration();
    when(samlConfigurationService.get()).thenReturn(configuration);

    cache.refresh();

    assertThat(cache.get()).isSameAs(configuration);
  }

  @Test
  public void refreshClearsCacheWhenSamlNotConfigured() {
    when(samlConfigurationService.get()).thenReturn(new SamlConfiguration());
    cache.refresh();

    when(samlConfigurationService.get()).thenReturn(null);
    cache.refresh();

    assertThat(cache.get()).isNull();
  }

  @Test
  public void registerDoesNotThrowOnInvalidConfiguration() {
    when(samlConfigurationService.get()).thenThrow(new RuntimeException("invalid SAML configuration"));

    // A bad persisted configuration must not crash startup/tenant provisioning; it is logged and left uncached.
    cache.register();

    assertThat(cache.get()).isNull();
  }

  @Test
  public void deregisterEvictsCachedConfiguration() {
    when(samlConfigurationService.get()).thenReturn(new SamlConfiguration());
    cache.refresh();
    assertThat(cache.get()).isNotNull();

    cache.deregister();

    assertThat(cache.get()).isNull();
  }
}
