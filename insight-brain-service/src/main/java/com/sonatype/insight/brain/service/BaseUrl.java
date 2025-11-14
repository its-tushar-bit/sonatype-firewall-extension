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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpURI.Mutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Named
@Singleton
public class BaseUrl
{
  private static final Logger log = LoggerFactory.getLogger(BaseUrl.class);

  private static final String[] UNSAFE_CHARACTERS = new String[]{"{", "}"};

  private static final String[] ESCAPED_CHARACTERS = new String[]{"%7B", "%7D"};

  public static final String ERR_MSG_BASE_URL_NOT_CONFIGURED = "The server base URL (baseUrl) is not configured. "
      + "More information at https://help.sonatype.com/en/configuring-base-url.html";

  private final Configuration configuration;

  private final ThreadLocal<HttpServletRequest> currentHttpRequest = new ThreadLocal<>();

  @Inject
  public BaseUrl(Configuration configuration) {
    this.configuration = configuration;
  }

  public void capture(HttpServletRequest httpRequest) {
    currentHttpRequest.set(httpRequest);
  }

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

  /**
   * Returns the server base URL:
   * - if the base URL is not forced (in the server configuration), it tries to extract the base URL from the incoming
   * HTTP request (if any);
   * - otherwise, it returns the configured server base URL.
   *
   * @throws IllegalStateException if the base URL cannot be determined.
   */
  public String get() {
    BaseUrlConfiguration baseUrlConfiguration = configuration.getBaseUrlConfiguration();
    if (!baseUrlConfiguration.isForceBaseUrl()) {
      String url = tryGetBaseUriWithEndingForwardSlash();
      if (url != null) {
        return url;
      }
    }
    return getConfigured(baseUrlConfiguration);
  }

  /**
   * Returns the configured server base URL.
   *
   * @throws IllegalStateException if the base URL is not configured.
   */
  public String getConfigured() {
    return getConfigured(configuration.getBaseUrlConfiguration());
  }

  private String getConfigured(BaseUrlConfiguration baseUrlConfiguration) {
    String url = baseUrlConfiguration.getBaseUrl();
    if (!isBlank(url)) {
      return url;
    }
    throw new IllegalStateException(ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  private String tryGetBaseUriWithEndingForwardSlash() {
    try {
      HttpServletRequest httpRequest = getHttpRequest();
      StringBuffer requestUrl = getRequestURL(httpRequest);
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

  private static StringBuffer getRequestURL(final HttpServletRequest request) {
    String proto = request.getHeader("x-forwarded-proto");

    if (isBlank(proto)) {
      return request.getRequestURL();
    }
    else {
      if (!proto.equals("http") && !proto.equals("https")) {
        log.warn("Invalid x-forwarded-proto header value: '{}'. Defaulting to request scheme '{}'",
            proto, request.getScheme());

        proto = request.getScheme();
      }

      Mutable updatedUrl = HttpURI.build(request.getRequestURL().toString()).scheme(proto);
      return new StringBuffer(updatedUrl.asString());
    }
  }

  public UriBuilder redirect() {
    String queryString = getHttpRequest().getQueryString();
    String escapedQueryString = StringUtils.replaceEach(queryString, UNSAFE_CHARACTERS, ESCAPED_CHARACTERS);
    return UriBuilder.fromUri(get()).replaceQuery(escapedQueryString);
  }
}
