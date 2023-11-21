/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryResultsDetailsFilter.SortField;

public class RepositoryResultsDetailsRequestDto
{
  public int page;

  public int pageSize;

  public List<Integer> threatLevelFilters;

  public List<MatchStateFilter> matchStateFilters;

  public List<ViolationStateFilter> violationStateFilters;

  public List<SearchFilter> searchFilters;

  public List<SortField> sortFields;

  public boolean aggregate;

  public enum MatchStateFilter
  {
    MATCH_STATE_ALL,
    MATCH_STATE_EXACT,
    MATCH_STATE_UNKNOWN
  }

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
      QUARANTINE_TIME,
      COMPONENT_COORDINATES
    }
  }
}
