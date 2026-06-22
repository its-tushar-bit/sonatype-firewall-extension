/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto.policy;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuidePolicyComplianceSummary(
    int highestThreatLevel,
    String worstAction,
    int activeViolationCount,
    int waivedViolationCount,
    Map<String, Integer> violationCountsByCategory)
{
}
