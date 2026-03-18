/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.hds.util.TelemetryTestUtils;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.DatabaseConfig;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.testing.BrainInjectedTest;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractHdsClientTest
    extends BrainInjectedTest
{
  @Inject
  protected PasswordHandler passwordHandler;

  @Inject
  private ApiConfigurationService configurationService;

  @Inject
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  protected static final String USER_AGENT_SUFFIX = "test suffix";

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private Server server;

  protected HdsClient client;

  protected HttpServlet handler;

  protected InsightConfig config;

  protected TelemetryId telemetryId;

  @Override
  public final void overrideTestBindings(final Binder binder) {
    config = new InsightConfig();
    config.setDatabase(new DatabaseConfig());
    binder.bind(InsightConfig.class).toInstance(config);
    super.configure(binder);
  }

  @Before
  public void init() throws Exception {
    server = new Server(0);

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    context.addServlet(new ServletHolder(new HttpServlet()
    {
      @Override
      protected void service(
          HttpServletRequest request,
          HttpServletResponse response) throws IOException, ServletException
      {
        if (handler != null) {
          handler.service(request, response);
        }
      }
    }), "/*");
    server.setHandler(context);
    server.start();

    setHdsUrl("http://localhost:" + ((NetworkConnector) server.getConnectors()[0]).getLocalPort());
    setUserAgentSuffix(USER_AGENT_SUFFIX);
    ((HttpConnectorFactory) ((DefaultServerFactory) config.getServerFactory()).getApplicationConnectors().get(0))
        .setPort(1234);

    var mockClusterIdentificationService = TelemetryTestUtils.setupReflectiveMockClusterIdentificationService();
    telemetryId = new TelemetryId(config, systemConfigurationPropertyDAO, mockClusterIdentificationService);
    initClient();
  }

  @After
  public void exit() throws Exception {
    if (server != null) {
      server.stop();
    }
    if (client != null) {
      client.stop();
    }
    resetBaseUrl();
  }

  protected abstract void initClient();

  public void setBaseUrl(String baseUrl) {
    ApiConfigurationService service = lookup(ApiConfigurationService.class);
    service.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.BASE_URL, baseUrl);
    service.applyConfigurationToClients(SystemConfigurationProperty.BASE_URL);
  }

  public void resetBaseUrl() {
    ApiConfigurationService service = lookup(ApiConfigurationService.class);
    Set<String> propertyNames =
        ImmutableSet.of(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL);
    service.deleteConfigurationInDatabaseNoAuthz(propertyNames);
    service.applyConfigurationToClients(propertyNames);
  }

  public void setHdsUrl(String hdsUrl) {
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.HDS_URL, hdsUrl);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.HDS_URL);
  }

  private void setUserAgentSuffix(String userAgentSuffix) {
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.USER_AGENT_SUFFIX,
        userAgentSuffix);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.USER_AGENT_SUFFIX);
  }
}
