/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.mcp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpRecommendationItem(
    String packageUrl,
    boolean success,
    String outcome,
    McpRecommendedVersion fromVersion,
    List<McpRecommendedVersion> toVersions,
    String error)
{
  public static McpRecommendationItem success(
      String packageUrl,
      String outcome,
      McpRecommendedVersion fromVersion,
      List<McpRecommendedVersion> toVersions)
  {
    return new McpRecommendationItem(packageUrl, true, outcome, fromVersion, toVersions, null);
  }

  public static McpRecommendationItem failure(String packageUrl, String error) {
    return new McpRecommendationItem(packageUrl, false, null, null, null, error);
  }
}
