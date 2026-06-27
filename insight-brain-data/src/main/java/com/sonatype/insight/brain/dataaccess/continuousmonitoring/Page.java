/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.continuousmonitoring;

import java.util.List;
import java.util.Objects;

/**
 * One page of eligibility results plus the cursor needed to fetch the next page (CLM-41005).
 * {@code hasMore == false} signals end-of-stream — the producer stops the cycle. When
 * {@code hasMore == true}, {@code nextCursor} is non-null and identifies the last row in
 * {@code rows} so the next {@code fetchPage} call advances strictly past it.
 * <p>
 * <strong>Invariants enforced by the compact constructor:</strong>
 * <ol>
 * <li>{@code hasMore == true} ⇒ {@code nextCursor != null} — paging cannot continue without a
 * cursor.</li>
 * <li>{@code rows.isEmpty()} ⇒ {@code hasMore == false} — an empty page with
 * {@code hasMore == true} would bypass the producer's empty-rows early-exit and the
 * safety-net WARN, silently aborting the cycle as {@code success(0)}. Returning
 * {@link #empty()} is the only correct way for a selector to signal "no rows left."</li>
 * </ol>
 *
 * @param <T> the flow-specific candidate type returned by the {@code EligibilitySelector}
 */
public record Page<T>(List<T> rows, EligibilityCursor nextCursor, boolean hasMore)
{
  public Page {
    Objects.requireNonNull(rows, "rows");
    if (hasMore && nextCursor == null) {
      throw new IllegalArgumentException("hasMore implies nextCursor != null");
    }
    if (rows.isEmpty() && hasMore) {
      throw new IllegalArgumentException(
          "empty rows with hasMore=true violates the EligibilitySelector contract — "
              + "an empty page must signal end-of-stream (use Page.empty())");
    }
  }

  /**
   * Convenience for the "no eligible work" / "end of stream after last page" case.
   */
  public static <T> Page<T> empty() {
    return new Page<>(List.of(), null, false);
  }
}
