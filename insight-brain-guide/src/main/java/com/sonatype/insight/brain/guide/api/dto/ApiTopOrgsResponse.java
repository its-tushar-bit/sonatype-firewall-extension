/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response for {@code GET /api/v2/policy-context/owners/top-orgs}.
 *
 * @param orgs top organizations the caller has {@code EVALUATE_APPLICATION} on,
 *          sorted alphabetically and capped by the request limit
 * @param totalOrgCount total number of orgs matching the permission filter, so the frontend
 *          can render a "+ N more — search to find" hint when
 *          {@code totalOrgCount > orgs.size()}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiTopOrgsResponse(
    List<OrgSummary> orgs,
    long totalOrgCount)
{
}
