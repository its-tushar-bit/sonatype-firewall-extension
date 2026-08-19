/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Date;
import java.util.Set;

public class ApiSourceControlEventFilterDTO
{
  private Set<String> applicationIds;

  private Date createdOnOrAfter;

  private boolean ascending;

  private int limit;

  private int offset;

  public ApiSourceControlEventFilterDTO() {
  }

  public ApiSourceControlEventFilterDTO(
      Set<String> applicationIds,
      long createdOnOrAfter,
      boolean ascending,
      int limit,
      int offset)
  {
    this.applicationIds = applicationIds;
    this.createdOnOrAfter = new Date(createdOnOrAfter);
    this.ascending = ascending;
    this.limit = limit;
    this.offset = offset;
  }

  public ApiSourceControlEventFilterDTO(
      long createdOnOrAfter,
      boolean ascending,
      int limit,
      int offset)
  {
    this.createdOnOrAfter = new Date(createdOnOrAfter);
    this.ascending = ascending;
    this.limit = limit;
    this.offset = offset;
  }

  public Set<String> getApplicationIds() {
    return applicationIds;
  }

  public void setApplicationIds(Set<String> applicationIds) {
    this.applicationIds = applicationIds;
  }

  public Date getCreatedOnOrAfter() {
    return createdOnOrAfter;
  }

  public void setCreatedOnOrAfter(Date createdOnOrAfter) {
    this.createdOnOrAfter = createdOnOrAfter;
  }

  public boolean isAscending() {
    return ascending;
  }

  public void setAscending(boolean ascending) {
    this.ascending = ascending;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  @Override
  public String toString() {
    return "ApiSourceControlEventFilter{" +
        "applicationIds=" + applicationIds +
        ", createdOnOrAfter=" + createdOnOrAfter +
        ", ascending=" + ascending +
        ", limit=" + limit +
        ", offset=" + offset +
        '}';
  }
}
