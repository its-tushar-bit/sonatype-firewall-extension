/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.api.v2.service.ApiProxyConfigurationServiceV2;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyConfigurationDAO;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.service.ProxyConfig;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.UserAgentUtils;
import com.sonatype.insight.test.PortAllocator;
import com.sonatype.insight.test.SslProperties;

import com.google.common.net.HttpHeaders;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HdsClientProxyTest
{
  static {
    SslProperties.use();
  }

  private Server server;

  private HdsClient client;

  private AbstractHandler handler;

  private InsightConfig config;

  private TelemetryId telemetryId;

  @Before
  public void init() throws Exception {
    int port = PortAllocator.findFreePort(8090);
    server = new Server(port);
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
    ProxyConfig proxyConfig = new ProxyConfig();
    proxyConfig.setHostname("localhost");
    proxyConfig.setPort(port);
    config.setProxyConfig(proxyConfig);
    config.setHdsUrl("https://www.somehost.com/");
    telemetryId = new TelemetryId(config);
    initClient();
  }

  private void initClient() {
    CLMLicenseManager licenseManager = mock(CLMLicenseManager.class);
    when(licenseManager.getLicenseFingerprint()).thenReturn("license-fingerprint");
    ApiProxyConfigurationServiceV2 proxyConfig = new ApiProxyConfigurationServiceV2(new ProxyConfigurationDAO());
    client = new HdsClient(new InsightProxy(config, proxyConfig), licenseManager, config, new VersionService(),
        mock(IdleConnectionReaper.class), telemetryId);
  }

  @After
  public void exit() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void testUserAgentAddedToConnectRequest() throws Exception {

    HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
    when(mockedRequest.getMethod()).thenReturn("GET");
    when(mockedRequest.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList(HttpHeaders.USER_AGENT)));

    final Map<String, String> headers = new HashMap<>();
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
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
}
