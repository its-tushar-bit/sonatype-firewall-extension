/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.DefaultVersionService;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.test.LogOutput;

import com.google.common.net.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
@RunWith(MockitoJUnitRunner.class)
public class PingHdsClientTest
    extends AbstractHdsClientTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(PingHdsClient.class);

  @Inject
  private Configuration configuration;

  @Override
  protected void initClient() {
    ProductLicense productLicense = mock(ProductLicense.class);
    client = new PingHdsClient(new InsightProxy(configuration, passwordHandler), productLicense, configuration,
        new DefaultVersionService(), telemetryId, null);
  }

  @Test
  public void testSocketTimeout() {
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        try {
          Thread.sleep(PingHdsClient.SOCKET_TIMEOUT + 1000);
        }
        catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        response.setStatus(HttpStatus.OK_200);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("alive");
        baseRequest.setHandled(true);
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
