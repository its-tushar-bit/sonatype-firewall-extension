/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.error.exception.BadGatewayException;

import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SaasClientTest
{

  private Server server;

  private SaasClient client;

  private AbstractHandler handler;

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

    InsightConfig config = new InsightConfig();
    config.setSaasAddress("http://localhost:" + server.getConnectors()[0].getLocalPort());
    CLMLicenseManager licenseManager = mock(CLMLicenseManager.class);
    when(licenseManager.getLicenseFingerprint()).thenReturn("license-fingerprint");
    client = new SaasClient(new InsightProxy(config), licenseManager);
  }

  @After
  public void exit() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void testDoNotLeakUserCredentialsToSaas() throws Exception {
    final Set<String> headers = new HashSet<String>();
    handler = new AbstractHandler()
    {

      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements();) {
          headers.add(en.nextElement().toLowerCase(Locale.ENGLISH));
        }
        baseRequest.setHandled(true);
      }
    };

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(
        Collections.enumeration(Arrays.asList(HttpHeaders.AUTHORIZATION, HttpHeaders.PROXY_AUTHORIZATION,
            HttpHeaders.COOKIE)));
    when(request.getHeader(any(String.class))).thenReturn("header-value");
    when(request.getMethod()).thenReturn("GET");

    client.doProxy(request, "/rest/test");

    assertThat(HttpHeaders.AUTHORIZATION.toLowerCase(Locale.ENGLISH), not(isIn(headers)));
    assertThat(HttpHeaders.PROXY_AUTHORIZATION.toLowerCase(Locale.ENGLISH), not(isIn(headers)));
    assertThat(HttpHeaders.COOKIE.toLowerCase(Locale.ENGLISH), not(isIn(headers)));
    assertThat(headers, not(empty()));
  }

  @Test
  public void testTransformAuthErrors_401() throws Exception {
    handler = new AbstractHandler()
    {

      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        response.setStatus(HttpStatus.UNAUTHORIZED_401);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("PASSED");
        baseRequest.setHandled(true);
      }
    };

    try {
      client.get(String.class, "/any", null);
    }
    catch (BadGatewayException e) {
      assertThat(e.getMessage(), containsString("401"));
      assertThat(e.getMessage(), containsString("PASSED"));
    }
  }

  @Test
  public void testTransformAuthErrors_403() throws Exception {
    handler = new AbstractHandler()
    {

      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        response.setStatus(HttpStatus.FORBIDDEN_403);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("PASSED");
        baseRequest.setHandled(true);
      }
    };

    try {
      client.get(String.class, "/any", null);
    }
    catch (BadGatewayException e) {
      assertThat(e.getMessage(), containsString("403"));
      assertThat(e.getMessage(), containsString("PASSED"));
    }
  }

  @Test
  public void testTransformAuthErrors_407() throws Exception {
    handler = new AbstractHandler()
    {

      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        response.setStatus(HttpStatus.PROXY_AUTHENTICATION_REQUIRED_407);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("PASSED");
        baseRequest.setHandled(true);
      }
    };

    try {
      client.get(String.class, "/any", null);
    }
    catch (BadGatewayException e) {
      assertThat(e.getMessage(), containsString("407"));
      assertThat(e.getMessage(), containsString("PASSED"));
    }
  }

  @Test
  public void testTransformServiceUnavailable() throws Exception {
    handler = new AbstractHandler()
    {

      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE_503);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("PASSED");
        baseRequest.setHandled(true);
      }
    };

    try {
      client.get(String.class, "/any", null);
    }
    catch (BadGatewayException e) {
      assertThat(e.getMessage(), containsString("Sonatype Support"));
    }
  }

  @Test
  public void testTransformBadGateway() throws Exception {
    handler = new AbstractHandler()
    {

      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        response.setStatus(HttpStatus.BAD_GATEWAY_502);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("PASSED");
        baseRequest.setHandled(true);
      }
    };

    try {
      client.get(String.class, "/any", null);
    }
    catch (BadGatewayException e) {
      assertThat(e.getMessage(), containsString("Sonatype Support"));
    }
  }
}
