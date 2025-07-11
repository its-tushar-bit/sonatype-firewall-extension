/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.net.URI;

import com.sonatype.insight.brain.search.SearchConfig.HttpOpenSearchConfig;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Configuration for connection information for OpenSearch
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = HttpOpenSearchConfig.class, name = "http"),
})
public interface SearchConfig
{
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
  }
}
