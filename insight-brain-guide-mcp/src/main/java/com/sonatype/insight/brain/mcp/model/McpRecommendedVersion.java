/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpRecommendedVersion(
    String version,
    String breakingChangesCount,
    Map<String, Double> directVulnerabilities,
    Map<String, Double> transitiveVulnerabilities,
    Map<String, Integer> licenseThreatLevels,
    List<McpVulnerableMethod> vulnerableMethods,
    Integer developerTrustScore)
{
}
