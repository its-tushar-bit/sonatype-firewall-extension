/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsForImageContainerFilter.SortField;

public class RepositoryResultsForImageContainerRequestDto
{
  public int page;

  public int pageSize;

  public List<Integer> threatLevelFilters;

  public List<ViolationStateFilter> violationStateFilters;

  public List<SearchFilter> searchFilters;

  public List<SortField> sortFields;

  public boolean aggregate;

  public enum ViolationStateFilter
  {
    VIOLATION_STATE_ALL,
    VIOLATION_STATE_NOT_VIOLATING,
    VIOLATION_STATE_OPEN,
    VIOLATION_STATE_QUARANTINED,
    VIOLATION_STATE_WAIVED
  }

  public static class SearchFilter
  {
    public FilterableField filterableField;

    public String value;

    public enum FilterableField
    {
      POLICY_NAME,
      POLICY_THREAT_LEVEL,
      QUARANTINE_TIME,
      OBJECT_NAME,
      VIOLATION_COUNT
    }
  }
}
