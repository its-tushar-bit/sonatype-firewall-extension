/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.dto;

import java.util.ArrayList;
import java.util.List;

public class ApiPageResult<T>
{
  private long total;

  private int page;

  private int pageSize;

  private long pageCount;

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
