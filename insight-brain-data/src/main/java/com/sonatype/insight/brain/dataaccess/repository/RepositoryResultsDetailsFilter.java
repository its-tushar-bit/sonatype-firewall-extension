/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RepositoryResultsDetailsFilter
{
  public int page;

  public int pageSize;

  public String matchStateFilter;

  public Set<String> violationStateFilters;

  public List<Integer> threatLevelFilters;

  public Map<String, String> searchFilters;

  public List<SortField> sortFields;

  public boolean aggregate;

  public Map<String, List<String>> formatExclusionPatterns;

  public static class SortField
  {
    public SortableField sortableField;

    public boolean asc;

    public int sortPriority;

    public enum SortableField
    {
      POLICY_THREAT_LEVEL,
      POLICY_NAME,
      COMPONENT_COORDINATES,
      QUARANTINE_TIME
    }
  }
}
