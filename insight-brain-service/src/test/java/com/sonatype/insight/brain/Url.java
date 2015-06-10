/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import javax.ws.rs.core.UriBuilder;

/**
 * Builder-style utility to create URLs for REST calls.
 */
class Url
{
  private UriBuilder builder;

  private Object[] parameters;

  public Url(String base) {
    builder = UriBuilder.fromUri(base);
    parameters = new Object[0];
  }

  public Url path(String... paths) {
    for (String path : paths) {
      builder.path(path);
    }
    return this;
  }

  public Url query(String name, Object value) {
    if (value != null) {
      builder.replaceQueryParam(name, value);
    }
    return this;
  }

  public Url query(String query) {
    builder.replaceQuery(query);
    return this;
  }

  public Url parameter(Object... parameters) {
    this.parameters = parameters.clone();
    return this;
  }

  public String build() {
    return builder.build(parameters).toString();
  }
}
