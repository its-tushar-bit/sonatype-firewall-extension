/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess;

import java.util.Date;

public class CIApplicationFilter
{
  private int page;

  private int pageSize;

  private Date sinceUtcTimestamp;

  public CIApplicationFilter() {
  }

  public CIApplicationFilter(final int page, final int pageSize, final Date sinceUtcTimestamp) {
    this.page = page;
    this.pageSize = pageSize;
    this.sinceUtcTimestamp = sinceUtcTimestamp;
  }

  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public Date getSinceUtcTimestamp() {
    return sinceUtcTimestamp;
  }
}
