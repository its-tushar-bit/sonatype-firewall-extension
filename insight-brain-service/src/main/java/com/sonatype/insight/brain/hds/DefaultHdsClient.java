/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.api.v2.service.ProxyServerConfigurationListener;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.GatewayTimeoutException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.lifecycle.Managed;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client for accessing Sonatype Data Services.
 */
@Named
@Singleton
public class DefaultHdsClient
    implements HdsClient, Managed, ProxyServerConfigurationListener
{
  // Logger is instance variable so that subclasses will have a different one which can be configured differently
  private final Logger log = LoggerFactory.getLogger(getClass());

  private volatile Configuration config;

  private volatile CloseableHttpClient client;

  private final int connectionPoolSize;

  private final InsightProxy proxy;

  private final InsightConfig insightConfig;

  private final ProductLicense productLicense;

  private final TelemetryId telemetryId;

  private final VersionService versionService;

  private static volatile String version;

  public static final String UPLOAD_FILE_ATTRIBUTE = "hds.upload.file";

  public static final String CLM_CLIENT_USER_AGENT_HEADER = "X-CLM-Client-User-Agent";

  private final String rutHeader;

  static final String OWNER_TYPE_HEADER = "X-CLM-Owner-Type";

  static final String OWNER_ID_HEADER = "X-CLM-Owner-Id";

  static final String TELEMETRY_ID_HEADER = "X-CLM-Instance-Id";

  @Inject
  public DefaultHdsClient(final InsightProxy proxy,
                          ProductLicense productLicense,
                          InsightConfig insightConfig,
                          VersionService versionService,
                          TelemetryId telemetryId)
  {
    this(proxy, productLicense, insightConfig, versionService, telemetryId, 20);
  }

  protected DefaultHdsClient(final InsightProxy proxy,
                             ProductLicense productLicense,
                             InsightConfig insightConfig,
                             VersionService versionService,
                             TelemetryId telemetryId,
                             int poolSize)
  {
    this.proxy = proxy;
    this.productLicense = productLicense;
    this.insightConfig = insightConfig;
    connectionPoolSize = poolSize;
    updateClient();
    this.versionService = versionService;
    rutHeader = insightConfig.getReverseProxyAuthentication().isEnabled()
        ? insightConfig.getReverseProxyAuthentication().getUsernameHeader() : null;
    // TODO Need to determine if there is additional information we should be sending to the HDS
    loadVersion();
    this.telemetryId = telemetryId;
  }

  private synchronized void updateClient() {
    Configuration config = new Configuration();
    config.setConnectTimeout(insightConfig.getConnectTimeoutInSeconds() * 1000);
    config.setSocketTimeout(insightConfig.getSocketTimeoutInSeconds() * 1000);
    customizeConfiguration(config);
    proxy.contextualize(config);
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
            Thread.sleep(TimeUnit.MINUTES.toMillis(15));
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

  protected void customizeConfiguration(@SuppressWarnings("unused") Configuration configuration) {
  }

  @Override
  public void start() throws Exception {
  }

  @Override
  public void stop() throws Exception {
    client.close();
  }

  @Override
  public void proxyServerConfigurationChanged() {
    updateClient();
    log.debug("Applied new proxy server configuration");
  }

  @Override
  public <T> T get(Class<T> clazz, String path, Map<String, String> queryParams, String... uriParams) {
    return internalGet(clazz, buildUri(null, path, queryParams, uriParams));
  }

  @Override
  public <T> T get(Class<T> clazz, String url) {
    return internalGet(clazz, buildUri(url));
  }

  private <T> T internalGet(Class<T> clazz, String url) {
    HttpGet cloudReq = createGetRequest(url, null, null);
    return execute(cloudReq, clazz);
  }

  @Override
  public <T> T relay(HttpServletRequest request, Class<T> clazz, String path, String... uriParams) throws IOException {
    return relay(request, clazz, path, null, uriParams);
  }

  @Override
  public <T> T relay(HttpServletRequest request,
                     Class<T> clazz,
                     String path,
                     Map<String, String> queryParams,
                     String... uriParams)
      throws IOException
  {
    return relay(request, null, clazz, path, queryParams, uriParams);
  }

  @Override
  public <T> T relay(HttpServletRequest request,
                     HdsClientAnalytics analytics,
                     Class<T> clazz,
                     String path,
                     Map<String, String> queryParams,
                     String... uriParams)
      throws IOException
  {
    String url = buildUri(request, path, queryParams, uriParams);
    HttpUriRequest cloudReq = createRequest(request, url, analytics);
    return execute(cloudReq, clazz);
  }

  public HttpResponse forwardingProxy(HttpServletRequest request, Map<String, String> queryParams)
      throws IOException
  {
    String url = buildUri(request, request.getPathInfo(), queryParams);
    HttpUriRequest labReq = createRequest(request, url, null);
    return execute(labReq);
  }

  private <T> T fromHttpResponse(HttpResponse response, Class<T> clazz) {
    throwErrorIfNeeded(response);
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
        return JsonUtils.parse(EntityUtils.toByteArray(entity), clazz);
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
        case 502:  // Bad Gateway
        case 504:  // Gateway Timeout
          throw new BadGatewayException(
              "Could not contact Sonatype Data Services, please verify the network configuration of your Nexus IQ " +
                  "Server. Sonatype Data Services error " + status + ": " + getErrorMessage(response));
        case 503:  // Service Unavailable
          throw new BadGatewayException(
              "The Sonatype Data Services are currently out of service, please retry in a bit. If the outage " +
                  "persists, please verify the network configuration of your Nexus IQ Server " +
                  "and contact Sonatype Support.");
        default:
          // Since this is for any other errors, the error message may contain anything, so log it, but don't send it
          // back to the client.
          log.error("Sonatype Data Services error " + status + ": " + getErrorMessage(response));
          throw new BadGatewayException("The Sonatype Data Services returned error " + status + 
              ", please retry in a bit.");
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
        && response.getEntity() != null) {
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

  private HttpUriRequest createRequest(HttpServletRequest request, String url, HdsClientAnalytics analytics)
      throws IOException
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

  @Override
  public void post(String path, HttpEntity httpEntity, String clientUserAgent) {
    HttpPost cloudReq = createPostRequest(buildUri(path), null, clientUserAgent);
    cloudReq.setEntity(httpEntity);
    execute(cloudReq, null);
  }

  @Override
  public <T> T post(Class<T> clazz, String path, Object jsonSerializableObject, String... uriParams) {
    return post(null /* analytics */, clazz, path, null /* clientUserAgent */, jsonSerializableObject, uriParams);
  }

  private HttpGet createGetRequest(String url, HdsClientAnalytics analytics, String clientUserAgent) {
    return createGetRequest(url, analytics, null, clientUserAgent);
  }

  private HttpGet createGetRequest(String url,
                                   HdsClientAnalytics analytics,
                                   HttpServletRequest request,
                                   String clientUserAgent)
  {
    HttpGet cloudReq = new HttpGet(url);
    populateRequest(request, cloudReq, analytics);
    setClientUserAgentHeader(cloudReq, clientUserAgent);
    return cloudReq;
  }

  @Override
  public <T> T post(HdsClientAnalytics analytics,
                    Class<T> clazz,
                    String path,
                    final String clientUserAgent,
                    Object jsonSerializableObject,
                    String... uriParams)
  {
    HttpPost cloudReq = createPostRequest(buildUri(path, uriParams), analytics, clientUserAgent);
    HttpEntity entity;
    if (jsonSerializableObject instanceof byte[]) {
      entity = new ByteArrayEntity((byte[]) jsonSerializableObject, ContentType.APPLICATION_OCTET_STREAM);
    }
    else {
      entity = new StringEntity(JsonUtils.format(jsonSerializableObject), ContentType.APPLICATION_JSON);
    }
    cloudReq.setEntity(entity);
    cloudReq.setHeader(HttpHeaders.ACCEPT, "application/json");

    return execute(cloudReq, clazz);
  }

  private HttpPut createPutRequest(String url, HdsClientAnalytics analytics, String clientUserAgent) {
    HttpPut cloudReq = new HttpPut(url);
    populateRequest(null, cloudReq, analytics);
    setClientUserAgentHeader(cloudReq, clientUserAgent);
    return cloudReq;
  }

  @Override
  public <T> T put(
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String clientUserAgent,
      String path,
      File uploadFile,
      Map<String, String> queryParams,
      String... uriParams) throws IOException
  {
    if (!uploadFile.exists()) {
      throw new FileNotFoundException(uploadFile.getAbsolutePath());
    }

    HttpPut cloudReq = createPutRequest(buildUri(null, path, queryParams, uriParams), analytics, clientUserAgent);
    cloudReq.setEntity(new FileEntity(uploadFile, ContentType.DEFAULT_BINARY));
    return execute(cloudReq, clazz);
  }

  private HttpResponse execute(HttpUriRequest request) {
    log.debug("Starting request: {} {}", request.getMethod(), request.getURI());
    long start = System.currentTimeMillis();
    StatusLine statusLine = null;
    try {
      HttpResponse response = client.execute(request);
      statusLine = response.getStatusLine();
      return response;
    }
    catch (HttpHostConnectException e) {
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
      throw new BadGatewayException("The request to Sonatype Data Services failed, please retry in a bit.");
    }
    finally {
      log.debug("Completed request in {} ms. {}", System.currentTimeMillis() - start,
          statusLine != null ? statusLine.getStatusCode() : "");
    }
  }

  private <T> T execute(HttpUriRequest request, Class<T> clazz) {
    HttpResponse response = execute(request);
    return fromHttpResponse(response, clazz);
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
      ContentType contentType = request.getContentType() != null ? ContentType.create(request.getContentType())
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
            && !"COOKIE2".equalsIgnoreCase(headerName) && !headerName.equalsIgnoreCase(rutHeader)
            && !headerName.startsWith("X-Forward")) {
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

    req.setHeader("X-Brain-Version", version);
    req.setHeader("X-CLM-Token", productLicense.getFingerprint());

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

        uriBuilder.queryParam(queryParam.getKey(), queryParam.getValue().replace("{", "%7B").replace("}", "%7D"));
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
}
