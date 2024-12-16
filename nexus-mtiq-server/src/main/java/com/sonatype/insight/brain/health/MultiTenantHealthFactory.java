/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.health;

import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.health.DefaultHealthFactory;
import io.dropwizard.health.HealthEnvironment;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.AdminEnvironment;

/**
 * MTIQ uses the 'new style' health checks of DropWizard whereas on-prem IQ still uses the 'old style'.
 * <p>
 * These are not terms that DropWizard uses. The old style are <a
 * href="https://www.dropwizard.io/en/release-2.1.x/manual/core.html#health-checks">documented here</a> and the new
 * style are <a href="https://www.dropwizard.io/en/release-2.1.x/manual/core.html#health">documented here</a>.
 * The new style is more friendly to Kubernetes with its liveness and readiness probes. It also runs on a schedule
 * behind the scenes and not on demand. So full control retains with the config itself.
 * <p>
 * MTIQ disables the 'old style' by returning {@link java.util.Optional#empty()} in
 * {@link MultiTenantInsightConfig#getHealthFactory()} and then defines a custom config entry `mtiq-health` that follows
 * the new style. This is enabled with a DropWizard <a
 * href="https://www.dropwizard.io/en/stable/manual/configuration.html#polymorphic-configuration">polymorphic
 * configuration</a> (see META-INF/services).
 */
@JsonTypeName("mtiq-health")
public class MultiTenantHealthFactory
    extends DefaultHealthFactory
{
  @Override
  public void configure(
      final LifecycleEnvironment lifecycle,
      final ServletEnvironment servlets,
      final JerseyEnvironment jersey,
      final HealthEnvironment health,
      final ObjectMapper mapper,
      final String name)
  {
    if (servlets instanceof AdminEnvironment) {
      super.configure(lifecycle, servlets, jersey, health, mapper, name);
    }
    else {
      // In MultiTenantInsightConfig, we return Optional.empty() for healthFactory, so the default health checks are
      // never registered. Then in AdminResourceBundle we manually invoke this method to register against the admin
      // endpoint on port 8071. So this should never be hit.
      throw new IllegalStateException("MTIQ health checks are only supported in the admin servlet environment");
    }
  }
}
