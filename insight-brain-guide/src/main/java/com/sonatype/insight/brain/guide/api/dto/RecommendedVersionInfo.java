/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;
import java.util.Map;

import com.sonatype.guide.api.dto.RecommendationVulnerableMethod;
import com.sonatype.guide.api.dto.RecommendedVersion;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendedVersionInfo(
    String version,
    String breakingChangesCount,
    Map<String, Double> directVulnerabilities,
    Map<String, Double> transitiveVulnerabilities,
    Map<String, Integer> licenseThreatLevels,
    @JsonDeserialize(
        contentAs = GuideRecommendationVulnerableMethod.class) List<? extends RecommendationVulnerableMethod> vulnerableMethods,
    Integer developerTrustScore,
    Double maxSeverity,
    GuidePolicyCompliance policyCompliance)
    implements RecommendedVersion
{
}
