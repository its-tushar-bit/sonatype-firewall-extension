/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import static com.google.common.net.HttpHeaders.X_FORWARDED_PROTO;
import static org.apache.commons.lang.StringUtils.isBlank;

@Named
@Singleton
public class BaseUrl
    extends AbstractInjectable<BaseUrl>
{

  static final String ERR_MSG_BASE_URL_NOT_CONFIGURED = "baseUrl is not configured.";

  private final InsightConfig appConfig;

  // According to JAX-RS 2.0 specs, chapter 9, it is OK to inject UriInfo and HttpHeaders here even if this class is a
  // singleton.
  @Context
  private UriInfo uriInfo;

  @Context
  private HttpHeaders httpHeaders;

  @Inject
  public BaseUrl(final InsightConfig appConfig) {
    this.appConfig = appConfig;
  }

  /**
   * public for testing only
   */
  public BaseUrl(final InsightConfig appConfig, final UriInfo uriInfo, final HttpHeaders httpHeaders) {
    this.appConfig = appConfig;
    this.uriInfo = uriInfo;
    this.httpHeaders = httpHeaders;
  }

  public String get() {
    String url;
    if (!appConfig.isForceBaseUrl()) {
      url = tryGetBaseUriWithEndingForwardSlash();
      if (url != null) {
        return url;
      }
    }
    url = appConfig.getBaseUrl();
    if (!isBlank(url)) {
      return url;
    }
    throw new IllegalStateException(ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  private String tryGetBaseUriWithEndingForwardSlash() {
    try {
      if (uriInfo == null) {
        return null;
      }
      UriBuilder baseUri = uriInfo.getBaseUriBuilder();
      if (httpHeaders != null) {
        // Jetty 8.1.x does not correctly respect X-Forwarded-Proto when configured with an SSL connector.
        List<String> xForwardedProtoHeaders = httpHeaders.getRequestHeader(X_FORWARDED_PROTO);
        if (xForwardedProtoHeaders != null && !xForwardedProtoHeaders.isEmpty()) {
          baseUri.scheme(xForwardedProtoHeaders.get(0));
        }
      }
      String url = baseUri.build().toString();
      if (!url.endsWith("/")) {
        url += '/';
      }
      return url;
    }
    catch (IllegalStateException e) {
      // no request in scope
      return null;
    }
  }

  public UriBuilder redirect() {
    URI requestUri = uriInfo.getRequestUri();
    return UriBuilder.fromUri(get()).replaceQuery(requestUri.getRawQuery());
  }

}
