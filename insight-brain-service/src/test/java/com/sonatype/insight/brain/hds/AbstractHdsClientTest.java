/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.testing.BrainInjectedTest;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
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

  protected static final String USER_AGENT_SUFFIX = "test suffix";

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private Server server;

  protected DefaultHdsClient client;

  protected AbstractHandler handler;

  protected InsightConfig config;

  protected TelemetryId telemetryId;

  @Override
  public void configure(final Binder binder) {
    config = new InsightConfig();
    binder.bind(InsightConfig.class).toInstance(config);
    super.configure(binder);
  }

  @Before
  public void init() throws Exception {
    server = new Server(0);
    server.setHandler(new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        if (handler != null) {
          handler.handle(target, baseRequest, request, response);
        }
      }
    });
    server.start();

    setHdsUrl("http://localhost:" + ((NetworkConnector) server.getConnectors()[0]).getLocalPort());
    setUserAgentSuffix(USER_AGENT_SUFFIX);
    ((HttpConnectorFactory) ((DefaultServerFactory) config.getServerFactory()).getApplicationConnectors().get(0))
        .setPort(1234);
    telemetryId = new TelemetryId(config);
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
    service.setConfigurationNoAuthz(SystemConfigurationProperty.BASE_URL, baseUrl);
    service.applyConfigurationToClients(SystemConfigurationProperty.BASE_URL);
  }

  public void resetBaseUrl() {
    ApiConfigurationService service = lookup(ApiConfigurationService.class);
    Set<String> propertyNames =
        ImmutableSet.of(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL);
    service.deleteConfigurationNoAuthz(propertyNames);
    service.applyConfigurationToClients(propertyNames);
  }

  public void setHdsUrl(String hdsUrl) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.HDS_URL, hdsUrl);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.HDS_URL);
  }

  private void setUserAgentSuffix(String userAgentSuffix) {
    configurationService.setConfigurationNoAuthz(SystemConfigurationProperty.USER_AGENT_SUFFIX, userAgentSuffix);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.USER_AGENT_SUFFIX);
  }
}
