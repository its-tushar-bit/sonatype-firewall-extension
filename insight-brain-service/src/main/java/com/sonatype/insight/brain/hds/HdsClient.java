/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import static com.sonatype.insight.brain.common.config.ConfigUtil.getBooleanConfig;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.ENABLE_FEDRAMP_AUDIT;
import static com.sonatype.insight.brain.security.CurrentUser.ANONYMOUS;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.sonatype.insight.brain.model.configuration.ReverseProxyAuthenticationConfiguration;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.GatewayTimeoutException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.json.store.JsonUtils;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.net.ssl.SSLException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sonatype.insight.brain.lifecycle.Managed;
import org.springframework.context.annotation.Primary;

/**
 * HTTP client for accessing Sonatype Data Services.
 */
@Named
@Singleton
@Primary // Required: subclasses (PingHdsClient, FirewallQuarantineHdsClient, etc.) are also beans,
         // so injection points that request the base HdsClient type need @Primary to disambiguate.
public class HdsClient
    implements Managed
{
  // Logger is instance variable so that subclasses will have a different one which can be configured differently
  private final Logger log = LoggerFactory.getLogger(getClass());

  private volatile HttpClientUtils.Configuration config;

  private volatile CloseableHttpClient client;

  private final int connectionPoolSize;

  private final InsightProxy proxy;

  private final ProductLicense productLicense;

  private final TelemetryId telemetryId;

  private final VersionService versionService;

  private final Configuration configuration;

  private final CurrentUser currentUser;

  private final Function<String, Retry> retryCreator;

  // Cache the FedRAMP audit feature flag check for 5 minutes. Without this, every HDS POST issued by the telemetry
  // submitter triggers a SystemConfigurationPropertyDAO.getByName DB call inside maybeAddUsernameHeader; if the
  // connection pool is contended the submitter blocks indefinitely on borrowObject and the telemetry queue grows
  // unbounded (CLM-40144). Remove once #15913 (general SystemConfigurationProperty caching) is merged.
  private final Supplier<Boolean> fedRampAuditEnabled = Suppliers.memoizeWithExpiration(
      ENABLE_FEDRAMP_AUDIT::isEnabled, Duration.ofMinutes(5));

  private static volatile String version;

  public static final String UPLOAD_FILE_ATTRIBUTE = "hds.upload.file";

  public static final String CLM_CLIENT_USER_AGENT_HEADER = "X-CLM-Client-User-Agent";

  public static final String GET_PRODUCT_LICENSE_DETAILS_HDS_PATH = "rest/productLicense/v1";

  // Visible for testing
  static final Function<String, Retry> DEFAULT_RETRY_CREATOR =
      name -> new Retry(name, 4, null, BadGatewayException.class::isInstance, i -> Duration.ofSeconds(1));

  static final String OWNER_TYPE_HEADER = "X-CLM-Owner-Type";

  static final String OWNER_ID_HEADER = "X-CLM-Owner-Id";

  static final String TELEMETRY_ID_HEADER = "X-CLM-Instance-Id";

  static final String CLUSTER_ID_HEADER = "X-CLM-Cluster-Id";

  static final String USERNAME_HEADER = "X-CLM-Username";

  public static final String DISABLE_TELEMETRY_CONFIG_KEY = "com.sonatype.insight.disableOutboundTelemetryRequests";

  static final List<String> TELEMETRY_URLS = ImmutableList.of("environment/stats", "user-telemetry");

  public static final String CLIENT_INSTANCE_ID_HEADER = "X-CLM-Client-Instance-Id";
  // VisibleForTesting

  public static boolean waitToCloseOldClients = true;

  @Inject
  public HdsClient(
      InsightProxy proxy,
      ProductLicense productLicense,
      Configuration configuration,
      VersionService versionService,
      TelemetryId telemetryId,
      CurrentUser currentUser)
  {
    this(proxy, productLicense, configuration, versionService, telemetryId, currentUser, 20,
        DEFAULT_RETRY_CREATOR);
  }

  protected HdsClient(
      InsightProxy proxy,
      ProductLicense productLicense,
      Configuration configuration,
      VersionService versionService,
      TelemetryId telemetryId,
      CurrentUser currentUser,
      int poolSize)
  {
    this(proxy, productLicense, configuration, versionService, telemetryId, currentUser, poolSize,
        DEFAULT_RETRY_CREATOR);
  }

  protected HdsClient(
      InsightProxy proxy,
      ProductLicense productLicense,
      Configuration configuration,
      VersionService versionService,
      TelemetryId telemetryId,
      CurrentUser currentUser,
      int poolSize,
      Function<String, Retry> retryCreator)
  {
    this.proxy = proxy;
    this.productLicense = productLicense;
    connectionPoolSize = poolSize;
    this.versionService = versionService;
    this.configuration = configuration;
    this.telemetryId = telemetryId;
    this.currentUser = currentUser;
    this.retryCreator = retryCreator;
    updateClient();
    // TODO Need to determine if there is additional information we should be sending to the HDS
    loadVersion();
  }

  private String getRutHeader() {
    ReverseProxyAuthenticationConfiguration reverseProxyAuthenticationConfiguration =
        configuration.getReverseProxyAuthenticationConfiguration();
    return reverseProxyAuthenticationConfiguration != null && reverseProxyAuthenticationConfiguration.isEnabled()
        ? reverseProxyAuthenticationConfiguration.getUsernameHeader()
        : null;
  }

  private synchronized void updateClient() {
    HttpClientUtils.Configuration config = new HttpClientUtils.Configuration();
    config.setConnectTimeout(configuration.getConnectTimeoutInSeconds() * 1000);
    config.setSocketTimeout(configuration.getSocketTimeoutInSeconds() * 1000);
    customizeConfiguration(config);
    proxy.contextualize(config);
    log.debug("HDS URL: {}", config.getServerUrl());
    this.config = config;
    HttpClientBuilder clientBuilder = HttpClientUtils.create(config);
    clientBuilder.setMaxConnTotal(connectionPoolSize);
    clientBuilder.setMaxConnPerRoute(connectionPoolSize);
    clientBuilder.evictIdleConnections(30, TimeUnit.SECONDS);
    CloseableHttpClient oldClient = client;
    client = clientBuilder.build();
    if (oldClient != null) {
      new Thread("HttpClientCloser")
      {
        @Override
        public void run() {
          try {
            // If we wait in a unit test then this leads to the thread count increasing massively as each test
            // ultimately triggers a reset of the client.
            if (waitToCloseOldClients) {
              Thread.sleep(TimeUnit.MINUTES.toMillis(15));
            }
            // hopefully by now, the old connections are unused
            oldClient.close();
          }
          catch (Exception e) {
            log.error("Failed to cleanly shutdown obsolete HTTP client", e);
          }
        }
      }.start();
    }
  }

  protected void customizeConfiguration(@SuppressWarnings("unused") HttpClientUtils.Configuration configuration) {
  }

  /**
   * Exposes the business {@link Configuration} bean to subclasses (e.g. so {@link #customizeConfiguration}
   * overrides can read their own dedicated, admin-configurable properties instead of hardcoded constants).
   */
  protected Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public void stop() throws Exception {
    client.close();
  }

  public void serverConfigurationChanged() {
    updateClient();
    log.debug("Applied new server configuration");
  }

  public <T> T get(Class<T> clazz, String path, Map<String, String> queryParams, String... uriParams) {
    return get(retryCreator.apply(path), clazz, path, queryParams, uriParams);
  }

  public <T> T get(Retry retry, Class<T> clazz, String path, Map<String, String> queryParams, String... uriParams) {
    return internalGet(retry, clazz, buildUri(null, path, queryParams, uriParams), null /* clientUserAgent */);
  }

  public <T> T get(
      Class<T> clazz,
      String path,
      String clientUserAgent,
      Map<String, String> queryParams,
      String... uriParams)
  {
    return get(retryCreator.apply(path), clazz, path, clientUserAgent, queryParams, uriParams);
  }

  public <T> T get(
      Retry retry,
      Class<T> clazz,
      String path,
      String clientUserAgent,
      Map<String, String> queryParams,
      String... uriParams)
  {
    return internalGet(retry, clazz, buildUri(null, path, queryParams, uriParams), clientUserAgent);
  }

  public <T> T getWithMultimap(
      Class<T> clazz,
      String path,
      Multimap<String, String> queryParams,
      String... uriParams)
  {
    return getWithMultimap(retryCreator.apply(path), clazz, path, queryParams, uriParams);
  }

  public <T> T getWithMultimap(
      Retry retry,
      Class<T> clazz,
      String path,
      Multimap<String, String> queryParams,
      String... uriParams)
  {
    return internalGet(retry, clazz, buildUriWithMultimap(null, path, queryParams, uriParams), null);
  }

  public <T> T getWithMultimap(
      Class<T> clazz,
      String path,
      String clientUserAgent,
      Multimap<String, String> queryParams,
      String... uriParams)
  {
    return getWithMultimap(retryCreator.apply(path), clazz, path, clientUserAgent, queryParams, uriParams);
  }

  public <T> T getWithMultimap(
      Retry retry,
      Class<T> clazz,
      String path,
      String clientUserAgent,
      Multimap<String, String> queryParams,
      String... uriParams)
  {
    return internalGet(retry, clazz, buildUriWithMultimap(null, path, queryParams, uriParams), clientUserAgent);
  }

  public <T> T get(Class<T> clazz, String url) {
    return get(retryCreator.apply(url), clazz, url);
  }

  public <T> T get(Retry retry, Class<T> clazz, String url) {
    return internalGet(retry, clazz, buildUri(url), null /* clientUserAgent */);
  }

  private <T> T internalGet(Retry retry, Class<T> clazz, String url, String clientUserAgent) {
    HttpGet cloudReq = createGetRequest(url, null, null);
    setClientUserAgentHeader(cloudReq, clientUserAgent);
    return execute(retry, cloudReq, clazz);
  }

  public <T> RelayResponse<T> relay(
      HttpServletRequest request,
      Class<T> clazz,
      String path,
      String... uriParams) throws IOException
  {
    return relay(retryCreator.apply(path), request, clazz, path, uriParams);
  }

  public <T> RelayResponse<T> relay(
      Retry retry,
      HttpServletRequest request,
      Class<T> clazz,
      String path,
      String... uriParams) throws IOException
  {
    return relay(retry, request, clazz, path, null, uriParams);
  }

  public <T> RelayResponse<T> relay(
      HttpServletRequest request,
      Class<T> clazz,
      String path,
      Map<String, String> queryParams,
      String... uriParams) throws IOException
  {
    return relay(retryCreator.apply(path), request, clazz, path, queryParams, uriParams);
  }

  public <T> RelayResponse<T> relay(
      Retry retry,
      HttpServletRequest request,
      Class<T> clazz,
      String path,
      Map<String, String> queryParams,
      String... uriParams) throws IOException
  {
    return relay(retry, request, null, clazz, path, queryParams, uriParams);
  }

  public <T> RelayResponse<T> relay(
      HttpServletRequest request,
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String path,
      Map<String, String> queryParams,
      String... uriParams) throws IOException
  {
    return relay(retryCreator.apply(path), request, analytics, clazz, path, queryParams, uriParams);
  }

  public <T> RelayResponse<T> relay(
      Retry retry,
      HttpServletRequest request,
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String path,
      Map<String, String> queryParams,
      String... uriParams) throws IOException
  {
    String url = buildUri(request, path, queryParams, uriParams);
    HttpUriRequest cloudReq = createRequest(request, url, analytics);
    HttpResponse response = execute(retry, cloudReq);
    RelayResponse<T> relayResponse = new RelayResponse<>(fromHttpResponse(response, clazz));
    if (response.getEntity() != null && response.getEntity().getContentType() != null) {
      relayResponse.contentType = response.getEntity().getContentType().getValue();
    }
    return relayResponse;
  }

  public <T> RelayResponse<T> relayWithMultimap(
      HttpServletRequest request,
      Class<T> clazz,
      String path,
      Multimap<String, String> queryParams,
      String... uriParams) throws IOException
  {
    return relayWithMultimap(retryCreator.apply(path), request, clazz, path, queryParams, uriParams);
  }

  public <T> RelayResponse<T> relayWithMultimap(
      Retry retry,
      HttpServletRequest request,
      Class<T> clazz,
      String path,
      Multimap<String, String> queryParams,
      String... uriParams) throws IOException
  {
    return relayWithMultimap(retry, request, null, clazz, path, queryParams, uriParams);
  }

  public <T> RelayResponse<T> relayWithMultimap(
      HttpServletRequest request,
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String path,
      Multimap<String, String> queryParams,
      String... uriParams) throws IOException
  {
    return relayWithMultimap(retryCreator.apply(path), request, analytics, clazz, path, queryParams, uriParams);
  }

  public <T> RelayResponse<T> relayWithMultimap(
      Retry retry,
      HttpServletRequest request,
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String path,
      Multimap<String, String> queryParams,
      String... uriParams) throws IOException
  {
    String url = buildUriWithMultimap(request, path, queryParams, uriParams);
    HttpUriRequest cloudReq = createRequest(request, url, analytics);
    HttpResponse response = execute(retry, cloudReq);
    RelayResponse<T> relayResponse = new RelayResponse<>(fromHttpResponse(response, clazz));
    if (response.getEntity() != null && response.getEntity().getContentType() != null) {
      relayResponse.contentType = response.getEntity().getContentType().getValue();
    }
    return relayResponse;
  }

  public HttpResponse forwardingProxy(HttpServletRequest request, Map<String, String> queryParams) throws IOException {
    return forwardingProxy(retryCreator.apply(request.toString()), request, queryParams);
  }

  public HttpResponse forwardingProxy(
      Retry retry,
      HttpServletRequest request,
      Map<String, String> queryParams) throws IOException
  {
    String url = buildUri(request, getForwardingProxyPath(request), queryParams);
    HttpUriRequest labReq = createRequest(request, url, null);
    return execute(retry, labReq);
  }

  private String getForwardingProxyPath(HttpServletRequest request) {
    String path = request.getPathInfo();
    if (path != null) {
      return path;
    }

    String requestUri = request.getRequestURI();
    if (requestUri == null) {
      return request.getServletPath();
    }

    String contextPath = request.getContextPath();
    if (contextPath != null && requestUri.startsWith(contextPath)) {
      return requestUri.substring(contextPath.length());
    }
    return requestUri;
  }

  private <T> T fromHttpResponse(HttpResponse response, Class<T> clazz) {
    boolean usingStream = false;
    try {
      HttpEntity entity = response.getEntity();
      if (entity == null) {
        return null;
      }
      else if (clazz == null) {
        return null;
      }
      else if (String.class.equals(clazz)) {
        return clazz.cast(EntityUtils.toString(entity, StandardCharsets.UTF_8));
      }
      else if (InputStream.class.equals(clazz)) {
        usingStream = true;
        return clazz.cast(entity.getContent());
      }
      else {
        byte[] content = EntityUtils.toByteArray(entity);
        if (content == null || content.length == 0) {
          return null;
        }
        return JsonUtils.parse(content, clazz);
      }
    }
    catch (IOException e) {
      log.error("Failed to read response entity: {}", e.getMessage(), e);
      throw new BadGatewayException("Failed to read response entity received from Sonatype Data Services, please " +
          "retry in a bit.");
    }
    finally {
      if (!usingStream) {
        try {
          EntityUtils.consume(response.getEntity());
        }
        catch (IOException e) {
          log.error("Failed to consume response entity", e);
        }
      }
    }
  }

  private void throwErrorIfNeeded(HttpResponse response) {
    try {
      int status = response.getStatusLine().getStatusCode();
      switch (status) {
        case 200:
        case 201:
        case 202:
        case 204:
          return;
        case 400:
          throw new BadRequestException(getErrorMessage(response));
        case 401:
        case 403:
        case 407:
          // The HDS don't require auth, so these errors indicate bad proxy or URL config
          throw new BadGatewayException(
              "Could not contact Sonatype Data Services, please verify the network configuration of your Nexus IQ " +
                  "Server. Sonatype Data Services error " + status + ": " + getErrorMessage(response));
        case 402:
          throw new PaymentRequiredException(getErrorMessage(response));
        case 404:
          throw new NotFoundException(getErrorMessage(response));
        case 409:
          throw new ConflictException(getErrorMessage(response));
        case 500:
          throw new InternalServerErrorException(
              "The Sonatype Data Services returned error 500, please retry in a bit.");
        case 502: // Bad Gateway
        case 504: // Gateway Timeout
          throw new BadGatewayException(
              "Could not contact Sonatype Data Services, please verify the network configuration of your Nexus IQ "
                  + "Server. Sonatype Data Services error " + status
                  + formatRequestId(" (request %s)", getRequestId(response)) + ": " + getErrorMessage(response));
        case 503: // Service Unavailable
          throw new BadGatewayException("The Sonatype Data Services are currently out of service"
              + formatRequestId(" (request %s)", getRequestId(response)) + ", please retry in a bit. If the outage "
              + "persists, please verify the network configuration of your Nexus IQ Server "
              + "and contact Sonatype Support.");
        default:
          // Since this is for any other errors, the error message may contain anything, so log it, but don't send it
          // back to the client.
          log.error("Sonatype Data Services error " + status + ": " + getErrorMessage(response));
          throw new BadGatewayException("The Sonatype Data Services returned error " + status
              + formatRequestId(" (request %s)", getRequestId(response)) + ", please retry in a bit.");
      }
    }
    catch (RuntimeException e) {
      EntityUtils.consumeQuietly(response.getEntity());
      throw e;
    }
  }

  @VisibleForTesting
  String getErrorMessage(HttpResponse response) {
    Header hdr = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
    if (hdr != null && hdr.getValue() != null && hdr.getValue().contains(ContentType.TEXT_PLAIN.getMimeType())
        && response.getEntity() != null)
    {
      try {
        return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      }
      catch (Exception e) {
        log.error("Failed to read entity: {}, from response with status: {}", e.getMessage(), response.getStatusLine(),
            e);
      }
    }
    return response.getStatusLine().getReasonPhrase();
  }

  private HttpUriRequest createRequest(
      HttpServletRequest request,
      String url,
      HdsClientAnalytics analytics) throws IOException
  {
    HttpUriRequest cloudReq;
    if (request == null || "GET".equals(request.getMethod())) {
      cloudReq = new HttpGet(url);
    }
    else if ("POST".equals(request.getMethod())) {
      cloudReq = new HttpPost(url);

      ((HttpPost) cloudReq).setEntity(buildEntity(request));
    }
    else if ("PUT".equals(request.getMethod())) {
      cloudReq = new HttpPut(url);

      ((HttpPut) cloudReq).setEntity(buildEntity(request));
    }
    else if ("DELETE".equals(request.getMethod())) {
      cloudReq = new HttpDelete(url);
    }
    else {
      throw new IllegalArgumentException("Unknown request method " + request.getMethod());
    }
    populateRequest(request, cloudReq, analytics);
    return cloudReq;
  }

  public void post(String path, HttpEntity httpEntity, String clientUserAgent) {
    post(retryCreator.apply(path), path, httpEntity, clientUserAgent);
  }

  public <T> T post(Class<T> clazz, String path, HttpEntity httpEntity, String clientUserAgent) {
    HttpPost cloudReq = createPostRequest(buildUri(path), null, clientUserAgent);
    cloudReq.setEntity(httpEntity);
    return execute(retryCreator.apply(path), cloudReq, clazz);
  }

  public void post(Retry retry, String path, HttpEntity httpEntity, String clientUserAgent) {
    HttpPost cloudReq = createPostRequest(buildUri(path), null, clientUserAgent);
    cloudReq.setEntity(httpEntity);
    execute(retry, cloudReq, null);
  }

  public <T> T post(Class<T> clazz, String path, Object jsonSerializableObject, String... uriParams) {
    return post(retryCreator.apply(path), clazz, path, jsonSerializableObject, uriParams);
  }

  public <T> T post(
      Class<T> clazz,
      String path,
      HttpEntity httpEntity,
      Map<String, String> queryParams,
      String clientUserAgent)
  {
    HttpPost cloudReq = createPostRequest(buildUri(path, queryParams), null, clientUserAgent);
    cloudReq.setEntity(httpEntity);
    return execute(retryCreator.apply(path), cloudReq, clazz);
  }

  public <T> T post(Retry retry, Class<T> clazz, String path, Object jsonSerializableObject, String... uriParams) {
    return post(retry, null /* analytics */, clazz, path, null /* clientUserAgent */, jsonSerializableObject,
        Map.of(), uriParams);
  }

  private HttpGet createGetRequest(String url, HdsClientAnalytics analytics, String clientUserAgent) {
    return createGetRequest(url, analytics, null, clientUserAgent);
  }

  private HttpGet createGetRequest(
      String url,
      HdsClientAnalytics analytics,
      HttpServletRequest request,
      String clientUserAgent)
  {
    HttpGet cloudReq = new HttpGet(url);
    populateRequest(request, cloudReq, analytics);
    setClientUserAgentHeader(cloudReq, clientUserAgent);
    return cloudReq;
  }

  public <T> T post(
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String path,
      final String clientUserAgent,
      Object jsonSerializableObject,
      String... uriParams)
  {
    return post(retryCreator.apply(path), analytics, clazz, path, clientUserAgent, jsonSerializableObject,
        Map.of(), uriParams);
  }

  public <T> T post(
      Retry retry,
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String path,
      final String clientUserAgent,
      Object jsonSerializableObject,
      Map<String, String> queryParams,
      String... uriParams)
  {
    HttpPost cloudReq = createPostRequest(buildUri(path, queryParams, uriParams), analytics, clientUserAgent);
    HttpEntity entity;
    if (jsonSerializableObject instanceof byte[]) {
      entity = new ByteArrayEntity((byte[]) jsonSerializableObject, ContentType.APPLICATION_OCTET_STREAM);
    }
    else {
      entity = new StringEntity(JsonUtils.format(jsonSerializableObject), ContentType.APPLICATION_JSON);
    }
    cloudReq.setEntity(entity);
    cloudReq.setHeader(HttpHeaders.ACCEPT, "application/json");

    return execute(retry, cloudReq, clazz, jsonSerializableObject);
  }

  public <T> T post(
      Class<T> clazz,
      String path,
      Object jsonSerializableObject,
      Map<String, String> queryParams)
  {
    return post(retryCreator.apply(path), null, clazz, path, null, jsonSerializableObject,
        queryParams);
  }

  private HttpPut createPutRequest(String url, HdsClientAnalytics analytics, String clientUserAgent) {
    HttpPut cloudReq = new HttpPut(url);
    populateRequest(null, cloudReq, analytics);
    setClientUserAgentHeader(cloudReq, clientUserAgent);
    return cloudReq;
  }

  public <T> T put(
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String clientUserAgent,
      String path,
      ScanEntity uploadScanEntity,
      Map<String, String> queryParams,
      String... uriParams) throws IOException
  {
    return put(retryCreator.apply(path), analytics, clazz, clientUserAgent, path, uploadScanEntity, queryParams,
        uriParams);
  }

  public <T> T put(Class<T> clazz, String path, HttpEntity httpEntity, String clientUserAgent) {
    HttpPut cloudReq = createPutRequest(buildUri(path), null, clientUserAgent);
    cloudReq.setEntity(httpEntity);
    return execute(retryCreator.apply(path), cloudReq, clazz);
  }

  public <T> T put(
      Retry retry,
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String clientUserAgent,
      String path,
      ScanEntity scanEntity,
      Map<String, String> queryParams,
      String... uriParams) throws IOException
  {
    if (!scanEntity.exists()) {
      throw new RuntimeException("Missing scan " + scanEntity.getLocation());
    }

    HttpPut cloudReq = createPutRequest(buildUri(null, path, queryParams, uriParams), analytics, clientUserAgent);
    cloudReq.setEntity(
        new BufferedHttpEntity(new InputStreamEntity(scanEntity.getInputStream(), ContentType.DEFAULT_BINARY)));
    return execute(retry, cloudReq, clazz);
  }

  // Visible for testing
  HttpResponse execute(Retry retry, HttpUriRequest request) {
    AtomicInteger retryCount = new AtomicInteger();
    return retry.executeSupplier(() -> execute(request, retryCount.getAndIncrement()));
  }

  /**
   * Validates the product license if needed - i.e. for HDS requests that require a product license.
   *
   * The requests that do require a product license, pass it to HDS via the "X-CLM-Token" http header.
   * This method assumes that the above header was already set on the request param.
   */
  private void validateProductLicenseIfNeeded(HttpUriRequest request) {
    Header productLicenseHeader = request.getFirstHeader("X-CLM-Token");
    // If the request is for product license details, the currently installed license may be invalid/expired,
    // so don't check it. The HDS does not require a product license for this request anyway.
    String path = request.getURI().getPath();
    if (path.endsWith(GET_PRODUCT_LICENSE_DETAILS_HDS_PATH)) {
      return;
    }

    // Reference policies do not require a product license and may be requested before a license is installed
    if (path.endsWith(ReferencePolicyFetcher.REFERENCE_POLICY_PATH)) {
      return;
    }

    // License data does not require a product license and may be requested before a license is installed
    if (path.endsWith(DefaultLicenseDataUpdater.HDS_LICENSE_PATH)) {
      return;
    }

    // Prevent requests with null or blank license tokens from reaching HDS
    // These are guaranteed to fail with 402, and too many failures can cause HDS to cache
    // the 402 response for the SaaS IP, affecting other tenants with valid licenses
    if (productLicenseHeader == null || StringUtils.isBlank(productLicenseHeader.getValue()) ||
        !productLicense.isValid())
    {
      throw new InvalidLicenseException("The product license is invalid.");
    }
  }

  private HttpResponse execute(HttpUriRequest request, int retryCount) {
    validateProductLicenseIfNeeded(request);

    HttpRequestWrapper wrapper = HttpRequestWrapper.wrap(request);
    if (retryCount > 0) {
      wrapper.setURI(UriBuilder.fromUri(request.getURI()).queryParam("retryCount", retryCount).build());
    }
    log.debug("Starting request: {} {}, {}, {}", wrapper.getMethod(), wrapper.getURI(),
        request.getFirstHeader("X-CLM-Token"), request.getFirstHeader(CLUSTER_ID_HEADER));
    long start = System.currentTimeMillis();
    StatusLine statusLine = null;
    String requestId = null;
    HttpResponse response;
    try {
      response = getResponse(wrapper);
      requestId = getRequestId(response);
      statusLine = response.getStatusLine();
    }
    catch (HttpHostConnectException | ConnectTimeoutException e) {
      throw new GatewayTimeoutException(e.getMessage(), e);
    }
    catch (UnknownHostException e) {
      throw new BadGatewayException("The hostname for the Sonatype Data Services could not be resolved, "
          + "please verify the network configuration (DNS) at the site where the Nexus IQ Server is operated", e);
    }
    catch (SSLException e) {
      throw new BadGatewayException("The SSL/TLS connection to Sonatype Data Services could not be established, "
          + "contact your network or system administrator for help.", e);
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new BadGatewayException("The request to Sonatype Data Services failed, please retry in a bit.", e);
    }
    finally {
      log.debug("Completed request{} in {} ms. {}", formatRequestId(" %s", requestId),
          System.currentTimeMillis() - start, statusLine != null ? statusLine.getStatusCode() : "");
    }
    throwErrorIfNeeded(response);
    return response;
  }

  public HttpResponse getResponse(HttpUriRequest request) throws IOException {
    String uri = request.getURI().toString();

    boolean disableOutboundTelemetryRequests =
        getBooleanConfig(DISABLE_TELEMETRY_CONFIG_KEY, false);

    if (disableOutboundTelemetryRequests) {
      if (TELEMETRY_URLS.stream().anyMatch(uri::contains)) {
        return new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK"));
      }
    }

    return client.execute(request);
  }

  private <T> T execute(Retry retry, HttpUriRequest request, Class<T> clazz) {
    return execute(retry, request, clazz, null);
  }

  private <T> T execute(Retry retry, HttpUriRequest request, Class<T> clazz, @Nullable Object requestBody) {
    HttpResponse response = execute(retry, request);
    T result = fromHttpResponse(response, clazz);
    return result;
  }

  private String getRequestId(HttpResponse response) {
    Header header = response.getFirstHeader("X-Amz-Cf-Id");
    if (header == null) {
      header = response.getFirstHeader("X-Amzn-Trace-Id");
    }
    return header != null ? header.getValue() : null;
  }

  private String formatRequestId(String format, String requestId) {
    return requestId != null ? String.format(format, requestId) : "";
  }

  private HttpPost createPostRequest(String url, HdsClientAnalytics analytics, String clientUserAgent) {
    HttpPost cloudReq = new HttpPost(url);
    populateRequest(null, cloudReq, analytics);
    setClientUserAgentHeader(cloudReq, clientUserAgent);
    return cloudReq;
  }

  private HttpEntity buildEntity(HttpServletRequest request) throws IOException {
    File uploadFile = (File) request.getAttribute(UPLOAD_FILE_ATTRIBUTE);
    if (uploadFile != null) {
      ContentType contentType = request.getContentType() != null
          ? ContentType.create(request.getContentType())
          : ContentType.DEFAULT_BINARY;
      return new FileEntity(uploadFile, contentType);
    }

    return new BufferedHttpEntity(new InputStreamEntity(request.getInputStream()));
  }

  private void populateRequest(final HttpServletRequest orig, HttpUriRequest req, HdsClientAnalytics analytics) {
    if (orig != null) {
      for (Enumeration<String> e = orig.getHeaderNames(); e.hasMoreElements();) {
        String headerName = e.nextElement();
        if (!HttpHeaders.CONNECTION.equalsIgnoreCase(headerName) && !HttpHeaders.HOST.equalsIgnoreCase(headerName)
            && !HttpHeaders.ACCEPT_ENCODING.equalsIgnoreCase(headerName)
            && !HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(headerName)
            && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName)
            && !HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase(headerName)
            && !HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName)
            && !HttpHeaders.PROXY_AUTHORIZATION.equalsIgnoreCase(headerName) && !"COOKIE".equalsIgnoreCase(headerName)
            && !"COOKIE2".equalsIgnoreCase(headerName) && !headerName.equalsIgnoreCase(getRutHeader())
            && !headerName.startsWith("X-Forward"))
        {
          req.setHeader(headerName, orig.getHeader(headerName));
        }
      }
    }
    if (analytics != null) {
      req.setHeader(OWNER_TYPE_HEADER, analytics.getOwnerType().toString());
      req.setHeader(OWNER_ID_HEADER, analytics.getOwnerId());
    }

    String telemetryIdString = telemetryId.getId();
    if (telemetryIdString != null) {
      req.setHeader(TELEMETRY_ID_HEADER, telemetryIdString);
    }

    String clusterId = telemetryId.getClusterId();
    if (clusterId != null) {
      req.setHeader(CLUSTER_ID_HEADER, clusterId);
    }

    req.setHeader("X-Brain-Version", version);
    req.setHeader("X-CLM-Token", productLicense.getFingerprint());

    if (productLicense.isValid()) {
      telemetryId.flushPendingTelemetry();
    }

    maybeAddUsernameHeader(req);

    populateUserAgents(orig, req);
  }

  public static String getClientUserAgent(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    // some clients can't control the actual UA header and use an alternative header
    String clientUserAgent = request.getHeader(CLM_CLIENT_USER_AGENT_HEADER);
    if (clientUserAgent == null) {
      clientUserAgent = request.getHeader(HttpHeaders.USER_AGENT);
    }
    return clientUserAgent;
  }

  public static String getClientInstanceId(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    return request.getHeader(CLIENT_INSTANCE_ID_HEADER);
  }

  protected void populateUserAgents(HttpServletRequest orig, HttpUriRequest req) {
    if (orig != null) {
      setClientUserAgentHeader(req, getClientUserAgent(orig));
    }
    req.setHeader(HttpHeaders.USER_AGENT, config.getUserAgent());
  }

  private void setClientUserAgentHeader(HttpUriRequest request, String clientUserAgent) {
    if (clientUserAgent != null) {
      request.setHeader(CLM_CLIENT_USER_AGENT_HEADER, clientUserAgent);
    }
  }

  private String buildUri(String path, String... uriParams) {
    return buildUri(null /* base request */, path, null /* queryParams */, uriParams);
  }

  private String buildUri(String path, Map<String, String> queryParams, String... uriParams) {
    return buildUri(null /* base request */, path, queryParams, uriParams);
  }

  private String buildUri(HttpServletRequest base, String path, Map<String, String> queryParams, String... uriParams) {
    UriBuilder uriBuilder = UriBuilder.fromUri(config.getServerUrl());
    uriBuilder.path(path);
    if (base != null && queryParams == null) {
      uriBuilder.replaceQuery(base.getQueryString());
    }

    if (queryParams != null) {
      for (Entry<String, String> queryParam : queryParams.entrySet()) {
        // Jersey 1.18+ sees the "{" and "}" (e.g. a JSON object) as defining a template parameter, to avoid that we
        // encode the curly braces

        String paramValue = queryParam.getValue();
        if (paramValue != null) {
          uriBuilder.queryParam(queryParam.getKey(), paramValue.replace("{", "%7B").replace("}", "%7D"));
        }
      }
    }

    return uriBuilder.build((Object[]) uriParams).toString();
  }

  private String buildUriWithMultimap(String path, Multimap<String, String> queryParams, String... uriParams) {
    return buildUriWithMultimap(null, path, queryParams, uriParams);
  }

  private String buildUriWithMultimap(
      HttpServletRequest base,
      String path,
      Multimap<String, String> queryParams,
      String... uriParams)
  {
    UriBuilder uriBuilder = UriBuilder.fromUri(config.getServerUrl());
    uriBuilder.path(path);
    if (base != null && queryParams == null) {
      uriBuilder.replaceQuery(base.getQueryString());
    }

    if (queryParams != null) {
      for (Entry<String, String> queryParam : queryParams.entries()) {
        String paramValue = queryParam.getValue();
        if (paramValue != null) {
          uriBuilder.queryParam(queryParam.getKey(), paramValue.replace("{", "%7B").replace("}", "%7D"));
        }
      }
    }

    return uriBuilder.build((Object[]) uriParams).toString();
  }

  private void loadVersion() {
    if (version != null) {
      return;
    }

    version = versionService.getVersion("Unknown");
  }

  private boolean isFedRampAuditEnabled() {
    try {
      // Guava memoization does not cache exceptions, so a transient failure does not pin this at false for the full
      // expiry window; the next request re-evaluates rather than staying disabled until the cache rolls over.
      return fedRampAuditEnabled.get();
    }
    catch (Exception e) {
      log.warn("Failed to check FedRAMP audit config, defaulting to disabled", e);
      return false;
    }
  }

  private void maybeAddUsernameHeader(final HttpUriRequest req) {
    if (currentUser == null || !isFedRampAuditEnabled()) {
      return;
    }

    try {
      // CLM-35793 - Add user header for FedRAMP audit logging
      req.setHeader(USERNAME_HEADER, currentUser.isAnonymous() ? ANONYMOUS : currentUser.getUsername());
    }
    // catch any trouble with setting header/getting the username, which should not prevent a request from succeeding
    // this is purely for doing our best to Audit for FedRamp, but should not block requests.
    catch (Exception e) {
      log.debug("Could not set header {}", USERNAME_HEADER, e);
    }
  }

  public static class RelayResponse<T>
  {
    public T content;

    public String contentType;

    public RelayResponse(T content) {
      this(content, null);
    }

    public RelayResponse(T content, String contentType) {
      this.content = content;
      this.contentType = contentType;
    }
  }
}
