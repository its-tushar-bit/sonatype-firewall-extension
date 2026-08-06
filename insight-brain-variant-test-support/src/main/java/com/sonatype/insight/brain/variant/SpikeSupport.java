/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestCLMServer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

/**
 * Small helpers shared by the variant server extensions: a reusable no-op {@link Configurator} (kept
 * as a singleton so the launcher treats the server as reusable) and the post-start base-URL seeding
 * that the {@code ui/links/*} redirects need.
 */
final class SpikeSupport
{
  /**
   * Reusing a single Configurator instance matters: the launcher compares configurators by identity
   * when deciding whether a server can be reused, so a fresh lambda per call would defeat caching.
   */
  static final Configurator REUSABLE_NOOP_CONFIGURATOR = new Configurator()
  {
    @Override
    public void configure(final InsightConfig config) {
      // no custom Dropwizard config needed; the DB comes from TestDatabaseConfiguration via the holder
    }

    @Override
    public boolean isReusable() {
      return true;
    }
  };

  private SpikeSupport() {
  }

  /**
   * Seed a non-forced base URL so {@code BaseUrl} can build redirect targets and still honour the
   * {@code X-Forwarded-Proto} header from incoming requests.
   */
  static void seedBaseUrl(final TestCLMServer server) {
    ApiConfigurationService configurationService = server.getCLMServer().getInstance(ApiConfigurationService.class);
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, "http://localhost");
    properties.put(SystemConfigurationProperty.FORCE_BASE_URL, false);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configurationService.applyConfigurationToClients(Set.of(
        SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL));
  }
}
