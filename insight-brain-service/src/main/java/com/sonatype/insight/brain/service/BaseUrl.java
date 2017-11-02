/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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

import static org.apache.commons.lang.StringUtils.isBlank;

@Named
@Singleton
public class BaseUrl
    extends AbstractInjectable<BaseUrl>
{

  static final String ERR_MSG_BASE_URL_NOT_CONFIGURED = "baseUrl is not configured.";

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
    String url = tryGetBaseUriWithEndingForwardSlash();
    if (url != null) {
      return url;
    }
    url = appConfig.getBaseUrl();
    if (!isBlank(url)) {
      return url;
    }
    throw new IllegalStateException(ERR_MSG_BASE_URL_NOT_CONFIGURED);
  }

  private String tryGetBaseUriWithEndingForwardSlash() {
    try {
      if (uriInfo == null) {
        return null;
      }
      String url = uriInfo.getBaseUri().toString();
      if (!url.endsWith("/")) {
        url += '/';
      }
      return url;
    }
    catch (IllegalStateException e) {
      // no request in scope
      return null;
    }
  }

  public UriBuilder redirect() {
    URI requestUri = uriInfo.getRequestUri();
    return UriBuilder.fromUri(get()).replaceQuery(requestUri.getRawQuery());
  }

}
