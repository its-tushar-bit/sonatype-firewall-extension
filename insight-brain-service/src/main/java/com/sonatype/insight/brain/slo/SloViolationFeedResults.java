/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

/**
 * Result envelope for the SLO violation feed.
 *
 * @param stage the resolved lifecycle stage the feed was queried for
 * @param latestScanId the scan id of the <em>latest</em> policy evaluation for this application/stage (the scan the
 *          enrichment provenance is derived from); this is a feed-level value, not a per-violation scan id
 * @param violations the cursor-paged violations for the stage
 */
public record SloViolationFeedResults(
    String stage,
    String latestScanId,
    SloViolationPageResult violations)
{
}
