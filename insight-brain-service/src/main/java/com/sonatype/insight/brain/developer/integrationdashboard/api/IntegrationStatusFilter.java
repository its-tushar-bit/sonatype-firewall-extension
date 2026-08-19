/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard.api;

import org.apache.commons.lang3.StringUtils;

public class IntegrationStatusFilter
{
  private static final String DEFAULT_ORDER_BY = "NAME";

  private int page;

  private int pageSize;

  private String optionalOrderBy = DEFAULT_ORDER_BY;

  private String optionalFilterApplicationNamesBy = StringUtils.EMPTY;

  public IntegrationStatusFilter() {
  }

  public IntegrationStatusFilter(final int page, final int pageSize) {
    this.page = page;
    this.pageSize = pageSize;
    this.optionalOrderBy = DEFAULT_ORDER_BY;
    this.optionalFilterApplicationNamesBy = StringUtils.EMPTY;
  }

  public int getPage() {
    return page;
  }

  public int getPageSize() {
    return pageSize;
  }

  public String getOptionalOrderBy() {
    return optionalOrderBy;
  }

  public String getOptionalFilterApplicationNamesBy() {
    return optionalFilterApplicationNamesBy;
  }

  public void setOptionalOrderBy(final String optionalOrderBy) {
    this.optionalOrderBy = optionalOrderBy;
  }

  public void setOptionalFilterApplicationNamesBy(final String optionalFilterApplicationNamesBy) {
    this.optionalFilterApplicationNamesBy = optionalFilterApplicationNamesBy;
  }
}
