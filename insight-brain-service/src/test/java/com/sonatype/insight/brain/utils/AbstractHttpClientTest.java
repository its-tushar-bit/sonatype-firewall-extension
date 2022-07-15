/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadGatewayException;

import org.apache.http.client.HttpResponseException;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractHttpClientTest
    extends AbstractComponentTest
{
  protected abstract void pingUrl(String url) throws Exception;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private ApiProxyServerConfigurationService proxyServerConfigurationService;

  @Test
  public void testProxyUsage() throws Exception {
    Server proxyServer = new Server(0);
    AtomicBoolean proxyServerUsed = new AtomicBoolean();
    AtomicBoolean proxyAuthenticationProvided = new AtomicBoolean();
    proxyServer.setHandler(new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        proxyServerUsed.set(true);
        String proxyAuth = request.getHeader("Proxy-Authorization");
        if ("Basic dGVzdC1wcm94eS11c2VyOnRlc3QtcHJveHktcGFzcw==".equals(proxyAuth)) {
          proxyAuthenticationProvided.set(true);
          response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
        }
        else {
          response.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
          response.setHeader("Proxy-Authenticate", "Basic realm=\"ProxyTestRealm\"");
        }
        baseRequest.setHandled(true);
      }
    });

    proxyServer.start();
    try {
      tempEntity.setProxyServerConfiguration("localhost",
          ((NetworkConnector) proxyServer.getConnectors()[0]).getLocalPort(), "test-proxy-user",
          passwordHandler.encryptPassword("test-proxy-pass".toCharArray()));
      proxyServerConfigurationService.applyProxyServerConfigurationToClients();

      pingUrl("http://proxy.test/");
    }
    catch (HttpResponseException | BadGatewayException ignored) {
      // given a generic HTTP 501 response from the proxy, client failure isn't unusual (but not required)
    }
    finally {
      proxyServer.stop();
    }

    assertThat(proxyServerUsed).isTrue();
    assertThat(proxyAuthenticationProvided).isTrue();
  }

  @Test
  public void testProxyExclusion() throws Exception {
    Server targetServer = new Server(0);
    AtomicBoolean proxyServerBypassed = new AtomicBoolean();
    targetServer.setHandler(new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        proxyServerBypassed.set(true);
        response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
        baseRequest.setHandled(true);
      }
    });

    targetServer.start();
    try {
      tempEntity.setProxyServerConfiguration("proxy.test", 80, null, null, "localhost");
      proxyServerConfigurationService.applyProxyServerConfigurationToClients();
      pingUrl("http://localhost:" + ((NetworkConnector) targetServer.getConnectors()[0]).getLocalPort() + "/");
    }
    catch (HttpResponseException | BadGatewayException ignored) {
      // given a generic HTTP 501 response from the server, client failure isn't unusual (but not required)
    }
    finally {
      targetServer.stop();
    }

    assertThat(proxyServerBypassed).isTrue();
  }
}
