/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.error.exception.BadGatewayException;

import java.io.IOException;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dedicated HTTP client with a short timeout for accessing user telemetry(Gainsight) endpoints.
 *
 * @since 1.196
 */
@Named
@Singleton
public class GainsightTelemetryClient extends HdsClient
{
  static final int SOCKET_TIMEOUT = 5000;

  static final int CONNECT_TIMEOUT = SOCKET_TIMEOUT;

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Inject
  public GainsightTelemetryClient(
      InsightProxy proxy,
      ProductLicense productLicense,
      Configuration configuration,
      VersionService versionService,
      TelemetryId telemetryId,
      CurrentUser currentUser)
  {
    super(proxy, productLicense, configuration, versionService, telemetryId, currentUser);
  }

  @Override
  protected void customizeConfiguration(HttpClientUtils.Configuration configuration) {
    configuration.setSocketTimeout(SOCKET_TIMEOUT);
    configuration.setConnectTimeout(CONNECT_TIMEOUT);
  }

  public <T> RelayResponse<T> relayNoRetry(
      HttpServletRequest request,
      Class<T> clazz,
      String path,
      String... uriParams) throws IOException
  {
    return relayNoRetry(request, null, clazz, path, null, uriParams);
  }

  public <T> RelayResponse<T> relayNoRetry(
      HttpServletRequest request,
      HdsClientAnalytics analytics,
      Class<T> clazz,
      String path,
      Map<String, String> queryParams,
      String... uriParams) throws IOException
  {
    String url = buildUri(request, path, queryParams, uriParams);
    HttpUriRequest cloudReq = createRequest(request, url, analytics);

    try {
      HttpResponse response = execute(cloudReq, 0);
      RelayResponse<T> relayResponse = new RelayResponse<>(fromHttpResponse(response, clazz));
      if (response.getEntity() != null && response.getEntity().getContentType() != null) {
        relayResponse.contentType = response.getEntity().getContentType().getValue();
      }
      return relayResponse;
    }
    catch (BadGatewayException e) {
      log.debug("Failed to execute request {} with timeout {} seconds", cloudReq, CONNECT_TIMEOUT / 1000, e);
    }
    catch (Exception e) {
      log.debug("Failed to execute request {}", cloudReq, e);
    }
    return null;
  }

  public <T> T getWithTimeoutNoRetry(Class<T> clazz, String path) {
    String url = buildUri(path);
    HttpGet cloudReq = createGetRequest(url, null, null);
    setClientUserAgentHeader(cloudReq, null /* clientUserAgent */);
    return fromHttpResponse(execute(cloudReq, 0), clazz);
  }
}
