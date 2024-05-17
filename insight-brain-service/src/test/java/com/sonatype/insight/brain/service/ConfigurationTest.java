/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Map;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationTest
    extends AbstractComponentTest
{
  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private Configuration configuration;

  @Inject
  private AsyncEventBus asyncEventBus;

  @Test
  public void testConfigurationChanged_OtherProperty() {
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE,
        AsyncEventBus.DEFAULT_MAX_POOL_SIZE + 1);

    configuration.configurationChanged(ImmutableSet.of(SystemConfigurationProperty.BASE_URL));

    assertThat(asyncEventBus.getMaxPoolSize()).isEqualTo(AsyncEventBus.DEFAULT_MAX_POOL_SIZE);
  }

  @Test
  public void testConfigurationChanged() {
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE,
        AsyncEventBus.DEFAULT_MAX_POOL_SIZE + 1);

    configuration.configurationChanged(ImmutableSet.of(SystemConfigurationProperty.EVENT_BUS_MAX_THREAD_POOL_SIZE));

    assertThat(asyncEventBus.getMaxPoolSize()).isEqualTo(AsyncEventBus.DEFAULT_MAX_POOL_SIZE + 1);
  }

  @Test
  public void testGetMatcherConfiguration_DisableConanNamespaceMatching_False() {
    Map<String, String> matcherConfiguration = configuration.getMatcherConfiguration();

    assertThat(matcherConfiguration).containsEntry("disableConanNamespaceMatching", "false");
  }

  @Test
  public void testGetMatcherConfiguration_DisableConanNamespaceMatching_True() {
    configurationService.setConfigurationInDatabaseNoAuthz(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING, true);
    configurationService.applyConfigurationToClients(
        SystemConfigurationProperty.MATCHER_CONFIGURATION_DISABLE_CONAN_NAMESPACE_MATCHING);

    Map<String, String> matcherConfiguration = configuration.getMatcherConfiguration();

    assertThat(matcherConfiguration).containsEntry("disableConanNamespaceMatching", "true");
  }
}
