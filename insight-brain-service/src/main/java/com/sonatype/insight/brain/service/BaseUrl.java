/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

@Named
@Singleton
public class BaseUrl
    extends AbstractInjectable<BaseUrl>
{
  private final InsightConfig appConfig;

  @Context
  private final UriInfo uriInfo;

  @Inject
  public BaseUrl(final InsightConfig appConfig) {
    this.appConfig = appConfig;
    this.uriInfo = null; // set via reflection by Jersey's dependency injection
  }

  /**
   * public for testing only
   */
  public BaseUrl(final InsightConfig appConfig, final UriInfo uriInfo) {
    this.appConfig = appConfig;
    this.uriInfo = uriInfo;
  }

  public String get() {
    String url = appConfig.getBaseUrl();
    if (url != null && !url.isEmpty()) {
      return url;
    }
    url = uriInfo.getBaseUri().toString();
    if (!url.endsWith("/")) {
      url += '/';
    }
    return url;
  }

  public UriBuilder redirect() {
    URI requestUri = uriInfo.getRequestUri();
    return UriBuilder.fromUri(get()).replaceQuery(requestUri.getRawQuery());
  }

}
