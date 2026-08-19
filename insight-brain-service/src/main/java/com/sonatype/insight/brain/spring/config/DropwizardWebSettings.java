/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring-side compatibility settings parsed from the legacy Dropwizard {@code web:} configuration section.
 */
public final class DropwizardWebSettings
{
  private static final String DEFAULT_URI_PATH = "/";

  private static final String DEFAULT_URL_PATTERN = "/*";

  private static final CorsSettings DEFAULT_CORS_SETTINGS = new CorsSettings(
      List.of(),
      List.of(),
      List.of("GET", "POST", "HEAD"),
      List.of("Content-Type"),
      Duration.ofSeconds(60),
      false,
      List.of(),
      false);

  private final String uriPath;

  private final String urlPattern;

  private final Map<String, String> headers;

  private final CorsSettings corsSettings;

  static DropwizardWebSettings empty() {
    return new DropwizardWebSettings(DEFAULT_URI_PATH, Map.of(), null);
  }

  static DropwizardWebSettings of(
      final String uriPath,
      final Map<String, String> headers,
      final CorsSettings corsSettings)
  {
    return new DropwizardWebSettings(uriPath, headers, corsSettings);
  }

  private DropwizardWebSettings(
      final String uriPath,
      final Map<String, String> headers,
      final CorsSettings corsSettings)
  {
    this.uriPath = normalizeUriPath(uriPath);
    this.urlPattern = deriveUrlPattern(this.uriPath);
    this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    this.corsSettings = corsSettings;
  }

  public String getUriPath() {
    return uriPath;
  }

  public String getUrlPattern() {
    return urlPattern;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public boolean hasHeaders() {
    return !headers.isEmpty();
  }

  public CorsSettings getCorsSettings() {
    return corsSettings;
  }

  public CorsSettings getCorsSettingsOrDefault() {
    return corsSettings != null ? corsSettings : DEFAULT_CORS_SETTINGS;
  }

  public boolean hasCorsSettings() {
    return corsSettings != null;
  }

  private static String normalizeUriPath(final String uriPath) {
    if (uriPath == null || uriPath.isBlank()) {
      return DEFAULT_URI_PATH;
    }

    String normalized = uriPath.trim();
    if (!normalized.startsWith("/")) {
      normalized = "/" + normalized;
    }
    return normalized;
  }

  private static String deriveUrlPattern(final String uriPath) {
    if (DEFAULT_URI_PATH.equals(uriPath)) {
      return DEFAULT_URL_PATTERN;
    }
    if (uriPath.endsWith("/")) {
      return uriPath + "*";
    }
    return uriPath + "/*";
  }

  public static final class CorsSettings
  {
    private final List<String> allowedOrigins;

    private final List<String> allowedTimingOrigins;

    private final List<String> allowedMethods;

    private final List<String> allowedHeaders;

    private final Duration preflightMaxAge;

    private final boolean allowCredentials;

    private final List<String> exposedHeaders;

    private final boolean chainPreflight;

    static CorsSettings defaults() {
      return DEFAULT_CORS_SETTINGS;
    }

    static CorsSettings of(
        final List<String> allowedOrigins,
        final List<String> allowedTimingOrigins,
        final List<String> allowedMethods,
        final List<String> allowedHeaders,
        final Duration preflightMaxAge,
        final boolean allowCredentials,
        final List<String> exposedHeaders,
        final boolean chainPreflight)
    {
      return new CorsSettings(
          allowedOrigins,
          allowedTimingOrigins,
          allowedMethods,
          allowedHeaders,
          preflightMaxAge,
          allowCredentials,
          exposedHeaders,
          chainPreflight);
    }

    private CorsSettings(
        final List<String> allowedOrigins,
        final List<String> allowedTimingOrigins,
        final List<String> allowedMethods,
        final List<String> allowedHeaders,
        final Duration preflightMaxAge,
        final boolean allowCredentials,
        final List<String> exposedHeaders,
        final boolean chainPreflight)
    {
      this.allowedOrigins = immutableList(allowedOrigins);
      this.allowedTimingOrigins = immutableList(allowedTimingOrigins);
      this.allowedMethods = immutableList(allowedMethods);
      this.allowedHeaders = immutableList(allowedHeaders);
      this.preflightMaxAge = preflightMaxAge;
      this.allowCredentials = allowCredentials;
      this.exposedHeaders = immutableList(exposedHeaders);
      this.chainPreflight = chainPreflight;
    }

    public List<String> getAllowedOrigins() {
      return allowedOrigins;
    }

    public List<String> getAllowedTimingOrigins() {
      return allowedTimingOrigins;
    }

    public List<String> getAllowedMethods() {
      return allowedMethods;
    }

    public List<String> getAllowedHeaders() {
      return allowedHeaders;
    }

    public Duration getPreflightMaxAge() {
      return preflightMaxAge;
    }

    public boolean isAllowCredentials() {
      return allowCredentials;
    }

    public List<String> getExposedHeaders() {
      return exposedHeaders;
    }

    public boolean isChainPreflight() {
      return chainPreflight;
    }

    private static List<String> immutableList(final List<String> values) {
      return Collections.unmodifiableList(new ArrayList<>(values));
    }
  }
}
