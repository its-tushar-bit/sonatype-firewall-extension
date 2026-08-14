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
 * Configuration for the search backend.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = LuceneSearchConfig.class)
@JsonSubTypes({
  @JsonSubTypes.Type(value = HttpOpenSearchConfig.class, name = "http"),
  @JsonSubTypes.Type(value = AwsHttpOpenSearchConfig.class, name = "aws"),
})
public interface SearchConfig
{
  /**
   * Validates the configuration and throws an exception if invalid.
   *
   * @throws SearchConfigurationException if the configuration is invalid
   */
  void validate();

  SearchMode getMode();

  /**
   * Point-in-time keep-alive shared by
   * {@link com.sonatype.insight.brain.search.opensearch.OpenSearchIndexReadSession} (session lifetime)
   * and OpenSearch {@code rankGroupsByMaxMetric} composite scans (one bounded multi-page walk). Tuning
   * the default down for session behaviour also shortens ranked-groups PIT lifetime.
   */
  default String getPitKeepAlive() {
    return "15m";
  }

  /**
   * Abstract base class for OpenSearch configurations that provides common bulk indexing
   * configuration fields, getters/setters, and validation logic.
   */
  abstract class AbstractSearchConfig
      implements SearchConfig
  {
    private SearchMode mode;

    // Common bulk indexing configuration fields
    private Integer bulkBatchSize;

    private Integer bulkBatchDelayMs;

    private Integer bulkMaxRetries;

    private Integer bulkRetryBackoffSeconds;

    private String pitKeepAlive;

    private static final String DEFAULT_PIT_KEEP_ALIVE = "15m";

    private static final Pattern PIT_KEEP_ALIVE_PATTERN = Pattern.compile("^\\d+(ms|s|m|h|d)$");

    /**
     * @return the default batch size for bulk operations
     */
    protected abstract int getDefaultBulkBatchSize();

    /**
     * @return the default delay in milliseconds between batches
     */
    protected abstract int getDefaultBulkBatchDelayMs();

    /**
     * @return the default maximum number of retries for bulk operations
     */
    protected abstract int getDefaultBulkMaxRetries();

    /**
     * @return the default retry backoff in seconds
     */
    protected abstract int getDefaultBulkRetryBackoffSeconds();

    /**
     * @return the maximum allowed retry backoff in seconds
     */
    public abstract int getMaxBulkRetryBackoffSeconds();

    @Override
    public SearchMode getMode() {
      return mode != null ? mode : SearchMode.HYBRID;
    }

    public void setMode(final SearchMode mode) {
      this.mode = mode;
    }

    public Integer getBulkBatchSize() {
      return bulkBatchSize != null ? bulkBatchSize : getDefaultBulkBatchSize();
    }

    public void setBulkBatchSize(final Integer bulkBatchSize) {
      this.bulkBatchSize = bulkBatchSize;
    }

    public Integer getBulkBatchDelayMs() {
      return bulkBatchDelayMs != null ? bulkBatchDelayMs : getDefaultBulkBatchDelayMs();
    }

    public void setBulkBatchDelayMs(final Integer bulkBatchDelayMs) {
      this.bulkBatchDelayMs = bulkBatchDelayMs;
    }

    public Integer getBulkMaxRetries() {
      return bulkMaxRetries != null ? bulkMaxRetries : getDefaultBulkMaxRetries();
    }

    public void setBulkMaxRetries(final Integer bulkMaxRetries) {
      this.bulkMaxRetries = bulkMaxRetries;
    }

    public Integer getBulkRetryBackoffSeconds() {
      return bulkRetryBackoffSeconds != null ? bulkRetryBackoffSeconds : getDefaultBulkRetryBackoffSeconds();
    }

    public void setBulkRetryBackoffSeconds(final Integer bulkRetryBackoffSeconds) {
      this.bulkRetryBackoffSeconds = bulkRetryBackoffSeconds;
    }

    @Override
    public String getPitKeepAlive() {
      return pitKeepAlive != null ? pitKeepAlive : DEFAULT_PIT_KEEP_ALIVE;
    }

    public void setPitKeepAlive(final String pitKeepAlive) {
      this.pitKeepAlive = pitKeepAlive;
    }

    /**
     * Validates bulk indexing configuration parameters.
     * Subclasses should call this method from their validate() implementation.
     *
     * @throws OpenSearchConfigurationException if bulk configuration is invalid
     */
    protected void validateBulkConfig() {
      if (bulkBatchSize != null && bulkBatchSize < 1) {
        throw new OpenSearchConfigurationException(
            "bulkBatchSize must be at least 1, but was: " + bulkBatchSize);
      }

      if (bulkBatchDelayMs != null && bulkBatchDelayMs < 0) {
        throw new OpenSearchConfigurationException(
            "bulkBatchDelayMs must not be negative, but was: " + bulkBatchDelayMs);
      }

      if (bulkMaxRetries != null && bulkMaxRetries < 0) {
        throw new OpenSearchConfigurationException(
            "bulkMaxRetries must not be negative, but was: " + bulkMaxRetries);
      }

      if (bulkRetryBackoffSeconds != null && bulkRetryBackoffSeconds < 0) {
        throw new OpenSearchConfigurationException(
            "bulkRetryBackoffSeconds must not be negative, but was: " + bulkRetryBackoffSeconds);
      }

      if (pitKeepAlive != null && !PIT_KEEP_ALIVE_PATTERN.matcher(pitKeepAlive).matches()) {
        throw new OpenSearchConfigurationException(
            "pitKeepAlive must match ^\\d+(ms|s|m|h|d)$, but was: " + pitKeepAlive);
      }
    }
  }

