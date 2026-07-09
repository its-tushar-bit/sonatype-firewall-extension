/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.List;

/**
 * Cursor-paged slice of the SLO violation feed. To fetch the next slice, callers copy the fields of
 * {@link #nextPageCursor} straight onto the request's {@code updatedSince} and {@code afterViolationId} query
 * parameters (the cursor field names match the params on purpose). {@code nextPageCursor} is {@code null} on the last
 * page; the walk is complete when it is {@code null}, regardless of whether the last non-empty page was full. When the
 * match count is an exact multiple of the requested page size the final follow-up request returns an empty
 * {@code results} list with a {@code null} cursor — a normal keyset-pagination trailing empty page. The response
 * deliberately does not echo the requested page size, since page size carries no server-side navigation signal and
 * comparing it against {@code results.size()} to infer "last page" is unreliable (partial and trailing empty pages
 * both break that comparison — the only end-of-walk signal is {@code nextPageCursor == null}).
 * <p>
 * <b>Client dedupe contract (canonical):</b> the continuation point is frozen at the moment this page was produced; it
 * is not re-resolved from the cursor row's current position. This guarantees rows are never silently skipped when a
 * violation's update time moves while a caller walks the feed, at the cost of possibly re-delivering a row whose update
 * time moved after the caller saw it. Callers must therefore deduplicate by {@code violationId} — the same guidance
 * that already applies to {@code updatedSince} watermark polling.
 *
 * @param total count of violations matching <em>this request</em> (i.e. at/after the supplied {@code updatedSince});
 *          because {@code updatedSince} advances as the caller pages, this is the grand total on the first page and
 *          effectively the remaining count on later pages. Best-effort: the count and the slice are read in separate
 *          transactions, so it may be slightly stale under a concurrent scan.
 * @param results violations in ascending update-time order
 * @param nextPageCursor pass its fields back as {@code updatedSince} + {@code afterViolationId} to fetch the next
 *          slice; {@code null} when this slice is the last page
 */
public record SloViolationPageResult(
    long total,
    List<SloViolation> results,
    SloViolationFeedCursor nextPageCursor)
{
}
