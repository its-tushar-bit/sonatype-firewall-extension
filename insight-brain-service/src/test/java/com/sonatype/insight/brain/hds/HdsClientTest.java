/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.UserAgentUtils;
import com.sonatype.insight.error.exception.BadGatewayException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
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
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HdsClientTest
{
  private static final String USER_AGENT_SUFFIX = "test suffix";

  private Server server;

  private HdsClient client;

  private AbstractHandler handler;

  private InsightConfig config;

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
    config.setHdsUrl("http://localhost:" + server.getConnectors()[0].getLocalPort());
    config.setUserAgentSuffix(USER_AGENT_SUFFIX);
    initClient();
  }

  private void initClient() {
    CLMLicenseManager licenseManager = mock(CLMLicenseManager.class);
    when(licenseManager.getLicenseFingerprint()).thenReturn("license-fingerprint");
    client = new HdsClient(new InsightProxy(config), licenseManager, new VersionService(),
        mock(IdleConnectionReaper.class));
  }

  @After
  public void exit() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void testClientUserAgentOnRequests() throws Exception {
    String testPath = "/rest/test";
    String testClmClientUserAgent = "client_user_agent";

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

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList(HttpHeaders.USER_AGENT)));
    when(request.getHeader(eq(HttpHeaders.USER_AGENT))).thenReturn(testClmClientUserAgent);
    when(request.getMethod()).thenReturn("GET");

    client.doProxy(request, testPath);
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER), is(testClmClientUserAgent));
    client.doProxy(request, testPath, null, new String[] {});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER), is(testClmClientUserAgent));
    // Method does not pass an original request, hence the null header.
    client.get(InputStream.class, testPath, null, new String[] {});
    assertNull(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER));
    client.get(request, InputStream.class, testPath, new String[] {});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER), is(testClmClientUserAgent));
    client.get(request, InputStream.class, testPath, null, new String[] {});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER), is(testClmClientUserAgent));
    client.getResponse(request, testPath, null, new String[] {});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER), is(testClmClientUserAgent));
    // Method does not pass an original request, hence the null header.
    client.put(null, InputStream.class, testPath,
        new File(HdsClientTest.class.getResource("/config-test.yml").toURI()), new String[] {});
    assertNull(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER));

    when(request.getHeader(eq(HttpHeaders.USER_AGENT))).thenReturn("ua-we-cannot-control");
    when(request.getHeader(eq(HdsClient.CLM_CLIENT_USER_AGENT_HEADER))).thenReturn(testClmClientUserAgent);
    client.get(request, InputStream.class, testPath, new String[] {});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER), is(testClmClientUserAgent));
  }

  @Test
  public void testClmUserAgentOnRequests() throws Exception {
    String userAgent = UserAgentUtils.getDefaultUserAgent() + " " + USER_AGENT_SUFFIX;
    final Set<String> headers = new HashSet<>();
    String testPath = "/rest/test";
    handler = new AbstractHandler()
    {

      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        headers.clear();
        for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements();) {
          headers.add(request.getHeader(en.nextElement()));
        }
        baseRequest.setHandled(true);
      }
    };

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList(HttpHeaders.USER_AGENT)));
    when(request.getHeader(any(String.class))).thenReturn("header-value");
    when(request.getMethod()).thenReturn("GET");

    client.doProxy(request, testPath);
    assertThat(headers, hasItem(userAgent));
    client.doProxy(request, testPath, null, new String[] {});
    assertThat(headers, hasItem(userAgent));
    client.get(InputStream.class, testPath, null, new String[] {});
    assertThat(headers, hasItem(userAgent));
    client.get(request, InputStream.class, testPath, new String[] {});
    assertThat(headers, hasItem(userAgent));
    client.get(request, InputStream.class, testPath, null, new String[] {});
    assertThat(headers, hasItem(userAgent));
    client.getResponse(request, testPath, null, new String[] {});
    assertThat(headers, hasItem(userAgent));
    client.put(null, InputStream.class, testPath,
        new File(HdsClientTest.class.getResource("/config-test.yml").toURI()), new String[] {});
    assertThat(headers, hasItem(userAgent));
  }

  @Test
  public void testDoNotLeakUserCredentialsToHds() throws Exception {
    final Set<String> headers = new HashSet<>();
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
      fail("Expected exception");
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
      fail("Expected exception");
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
      fail("Expected exception");
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
      fail("Expected exception");
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
      fail("Expected exception");
    }
    catch (BadGatewayException e) {
      assertThat(e.getMessage(), containsString("Sonatype Support"));
    }
  }

  @Test
  public void testTransformUnknownHost() throws Exception {
    config.setHdsUrl("http://an.unresolvable.hostname/");
    initClient();
    try {
      client.get(String.class, "/any", null);
      fail("Expected exception");
    }
    catch (BadGatewayException e) {
      assertThat(e.getMessage(), is("The hostname for the Sonatype HDS could not be resolved,"
          + " please verify the network configuration (DNS) at the site where the Nexus IQ Server is operated"));
    }
  }

  @Test
  public void testAppIdOnRequests() throws Exception {
    final Map<String, String> headers = new HashMap<>();
    String testPath = "/rest/test";
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        headers.clear();
        headers.put(HdsClient.OWNER_TYPE_HEADER, request.getHeader(HdsClient.OWNER_TYPE_HEADER));
        headers.put(HdsClient.OWNER_ID_HEADER, request.getHeader(HdsClient.OWNER_ID_HEADER));
        baseRequest.setHandled(true);
      }
    };

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.<String> emptyList()));
    when(request.getMethod()).thenReturn("GET");

    HdsClientAnalytics analytics = HdsClientAnalytics.forApplication("test-app-id");

    client.get(request, analytics, InputStream.class, testPath, null, new String[] {});
    assertThat(headers, hasEntry(HdsClient.OWNER_TYPE_HEADER, analytics.getOwnerType().toString()));
    assertThat(headers, hasEntry(HdsClient.OWNER_ID_HEADER, analytics.getOwnerId()));

    client.put(analytics, String.class, testPath, File.createTempFile("test", ".tmp"), new String[] {});
    assertThat(headers, hasEntry(HdsClient.OWNER_TYPE_HEADER, analytics.getOwnerType().toString()));
    assertThat(headers, hasEntry(HdsClient.OWNER_ID_HEADER, analytics.getOwnerId()));
  }

  @Test
  public void testTransformOtherErrors() throws Exception {
    testTransformOtherErrors(456);
    testTransformOtherErrors(567);
  }

  private void testTransformOtherErrors(final int statusCode) throws Exception {
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        response.setStatus(statusCode);
        baseRequest.setHandled(true);
      }
    };

    try {
      client.get(String.class, "/any", null);
      fail("Expected exception");
    }
    catch (BadGatewayException e) {
      assertThat(e.getMessage(), is("The Sonatype HDS returned error " + statusCode + ", please retry in a bit."));
    }
  }

  @Test
  public void testIOExceptionReadingResponse() throws Exception {
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        response.setStatus(HttpStatus.OK_200);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("Not an integer");
        baseRequest.setHandled(true);
      }
    };

    try {
      client.get(Integer.class, "/any", null);
      fail("Expected exception");
    }
    catch (BadGatewayException e) {
      assertThat(
          e.getMessage(),
          is("Failed to read response entity received from Sonatype HDS, please retry in a bit."));
    }
  }

  @Test
  public void testIOExceptionFromHttpClientExecute() throws Exception {
    // Use reflection to get access to the private http client and set it to a mocked instance.
    Class<?> clientClass = client.getClass();
    Field httpClientField = clientClass.getDeclaredField("client");
    httpClientField.setAccessible(true);
    HttpClient httpClient = mock(HttpClient.class);
    httpClientField.set(client, httpClient);

    when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(new IOException("Test"));

    try {
      client.get(String.class, "/any", null);
      fail("Expected exception");
    }
    catch (BadGatewayException e) {
      assertThat(e.getMessage(), is("The request to Sonatype HDS failed, please retry in a bit."));
    }
  }

  @Test
  public void testTransformInternalServerError_500() throws Exception {
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("Some error message");
        baseRequest.setHandled(true);
      }
    };

    try {
      client.get(String.class, "/any", null);
      fail("Expected exception");
    }
    catch (BadGatewayException e) {
      assertThat(e.getMessage(), is("The Sonatype HDS returned error 500, please retry in a bit."));
    }
  }
}
