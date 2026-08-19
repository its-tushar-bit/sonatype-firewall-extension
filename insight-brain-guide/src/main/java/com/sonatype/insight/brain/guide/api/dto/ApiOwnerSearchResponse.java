/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response for {@code GET /api/v2/policy-context/owners/search}. Orgs and apps are returned
 * as separate arrays so the picker can render All / Orgs / Apps tab counts client-side. Each
 * array carries its own truncation flag; tab counts reflect the returned (limit-capped)
 * results, not true totals.
 *
 * <p>
 * <b>Truncation semantics:</b> The {@code *Truncated} flags reflect the pre-permission-filter
 * state — i.e., whether more name-matching owners exist in the database than the limit, before
 * permission filtering is applied. This means the frontend may see {@code true} but receive
 * fewer than {@code limit} results if the caller lacks permission on some matches. This is
 * acceptable: the UI uses truncation only to show "more results available — refine search",
 * not to display exact counts.
 *
 * @param orgs organizations whose name contains the query substring, permission-
 *          filtered and limit-capped
 * @param orgsTruncated {@code true} if more matching orgs exist than were returned (before
 *          permission filtering)
 * @param apps applications whose name contains the query substring, permission-
 *          filtered and limit-capped
 * @param appsTruncated {@code true} if more matching apps exist than were returned (before
 *          permission filtering)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiOwnerSearchResponse(
    List<OrgSummary> orgs,
    boolean orgsTruncated,
    List<AppSummary> apps,
    boolean appsTruncated)
{
}
