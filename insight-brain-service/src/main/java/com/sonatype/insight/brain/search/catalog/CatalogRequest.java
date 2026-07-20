/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Pagination differs by source: the catalog source uses {@code page} as an offset (offset-from-page)
 * and ignores {@code sort}/{@code searchAfter}; the local source uses an opaque {@code searchAfter}
 * cursor and ignores {@code page} (page&gt;1 without a cursor is rejected).
 */
public final class CatalogRequest
{
  private final String entityType;

  private final String source;

  private final Map<String, Object> filters;

  private final Integer page;

  private final Integer pageSize;

  private final String sort;

  private final String searchAfter;

  private final boolean includeFacets;

  @JsonCreator
  public CatalogRequest(
      @JsonProperty("entityType") final String entityType,
      @JsonProperty("source") final String source,
      @JsonProperty("filters") final Map<String, Object> filters,
      @JsonProperty("page") final Integer page,
      @JsonProperty("pageSize") final Integer pageSize,
      @JsonProperty("sort") final String sort,
      @JsonProperty("searchAfter") final String searchAfter,
      @JsonProperty("includeFacets") final Boolean includeFacets)
  {
    this.entityType = entityType;
    this.source = source;
    this.filters = filters == null ? Collections.emptyMap() : new LinkedHashMap<>(filters);
    this.page = page;
    this.pageSize = pageSize;
    this.sort = sort;
    this.searchAfter = searchAfter;
    this.includeFacets = Boolean.TRUE.equals(includeFacets);
  }

  public String getEntityType() {
    return entityType;
  }

  public String getSource() {
    return source;
  }

  public Map<String, Object> getFilters() {
    return Collections.unmodifiableMap(filters);
  }

  public Integer getPage() {
    return page;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public String getSort() {
    return sort;
  }

  public String getSearchAfter() {
    return searchAfter;
  }

  public boolean isIncludeFacets() {
    return includeFacets;
  }
}
