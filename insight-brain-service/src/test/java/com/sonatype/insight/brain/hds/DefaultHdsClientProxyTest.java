/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.utils.AbstractHttpClientTest;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.UserAgentUtils;

import com.google.common.net.HttpHeaders;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultHdsClientProxyTest
    extends AbstractHttpClientTest
{
  @Inject
  private PasswordHandler passwordHandler;

  private Server server;

  private HdsClient client;

  private AbstractHandler handler;

  @Inject
  private TelemetryId telemetryId;

  @Inject
  private Configuration configuration;

  @Inject
  private ApiProxyServerConfigurationService proxyServerConfigurationService;

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

    tempEntity.setProxyServerConfiguration("localhost", ((NetworkConnector) server.getConnectors()[0]).getLocalPort());
    proxyServerConfigurationService.applyProxyServerConfigurationToClients();

    setHdsUrl("https://www.somehost.com/");
    initClient();
  }

  private void initClient() {
    ProductLicense productLicense = mock(ProductLicense.class);
    lenient().when(productLicense.getFingerprint()).thenReturn("license-fingerprint");
    client = new DefaultHdsClient(new InsightProxy(configuration, passwordHandler), productLicense, configuration,
        new VersionService(), telemetryId, 20, name -> new Retry(name, 0, null, e -> false, i -> Duration.ZERO));
  }

  @After
  public void exit() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Override
  protected void pingUrl(String url) {
    setHdsUrl(url);
    initClient();
    client.get(String.class, "test/path");
  }

  @Test
  public void testUserAgentAddedToConnectRequest() {

    HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
    when(mockedRequest.getMethod()).thenReturn("GET");
    when(mockedRequest.getHeaderNames()).thenReturn(Collections.enumeration(
        Collections.singletonList(HttpHeaders.USER_AGENT)));

    final Map<String, String> headers = new HashMap<>();
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if ("CONNECT".equals(request.getMethod())) {
          headers.clear();
          for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements();) {
            String headerName = en.nextElement();
            headers.put(headerName, request.getHeader(headerName));
          }
        }
        baseRequest.setHandled(true);
      }
    };

    try {
      client.relay(mockedRequest, null, "some/path", Collections.emptyMap());
    }
    catch (Exception ignore) {
      // noop
    }

    assertThat(headers).containsEntry(HttpHeaders.USER_AGENT, UserAgentUtils.getDefaultUserAgent());
  }

  @Test
  public void testProxyServerConfigurationChanged() {
    new ProxyServerConfigurationDAO().delete();
    setHdsUrl("http://proxy.test/");
    tempEntity.setProxyServerConfiguration("localhost", ((NetworkConnector) server.getConnectors()[0]).getLocalPort());
    configuration.proxyServerConfigurationChanged();
    initClient();

    String proxyResponse = "PROXY-TEST-PASSED";
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().print(proxyResponse);
        baseRequest.setHandled(true);
      }
    };

    assertThat(client.get(String.class, "/rest/test")).isEqualTo(proxyResponse);
  }
}
