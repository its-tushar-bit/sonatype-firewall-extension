/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.net.URI;
import java.time.Duration;

import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;
import com.sonatype.insight.brain.search.opensearch.OpenSearchConfigurationException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SearchConfigTest
{
  @Test
  public void testHttpConfig_DefaultMode() {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();

    // When/Then
    assertThat(config.getMode()).isEqualTo(SearchMode.HYBRID);
  }

  @Test
  public void testHttpConfig_OpenSearchMode() {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setMode(SearchMode.OPENSEARCH);

    // When/Then
    assertThat(config.getMode()).isEqualTo(SearchMode.OPENSEARCH);
  }

  @Test
  public void testHttpConfig_LuceneMode_SkipsValidation() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setMode(SearchMode.LUCENE);

    // When/Then - LUCENE mode ignores HTTP fields, so missing uri/username/password is fine
    assertThatCode(config::validate).doesNotThrowAnyException();
  }

  @Test
  public void testHttpConfig_OpenSearchMode_RequiresUri() {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setMode(SearchMode.OPENSEARCH);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("URI is required");
  }

  @Test
  public void testLuceneConfig_DefaultMode() {
    // Given
    LuceneSearchConfig config = new LuceneSearchConfig();

    // When/Then
    assertThat(config.getMode()).isEqualTo(SearchMode.LUCENE);
  }

  @Test
  public void testLuceneConfig_ExplicitMode() {
    // Given
    LuceneSearchConfig config = new LuceneSearchConfig();
    config.setMode(SearchMode.LUCENE);

    // When/Then
    assertThatCode(config::validate).doesNotThrowAnyException();
    assertThat(config.getMode()).isEqualTo(SearchMode.LUCENE);
  }

  @Test
  public void testLuceneConfig_NonLuceneMode_ThrowsException() {
    // Given
    LuceneSearchConfig config = new LuceneSearchConfig();
    config.setMode(SearchMode.OPENSEARCH);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(SearchConfigurationException.class)
        .hasMessageContaining("requires a search type");
  }

  @Test
  public void testLuceneConfig_HybridMode_ThrowsException() {
    LuceneSearchConfig config = new LuceneSearchConfig();
    config.setMode(SearchMode.HYBRID);

    assertThatThrownBy(config::validate)
        .isInstanceOf(SearchConfigurationException.class)
        .hasMessageContaining("requires a search type");
  }

  @Test
  public void testAwsConfig_DefaultMode() {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();

    // When/Then
    assertThat(config.getMode()).isEqualTo(SearchMode.HYBRID);
  }

  @Test
  public void testAwsConfig_ExplicitMode() {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setMode(SearchMode.OPENSEARCH);

    // When/Then
    assertThat(config.getMode()).isEqualTo(SearchMode.OPENSEARCH);
  }

  @Test
  public void testAwsConfig_LuceneMode_SkipsValidation() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setMode(SearchMode.LUCENE);

    // When/Then - LUCENE mode ignores AWS fields, so missing domain/region is fine
    assertThatCode(config::validate).doesNotThrowAnyException();
  }

  @Test
  public void testAwsConfig_OpenSearchMode_RequiresDomain() {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setMode(SearchMode.OPENSEARCH);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("domain URI is required");
  }

  @Test
  public void testAwsConfig_Valid() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");

    // When/Then - Should not throw
    assertThatCode(config::validate).doesNotThrowAnyException();
  }

  @Test
  public void testAwsConfig_NullDomain_ThrowsException() {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setRegion("us-east-1");

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("domain URI is required");
  }

  @Test
  public void testAwsConfig_NonHttpsScheme_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("http://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("must use HTTPS scheme");
  }

  @Test
  public void testAwsConfig_NoHost_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https:///path"));
    config.setRegion("us-east-1");

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("must have a valid host");
  }

  @Test
  public void testAwsConfig_NullRegion_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("region is required");
  }

  @Test
  public void testAwsConfig_EmptyRegion_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("");

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("region is required");
  }

  @Test
  public void testAwsConfig_InvalidRegionFormat_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("invalid-region");

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("Invalid AWS region format")
        .hasMessageContaining("Expected format: us-east-1, eu-west-1");
  }

  @Test
  public void testAwsConfig_VariousValidRegions() throws Exception {
    String[] validRegions = {
      "us-east-1", "us-west-2", "eu-west-1", "eu-central-1",
      "ap-south-1", "ap-northeast-1", "ca-central-1", "sa-east-1"
    };

    for (String region : validRegions) {
      AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
      config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
      config.setRegion(region);

      // When/Then - Should not throw
      assertThatCode(config::validate)
          .as("Region %s should be valid", region)
          .doesNotThrowAnyException();
    }
  }

  @Test
  public void testAwsConfig_NegativeMaxConcurrency_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");
    config.setMaxConcurrency(0);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("maxConcurrency must be at least 1");
  }

  @Test
  public void testAwsConfig_NegativeConnectionTimeout_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");
    config.setConnectionTimeout(Duration.ofSeconds(-1));

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("connectionTimeout must not be negative");
  }

  @Test
  public void testAwsConfig_NegativeConnectionAcquisitionTimeout_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");
    config.setConnectionAcquisitionTimeout(Duration.ofSeconds(-1));

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("connectionAcquisitionTimeout must not be negative");
  }

  @Test
  public void testAwsConfig_DefaultValues() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");

    // When/Then - Should use default values
    assertThat(config.getMaxConcurrency()).isEqualTo(50);
    assertThat(config.getConnectionTimeout()).isEqualTo(Duration.ofSeconds(30));
    assertThat(config.getConnectionAcquisitionTimeout()).isEqualTo(Duration.ofSeconds(10));
  }

  @Test
  public void testAwsConfig_BulkConfigurationDefaults() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");

    // When/Then - Should use default bulk configuration values
    assertThat(config.getBulkBatchSize()).isEqualTo(5000);
    assertThat(config.getBulkBatchDelayMs()).isEqualTo(250);
    assertThat(config.getBulkMaxRetries()).isEqualTo(15);
    assertThat(config.getBulkRetryBackoffSeconds()).isEqualTo(5);
    assertThat(config.getMaxBulkRetryBackoffSeconds()).isEqualTo(600);
  }

  @Test
  public void testAwsConfig_BulkConfigurationCustomValues() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");
    config.setBulkBatchSize(1000);
    config.setBulkBatchDelayMs(100);
    config.setBulkMaxRetries(5);
    config.setBulkRetryBackoffSeconds(10);

    // When/Then - Should use custom bulk configuration values
    assertThatCode(config::validate).doesNotThrowAnyException();
    assertThat(config.getBulkBatchSize()).isEqualTo(1000);
    assertThat(config.getBulkBatchDelayMs()).isEqualTo(100);
    assertThat(config.getBulkMaxRetries()).isEqualTo(5);
    assertThat(config.getBulkRetryBackoffSeconds()).isEqualTo(10);
  }

  @Test
  public void testAwsConfig_ZeroBulkBatchSize_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");
    config.setBulkBatchSize(0);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("bulkBatchSize must be at least 1");
  }

  @Test
  public void testAwsConfig_NegativeBulkBatchSize_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");
    config.setBulkBatchSize(-1);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("bulkBatchSize must be at least 1");
  }

  @Test
  public void testAwsConfig_NegativeBulkBatchDelayMs_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");
    config.setBulkBatchDelayMs(-1);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("bulkBatchDelayMs must not be negative");
  }

  @Test
  public void testAwsConfig_NegativeBulkMaxRetries_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");
    config.setBulkMaxRetries(-1);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("bulkMaxRetries must not be negative");
  }

  @Test
  public void testAwsConfig_NegativeBulkRetryBackoffSeconds_ThrowsException() throws Exception {
    // Given
    AwsHttpOpenSearchConfig config = new AwsHttpOpenSearchConfig();
    config.setDomain(new URI("https://search-domain.us-east-1.es.amazonaws.com"));
    config.setRegion("us-east-1");
    config.setBulkRetryBackoffSeconds(-1);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("bulkRetryBackoffSeconds must not be negative");
  }

  @Test
  public void testHttpConfig_Valid() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("secret");

    // When/Then - Should not throw
    assertThatCode(config::validate).doesNotThrowAnyException();
  }

  @Test
  public void testHttpConfig_ValidWithHttp() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("http://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("secret");

    // When/Then - Should not throw (HTTP is allowed for standalone)
    assertThatCode(config::validate).doesNotThrowAnyException();
  }

  @Test
  public void testHttpConfig_NullUri_ThrowsException() {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUsername("admin");
    config.setPassword("secret");

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("URI is required");
  }

  @Test
  public void testHttpConfig_InvalidScheme_ThrowsException() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("ftp://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("secret");

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("must use HTTP or HTTPS scheme");
  }

  @Test
  public void testHttpConfig_NoHost_ThrowsException() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https:///path"));
    config.setUsername("admin");
    config.setPassword("secret");

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("must have a valid host");
  }

  @Test
  public void testHttpConfig_NullUsername_ThrowsException() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https://localhost:9200"));
    config.setPassword("secret");

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("username is required");
  }

  @Test
  public void testHttpConfig_EmptyUsername_ThrowsException() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https://localhost:9200"));
    config.setUsername("");
    config.setPassword("secret");

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("username is required");
  }

  @Test
  public void testHttpConfig_NullPassword_ThrowsException() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https://localhost:9200"));
    config.setUsername("admin");

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("password is required");
  }

  @Test
  public void testHttpConfig_EmptyPassword_ThrowsException() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("");

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("password is required");
  }

  @Test
  public void testHttpConfig_BulkConfigurationDefaults() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("secret");

    // When/Then - Should use default bulk configuration values
    assertThat(config.getBulkBatchSize()).isEqualTo(10000);
    assertThat(config.getBulkBatchDelayMs()).isEqualTo(0);
    assertThat(config.getBulkMaxRetries()).isEqualTo(0);
    assertThat(config.getBulkRetryBackoffSeconds()).isEqualTo(0);
    assertThat(config.getMaxBulkRetryBackoffSeconds()).isEqualTo(30);
  }

  @Test
  public void testHttpConfig_BulkConfigurationCustomValues() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("secret");
    config.setBulkBatchSize(5000);
    config.setBulkBatchDelayMs(50);
    config.setBulkMaxRetries(3);
    config.setBulkRetryBackoffSeconds(2);

    // When/Then - Should use custom bulk configuration values
    assertThatCode(config::validate).doesNotThrowAnyException();
    assertThat(config.getBulkBatchSize()).isEqualTo(5000);
    assertThat(config.getBulkBatchDelayMs()).isEqualTo(50);
    assertThat(config.getBulkMaxRetries()).isEqualTo(3);
    assertThat(config.getBulkRetryBackoffSeconds()).isEqualTo(2);
  }

  @Test
  public void testHttpConfig_ZeroBulkBatchSize_ThrowsException() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("secret");
    config.setBulkBatchSize(0);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("bulkBatchSize must be at least 1");
  }

  @Test
  public void testHttpConfig_NegativeBulkBatchSize_ThrowsException() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("secret");
    config.setBulkBatchSize(-1);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("bulkBatchSize must be at least 1");
  }

  @Test
  public void testHttpConfig_NegativeBulkBatchDelayMs_ThrowsException() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("secret");
    config.setBulkBatchDelayMs(-1);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("bulkBatchDelayMs must not be negative");
  }

  @Test
  public void testHttpConfig_NegativeBulkMaxRetries_ThrowsException() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("secret");
    config.setBulkMaxRetries(-1);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("bulkMaxRetries must not be negative");
  }

  @Test
  public void testHttpConfig_NegativeBulkRetryBackoffSeconds_ThrowsException() throws Exception {
    // Given
    HttpOpenSearchConfig config = new HttpOpenSearchConfig();
    config.setUri(new URI("https://localhost:9200"));
    config.setUsername("admin");
    config.setPassword("secret");
    config.setBulkRetryBackoffSeconds(-1);

    // When/Then
    assertThatThrownBy(config::validate)
        .isInstanceOf(OpenSearchConfigurationException.class)
        .hasMessageContaining("bulkRetryBackoffSeconds must not be negative");
  }
}
