/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.model.Owner;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public record NonBreakingRecommendationTelemetryStats(
    ApiVersionChangeOptionType recommendedNonBreakingVersionChangeOptionType,
    String recommendedNonBreakingVersion,
    String recommendedNonBreakingVersionPackageUrl,
    SourceEndpoint sourceEndpoint,
    Owner owner)
{
  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(recommendedNonBreakingVersionChangeOptionType)
        .append(recommendedNonBreakingVersion)
        .append(recommendedNonBreakingVersionPackageUrl)
        .append(sourceEndpoint)
        .append(owner.getType())
        .append(owner.getId())
        .toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return new EqualsBuilder()
        .append(recommendedNonBreakingVersionChangeOptionType,
            ((NonBreakingRecommendationTelemetryStats) obj).recommendedNonBreakingVersionChangeOptionType)
        .append(recommendedNonBreakingVersion,
            ((NonBreakingRecommendationTelemetryStats) obj).recommendedNonBreakingVersion)
        .append(recommendedNonBreakingVersionPackageUrl,
            ((NonBreakingRecommendationTelemetryStats) obj).recommendedNonBreakingVersionPackageUrl)
        .append(sourceEndpoint, ((NonBreakingRecommendationTelemetryStats) obj).sourceEndpoint)
        .append(owner.getType(), ((NonBreakingRecommendationTelemetryStats) obj).owner.getType())
        .append(owner.getId(), ((NonBreakingRecommendationTelemetryStats) obj).owner.getId())
        .isEquals();
  }

  public enum SourceEndpoint
  {
    IDE,
    REPO_MANAGER,
    COMPONENT_INFO,
    QUARANTINED_COMPONENT,
    API_COMPONENT_REMEDIATION,
    PULL_REQUEST_COMMENTING,
    SCAN_POLICY_EVALUATOR,
    DEVELOPMENT_PRIORITIZATION,
    MANUAL_PULL_REQUEST,
  }
}
