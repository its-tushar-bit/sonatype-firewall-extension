/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import com.sonatype.insight.brain.search.SearchConfig.AwsOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Configuration for connection information for OpenSearch
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = HttpOpenSearchConfig.class, name = "http"),
    @JsonSubTypes.Type(value = AwsOpenSearchConfig.class, name = "aws")
})
public interface SearchConfig
{
  class HttpOpenSearchConfig
      implements SearchConfig
  {
    private String hostname;

    private String scheme;

    private int port;

    private String username;

    private String password;

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

    public String getHostname() {
      return hostname;
    }

    public void setHostname(final String hostname) {
      this.hostname = hostname;
    }

    public String getScheme() {
      return scheme;
    }

    public void setScheme(final String scheme) {
      this.scheme = scheme;
    }

    public int getPort() {
      return port;
    }

    public void setPort(final int port) {
      this.port = port;
    }
  }

  class AwsOpenSearchConfig
      implements SearchConfig
  {
    private String endpoint;

    private String region;

    public String getEndpoint() {
      return endpoint;
    }

    public void setEndpoint(final String endpoint) {
      this.endpoint = endpoint;
    }

    public String getRegion() {
      return region;
    }

    public void setRegion(final String region) {
      this.region = region;
    }
  }
}
