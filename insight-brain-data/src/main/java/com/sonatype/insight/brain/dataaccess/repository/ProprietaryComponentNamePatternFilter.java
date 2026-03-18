/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

/**
 * @since 1.152
 */
public class ProprietaryComponentNamePatternFilter
{
  public int page;

  public int pageSize;

  public List<SearchFilter> searchFilters;

  public List<SortField> sortFields;

  public static class SortField
  {
    public SortableField sortableField;

    public boolean asc;

    public int sortPriority;

    public SortField() {
    }

    public SortField(SortableField sortableField, boolean asc, int sortPriority) {
      this.sortableField = sortableField;
      this.asc = asc;
      this.sortPriority = sortPriority;
    }

    public static enum SortableField
    {
      PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME,
      REPOSITORY_MANAGER_INSTANCE_ID_OR_NAME,
      REPOSITORY_PUBLIC_ID,
      ENABLED
    }
  }

  public static class SearchFilter
  {
    public FilterableField filterableField;

    public String value;

    public SearchFilter() {
    }

    public SearchFilter(FilterableField filterableField, String value) {
      this.filterableField = filterableField;
      this.value = value;
    }

    public static enum FilterableField
    {
      PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME
    }
  }
}
