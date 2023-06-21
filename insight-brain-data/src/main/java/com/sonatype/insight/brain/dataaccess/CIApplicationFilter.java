/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.apache.commons.lang3.StringUtils;

public class CIApplicationFilter
{
  private static final String DEFAULT_ORDER_BY = "-TOTAL_RISK";

  private int page;

  private int pageSize;

  private Date sinceUtcTimestamp;

  @JsonSetter(nulls = Nulls.SKIP)
  private String optionalOrderBy = DEFAULT_ORDER_BY;

  @JsonSetter(nulls = Nulls.SKIP)
  private String optionalFilterApplicationNamesBy = StringUtils.EMPTY;

  public CIApplicationFilter() {
  }

  public CIApplicationFilter(final int page, final int pageSize, final Date sinceUtcTimestamp) {
    this.page = page;
    this.pageSize = pageSize;
    this.sinceUtcTimestamp = sinceUtcTimestamp;
    this.optionalOrderBy = DEFAULT_ORDER_BY;
    this.optionalFilterApplicationNamesBy = StringUtils.EMPTY;
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

  public String getOptionalOrderBy() {
    return optionalOrderBy;
  }

  public String getOptionalFilterApplicationNamesBy() {
    return optionalFilterApplicationNamesBy;
  }

  public CIApplicationFilter setOptionalOrderBy(final String optionalOrderBy) {
    this.optionalOrderBy = optionalOrderBy;
    return this;
  }

  public CIApplicationFilter setOptionalFilterApplicationNamesBy(final String optionalFilterApplicationNamesBy) {
    this.optionalFilterApplicationNamesBy = optionalFilterApplicationNamesBy;
    return this;
  }
}
