/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.test.InjectedTest;

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
    extends InjectedTest
{
  @Inject
  protected PasswordHandler passwordHandler;

  private static final String USER_AGENT_SUFFIX = "test suffix";

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private Server server;

  protected HdsClient client;

  protected AbstractHandler handler;

  protected InsightConfig config;

  protected TelemetryId telemetryId;

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

    config = new InsightConfig();
    config.setHdsUrl("http://localhost:" + ((NetworkConnector) server.getConnectors()[0]).getLocalPort());
    config.setUserAgentSuffix(USER_AGENT_SUFFIX);
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
  }

  protected abstract void initClient();
}
