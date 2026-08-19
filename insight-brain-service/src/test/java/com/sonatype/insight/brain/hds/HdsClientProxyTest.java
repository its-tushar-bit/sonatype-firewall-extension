/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.utils.AbstractHttpClientTest;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.brain.version.DefaultVersionService;
import com.sonatype.insight.client.utils.UserAgentUtils;

import com.google.common.net.HttpHeaders;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
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
public class HdsClientProxyTest
    extends AbstractHttpClientTest
{
  @Inject
  private PasswordHandler passwordHandler;

  private Server server;

  private HdsClient client;

  private HttpServlet handler;

  @Inject
  private TelemetryId telemetryId;

  @Inject
  private Configuration configuration;

  @Inject
  private ProxyServerConfigurationDAO proxyServerConfigurationDAO;

  @Inject
  private ApiProxyServerConfigurationService proxyServerConfigurationService;

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

    tempEntity.setProxyServerConfiguration("localhost", ((NetworkConnector) server.getConnectors()[0]).getLocalPort());
    proxyServerConfigurationService.applyProxyServerConfigurationToClients();

    setHdsUrl("https://www.somehost.com/");
    initClient();
  }

  private void initClient() {
    ProductLicense mockProductLicense = mock(ProductLicense.class);
    lenient().when(mockProductLicense.isValid()).thenReturn(true);
    lenient().when(mockProductLicense.getFingerprint()).thenReturn("license-fingerprint");
    client = new HdsClient(new InsightProxy(configuration, passwordHandler), mockProductLicense, configuration,
        new DefaultVersionService(), telemetryId, null, 20,
        name -> new Retry(name, 0, null, e -> false, i -> Duration.ZERO));
  }

  @After
  public void exit() throws Exception {
    try {
      if (server != null) {
        server.stop();
      }
    }
    finally {
      proxyServerConfigurationDAO.delete();
      proxyServerConfigurationService.applyProxyServerConfigurationToClients();
    }
  }

  @Override
  protected void pingUrl(String url) {
    setHdsUrl(url);
    initClient();
    client.get(String.class, "test/path");
  }

  @Test
  public void testUserAgentAddedToConnectRequest() throws Exception {
    final Map<String, String> connectHeaders = new HashMap<>();
    final CountDownLatch connectReceived = new CountDownLatch(1);
    try (ServerSocket rawProxy = new ServerSocket(0)) {
      Thread acceptor = new Thread(() -> {
        try (Socket client = rawProxy.accept();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream(), StandardCharsets.ISO_8859_1)))
        {
          String requestLine = reader.readLine();
          if (requestLine != null && requestLine.startsWith("CONNECT ")) {
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
              int colon = line.indexOf(':');
              if (colon > 0) {
                connectHeaders.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
              }
            }
            connectReceived.countDown();
          }
          // Reply with 502 and close so the client aborts cleanly (we won't tunnel).
          client.getOutputStream()
              .write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n"
                  .getBytes(StandardCharsets.ISO_8859_1));
          client.getOutputStream().flush();
        }
        catch (IOException ignore) {
          // socket closed by client while we were writing — expected
        }
      }, "test-raw-connect-proxy");
      acceptor.setDaemon(true);
      acceptor.start();

      tempEntity.setProxyServerConfiguration("localhost", rawProxy.getLocalPort());
      proxyServerConfigurationService.applyProxyServerConfigurationToClients();
      setHdsUrl("https://www.somehost.com/");
      initClient();

      HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
      when(mockedRequest.getMethod()).thenReturn("GET");
      when(mockedRequest.getHeaderNames()).thenReturn(Collections.enumeration(
          Collections.singletonList(HttpHeaders.USER_AGENT)));

      try {
        client.relay(mockedRequest, null, "some/path", Collections.emptyMap());
      }
      catch (Exception ignore) {
        // noop — tunnel is rejected on purpose
      }

      assertThat(connectReceived.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(connectHeaders).containsEntry(HttpHeaders.USER_AGENT, UserAgentUtils.getDefaultUserAgent());
    }
  }

  @Test
  public void testProxyServerConfigurationChanged() {
    proxyServerConfigurationDAO.delete();
    setHdsUrl("http://proxy.test/");
    tempEntity.setProxyServerConfiguration("localhost", ((NetworkConnector) server.getConnectors()[0]).getLocalPort());
    configuration.proxyServerConfigurationChanged();
    initClient();

    String proxyResponse = "PROXY-TEST-PASSED";
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().print(proxyResponse);
      }
    };

    assertThat(client.get(String.class, "/rest/test")).isEqualTo(proxyResponse);
  }
}
