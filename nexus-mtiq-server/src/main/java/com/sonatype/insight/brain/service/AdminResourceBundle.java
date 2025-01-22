/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.health.MultiTenantHealthFactory;

import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.servlet.ServletContainer;

public class AdminResourceBundle
    implements ConfiguredBundle<InsightConfig>
{
  private final String basePath;

  private JerseyEnvironment jerseyAdminEnvironment;

  public AdminResourceBundle(String basePath) {
    this.basePath = basePath;
  }

  @Override
  public void run(InsightConfig configuration, Environment environment) {
    this.jerseyAdminEnvironment = this.setupAdminEnvironment(environment);
    this.jerseyAdminEnvironment.register(MultiPartFeature.class);

    setupHealthChecks(configuration, environment);
  }

  /**
   * Override DropWizard health check setup. The default in {@link io.dropwizard.core.cli.EnvironmentCommand} passes in
   * a {@link io.dropwizard.jetty.setup.ServletEnvironment} which registers it under port 8070. We want to register it
   * on the admin port so need to use {@link io.dropwizard.core.setup.AdminEnvironment} instead.
   */
  private void setupHealthChecks(final InsightConfig configuration, final Environment environment) {
    MultiTenantInsightConfig multiTenantInsightConfig = (MultiTenantInsightConfig) configuration;
    MultiTenantHealthFactory multiTenantHealthFactory = multiTenantInsightConfig.getMultiTenantHealthFactory();
    if (multiTenantHealthFactory == null) {
      throw new IllegalStateException("Missing MTIQ health checks. See https://sonatype.atlassian.net/wiki/x/iQCONw.");
    }
    multiTenantHealthFactory.configure(environment.lifecycle(), environment.admin(), environment.jersey(),
        environment.health(), environment.getObjectMapper(), "mtiq-health");
  }

  private JerseyEnvironment setupAdminEnvironment(final Environment environment) {
    final DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(
        environment.metrics());
    final JerseyContainerHolder servletContainerHolder = new JerseyContainerHolder(
        new ServletContainer(jerseyConfig));
    final JerseyEnvironment jerseyEnvironment = new JerseyEnvironment(servletContainerHolder,
        jerseyConfig);

    environment.admin()
        .addServlet("api", servletContainerHolder.getContainer())
        .addMapping(this.basePath);

    return jerseyEnvironment;
  }

  public JerseyEnvironment jersey() {
    return this.jerseyAdminEnvironment;
  }
}
