/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.Collections;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.DefaultVersionService;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.test.LogOutput;

import com.google.common.net.HttpHeaders;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PingHdsClientTest
    extends AbstractHdsClientTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(PingHdsClient.class);

  @Inject
  private Configuration configuration;

  private ProductLicense mockProductLicense;

  @Override
  protected void initClient() {
    mockProductLicense = mock(ProductLicense.class);
    client = new PingHdsClient(new InsightProxy(configuration, passwordHandler), mockProductLicense, configuration,
        new DefaultVersionService(), telemetryId, null);
  }

  @Test
  public void testSocketTimeout() {
    when(mockProductLicense.isValid()).thenReturn(true);
    when(mockProductLicense.getFingerprint()).thenReturn("license-fingerprint");

    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
          Thread.sleep(PingHdsClient.SOCKET_TIMEOUT + 1000);
        }
        catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        response.setStatus(HttpStatus.OK_200);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("alive");
      }
    };
    long start = System.currentTimeMillis();

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singletonList(HttpHeaders.USER_AGENT)));
    when(request.getMethod()).thenReturn("GET");
    assertThatThrownBy(() -> {
      client.relay(request, String.class, "/rest/test");
      // SocketTimeoutException gets converted by HdsClient to BadGatewayException
    }).isInstanceOf(BadGatewayException.class)
        .hasMessage("The request to Sonatype Data Services failed, please retry in a bit.");
    // make sure that the log recorded a Read timed out error (SocketTimeoutException)
    assertThat(logOutput).atErrorLevel().contains("Read timed out");
    assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(PingHdsClient.SOCKET_TIMEOUT);
  }
}
