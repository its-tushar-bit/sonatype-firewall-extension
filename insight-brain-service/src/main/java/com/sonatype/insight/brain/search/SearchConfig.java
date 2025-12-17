/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.net.URI;
import java.time.Duration;
import java.util.regex.Pattern;

import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.opensearch.OpenSearchConfigurationException;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.lang3.StringUtils;

/**
 * Configuration for connection information for OpenSearch
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = HttpOpenSearchConfig.class, name = "http"),
    @JsonSubTypes.Type(value = AwsHttpOpenSearchConfig.class, name = "aws"),
})
public interface SearchConfig
{
  /**
   * Validates the configuration and throws an exception if invalid.
   *
   * @throws OpenSearchConfigurationException if the configuration is invalid
   */
  void validate();

  class AwsHttpOpenSearchConfig
      implements SearchConfig
  {
    private static final Pattern AWS_REGION_PATTERN = Pattern.compile("^[a-z]{2}-[a-z]+-\\d{1}$");

    private static final int DEFAULT_MAX_CONCURRENCY = 50;

    private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration DEFAULT_CONNECTION_ACQUISITION_TIMEOUT = Duration.ofSeconds(10);

    private URI domain;

    private String region;

    private Integer maxConcurrency;

    private Duration connectionTimeout;

    private Duration connectionAcquisitionTimeout;

    public String getRegion() {
      return region;
    }

    public void setRegion(final String region) {
      this.region = region;
    }

    public URI getDomain() {
      return domain;
    }

    public void setDomain(final URI domain) {
      this.domain = domain;
    }

    public Integer getMaxConcurrency() {
      return maxConcurrency != null ? maxConcurrency : DEFAULT_MAX_CONCURRENCY;
    }

    public void setMaxConcurrency(final Integer maxConcurrency) {
      this.maxConcurrency = maxConcurrency;
    }

    public Duration getConnectionTimeout() {
      return connectionTimeout != null ? connectionTimeout : DEFAULT_CONNECTION_TIMEOUT;
    }

    public void setConnectionTimeout(final Duration connectionTimeout) {
      this.connectionTimeout = connectionTimeout;
    }

    public Duration getConnectionAcquisitionTimeout() {
      return
          connectionAcquisitionTimeout != null ? connectionAcquisitionTimeout : DEFAULT_CONNECTION_ACQUISITION_TIMEOUT;
    }

    public void setConnectionAcquisitionTimeout(final Duration connectionAcquisitionTimeout) {
      this.connectionAcquisitionTimeout = connectionAcquisitionTimeout;
    }

    @Override
    public void validate() {
      if (domain == null) {
        throw new OpenSearchConfigurationException("AWS OpenSearch domain URI is required");
      }

      String scheme = domain.getScheme();
      if (scheme == null || !scheme.equalsIgnoreCase("https")) {
        throw new OpenSearchConfigurationException(
            "AWS OpenSearch domain URI must use HTTPS scheme, but was: " + scheme);
      }

      if (StringUtils.isBlank(domain.getHost())) {
        throw new OpenSearchConfigurationException(
            "AWS OpenSearch domain URI must have a valid host");
      }

      if (StringUtils.isBlank(region)) {
        throw new OpenSearchConfigurationException("AWS region is required");
      }

      if (!AWS_REGION_PATTERN.matcher(region).matches()) {
        throw new OpenSearchConfigurationException(
            "Invalid AWS region format: " + region + ". Expected format: us-east-1, eu-west-1, etc.");
      }

      if (maxConcurrency != null && maxConcurrency < 1) {
        throw new OpenSearchConfigurationException(
            "maxConcurrency must be at least 1, but was: " + maxConcurrency);
      }

      if (connectionTimeout != null && connectionTimeout.isNegative()) {
        throw new OpenSearchConfigurationException(
            "connectionTimeout must not be negative");
      }

      if (connectionAcquisitionTimeout != null && connectionAcquisitionTimeout.isNegative()) {
        throw new OpenSearchConfigurationException(
            "connectionAcquisitionTimeout must not be negative");
      }
    }
  }

  class HttpOpenSearchConfig
      implements SearchConfig
  {
    private URI uri;

    private String username;

    private String password;

    public URI getUri() {
      return uri;
    }

    public void setUri(final URI uri) {
      this.uri = uri;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(final String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(final String password) {
      this.password = password;
    }

    @Override
    public void validate() {
      if (uri == null) {
        throw new OpenSearchConfigurationException("OpenSearch URI is required");
      }

      String scheme = uri.getScheme();
      if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
        throw new OpenSearchConfigurationException(
            "OpenSearch URI must use HTTP or HTTPS scheme, but was: " + scheme);
      }

      if (StringUtils.isBlank(uri.getHost())) {
        throw new OpenSearchConfigurationException(
            "OpenSearch URI must have a valid host");
      }

      if (StringUtils.isBlank(username)) {
        throw new OpenSearchConfigurationException("OpenSearch username is required");
      }

      if (StringUtils.isBlank(password)) {
        throw new OpenSearchConfigurationException("OpenSearch password is required");
      }
    }
  }
}
