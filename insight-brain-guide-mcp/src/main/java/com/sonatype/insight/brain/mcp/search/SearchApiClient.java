/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp.search;

public interface SearchApiClient
{
  /** Get component detail by PURL. Returns JSON string from search-server. */
  String getComponentByPurl(String purl);

  /** Get latest version of a component by PURL. */
  String getLatestComponentVersion(String purl);

  /** Get upgrade recommendations for a component by PURL. */
  String getRecommendations(String purl);
}
