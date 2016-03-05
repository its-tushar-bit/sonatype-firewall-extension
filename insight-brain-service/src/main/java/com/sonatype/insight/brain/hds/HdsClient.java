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
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.GatewayTimeoutException;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.util.HashUtils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client for accessing Sonatype hosted data services (HDS).
 */
@Named
@Singleton
public class HdsClient
{
  private static final Logger log = LoggerFactory.getLogger(HdsClient.class);

  private final Configuration config;

  private final HttpClient client;

  private final CLMLicenseManager licenseManager;

  private final VersionService versionService;

  private static volatile String version;

  public static final String UPLOAD_FILE_ATTRIBUTE = "hds.upload.file";

  public static final String CLM_CLIENT_USER_AGENT_HEADER = "X-CLM-Client-User-Agent";

  static final String OWNER_TYPE_HEADER = "X-CLM-Owner-Type";

  static final String OWNER_ID_HEADER = "X-CLM-Owner-Id";

  @Inject
  public HdsClient(final InsightProxy proxy,
                   final CLMLicenseManager licenseManager,
                   VersionService versionService,
                   IdleConnectionReaper idleConnectionReaper)
  {
    this(proxy, licenseManager, versionService, idleConnectionReaper, 20);
  }

  protected HdsClient(final InsightProxy proxy,
                      final CLMLicenseManager licenseManager,
                      VersionService versionService,
                      IdleConnectionReaper idleConnectionReaper,
                      int poolSize)
  {
    this.licenseManager = licenseManager;
    config = proxy.contextualize(new Configuration());
    HttpClientBuilder clientBuilder = HttpClientUtils.create(config);
    HttpClientConnectionManager connectionManager = buildHttpClientConnectionManager(poolSize);
    idleConnectionReaper.register(connectionManager);
    clientBuilder.setConnectionManager(connectionManager);
    client = clientBuilder.build();
    this.versionService = versionService;
    // TODO Need to determine if there is additional information we should be sending to the HDS
    loadVersion();
  }

  public HttpResponse getResponse(HttpServletRequest request,
                                  String path,
                                  Map<String, String> queryParams,
                                  String... uriParams) throws IOException
  {
    return getResponse(request, null, path, queryParams, uriParams);
  }

  private HttpResponse getResponse(HttpServletRequest request,
                                   HdsClientAnalytics analytics,
                                   String path,
                                   Map<String, String> queryParams,
                                   String... uriParams) throws IOException
  {
    return execute(request, buildUri(request, path, queryParams, uriParams), analytics);
  }

  private HttpResponse getResponse(HttpServletRequest request, String path) throws IOException {
    String url = config.getServerUrl();
    if (!url.endsWith("/")) {
      url += '/';
    }
    if (path.startsWith("/")) {
      url += path.substring(1);
    }
    else {
      url += path;
    }
    return execute(request, url, null);
  }

  public <T> T get(Class<T> clazz, String path, Map<String, String> queryParams, String... uriParams)
      throws IOException
  {
    return get(null, clazz, path, queryParams, uriParams);
  }

  public <T> T get(HttpServletRequest request, Class<T> clazz, String path, String... uriParams) throws IOException {
    return get(request, clazz, path, null, uriParams);
  }

  public <T> T get(HttpServletRequest request,
                   Class<T> clazz,
                   String path,
                   Map<String, String> queryParams,
                   String... uriParams) throws IOException
  {
    return get(request, null, clazz, path, queryParams, uriParams);
  }

  public <T> T get(HttpServletRequest request,
                   HdsClientAnalytics analytics,
                   Class<T> clazz,
                   String path,
                   Map<String, String> queryParams,
                   String... uriParams) throws IOException
  {
    long start = System.currentTimeMillis();

    try {
      HttpResponse response = getResponse(request, analytics, path, queryParams, uriParams);
      return fromHttpResponse(response, clazz);
    }
    finally {
      log.debug("Completed HDS request in {} ms.", System.currentTimeMillis() - start);
    }
  }

  public <T> T get(Class<T> clazz, String url) throws IOException {
    return get(null, clazz, url);
  }

  private <T> T get(HttpServletRequest request, Class<T> clazz, String url) throws IOException {
    long start = System.currentTimeMillis();

    try {
      HttpResponse response = getResponse(request, url);
      return fromHttpResponse(response, clazz);
    }
    finally {
      log.debug("Completed HDS request in {} ms.", System.currentTimeMillis() - start);
    }
  }

