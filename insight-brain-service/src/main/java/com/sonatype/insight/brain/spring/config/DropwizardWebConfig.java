/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class DropwizardWebConfig
{
  @JsonProperty
  String uriPath;

  @JsonProperty("hsts")
  DropwizardWebConfig.Hsts hsts;

  @JsonProperty("frame-options")
  DropwizardWebConfig.FrameOptions frameOptions;

  @JsonProperty("content-type-options")
  DropwizardWebConfig.ContentTypeOptions contentTypeOptions;

  @JsonProperty("xss-protection")
  DropwizardWebConfig.XssProtection xssProtection;

  @JsonProperty("csp")
  DropwizardWebConfig.Csp csp;

  @JsonProperty("cors")
  DropwizardWebConfig.Cors cors;

  @JsonProperty
  Map<String, String> headers;

  static class Hsts
  {
    @JsonProperty
    Boolean enabled;

    @JsonProperty
    Object maxAge;

    @JsonProperty
    Boolean includeSubDomains;

    @JsonProperty
    Boolean preload;
  }

  static class FrameOptions
  {
    @JsonProperty
    Boolean enabled;

    @JsonProperty
    String option;

    @JsonProperty
    String origin;
  }

  static class ContentTypeOptions
  {
    @JsonProperty
    Boolean enabled;
  }

  static class XssProtection
  {
    @JsonProperty
    Boolean enabled;

    @JsonProperty
    Boolean on;

    @JsonProperty
    Boolean block;
  }

  static class Csp
  {
    @JsonProperty
    Boolean enabled;

    @JsonProperty
    String policy;

    @JsonProperty
    String reportOnlyPolicy;
  }

  static class Cors
  {
    @JsonProperty
    Object allowedOrigins;

    @JsonProperty
    Object allowedTimingOrigins;

    @JsonProperty
    Object allowedMethods;

    @JsonProperty
    Object allowedHeaders;

    @JsonProperty
    Object preflightMaxAge;

    @JsonProperty
    Boolean allowCredentials;

    @JsonProperty
    Object exposedHeaders;

    @JsonProperty
    Boolean chainPreflight;
  }
}
