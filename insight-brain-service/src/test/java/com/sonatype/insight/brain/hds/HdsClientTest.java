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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.inject.Inject;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.ws.rs.InternalServerErrorException;

import com.sonatype.insight.brain.NetworkingHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scan.datastore.FileScanEntity;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.brain.version.DefaultVersionService;
import com.sonatype.insight.client.utils.UserAgentUtils;
import com.sonatype.insight.error.exception.BadGatewayException;

import com.google.common.net.HttpHeaders;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.utils.HttpHelper.createMockResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HdsClientTest
    extends AbstractHdsClientTest
{
  @Inject
  private Configuration configuration;

  private InsightProxy spyInsightProxy;

  private ProductLicense mockProductLicense;

  private CurrentUser mockCurrentUser;

  @Override
  protected void initClient() {
    mockProductLicense = mock(ProductLicense.class);
    when(mockProductLicense.isValid()).thenReturn(true);
    when(mockProductLicense.getFingerprint()).thenReturn("license-fingerprint");
    mockCurrentUser = mock(CurrentUser.class);
    when(mockCurrentUser.isAnonymous()).thenReturn(false);
    when(mockCurrentUser.getUsername()).thenReturn("testuser");
    spyInsightProxy = spy(new InsightProxy(configuration, passwordHandler));
    client =
        new HdsClient(spyInsightProxy, mockProductLicense, configuration, new DefaultVersionService(), telemetryId,
            mockCurrentUser, 20,
            name -> new Retry(name, 0, null, e -> false, i -> Duration.ZERO));
  }

  /**
   * Configures an HTTP request handler that captures the HTTP request headers.
   *
   * @return a map of HTTP headers that can be used to assert HTTP headers for the latest HTTP request
   */
  private Map<String, String> setHttpHeaderCaptorRequestHandler() {
    final Map<String, String> headers = new HashMap<>();
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        headers.clear();
        for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements(); ) {
          String headerName = en.nextElement();
          headers.put(headerName, request.getHeader(headerName));
        }
        response.setStatus(HttpStatus.OK_200);
      }
    };
    return headers;
  }

  @Test
  public void testGet_ClientUserAgentOnRequests() {
    String testPath = "/rest/test";

    Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

    String clientUserAgent = "testClientUserAgent";
    client.get(InputStream.class, testPath, clientUserAgent, null, new String[]{});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(clientUserAgent);
  }

  @Test
  public void testGet_ClientUserAgentOnRequests_NoClientUserAgent() {
    String testPath = "/rest/test";

    Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

    // Method does not pass an original request, hence the null header.
    client.get(InputStream.class, testPath, null, new String[]{});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isNull();
  }

  @Test
  public void testRelay_ClientUserAgentOnRequests() throws Exception {
    String testPath = "/rest/test";
    String testClientUserAgent = "client_user_agent";

    Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singletonList(HttpHeaders.USER_AGENT)));
    when(request.getHeader(eq(HttpHeaders.USER_AGENT))).thenReturn(testClientUserAgent);
    when(request.getMethod()).thenReturn("GET");

    client.relay(request, InputStream.class, testPath, new String[]{});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);
    client.relay(request, InputStream.class, testPath, null, new String[]{});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);
    client.relay(request, null, InputStream.class, testPath, null, new String[]{});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);

    String testExplicitClientUserAgent = "explicit_client_user_agent";
    when(request.getHeader(eq(HdsClient.CLM_CLIENT_USER_AGENT_HEADER))).thenReturn(testExplicitClientUserAgent);
    client.relay(request, InputStream.class, testPath, new String[]{});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testExplicitClientUserAgent);
  }

  @Test
  public void testPut_ClientUserAgentOnRequests() throws Exception {
    String testPath = "/rest/test";
    String testClientUserAgent = "client_user_agent";

    Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

    client.put(null, InputStream.class, testClientUserAgent, testPath,
        new FileScanEntity(new File(getClass().getResource("/config-test.yml").toURI()).toPath()),
        Collections.emptyMap(),
        new String[]{});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);
  }

  @Test
  public void testPost_ClientUserAgentOnRequests() {
    String testPath = "/rest/test";
    String testClientUserAgent = "client_user_agent";

    Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

    HttpEntity httpEntity = MultipartEntityBuilder.create().build();
    client.post(testPath, httpEntity, null /* clientUserAgent */);
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isNull();
    client.post(testPath, httpEntity, testClientUserAgent);
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);

    String[] emptyUriParams = new String[]{};

    // Method does not pass an original request or client user agent, hence the null header.
    client.post(InputStream.class, testPath, "httpEntity", emptyUriParams);
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isNull();

    client.post(null /* analytics */, String.class, testPath, null /* clientUserAgent */, "httpEntity", emptyUriParams);
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isNull();
    client.post(null /* analytics */, String.class, testPath, testClientUserAgent, "httpEntity", emptyUriParams);
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);
  }

  private String getBrainVersion() throws IOException {
    Properties props = new Properties();
    try (InputStream is = this.getClass()
        .getResourceAsStream("/HdsClientTest/testBrainUserAgentOnRequests.properties")) {
      props.load(is);
      return props.getProperty("version");
    }
  }

  @Test
  public void testGet_BrainUserAgentOnRequests() throws Exception {
    String expectedUserAgent = UserAgentUtils.getDefaultUserAgent() + " " + USER_AGENT_SUFFIX;
    assertThat(expectedUserAgent).startsWith("Sonatype_CLM_Server/" + getBrainVersion());
    Map<String, String> headers = setHttpHeaderCaptorRequestHandler();
    String testPath = "/rest/test";

    client.get(InputStream.class, testPath, null, new String[]{});
    assertThat(headers.get(HttpHeaders.USER_AGENT)).isEqualTo(expectedUserAgent);
    client.get(InputStream.class, testPath);
    assertThat(headers.get(HttpHeaders.USER_AGENT)).isEqualTo(expectedUserAgent);
  }

  @Test
  public void testGet_FedRAMPAuditHeaderWhenFeatureEnabledWithAuthenticatedUser() {
    // Given: Feature enabled and authenticated user
    boolean originalState = SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.isEnabled();
    try {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(true);
      when(mockCurrentUser.isAnonymous()).thenReturn(false);
      when(mockCurrentUser.getUsername()).thenReturn("testuser");

      String testPath = "/rest/test";
      Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

      // When: Making request
      client.get(InputStream.class, testPath, null, new String[]{});

      // Then: User header is present with username
      assertThat(headers.get(HdsClient.USERNAME_HEADER)).isEqualTo("testuser");
    }
    finally {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(originalState);
    }
  }

  @Test
  public void testGet_FedRAMPAuditHeaderWhenFeatureEnabledWithAnonymousUser() {
    // Given: Feature enabled and anonymous user
    boolean originalState = SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.isEnabled();
    try {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(true);
      when(mockCurrentUser.isAnonymous()).thenReturn(true);
      // Note: getUsername() is not stubbed because it's not called when isAnonymous() returns true

      String testPath = "/rest/test";
      Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

      // When: Making request
      client.get(InputStream.class, testPath, null, new String[]{});

      // Then: User header is present with "anonymous"
      assertThat(headers.get(HdsClient.USERNAME_HEADER)).isEqualTo("anonymous");
    }
    finally {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(originalState);
    }
  }

  @Test
  public void testGet_NoFedRAMPAuditHeaderWhenFeatureDisabled() {
    // Given: Feature disabled
    boolean originalState = SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.isEnabled();
    try {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(false);
      // Note: No currentUser stubbing needed because feature is disabled and currentUser methods won't be called

      String testPath = "/rest/test";
      Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

      // When: Making request
      client.get(InputStream.class, testPath, null, new String[]{});

      // Then: User header is not present
      assertThat(headers.get(HdsClient.USERNAME_HEADER)).isNull();
    }
    finally {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(originalState);
    }
  }

  @Test
  public void testPost_FedRAMPAuditHeaderWhenFeatureEnabled() {
    // Given: Feature enabled
    boolean originalState = SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.isEnabled();
    try {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(true);
      when(mockCurrentUser.isAnonymous()).thenReturn(false);
      when(mockCurrentUser.getUsername()).thenReturn("postuser");

      String testPath = "/rest/test";
      Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

      // When: Making POST request
      client.post(String.class, testPath, "testData");

      // Then: User header is present
      assertThat(headers.get(HdsClient.USERNAME_HEADER)).isEqualTo("postuser");
    }
    finally {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(originalState);
    }
  }

  @Test
  public void testPost_FedRAMPAuditHeaderWhenFeatureEnabledWithAnonymousUser() {
    // Given: Feature enabled and anonymous user
    boolean originalState = SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.isEnabled();
    try {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(true);
      when(mockCurrentUser.isAnonymous()).thenReturn(true);
      // Note: getUsername() is not stubbed because it's not called when isAnonymous() returns true

      String testPath = "/rest/test";
      Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

      // When: Making POST request
      client.post(String.class, testPath, "testData");

      // Then: User header is present with "anonymous"
      assertThat(headers.get(HdsClient.USERNAME_HEADER)).isEqualTo("anonymous");
    }
    finally {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(originalState);
    }
  }

  @Test
  public void testPost_NoFedRAMPAuditHeaderWhenFeatureDisabled() {
    // Given: Feature disabled
    boolean originalState = SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.isEnabled();
    try {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(false);
      // Note: No currentUser stubbing needed because feature is disabled and currentUser methods won't be called

      String testPath = "/rest/test";
      Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

      // When: Making POST request
      client.post(String.class, testPath, "testData");

      // Then: User header is not present
      assertThat(headers.get(HdsClient.USERNAME_HEADER)).isNull();
    }
    finally {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(originalState);
    }
  }

  @Test
  public void testGet_NoFedRAMPAuditHeaderWhenCurrentUserThrowsException() {
    // Given: Feature enabled but currentUser throws exception when getUsername() is called
    boolean originalState = SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.isEnabled();
    try {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(true);
      when(mockCurrentUser.isAnonymous()).thenReturn(false);
      when(mockCurrentUser.getUsername()).thenThrow(new RuntimeException("Username retrieval failed"));

      String testPath = "/rest/test";
      Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

      // When: Making request (should not fail despite exception in username retrieval)
      client.get(InputStream.class, testPath, null, new String[]{});

      // Then: User header is not present due to exception, but request succeeded
      assertThat(headers.get(HdsClient.USERNAME_HEADER)).isNull();
    }
    finally {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(originalState);
    }
  }

  @Test
  public void testPost_NoFedRAMPAuditHeaderWhenCurrentUserThrowsException() {
    // Given: Feature enabled but currentUser throws exception when getUsername() is called
    boolean originalState = SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.isEnabled();
    try {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(true);
      when(mockCurrentUser.isAnonymous()).thenReturn(false);
      when(mockCurrentUser.getUsername()).thenThrow(new RuntimeException("Username retrieval failed"));

      String testPath = "/rest/test";
      Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

      // When: Making POST request (should not fail despite exception in username retrieval)
      client.post(String.class, testPath, "testData");

      // Then: User header is not present due to exception, but request succeeded
      assertThat(headers.get(HdsClient.USERNAME_HEADER)).isNull();
    }
    finally {
      SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT.setEnabled(originalState);
    }
  }

  @Test
  public void testPost_BrainUserAgentOnRequests() throws Exception {
    String expectedUserAgent = UserAgentUtils.getDefaultUserAgent() + " " + USER_AGENT_SUFFIX;
    assertThat(expectedUserAgent).startsWith("Sonatype_CLM_Server/" + getBrainVersion());
    Map<String, String> headers = setHttpHeaderCaptorRequestHandler();
    String testPath = "/rest/test";

    client.post(testPath, new StringEntity(""), "test-client-user-agent");
    assertThat(headers.get(HttpHeaders.USER_AGENT)).isEqualTo(expectedUserAgent);
    client.post(InputStream.class, testPath, Collections.emptyMap());
    assertThat(headers.get(HttpHeaders.USER_AGENT)).isEqualTo(expectedUserAgent);
    client.post(null, InputStream.class, testPath, "test-client-user-agent", Collections.emptyMap());
    assertThat(headers.get(HttpHeaders.USER_AGENT)).isEqualTo(expectedUserAgent);
  }

  @Test
  public void testRelay_BrainUserAgentOnRequests() throws Exception {
    String expectedUserAgent = UserAgentUtils.getDefaultUserAgent() + " " + USER_AGENT_SUFFIX;
    assertThat(expectedUserAgent).startsWith("Sonatype_CLM_Server/" + getBrainVersion());
    Map<String, String> headers = setHttpHeaderCaptorRequestHandler();
    String testPath = "/rest/test";

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singletonList(HttpHeaders.USER_AGENT)));
    when(request.getHeader(any(String.class))).thenReturn("header-value");
    when(request.getMethod()).thenReturn("GET");

    client.relay(request, InputStream.class, testPath, new String[]{});
    assertThat(headers.get(HttpHeaders.USER_AGENT)).isEqualTo(expectedUserAgent);
    client.relay(request, InputStream.class, testPath, null, new String[]{});
    assertThat(headers.get(HttpHeaders.USER_AGENT)).isEqualTo(expectedUserAgent);
    client.relay(request, null, InputStream.class, testPath, null, new String[]{});
    assertThat(headers.get(HttpHeaders.USER_AGENT)).isEqualTo(expectedUserAgent);
  }

  @Test
  public void testPut_BrainUserAgentOnRequests() throws Exception {
    String expectedUserAgent = UserAgentUtils.getDefaultUserAgent() + " " + USER_AGENT_SUFFIX;
    assertThat(expectedUserAgent).startsWith("Sonatype_CLM_Server/" + getBrainVersion());
    Map<String, String> headers = setHttpHeaderCaptorRequestHandler();
    String testPath = "/rest/test";

    client.put(null, InputStream.class, "client_user_agent", testPath,
        new FileScanEntity(new File(getClass().getResource("/config-test.yml").toURI()).toPath()),
        Collections.emptyMap(),
        new String[]{});
    assertThat(headers.get(HttpHeaders.USER_AGENT)).isEqualTo(expectedUserAgent);
  }

  @Test
  public void testDoNotLeakUserCredentialsToHds() throws Exception {
    String usernameHeader = "My-User-Header";
    tempEntity.newReverseProxyAuthenticationConfiguration(true, usernameHeader, false, null);
    configuration.reverseProxyAuthenticationConfigurationChanged();
    initClient();

    final Set<String> headers = new HashSet<>();
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements(); ) {
          headers.add(en.nextElement());
        }
      }
    };

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(
        Collections.enumeration(Arrays.asList(HttpHeaders.AUTHORIZATION, HttpHeaders.PROXY_AUTHORIZATION,
            HttpHeaders.COOKIE, "Cookie2", usernameHeader)));
    when(request.getHeader(any(String.class))).thenReturn("header-value");
    when(request.getMethod()).thenReturn("GET");

    client.relay(request, String.class, "/rest/test");

    assertThat(headers).usingElementComparator(String.CASE_INSENSITIVE_ORDER).isNotEmpty().doesNotContain(
        HttpHeaders.AUTHORIZATION, HttpHeaders.PROXY_AUTHORIZATION, HttpHeaders.COOKIE, "Cookie2", usernameHeader);
  }

  @Test
  public void testRemoveXForwarded() throws Exception {
    final Set<String> headers = new HashSet<>();
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements(); ) {
          headers.add(en.nextElement());
        }
      }
    };

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(
        Collections.enumeration(Arrays.asList("X-Forwarded-Host", "X-Forwarded-Server", "X-Forwarded-For")));
    when(request.getHeader(any(String.class))).thenReturn("header-value");
    when(request.getMethod()).thenReturn("GET");

    client.relay(request, String.class, "/rest/test");

    assertThat(headers).usingElementComparator(String.CASE_INSENSITIVE_ORDER).isNotEmpty()
        .doesNotContain("X-Forwarded-Host", "X-Forwarded-Server", "X-Forwarded-For");
  }

  @Test
  public void testRequestUsesProperContentLength() throws Exception {
    Map<String, String> headers = setHttpHeaderCaptorRequestHandler();
    byte[] test = "test".getBytes();

    HttpServletRequest request = mock(HttpServletRequest.class);

    when(request.getInputStream()).thenReturn(new ServletInputStreamImpl(test));
    when(request.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singletonList(HttpHeaders.USER_AGENT)));
    // Use a smaller content-length than the actual incoming request (simulate gzip entity)
    when(request.getContentLength()).thenReturn(1);
    when(request.getMethod()).thenReturn("POST");

    client.relay(request, String.class, "/rest/test");

    assertThat(request.getContentLength()).isNotEqualTo(Integer.parseInt(headers.get(HttpHeaders.CONTENT_LENGTH)));
    assertThat(headers.get(HttpHeaders.CONTENT_LENGTH)).isEqualTo(Integer.toString(test.length));
  }

  @Test
  public void testTransformAuthErrors_401() {
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        response.setStatus(HttpStatus.UNAUTHORIZED_401);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("PASSED");
      }
    };

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.get(String.class, "/any", null))
        .withMessageContaining("401").withMessageContaining("PASSED");
  }

  @Test
  public void testTransformAuthErrors_403() {
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        response.setStatus(HttpStatus.FORBIDDEN_403);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("PASSED");
      }
    };

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.get(String.class, "/any", null))
        .withMessageContaining("403").withMessageContaining("PASSED");
  }

  @Test
  public void testTransformAuthErrors_407() {
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        response.setStatus(HttpStatus.PROXY_AUTHENTICATION_REQUIRED_407);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("PASSED");
      }
    };

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.get(String.class, "/any", null))
        .withMessageContaining("407").withMessageContaining("PASSED");
  }

  @Test
  public void testTransformServiceUnavailable() {
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE_503);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("PASSED");
      }
    };

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.get(String.class, "/any", null))
        .withMessageContaining("Sonatype Support");
  }

  @Test
  public void testTransformBadGateway() {
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        response.setStatus(HttpStatus.BAD_GATEWAY_502);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("PASSED");
      }
    };

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.get(String.class, "/any", null))
        .withMessageContaining("Sonatype Data Services");
  }

  @Test
  public void testTransformUnknownHost() {
    NetworkingHelper.assumeDnsResolutionIsNormal();
    setHdsUrl("http://an.unresolvable.hostname/");
    initClient();
    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.get(String.class, "/any", null))
        .withMessage("The hostname for the Sonatype Data Services could not be resolved,"
            + " please verify the network configuration (DNS) at the site where the Nexus IQ Server is operated");
  }

  @Test
  public void testAnalyticsIdOnRequests() throws Exception {
    final Map<String, String> headers = new HashMap<>();
    String testPath = "/rest/test";
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        headers.clear();
        headers.put(HdsClient.OWNER_TYPE_HEADER, request.getHeader(HdsClient.OWNER_TYPE_HEADER));
        headers.put(HdsClient.OWNER_ID_HEADER, request.getHeader(HdsClient.OWNER_ID_HEADER));
      }
    };

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
    when(request.getMethod()).thenReturn("GET");

    Application app = new Application();
    app.setId("test-app-id");
    HdsClientAnalytics analytics = HdsClientAnalytics.forOwner(app);

    client.relay(request, analytics, InputStream.class, testPath, null, new String[]{});
    assertThat(headers).containsEntry(HdsClient.OWNER_TYPE_HEADER, analytics.getOwnerType().toString())
        .containsEntry(HdsClient.OWNER_ID_HEADER, analytics.getOwnerId());

    client.put(analytics, String.class, null, testPath, new FileScanEntity(tempDir.newFile().toPath()),
        Collections.emptyMap(),
        new String[]{});
    assertThat(headers).containsEntry(HdsClient.OWNER_TYPE_HEADER, analytics.getOwnerType().toString())
        .containsEntry(HdsClient.OWNER_ID_HEADER, analytics.getOwnerId());

    client.post(analytics, String.class, testPath, null, tempDir.newFile(), new String[]{});
    assertThat(headers).containsEntry(HdsClient.OWNER_TYPE_HEADER, analytics.getOwnerType().toString())
        .containsEntry(HdsClient.OWNER_ID_HEADER, analytics.getOwnerId());
  }

  @Test
  public void testTransformOtherErrors() throws Exception {
    testTransformOtherErrors(456);
    testTransformOtherErrors(567);
  }

  private void testTransformOtherErrors(final int statusCode) {
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(statusCode);
      }
    };

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.get(String.class, "/any", null))
        .withMessage("The Sonatype Data Services returned error " + statusCode + ", please retry in a bit.");
  }

  @Test
  public void testIOExceptionReadingResponse() {
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        response.setStatus(HttpStatus.OK_200);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("Not an integer");
      }
    };

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.get(Integer.class, "/any", null))
        .withMessage("Failed to read response entity received from Sonatype Data Services, please retry in a bit.");
  }

  @Test
  public void testIOExceptionFromHttpClientExecute() throws Exception {
    // Use reflection to get access to the private http client and set it to a mocked instance.
    Class<?> clientClass = client.getClass();
    Field httpClientField = clientClass.getDeclaredField("client");
    httpClientField.setAccessible(true);
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    httpClientField.set(client, httpClient);

    when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(new IOException("Test"));

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.get(String.class, "/any", null))
        .withMessage("The request to Sonatype Data Services failed, please retry in a bit.");
  }

  @Test
  public void testSSLExceptionFromHttpClientExecute() {
    String hdsUrl = configuration.getHdsUrl();
    setHdsUrl(hdsUrl.replace("http:", "https:"));
    initClient();
    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.get(String.class, "/any", null))
        .withMessage("The SSL/TLS connection to Sonatype Data Services could not be established, "
            + "contact your network or system administrator for help.");
  }

  @Test
  public void testTransformInternalServerError_500() {
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("Some error message");
      }
    };

    assertThatExceptionOfType(InternalServerErrorException.class)
        .isThrownBy(() -> client.get(String.class, "/any", null))
        .withMessage("The Sonatype Data Services returned error 500, please retry in a bit.");
  }

  @Test
  public void testGetErrorMessage_CatchesException() throws Exception {
    assertThat(client.getErrorMessage(createMockResponse(new IOException()))).isEqualTo("reason");
    assertThat(client.getErrorMessage(createMockResponse(new RuntimeException()))).isEqualTo("reason");
  }

  @Test
  public void testGet_EncodeQueryParameters() throws Exception {
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        response.setStatus(HttpStatus.OK_200);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println(request.getQueryString());
      }
    };
    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getMethod()).thenReturn("GET");
    when(httpServletRequest.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
    Map<String, String> queryParams = new HashMap<>();

    queryParams.put("name1", "{ }+&;/?:@=<>#%|\\^~[]`");

    queryParams.put("name2", "{\"format\":\"a-name\",\"coordinates\":{\"name\":\"org.dojotoolkit dojo\",\"qualifier\"" +
        ":\"\",\"version\":\"1.8.14\"}} ");

    // Null params should not cause exceptions and should not be included
    queryParams.put("name3", null);

    //making sure reserved characters are preserved where they should be and encoded in the query values
    String requestUri = client.relay(httpServletRequest, String.class, "rest/ci/componentDetails", queryParams).content;
    assertThat("&" + requestUri).contains(
        "&name2=%7B%22format%22%3A%22a-name%22%2C%22coordinates%22%3A%7B%22name%22%3A%22org.dojotoolkit"
            + "+dojo%22%2C%22qualifier%22%3A%22%22%2C%22version%22%3A%221.8.14%22%7D%7D",
        "&name1=%7B+%7D%2B%26%3B%2F%3F%3A%40%3D%3C%3E%23%25%7C%5C%5E~%5B%5D%60");
    assertThat("&" + requestUri).doesNotContain("name3");
  }

  private static class ServletInputStreamImpl
      extends ServletInputStream
  {
    // ByteArrayInputStream.close is a noop, so we don't need to close this stream
    private final ByteArrayInputStream wrappedInputStream;

    public ServletInputStreamImpl(byte[] data) {
      wrappedInputStream = new ByteArrayInputStream(data);
    }

    @Override
    public int read() {
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
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        headers.clear();
        headers.put(HdsClient.TELEMETRY_ID_HEADER, request.getHeader(HdsClient.TELEMETRY_ID_HEADER));
        response.setStatus(HttpStatus.OK_200);
      }
    };

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
    when(request.getMethod()).thenReturn("GET");

    client.relay(request, null, InputStream.class, testPath, null, new String[]{});
    assertThat(headers).containsEntry(HdsClient.TELEMETRY_ID_HEADER, telemetryId.getId());

    client.post(String.class, testPath, "foo", new String[]{});
    assertThat(headers).containsEntry(HdsClient.TELEMETRY_ID_HEADER, telemetryId.getId());

    client.post(testPath, MultipartEntityBuilder.create().build(), "test_client_user_agent");
    assertThat(headers).containsEntry(HdsClient.TELEMETRY_ID_HEADER, telemetryId.getId());

    client.put(null, String.class, null, testPath, new FileScanEntity(tempDir.newFile().toPath()),
        Collections.emptyMap(),
        new String[]{});
    assertThat(headers).containsEntry(HdsClient.TELEMETRY_ID_HEADER, telemetryId.getId());
  }

  @Test
  public void testClusterId() throws Exception {
    final Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

    String testPath = "/rest/test";

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
    when(request.getMethod()).thenReturn("GET");

    client.relay(request, null, InputStream.class, testPath, null, new String[]{});
    assertThat(headers).containsEntry(HdsClient.CLUSTER_ID_HEADER, telemetryId.getClusterId());

    client.post(String.class, testPath, "foo", new String[]{});
    assertThat(headers).containsEntry(HdsClient.CLUSTER_ID_HEADER, telemetryId.getClusterId());

    client.post(testPath, MultipartEntityBuilder.create().build(), "test_client_user_agent");
    assertThat(headers).containsEntry(HdsClient.CLUSTER_ID_HEADER, telemetryId.getClusterId());

    client.put(null, String.class, null, testPath, new FileScanEntity(tempDir.newFile().toPath()),
        Collections.emptyMap(),
        new String[]{});
    assertThat(headers).containsEntry(HdsClient.CLUSTER_ID_HEADER, telemetryId.getClusterId());
  }

  @Test
  public void testPost_Multipart() throws Exception {
    final int[] statusCode = new int[1];
    final BodyPart[] fileBodyReceived = new BodyPart[1];

    String testPath = "/rest/test";
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
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
      }
    };

    File fileSent = tempDir.newFile();
    FileUtils.write(fileSent, "Test", StandardCharsets.UTF_8);
    FileBody fileBodySent = new FileBody(fileSent);
    HttpEntity httpEntity = MultipartEntityBuilder.create().addPart("file", fileBodySent).build();
    client.post(testPath, httpEntity, "test_client_user_agent");
    assertThat(statusCode[0]).isEqualTo(HttpStatus.NO_CONTENT_204);
    assertThat(fileBodyReceived[0].getFileName()).isEqualTo(fileBodySent.getFilename());
    assertThat(IOUtils.toString(fileBodyReceived[0].getInputStream(), StandardCharsets.UTF_8)).isEqualTo("Test");
  }

  @Test
  public void testPut_QueryParams() throws Exception {
    final String[] queryString = {""};
    final String queryParam = "stageTypeId";
    final String queryParamValue = "testvalue";

    String testPath = "/rest/test";
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        queryString[0] = request.getQueryString();
      }
    };

    Map<String, String> testQueryParams = new HashMap<>();
    testQueryParams.put(queryParam, queryParamValue);

    client.put(null, String.class, "client_user_agent", testPath, new FileScanEntity(tempDir.newFile().toPath()), null);
    assertThat(queryString[0]).isNull();

    client.put(null, String.class, "client_user_agent", testPath, new FileScanEntity(tempDir.newFile().toPath()),
        testQueryParams);
    assertThat(queryString[0]).contains(queryParam + "=" + queryParamValue);

    // Null params should not cause exceptions and should not be included
    testQueryParams.put(queryParam, null);
    client.put(null, String.class, "client_user_agent", testPath, new FileScanEntity(tempDir.newFile().toPath()),
        testQueryParams);
    assertThat(queryString[0]).isNull();

    client.put(null, String.class, "client_user_agent", testPath, new FileScanEntity(tempDir.newFile().toPath()),
        Collections.emptyMap());
    assertThat(queryString[0]).isNull();
  }

  @Test
  public void testLabsProxy_Headers() throws Exception {
    final Map<String, String> headers = new HashMap<>();
    setBaseUrl("http://localhost:8070");
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        for (Enumeration<String> en = request.getHeaderNames(); en.hasMoreElements(); ) {
          String headerName = en.nextElement();
          headers.put(headerName, request.getHeader(headerName));
        }
      }
    };

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
    when(request.getMethod()).thenReturn("GET");
    when(request.getPathInfo()).thenReturn("/rest/labs");

    Application app = new Application();
    app.setId("test-app-id");
    Map<String, String> queryParams = new HashMap<>();
    queryParams.put("command", "command");
    queryParams.put("values", "values");
    client.forwardingProxy(request, queryParams);
    assertThat(headers).containsEntry("X-CLM-Token", "license-fingerprint");
    assertThat(headers).containsEntry("X-CLM-Instance-Id", telemetryId.getId());
  }

  @Test
  public void testDefaultHdsClient_NoDatabaseHdsUrl_NoInsightConfigHdsUrl() {
    config.setHdsUrl(null);
    setHdsUrl(null);

    initClient();

    verify(spyInsightProxy).contextualize(any(), eq("https://clm.sonatype.com/"));
  }

  @Test
  public void testDefaultHdsClient_NoDatabaseHdsUrl_InsightConfigHdsUrl() {
    String expected = "http://my-config-hds-url/";
    config.setHdsUrl(expected);
    setHdsUrl(null);

    initClient();

    verify(spyInsightProxy).contextualize(any(), eq(expected));
  }

  @Test
  public void testDefaultHdsClient_DatabaseHdsUrl_NoInsightConfigHdsUrl() {
    String expected = "http://my-db-hds-url/";
    config.setHdsUrl(null);
    setHdsUrl(expected);

    initClient();

    verify(spyInsightProxy).contextualize(any(), eq(expected));
  }

  @Test
  public void testDefaultHdsClient_DatabaseHdsUrl_InsightConfigHdsUrl() {
    String expected = "http://my-db-hds-url/";
    config.setHdsUrl("http://my-config-hds-url");
    setHdsUrl(expected);

    initClient();

    verify(spyInsightProxy).contextualize(any(), eq(expected));
  }

  @Test
  public void testExecute_DefaultRetry_BadGatewayErrorsOutEventually() {
    AtomicInteger requests = new AtomicInteger();
    List<String> queryStrings = new ArrayList<>();
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        requests.incrementAndGet();
        response.setStatus(HttpStatus.BAD_GATEWAY_502);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println("Some error message");
        queryStrings.add(request.getQueryString());
      }
    };

    assertThatThrownBy(
        () -> client.execute(HdsClient.DEFAULT_RETRY_CREATOR.apply("test"),
            new HttpGet(configuration.getHdsUrl())))
        .isInstanceOf(BadGatewayException.class);
    assertThat(requests.get()).isEqualTo(5);
    assertThat(queryStrings).containsExactly(null, "retryCount=1", "retryCount=2", "retryCount=3", "retryCount=4");
  }

  @Test
  public void testExecute_DefaultRetry_BadGatewayCanSucceed() {
    AtomicInteger requests = new AtomicInteger();
    List<String> queryStrings = new ArrayList<>();
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        requests.incrementAndGet();
        if (requests.get() > 1) {
          response.setStatus(HttpStatus.OK_200);
        }
        else {
          response.setStatus(HttpStatus.BAD_GATEWAY_502);
          response.setContentType("text/plain;charset=UTF-8");
          response.getWriter().println("Some error message");
        }
        queryStrings.add(request.getQueryString());
      }
    };

    client.execute(HdsClient.DEFAULT_RETRY_CREATOR.apply("test"), new HttpGet(configuration.getHdsUrl()));
    assertThat(requests.get()).isEqualTo(2);
    assertThat(queryStrings).containsExactly(null, "retryCount=1");
  }

  @Test
  public void testGet_BlockedTelemetry() {
    try {
      System.setProperty(HdsClient.DISABLE_TELEMETRY_CONFIG_KEY, "true");

      Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

      for (String telemetryUrl : HdsClient.TELEMETRY_URLS) {
        // If no request is made then there won't be any headers because the response is faked
        client.get(InputStream.class, telemetryUrl, null);
        assertThat(headers).isEmpty();
      }
    }
    finally {
      System.clearProperty(HdsClient.DISABLE_TELEMETRY_CONFIG_KEY);
    }
  }

  @Test
  public void testGet_InvalidProductLicense() throws Exception {
    when(mockProductLicense.isValid()).thenReturn(false);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      client.get(InputStream.class, "/rest/test", null, new String[]{});
    }).withMessage("The product license is invalid.");
  }

  @Test
  public void testPost_InvalidProductLicense() throws Exception {
    when(mockProductLicense.isValid()).thenReturn(false);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      client.post("/rest/test", new StringEntity(""), "test-client-user-agent");
    }).withMessage("The product license is invalid.");
  }

  @Test
  public void testPut_InvalidProductLicense() throws Exception {
    when(mockProductLicense.isValid()).thenReturn(false);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> {
      client.put(null, InputStream.class, "client_user_agent", "/rest/test",
          new FileScanEntity(new File(getClass().getResource("/config-test.yml").toURI()).toPath()),
          Collections.emptyMap(),
          new String[]{});
    }).withMessage("The product license is invalid.");
  }

  @Test
  public void testGetWithMultimap_ClientUserAgentOnRequests() {
    String testPath = "/rest/test";

    Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

    String clientUserAgent = "testClientUserAgent";
    com.google.common.collect.Multimap<String, String> queryParams = com.google.common.collect.HashMultimap.create();
    queryParams.put("refId", "CVE-2025-1");
    queryParams.put("refId", "CVE-2025-2");

    client.getWithMultimap(InputStream.class, testPath, clientUserAgent, queryParams, new String[]{});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(clientUserAgent);
  }

  @Test
  public void testGetWithMultimap_EncodeQueryParameters() throws Exception {
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException
      {
        response.setStatus(HttpStatus.OK_200);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println(request.getQueryString());
      }
    };

    com.google.common.collect.Multimap<String, String> queryParams = com.google.common.collect.HashMultimap.create();
    queryParams.put("refId", "CVE-2025-55182");
    queryParams.put("refId", "CVE-2025-55183");
    queryParams.put("filter", "{ }+&;/?:@=<>#%|\\^~[]`");

    String result = client.getWithMultimap(String.class, "/rest/vulnerability/affected", queryParams);

    assertThat(result).contains("refId=CVE-2025-55182");
    assertThat(result).contains("refId=CVE-2025-55183");
    assertThat(result).contains("filter=%7B+%7D%2B%26%3B%2F%3F%3A%40%3D%3C%3E%23%25%7C%5C%5E~%5B%5D%60");
  }

  @Test
  public void testGetWithMultimap_MultipleValuesForSameKey() throws Exception {
    final String[] queryString = {""};

    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) {
        queryString[0] = request.getQueryString();
      }
    };

    com.google.common.collect.Multimap<String, String> queryParams = com.google.common.collect.HashMultimap.create();
    queryParams.put("refId", "CVE-2025-1");
    queryParams.put("refId", "CVE-2025-2");
    queryParams.put("refId", "CVE-2025-3");

    client.getWithMultimap(InputStream.class, "/rest/test", queryParams);

    assertThat(queryString[0]).contains("refId=CVE-2025-1");
    assertThat(queryString[0]).contains("refId=CVE-2025-2");
    assertThat(queryString[0]).contains("refId=CVE-2025-3");
  }

  @Test
  public void testRelayWithMultimap_ClientUserAgentOnRequests() throws Exception {
    String testPath = "/rest/test";
    String testClientUserAgent = "client_user_agent";

    Map<String, String> headers = setHttpHeaderCaptorRequestHandler();

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singletonList(HttpHeaders.USER_AGENT)));
    when(request.getHeader(eq(HttpHeaders.USER_AGENT))).thenReturn(testClientUserAgent);
    when(request.getMethod()).thenReturn("GET");

    com.google.common.collect.Multimap<String, String> queryParams = com.google.common.collect.HashMultimap.create();
    queryParams.put("refId", "CVE-2025-1");
    queryParams.put("refId", "CVE-2025-2");

    client.relayWithMultimap(request, InputStream.class, testPath, queryParams, new String[]{});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);

    client.relayWithMultimap(request, null, InputStream.class, testPath, queryParams, new String[]{});
    assertThat(headers.get(HdsClient.CLM_CLIENT_USER_AGENT_HEADER)).isEqualTo(testClientUserAgent);
  }

  @Test
  public void testRelayWithMultimap_EncodeQueryParameters() throws Exception {
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.OK_200);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().println(request.getQueryString());
      }
    };

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getMethod()).thenReturn("GET");
    when(httpServletRequest.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));

    com.google.common.collect.Multimap<String, String> queryParams = com.google.common.collect.HashMultimap.create();
    queryParams.put("refId", "CVE-2025-55182");
    queryParams.put("refId", "CVE-2025-55183");
    queryParams.put("name", "{\"format\":\"a-name\",\"version\":\"1.8.14\"}");

    String requestUri = client.relayWithMultimap(httpServletRequest, String.class, "/rest/ci/componentDetails",
        queryParams).content;

    assertThat("&" + requestUri).contains("refId=CVE-2025-55182", "refId=CVE-2025-55183");
    assertThat("&" + requestUri).contains("name=%7B%22format%22%3A%22a-name%22%2C%22version%22%3A%221.8.14%22%7D");
  }
}
