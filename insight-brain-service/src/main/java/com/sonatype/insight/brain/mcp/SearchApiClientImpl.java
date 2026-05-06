/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp;

import java.util.Map;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.mcp.search.SearchApiClient;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named
@Singleton
public class SearchApiClientImpl
    implements SearchApiClient
{
  private final HdsClient hdsClient;

  @Inject
  public SearchApiClientImpl(HdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  @Override
  public String getComponentByPurl(String purl) {
    return hdsClient.get(String.class, "rest/search/components/detail", Map.of("purl", purl));
  }

  @Override
  public String getLatestComponentVersion(String purl) {
    return hdsClient.post(String.class, "rest/search/components/latest-version", Map.of("purl", purl));
  }

  @Override
  public String getRecommendations(String purl) {
    return hdsClient.post(String.class, "rest/search/recommendations", Map.of("purl", purl));
  }
}
