/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

import jakarta.inject.Named;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.21
 */
@Named
public class HttpHeaderValidatorFilter
    implements Filter
{
  public static final String URL_PATTERN = "/*";

  @VisibleForTesting
  static final String CONTENT_TYPE = "text/plain;charset=UTF-8";

  private final Map<String, Pattern> headers;

  public HttpHeaderValidatorFilter() throws IOException {
    Properties properties = new Properties();
    properties.load(getClass().getResourceAsStream("http-headers.properties"));
    headers = new HashMap<>();
    for (String header : properties.stringPropertyNames()) {
      headers.put(header, Pattern.compile(properties.getProperty(header)));
    }
  }

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void doFilter(
      final ServletRequest request,
      final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException
  {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    for (Entry<String, Pattern> entry : headers.entrySet()) {
      // Only the singular value headers are checked as the implementation was added to protect against header value
      // injection of headers that are used in server responses.
      String header = httpRequest.getHeader(entry.getKey());
      if (StringUtils.isNotBlank(header) && !entry.getValue().matcher(header).matches()) {
        httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        httpResponse.setContentType(CONTENT_TYPE);
        try (PrintWriter writer = httpResponse.getWriter()) {
          writer.print("Illegal header value detected in '" + entry.getKey() + "'");
          return;
        }
      }
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
  }
}
