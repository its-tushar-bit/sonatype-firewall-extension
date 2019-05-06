/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.io.InputStream;
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
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;

import com.google.common.net.HttpHeaders;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserTelemetryHdsClientTest extends AbstractHdsClientTest
{
  @Override
  protected void initClient() {
    CLMLicenseManager licenseManager = mock(CLMLicenseManager.class);
    ApiProxyConfigurationServiceV2 proxyConfig = new ApiProxyConfigurationServiceV2(new ProxyConfigurationDAO());
    when(licenseManager.getLicenseFingerprint()).thenReturn("license-fingerprint");
    client = new UserTelemetryHdsClient(new InsightProxy(config, proxyConfig), licenseManager, config,
        new VersionService(), mock(IdleConnectionReaper.class), telemetryId);
  }

  @Test
  public void testUserAgent() throws IOException {
    final Map<String, String> headers = new HashMap<>();
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        headers.clear();
        for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements();) {
          String headerName = en.nextElement();
          headers.put(headerName, request.getHeader(headerName));
        }
        baseRequest.setHandled(true);
      }
    };
    String browserAgent = "Mozilla/5.0 some-other-stuff";

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singletonList(HttpHeaders.USER_AGENT)));
    when(request.getHeader(eq(HttpHeaders.USER_AGENT))).thenReturn(browserAgent);
    when(request.getMethod()).thenReturn("GET");

    client.relay(request, InputStream.class, "foo/bar");
    assertThat(headers.get(HttpHeaders.USER_AGENT)).isEqualTo(browserAgent);
  }
}
