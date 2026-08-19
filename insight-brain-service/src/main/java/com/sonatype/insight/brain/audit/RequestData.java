/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import com.google.common.net.HttpHeaders;
import com.sonatype.insight.brain.spring.config.SecurityConfiguration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.StringJoiner;

/**
 * Holds the audit data specific for a HTTP request.
 */
class RequestData
{
  private final String method;

  private final String uri;

  private final String remoteIpAddress;

  private final String forwarded;

  private final String userAgent;

  private final String sessionId;

  static RequestData newInstance(HttpServletRequest httpRequest) {
    String method = httpRequest.getMethod();
    String uri = httpRequest.getRequestURI();
    String queryParams = httpRequest.getQueryString();
    if (queryParams != null) {
      uri += '?' + queryParams;
    }
    String remoteIpAddress = httpRequest.getRemoteAddr();
    String forwarded = getAllHeaders(httpRequest.getHeaders(HttpHeaders.FORWARDED));
    if (forwarded == null) {
      forwarded = getAllHeaders(httpRequest.getHeaders(HttpHeaders.X_FORWARDED_FOR));
    }
    String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
    String sessionId = getCookie(httpRequest, SecurityConfiguration.SESSION_COOKIE_NAME);
    return new RequestData(method, uri, remoteIpAddress, forwarded, userAgent, sessionId);
  }

  private static String getAllHeaders(Enumeration<String> headers) {
    if (!headers.hasMoreElements()) {
      return null;
    }
    StringJoiner joiner = new StringJoiner(", ");
    while (headers.hasMoreElements()) {
      joiner.add(headers.nextElement());
    }
    return joiner.toString();
  }

  private static String getCookie(HttpServletRequest httpRequest, String cookieName) {
    Cookie[] cookies = httpRequest.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookieName.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  private RequestData(
      String method,
      String uri,
      String remoteIpAddress,
      String forwarded,
      String userAgent,
      String sessionId)
  {
    this.method = method;
    this.uri = uri;
    this.remoteIpAddress = remoteIpAddress;
    this.forwarded = forwarded;
    this.userAgent = userAgent;
    this.sessionId = sessionId;
  }

  String getMethod() {
    return method;
  }

  String getUri() {
    return uri;
  }

  String getRemoteIpAddress() {
    return remoteIpAddress;
  }

  String getForwarded() {
    return forwarded;
  }

  String getUserAgent() {
    return userAgent;
  }

  String getSessionId() {
    return sessionId;
  }
}