  private <T> T fromHttpResponse(HttpResponse response, Class<T> clazz) throws IOException {
    throwErrorIfNeeded(response);
    boolean usingStream = false;
    try {
      HttpEntity entity = response.getEntity();
      if (entity == null) {
        return null;
      }
      else if (String.class.equals(clazz)) {
        return clazz.cast(EntityUtils.toString(entity, "UTF-8"));
      }
      else if (InputStream.class.equals(clazz)) {
        usingStream = true;
        return clazz.cast(entity.getContent());
      }
      else {
        InputStream in = null;
        try {
          in = entity.getContent();
          return JsonUtils.parse(IOUtil.toByteArray(in), clazz);
        }
        finally {
          IOUtil.close(in);
        }
      }
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

  private void throwErrorIfNeeded(HttpResponse response) throws IOException {
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
              "Could not contact Sonatype HDS, please verify the network configuration of your CLM server. HDS error "
                  + status + ": " + getErrorMessage(response));
        case 402:
          throw new PaymentRequiredException(getErrorMessage(response));
        case 404:
          throw new NotFoundException(getErrorMessage(response));
        case 409:
          throw new ConflictException(getErrorMessage(response));
        case 502:
          // coming from Apache when webapp is down
        case 503:
          throw new BadGatewayException(
              "The Sonatype HDS is currently out of service, please retry in a bit. If the outage persists, please contact Sonatype Support.");
        default:
          throw new InternalServerException("HDS error " + status + ": " + getErrorMessage(response));
      }
    }
    catch (RuntimeException | IOException e) {
      EntityUtils.consumeQuietly(response.getEntity());
      throw e;
    }
  }

  private String getErrorMessage(HttpResponse response) throws IOException {
    Header hdr = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
    if (hdr != null && hdr.getValue() != null && hdr.getValue().contains(ContentType.TEXT_PLAIN.getMimeType())
        && response.getEntity() != null) {
      return EntityUtils.toString(response.getEntity(), "UTF-8");
    }
    return response.getStatusLine().getReasonPhrase();
  }

  public Response doProxy(HttpServletRequest request, String path, String... uriParams) throws IOException {
    return doProxy(request, path, null, uriParams);
  }

  public Response doProxy(HttpServletRequest request, String path, Map<String, String> queryParams, String... uriParams)
      throws IOException
  {
    HttpResponse response = getResponse(request, path, queryParams, uriParams);
    return buildResponse(response);
  }

  private HttpResponse execute(HttpServletRequest request, String url, HdsClientAnalytics analytics) throws IOException
  {
    HttpUriRequest cloudReq;
    if (request == null || "GET".equals(request.getMethod())) {
      cloudReq = new HttpGet(url);
    }
    else if ("POST".equals(request.getMethod())) {
      cloudReq = new HttpPost(url);

      ((HttpPost) cloudReq).setEntity(new BufferedHttpEntity(buildEntity(request)));
    }
    else if ("PUT".equals(request.getMethod())) {
      cloudReq = new HttpPut(url);

      ((HttpPut) cloudReq).setEntity(new BufferedHttpEntity(buildEntity(request)));
    }
    else if ("DELETE".equals(request.getMethod())) {
      cloudReq = new HttpDelete(url);
    }
    else {
      throw new IllegalArgumentException("Unknown request method " + request.getMethod());
    }
    populateRequest(request, cloudReq, analytics);
    return execute(cloudReq);
  }

  /**
   * @since 1.13.0
   */
  public <T> T post(Class<T> clazz, String path, Object jsonSerializableObject, String... uriParams) throws IOException
  {
    long start = System.currentTimeMillis();
    try {
      HttpPost cloudReq = new HttpPost(buildUri(path, uriParams));
      StringEntity entity = new StringEntity(JsonUtils.format(jsonSerializableObject));
      cloudReq.setEntity(entity);
      populateRequest(null /* base request */, cloudReq, null);
      cloudReq.setHeader(HttpHeaders.ACCEPT, "application/json");
      cloudReq.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

      HttpResponse response = execute(cloudReq);
      return fromHttpResponse(response, clazz);
    }
    finally {
      log.debug("Completed HDS request in {} ms.", System.currentTimeMillis() - start);
    }
  }

  /**
   * @since 1.8
   */
  public <T> T put(HdsClientAnalytics analytics, Class<T> clazz, String path, File uploadFile, String... uriParams)
      throws IOException
  {
    long start = System.currentTimeMillis();

    try {
      if (!uploadFile.exists()) {
        throw new FileNotFoundException(uploadFile.getAbsolutePath());
      }
      HttpPut cloudReq = new HttpPut(buildUri(path, uriParams));
      FileEntity fileEntity = new FileEntity(uploadFile, ContentType.DEFAULT_BINARY);
      cloudReq.setEntity(new BufferedHttpEntity(fileEntity));
      populateRequest(null /* base request */, cloudReq, analytics);
      HttpResponse response = execute(cloudReq);
      return fromHttpResponse(response, clazz);
    }
    finally {
      log.debug("Completed HDS request in {} ms.", System.currentTimeMillis() - start);
    }
  }

  private HttpResponse execute(HttpUriRequest request) throws IOException {
    try {
      return client.execute(request);
    }
    catch (HttpHostConnectException e) {
      throw new GatewayTimeoutException(e.getMessage(), e);
    }
    catch (UnknownHostException e) {
      throw new BadGatewayException("The hostname for the Sonatype HDS could not be resolved, "
          + "please verify the network configuration (DNS) at the site where the Sonatype CLM server is operated", e);
    }
  }

  private HttpEntity buildEntity(HttpServletRequest request) throws IOException {
    File uploadFile = (File) request.getAttribute(UPLOAD_FILE_ATTRIBUTE);
    if (uploadFile != null) {
      ContentType contentType = request.getContentType() != null ? ContentType.create(request.getContentType())
          : ContentType.DEFAULT_BINARY;
      return new FileEntity(uploadFile, contentType);
    }

    return new InputStreamEntity(request.getInputStream(), request.getContentLength());
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
            && !HttpHeaders.PROXY_AUTHORIZATION.equalsIgnoreCase(headerName) && !"COOKIE".equalsIgnoreCase(headerName)) {
          req.setHeader(headerName, orig.getHeader(headerName));
        }
      }

      req.setHeader(CLM_CLIENT_USER_AGENT_HEADER, getClientUserAgent(orig));
    }
    if (analytics != null) {
      req.setHeader(OWNER_TYPE_HEADER, analytics.getOwnerType().toString());
      req.setHeader(OWNER_ID_HEADER, analytics.getOwnerId());
    }

    req.setHeader("X-Brain-Version", version);
    req.setHeader("X-CLM-Token", licenseManager.getLicenseFingerprint());
    req.setHeader(HttpHeaders.USER_AGENT, config.getUserAgent());
  }

  private String getClientUserAgent(HttpServletRequest request) {
    // some clients can't control the actual UA header and use an alternative header
    String clientUserAgent = request.getHeader(CLM_CLIENT_USER_AGENT_HEADER);
    if (clientUserAgent == null) {
      clientUserAgent = request.getHeader(HttpHeaders.USER_AGENT);
    }
    return clientUserAgent;
  }

  private Response buildResponse(final HttpResponse response) throws IOException {
    throwErrorIfNeeded(response);
    ResponseBuilder builder = Response.status(response.getStatusLine().getStatusCode());

    // pass-back response metadata+content to servlet
    for (final Header h : response.getAllHeaders()) {
      final String name = h.getName();
      // ignore Transfer-Encoding since httpclient should have handled it
      if (!HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(name) && !HttpHeaders.CONTENT_ENCODING.equalsIgnoreCase(name)
          && !HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name) && !HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
        builder.header(name, h.getValue());
      }
    }

    final HttpEntity entity = response.getEntity();
    if (entity != null) {
      if (entity.getContentEncoding() != null) {
        builder.header(HttpHeaders.CONTENT_ENCODING, entity.getContentEncoding().getValue());
      }
      builder.header(HttpHeaders.CONTENT_LENGTH, entity.getContentLength());
      if (entity.getContentType() != null) {
        builder.header(HttpHeaders.CONTENT_TYPE, entity.getContentType().getValue());
      }

      builder.entity(new StreamingOutput()
      {
        @Override
        public void write(OutputStream output) throws IOException {
          entity.writeTo(output);
        }
      });
    }
    return builder.build();
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
        uriBuilder.queryParam(queryParam.getKey(), queryParam.getValue());
      }
    }

    String result = uriBuilder.build((Object[]) uriParams).toString();
    log.debug("Constructed HDS URI: {}", result);
    return result;
  }

  private HttpClientConnectionManager buildHttpClientConnectionManager(int poolSize) {
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
    connManager.setMaxTotal(poolSize);
    connManager.setDefaultMaxPerRoute(connManager.getMaxTotal());
    return connManager;
  }

  private void loadVersion() {
    if (version != null) {
      return;
    }

    version = versionService.getVersion("Unknown");
  }
}
