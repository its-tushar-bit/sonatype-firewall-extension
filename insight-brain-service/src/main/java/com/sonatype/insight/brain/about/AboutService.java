/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.about;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;

/**
 * Determines the actual target page when users browse to the server's about path.
 */
@Named
public class AboutService
{
  private final BaseUrl baseUrl;

  @Inject
  public AboutService(BaseUrl baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * Gets the URI the browser should redirect to in order to access the about page of the application.
   */
  public URI getDestination() {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(InsightBrainService.ABOUT_ASSET_PATH.substring(1) + "index.html");
    return uriBuilder.build();
  }
}
