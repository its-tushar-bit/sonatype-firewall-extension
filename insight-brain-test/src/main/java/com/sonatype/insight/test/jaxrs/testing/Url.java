/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-jaxrs-testing
package com.sonatype.insight.test.jaxrs.testing;

import jakarta.ws.rs.core.UriBuilder;

/**
 * Builder-style utility to create URLs for REST calls.
 */
class Url
{
  private UriBuilder pathBuilder;

  private UriBuilder queryBuilder;

  private Object[] parameters;

  public Url(String base) {
    pathBuilder = UriBuilder.fromUri(base).replaceQuery(null);
    queryBuilder = UriBuilder.fromUri(base).replacePath(null);
    parameters = new Object[0];
  }

  public Url path(String... paths) {
    for (String path : paths) {
      pathBuilder.path(path);
    }
    return this;
  }

  public Url query(String name, Object[] values) {
    Object[] encoded = null;
    if (values != null) {
      // NOTE: replaceQueryParam() recognizes template parameters, i.e. "{" and "}" have special meaning. In particular,
      // newer versions of Jersey (1.18+) will not encode enclosed characters (which are assumed to denote a parameter
      // name). This doesn't work out well for JSON-encoded parameters, so we pre-encode the braces ourselves (and
      // UriBuilder won't double-encode already percent-encoded characters) so any enclosed characters can get encoded.
      encoded = new String[values.length];
      for (int i = 0; i < values.length; i++) {
        encoded[i] = values[i].toString().replace("{", "%7B").replace("}", "%7D");
      }
    }
    queryBuilder.replaceQueryParam(name, encoded);
    return this;
  }

  public Url query(String query) {
    queryBuilder.replaceQuery(query);
    return this;
  }

  public Url parameter(Object... parameters) {
    this.parameters = parameters.clone();
    return this;
  }

  public String build() {
    UriBuilder builder = UriBuilder.fromUri(pathBuilder.build(parameters, false /* encodeSlashInPath */));
    // to avoid mistaking JSON-encoded query params as template parameters they use a parameter-less builder
    String query = queryBuilder.build().getRawQuery();
    return builder.replaceQuery(query).build().toString();
  }
}
