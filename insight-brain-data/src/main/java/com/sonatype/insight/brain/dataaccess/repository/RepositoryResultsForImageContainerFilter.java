/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RepositoryResultsForImageContainerFilter
{
  public int page;

  public int pageSize;

  public Set<String> violationStateFilters;

  public List<Integer> threatLevelFilters;

  public Map<String, String> searchFilters;

  public List<SortField> sortFields;

  public boolean aggregate;

  public static class SortField
  {
    public SortableField sortableField;

    public boolean asc;

    public int sortPriority;

    public enum SortableField
    {
      POLICY_NAME,
      VIOLATION_COUNT,
      OBJECT_NAME,
      QUARANTINE_TIME,
      POLICY_THREAT_LEVEL
    }
  }
}