  class AwsHttpOpenSearchConfig
      extends AbstractSearchConfig
  {
    private static final Pattern AWS_REGION_PATTERN = Pattern.compile("^[a-z]{2}-[a-z]+-\\d{1}$");

    private static final int DEFAULT_MAX_CONCURRENCY = 50;

    private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(30);

    private static final Duration DEFAULT_CONNECTION_ACQUISITION_TIMEOUT = Duration.ofSeconds(10);

    // Bulk indexing throttling defaults
    private static final int DEFAULT_BULK_BATCH_SIZE = 5000;

    private static final int DEFAULT_BULK_BATCH_DELAY_MS = 250;

    private static final int DEFAULT_BULK_MAX_RETRIES = 15;

    private static final int DEFAULT_BULK_RETRY_BACKOFF_SECONDS = 5;

    private static final int MAX_BULK_RETRY_BACKOFF_SECONDS = 600;

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
      return connectionAcquisitionTimeout != null
          ? connectionAcquisitionTimeout
          : DEFAULT_CONNECTION_ACQUISITION_TIMEOUT;
    }

    public void setConnectionAcquisitionTimeout(final Duration connectionAcquisitionTimeout) {
      this.connectionAcquisitionTimeout = connectionAcquisitionTimeout;
    }

    @Override
    protected int getDefaultBulkBatchSize() {
      return DEFAULT_BULK_BATCH_SIZE;
    }

    @Override
    protected int getDefaultBulkBatchDelayMs() {
      return DEFAULT_BULK_BATCH_DELAY_MS;
    }

    @Override
    protected int getDefaultBulkMaxRetries() {
      return DEFAULT_BULK_MAX_RETRIES;
    }

    @Override
    protected int getDefaultBulkRetryBackoffSeconds() {
      return DEFAULT_BULK_RETRY_BACKOFF_SECONDS;
    }

    /**
     * Returns the upper bound allowed for bulk retry backoff.
     * <p>
     * This value is intentionally a fixed constant rather than a configurable
     * property. It serves as a safety cap to prevent misconfiguration that
     * could lead to excessively long backoff periods (e.g., hours or days)
     * which would severely degrade indexing throughput and cause operational issues.
     * The 600-second (10-minute) maximum ensures that even with aggressive
     * exponential backoff configurations, the system remains responsive.
     */
    @Override
    public int getMaxBulkRetryBackoffSeconds() {
      return MAX_BULK_RETRY_BACKOFF_SECONDS;
    }

    @Override
    public void validate() {
      if (getMode() == SearchMode.LUCENE) {
        return;
      }

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

      validateBulkConfig();
    }
  }

  class HttpOpenSearchConfig
      extends AbstractSearchConfig
  {
    // Bulk indexing throttling defaults
    private static final int DEFAULT_BULK_BATCH_SIZE = 10000;

    private static final int DEFAULT_BULK_BATCH_DELAY_MS = 0;

    private static final int DEFAULT_BULK_MAX_RETRIES = 0;

    private static final int DEFAULT_BULK_RETRY_BACKOFF_SECONDS = 0;

    private static final int MAX_BULK_RETRY_BACKOFF_SECONDS = 30;

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
    protected int getDefaultBulkBatchSize() {
      return DEFAULT_BULK_BATCH_SIZE;
    }

    @Override
    protected int getDefaultBulkBatchDelayMs() {
      return DEFAULT_BULK_BATCH_DELAY_MS;
    }

    @Override
    protected int getDefaultBulkMaxRetries() {
      return DEFAULT_BULK_MAX_RETRIES;
    }

    @Override
    protected int getDefaultBulkRetryBackoffSeconds() {
      return DEFAULT_BULK_RETRY_BACKOFF_SECONDS;
    }

    /**
     * Returns the upper bound allowed for bulk retry backoff.
     * <p>
     * This value is intentionally a fixed constant rather than a configurable
     * property. It serves as a safety cap to prevent misconfiguration that
     * could lead to excessively long backoff periods which would degrade throughput.
     * The 30-second maximum for HTTP OpenSearch is lower than AWS (600s) since
     * self-managed instances typically have more predictable performance characteristics.
     */
    @Override
    public int getMaxBulkRetryBackoffSeconds() {
      return MAX_BULK_RETRY_BACKOFF_SECONDS;
    }

    @Override
    public void validate() {
      if (getMode() == SearchMode.LUCENE) {
        return;
      }

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

      validateBulkConfig();
    }
  }
}
