/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * Immutable request descriptor for a Global Search. A class, not a record, because Lucene
 * {@link Query} and {@link Sort} do not compose cleanly with record-generated
 * {@code equals}/{@code hashCode}.
 */
public final class GlobalSearchRequest
{
  private final Query baseQuery;

  private final Sort sort;

  private final int pageSize;

  private final List<String> searchAfter;

  public GlobalSearchRequest(
      final Query baseQuery,
      final Sort sort,
      final int pageSize,
      final List<String> searchAfter)
  {
    this.baseQuery = Objects.requireNonNull(baseQuery, "baseQuery");
    this.sort = sort;
    if (pageSize <= 0) {
      throw new IllegalArgumentException("pageSize must be > 0");
    }
    this.pageSize = pageSize;
    if (searchAfter == null) {
      this.searchAfter = Collections.emptyList();
    }
    else {
      // Reject nulls explicitly; List.copyOf would NPE with a less helpful message.
      if (searchAfter.stream().anyMatch(Objects::isNull)) {
        throw new IllegalArgumentException("searchAfter contains null elements");
      }
      this.searchAfter = List.copyOf(searchAfter);
    }
  }

  public Query baseQuery() {
    return baseQuery;
  }

  /** {@code null} means sort by relevance (_score desc). */
  public Sort sort() {
    return sort;
  }

  public int pageSize() {
    return pageSize;
  }

  /** Empty means first page. */
  public List<String> searchAfter() {
    return searchAfter;
  }
}
