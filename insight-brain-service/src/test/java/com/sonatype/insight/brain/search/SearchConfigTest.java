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
}
