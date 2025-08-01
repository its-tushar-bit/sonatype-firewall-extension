/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated response wrapper")
public class ApiPageResult<T>
{
  @Schema(description = "Total number of items")
  private long total;

  @Schema(description = "Current page number")
  private int page;

  @Schema(description = "Number of items per page")
  private int pageSize;

  @Schema(description = "Total number of pages")
  private long pageCount;

  @Schema(description = "List of items for the current page")
  private List<T> results;

  public ApiPageResult() {
    // needed for Jackson serialization
  }

  public ApiPageResult(final long total, final int page, final int pageSize) {
    this(total, page, pageSize, new ArrayList<>());
  }

  public ApiPageResult(final long total, final int page, final int pageSize, final List<T> results) {
    this.total = total;
    this.page = page;
    this.pageSize = pageSize;
    this.results = results;
    this.pageCount = PaginationResponseBuilder.calculateLastPage(pageSize, total);
  }

  public long getTotal() {
    return total;
  }

  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public List<T> getResults() {
    return results;
  }

  public long getPageCount() {
    return pageCount;
  }

  public void setResults(final List<T> results) {
    this.results = results;
  }
}
