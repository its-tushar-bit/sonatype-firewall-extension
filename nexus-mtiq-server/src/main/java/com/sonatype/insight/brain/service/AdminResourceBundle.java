/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.servlet.ServletContainer;

public class AdminResourceBundle
    implements ConfiguredBundle<Configuration>
{
  private final String basePath;

  private JerseyEnvironment jerseyAdminEnvironment;

  public AdminResourceBundle(String basePath) {
    this.basePath = basePath;
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    this.jerseyAdminEnvironment = this.setupAdminEnvironment(environment);
    this.jerseyAdminEnvironment.register(MultiPartFeature.class);
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
