/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response for {@code GET /api/v2/policy-context/owners/orgs/{orgId}/apps}.
 *
 * @param apps applications directly under the given org that the caller has
 *          {@code EVALUATE_COMPONENT} on, sorted alphabetically and capped by limit
 * @param truncated {@code true} if the org contains more permitted apps than were returned
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiOrgAppsResponse(
    List<AppSummary> apps,
    boolean truncated)
{
}
