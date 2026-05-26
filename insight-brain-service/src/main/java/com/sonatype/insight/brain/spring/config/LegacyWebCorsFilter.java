/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Servlet-filter compatibility for legacy Dropwizard {@code web.cors} configuration.
 */
public class LegacyWebCorsFilter
    implements Filter
{
  static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

  static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

  static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

  static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

  static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

  static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

  static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";

  static final String ORIGIN = "Origin";

  static final String TIMING_ALLOW_ORIGIN = "Timing-Allow-Origin";

  static final String VARY = "Vary";

  private final DropwizardWebSettings.CorsSettings corsSettings;

  private final boolean anyOriginAllowed;

  private final List<Pattern> allowedOriginPatterns;

  private final boolean anyTimingOriginAllowed;

  private final List<Pattern> allowedTimingOriginPatterns;

  public LegacyWebCorsFilter(final DropwizardWebSettings.CorsSettings corsSettings) {
    this.corsSettings = corsSettings;
    this.anyOriginAllowed = corsSettings.getAllowedOrigins().contains("*");
    this.allowedOriginPatterns = compilePatterns(corsSettings.getAllowedOrigins());
    this.anyTimingOriginAllowed = corsSettings.getAllowedTimingOrigins().contains("*");
    this.allowedTimingOriginPatterns = compilePatterns(corsSettings.getAllowedTimingOrigins());
  }

  @Override
  public void doFilter(
      final ServletRequest request,
      final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException
  {
    if (!(request instanceof HttpServletRequest httpRequest)
        || !(response instanceof HttpServletResponse httpResponse))
    {
      chain.doFilter(request, response);
      return;
    }

    addVaryOrigin(httpResponse);

    String origins = httpRequest.getHeader(ORIGIN);
    if (origins == null) {
      chain.doFilter(request, response);
      return;
    }

    boolean preflight = isPreflight(httpRequest);
    if (originMatches(origins, anyOriginAllowed, allowedOriginPatterns)) {
      if (preflight) {
        handlePreflightResponse(origins, httpResponse);
        if (!corsSettings.isChainPreflight()) {
          return;
        }
      }
      else {
        handleSimpleResponse(origins, httpResponse);
      }

      if (originMatches(origins, anyTimingOriginAllowed, allowedTimingOriginPatterns)) {
        httpResponse.setHeader(TIMING_ALLOW_ORIGIN, origins);
      }

      chain.doFilter(request, response);
      return;
    }

    if (preflight && !corsSettings.isChainPreflight()) {
      return;
    }

    chain.doFilter(request, response);
  }

  private boolean isPreflight(final HttpServletRequest request) {
    return "OPTIONS".equalsIgnoreCase(request.getMethod()) && request.getHeader(ACCESS_CONTROL_REQUEST_METHOD) != null;
  }

  private void handlePreflightResponse(final String origins, final HttpServletResponse response) {
    response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origins);
    if (corsSettings.isAllowCredentials()) {
      response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }
    if (!corsSettings.getAllowedMethods().isEmpty()) {
      response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, join(corsSettings.getAllowedMethods()));
    }
    if (!corsSettings.getAllowedHeaders().isEmpty()) {
      response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, join(corsSettings.getAllowedHeaders()));
    }
    if (!corsSettings.getPreflightMaxAge().isZero() && !corsSettings.getPreflightMaxAge().isNegative()) {
      response.setHeader(ACCESS_CONTROL_MAX_AGE, String.valueOf(corsSettings.getPreflightMaxAge().toSeconds()));
    }
  }

  private void handleSimpleResponse(final String origins, final HttpServletResponse response) {
    response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origins);
    if (corsSettings.isAllowCredentials()) {
      response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }
    if (!corsSettings.getExposedHeaders().isEmpty()) {
      response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, join(corsSettings.getExposedHeaders()));
    }
  }

  private boolean originMatches(
      final String origins,
      final boolean anyAllowed,
      final List<Pattern> allowedPatterns)
  {
    if (anyAllowed) {
      return true;
    }
    if (allowedPatterns.isEmpty()) {
      return false;
    }

    for (String origin : origins.split(" ")) {
      String trimmedOrigin = origin.trim();
      if (trimmedOrigin.isEmpty()) {
        continue;
      }
      for (Pattern pattern : allowedPatterns) {
        if (pattern.matcher(trimmedOrigin).matches()) {
          return true;
        }
      }
    }
    return false;
  }

  // Entries are treated as Java regexes (matching legacy Jetty CrossOriginHandler behavior).
  // Literal dots in domain names must be escaped as "\\." for strict matching.
  private List<Pattern> compilePatterns(final List<String> values) {
    List<Pattern> patterns = new ArrayList<>();
    for (String value : values) {
      String trimmedValue = value.trim();
      if (!trimmedValue.isEmpty() && !"*".equals(trimmedValue)) {
        try {
          patterns.add(Pattern.compile(trimmedValue, Pattern.CASE_INSENSITIVE));
        }
        catch (PatternSyntaxException e) {
          throw new IllegalStateException("Invalid regex in web.cors origin pattern: '" + trimmedValue + "'", e);
        }
      }
    }
    return patterns;
  }

  private void addVaryOrigin(final HttpServletResponse response) {
    Collection<String> varyHeaders = response.getHeaders(VARY);
    for (String varyHeader : varyHeaders) {
      for (String value : varyHeader.split(",")) {
        if (ORIGIN.equalsIgnoreCase(value.trim()) || "*".equals(value.trim())) {
          return;
        }
      }
    }
    response.addHeader(VARY, ORIGIN);
  }

  private String join(final List<String> values) {
    return String.join(",", values);
  }
}
