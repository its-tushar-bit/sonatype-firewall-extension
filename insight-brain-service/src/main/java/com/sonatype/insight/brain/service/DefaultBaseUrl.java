/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Named
@Singleton
public class DefaultBaseUrl implements BaseUrl
{
  public static final String ERR_MSG_BASE_URL_NOT_CONFIGURED = "The server base URL (baseUrl) is not configured. "
      + "More information at https://links.sonatype.com/products/clm/docs/base-url";

  private final InsightConfig appConfig;

  private final ThreadLocal<HttpServletRequest> currentHttpRequest = new ThreadLocal<>();

  @Inject
  public DefaultBaseUrl(final InsightConfig appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  public void capture(HttpServletRequest httpRequest) {
    currentHttpRequest.set(httpRequest);
  }

  @Override
  public void release() {
    currentHttpRequest.remove();
  }

  private HttpServletRequest getHttpRequest() {
    HttpServletRequest httpRequest = currentHttpRequest.get();
    if (httpRequest == null) {
      throw new IllegalStateException("Not inside a request");
    }
    return httpRequest;
  }

  @Override
  public String get() {
    if (!appConfig.isForceBaseUrl()) {
      String url = tryGetBaseUriWithEndingForwardSlash();
      if (url != null) {
        return url;
      }
    }
    return getConfigured();
  }

  @Override
  public String getConfigured() {
    String url = appConfig.getBaseUrl();
    if (!isBlank(url)) {
      return url;
    }
    throw new IllegalStateException(ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  private String tryGetBaseUriWithEndingForwardSlash() {
    try {
      HttpServletRequest httpRequest = getHttpRequest();
      StringBuffer requestUrl = httpRequest.getRequestURL();
      String requestUri = httpRequest.getRequestURI();
      String contextPath = httpRequest.getContextPath();
      String url = requestUrl.substring(0, requestUrl.length() - requestUri.length() + contextPath.length());
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

  @Override
  public UriBuilder redirect() {
    return UriBuilder.fromUri(get()).replaceQuery(getHttpRequest().getQueryString());
  }
}
