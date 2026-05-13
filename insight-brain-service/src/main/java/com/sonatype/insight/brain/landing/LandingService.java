/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;

/**
 * Determines the actual target page when users browse to the server's context root.
 */
@Named
public class LandingService
{
  public static final String ORIGIN_GUIDE = "guide";

  private final BaseUrl baseUrl;

  @Inject
  public LandingService(BaseUrl baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * Gets the URI the browser should redirect to in order to access the main page of the application.
   */
  public URI getDestination() {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(InsightBrainService.BRAIN_ASSET_PATH).path("index.html");
    return uriBuilder.build();
  }

  /**
   * Gets the URI the browser should redirect to in order to access the Guide SPA.
   */
  public URI getGuideDestination() {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(InsightBrainService.BRAIN_ASSET_PATH).path("guide/index.html");
    return uriBuilder.build();
  }
}
