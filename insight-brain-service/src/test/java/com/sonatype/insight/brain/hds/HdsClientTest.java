/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.ByteArrayInputStream;
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

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.UserAgentUtils;
import com.sonatype.insight.error.exception.BadGatewayException;

import com.google.common.net.HttpHeaders;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HdsClientTest
    extends AbstractHdsClientTest
{

  private static final String USER_AGENT_SUFFIX = "test suffix";

  @Override
  protected void initClient() {
    CLMLicenseManager licenseManager = mock(CLMLicenseManager.class);
    when(licenseManager.getLicenseFingerprint()).thenReturn("license-fingerprint");
    client = new HdsClient(new InsightProxy(config), licenseManager, config, new VersionService(),
        mock(IdleConnectionReaper.class), telemetryId);
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

    HttpEntity httpEntity = MultipartEntityBuilder.create().build();
    client.post(testPath, httpEntity, null /* testClmClientUserAgent */);
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER), is(nullValue()));
    client.post(testPath, httpEntity, testClmClientUserAgent);
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
    String usernameHeader = "My-User-Header";
    config.getReverseProxyAuthentication().setUsernameHeader(usernameHeader);
    config.getReverseProxyAuthentication().setEnabled(true);
    initClient();

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
            HttpHeaders.COOKIE, "Cookie2", usernameHeader)));
    when(request.getHeader(any(String.class))).thenReturn("header-value");
    when(request.getMethod()).thenReturn("GET");

    client.get(request, String.class, "/rest/test");

    assertThat(HttpHeaders.AUTHORIZATION.toLowerCase(Locale.ENGLISH), not(isIn(headers)));
    assertThat(HttpHeaders.PROXY_AUTHORIZATION.toLowerCase(Locale.ENGLISH), not(isIn(headers)));
    assertThat(HttpHeaders.COOKIE.toLowerCase(Locale.ENGLISH), not(isIn(headers)));
    assertThat("Cookie2".toLowerCase(Locale.ENGLISH), not(isIn(headers)));
    assertThat(usernameHeader.toLowerCase(Locale.ENGLISH), not(isIn(headers)));
    assertThat(headers, not(empty()));
  }

  @Test
  public void testRemoveXForwarded() throws Exception {
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
        Collections.enumeration(Arrays.asList("X-Forwarded-Host", "X-Forwarded-Server", "X-Forwarded-For")));
    when(request.getHeader(any(String.class))).thenReturn("header-value");
    when(request.getMethod()).thenReturn("GET");

    client.get(request, String.class, "/rest/test");

    assertThat("X-Forwarded-Host".toLowerCase(Locale.ENGLISH), not(isIn(headers)));
    assertThat("X-Forwarded-Server".toLowerCase(Locale.ENGLISH), not(isIn(headers)));
    assertThat("X-Forwarded-For".toLowerCase(Locale.ENGLISH), not(isIn(headers)));
    assertThat(headers, not(empty()));
  }

  @Test
  public void testRequestUsesProperContentLength() throws Exception {
    final Map<String, String> headers = new HashMap<>();
    byte[] test = "test".getBytes();
    handler = new AbstractHandler()
    {

      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException
      {
        headers.clear();
        for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements(); ) {
          String headerName = en.nextElement();
          headers.put(headerName, request.getHeader(headerName));
        }
        baseRequest.setHandled(true);
      }
    };

    HttpServletRequest request = mock(HttpServletRequest.class);

    when(request.getInputStream()).thenReturn(new ServletInputStreamImpl(test));
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList(HttpHeaders.USER_AGENT)));
    // Use a smaller content-length than the actual incoming request (simulate gzip entity) 
    when(request.getContentLength()).thenReturn(1);
    when(request.getMethod()).thenReturn("POST");

    client.get(request, String.class, "/rest/test");

    assertThat(request.getContentLength(), not(Integer.parseInt(headers.get(HttpHeaders.CONTENT_LENGTH))));
    assertThat(headers.get(HttpHeaders.CONTENT_LENGTH), is(Integer.toString(test.length)));
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
  public void testAnalyticsIdOnRequests() throws Exception {
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

    client.put(analytics, String.class, testPath, tempDir.newFile(), new String[] {});
    assertThat(headers, hasEntry(HdsClient.OWNER_TYPE_HEADER, analytics.getOwnerType().toString()));
    assertThat(headers, hasEntry(HdsClient.OWNER_ID_HEADER, analytics.getOwnerId()));

    client.post(analytics, String.class, testPath, null, tempDir.newFile(), new String[] {});
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
  public void testSSLExceptionFromHttpClientExecute() throws Exception {
    config.setHdsUrl(config.getHdsUrl().replace("http:", "https:"));
    initClient();
    try {
      client.get(String.class, "/any", null);
      fail("Expected exception");
    }
    catch (BadGatewayException e) {
      assertThat(e.getMessage(), is("The SSL/TLS connection to Sonatype HDS could not be established, "
          + "contact your network or system administrator for help."));
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

  @Test
  public void testGetErrorMessage_CatchesException() throws Exception {
    assertThat(client.getErrorMessage(createMockResponse(new IOException())), is(equalTo("reason")));
    assertThat(client.getErrorMessage(createMockResponse(new RuntimeException())), is(equalTo("reason")));
  }

  @Test
  public void testGet_EncodeQueryParameters() throws Exception {
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        response.setStatus(HttpStatus.OK_200);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println(request.getQueryString());
        baseRequest.setHandled(true);
      }
    };
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getMethod()).thenReturn("GET");
    when(httpServletRequest.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
    Map<String, String> queryParams = new HashMap<>();

    queryParams.put("name1", "{ }+&;/?:@=<>#%|\\^~[]`");

    queryParams.put("name2", "{\"format\":\"a-name\",\"coordinates\":{\"name\":\"org.dojotoolkit dojo\",\"qualifier\"" +
        ":\"\",\"version\":\"1.8.14\"}} ");

    //making sure reserved characters are preserved where they should be and encoded in the query values
    String requestUri = client.get(httpServletRequest, String.class, "rest/ci/componentDetails", queryParams);
    assertThat(requestUri,
        containsString("name2=%7B%22format%22%3A%22a-name%22%2C%22coordinates%22%3A%7B%22name%22%3A%22org.dojotoolkit" +
            "+dojo%22%2C%22qualifier%22%3A%22%22%2C%22version%22%3A%221.8.14%22%7D%7D"));
     assertThat(requestUri, containsString("name1=%7B+%7D%2B%26%3B%2F%3F%3A%40%3D%3C%3E%23%25%7C%5C%5E~%5B%5D%60"));
     assertThat(requestUri, anyOf(containsString("&name1="), containsString("&name2=")));
  }

  private HttpResponse createMockResponse(Exception e) throws Exception {
    HttpResponse mockResponse = mock(HttpResponse.class);
    Header mockHeader = mock(Header.class);
    when(mockHeader.getValue()).thenReturn("text/plain");
    when(mockResponse.getFirstHeader(org.apache.http.HttpHeaders.CONTENT_TYPE)).thenReturn(mockHeader);
    HttpEntity mockEntity = mock(HttpEntity.class);
    when(mockEntity.getContent()).thenThrow(e);
    when(mockResponse.getEntity()).thenReturn(mockEntity);
    StatusLine mockStatusLine = mock(StatusLine.class);
    when(mockStatusLine.getReasonPhrase()).thenReturn("reason");
    when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
    return mockResponse;
  }
  
  private static class ServletInputStreamImpl
      extends ServletInputStream
  {
    // ByteArrayInputStream.close is a noop, so we don't need to close this stream
    private ByteArrayInputStream wrappedInputStream;

    public ServletInputStreamImpl(byte[] data) {
      wrappedInputStream = new ByteArrayInputStream(data);
    }

    @Override
    public int read() throws IOException {
      return wrappedInputStream.read();
    }

    @Override
    public boolean isFinished() {
      return false;
    }

    @Override
    public boolean isReady() {
      return false;
    }

    @Override
    public void setReadListener(final ReadListener readListener) {
      // No implementation necessary
    }
  }

  @Test
  public void testTelemetryId() throws Exception {
    final Map<String, String> headers = new HashMap<>();
    String testPath = "/rest/test";
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      {
        headers.clear();
        headers.put(HdsClient.TELEMETRY_ID_HEADER, request.getHeader(HdsClient.TELEMETRY_ID_HEADER));
        baseRequest.setHandled(true);
      }
    };

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.<String> emptyList()));
    when(request.getMethod()).thenReturn("GET");

    client.get(request, null, InputStream.class, testPath, null, new String[] {});
    assertThat(headers, hasEntry(HdsClient.TELEMETRY_ID_HEADER, telemetryId.getId()));

    client.post(String.class, testPath, "foo", new String[] {});
    assertThat(headers, hasEntry(HdsClient.TELEMETRY_ID_HEADER, telemetryId.getId()));

    client.post(testPath, MultipartEntityBuilder.create().build(), "test_client_user_agent");
    assertThat(headers, hasEntry(HdsClient.TELEMETRY_ID_HEADER, telemetryId.getId()));

    client.put(null, String.class, testPath, tempDir.newFile(), new String[] {});
    assertThat(headers, hasEntry(HdsClient.TELEMETRY_ID_HEADER, telemetryId.getId()));
  }

  @Test
  public void testPost_Multipart() throws Exception {
    final int[] statusCode = new int[1];
    final BodyPart[] fileBodyReceived = new BodyPart[1];

    String testPath = "/rest/test";
    handler = new AbstractHandler()
    {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        try {
          ByteArrayDataSource multipartDataSource = new ByteArrayDataSource(request.getInputStream(),
              "multipart/form-data");
          MimeMultipart multipart = new MimeMultipart(multipartDataSource);
          fileBodyReceived[0] = multipart.getBodyPart(0);
        }
        catch (MessagingException e) {
          throw new IOException("Unable to read multipart body", e);
        }

        response.setStatus(HttpStatus.NO_CONTENT_204);
        statusCode[0] = HttpStatus.NO_CONTENT_204;
        baseRequest.setHandled(true);
      }
    };

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
    when(request.getMethod()).thenReturn("POST");
    File fileSent = tempDir.newFile();
    FileUtils.write(fileSent, "Test", "UTF-8");
    FileBody fileBodySent = new FileBody(fileSent);
    HttpEntity httpEntity = MultipartEntityBuilder.create().addPart("file", fileBodySent).build();
    client.post(testPath, httpEntity, "test_client_user_agent");
    assertThat(statusCode[0], is(HttpStatus.NO_CONTENT_204));
    assertThat(fileBodyReceived[0].getFileName(), is(fileBodySent.getFilename()));
    assertThat(IOUtils.toString(fileBodyReceived[0].getInputStream(), "UTF-8"), is("Test"));
  }
}
