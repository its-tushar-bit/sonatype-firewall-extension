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

  /**
   * Flag to indicate if the request is from the Bulk Waiver page.
   * When true:
   * - pageSize is limited to MAX_BULK_WAIVER_PAGE_SIZE (1000)
   * - All filters from the request are applied by backend (required data only principle)
   * - threat level 0 violations are excluded from results and count calculations
   * - filterCount and totalCount are populated in the response
   */
  public boolean isBulkWaiverPage;

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
      COMPONENT_COORDINATES,
      REPOSITORY_ID,
      REPOSITORY_MANAGER_ID
    }
  }
}
