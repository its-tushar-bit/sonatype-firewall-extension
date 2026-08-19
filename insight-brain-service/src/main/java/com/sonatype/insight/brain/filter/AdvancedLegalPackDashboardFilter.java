/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AdvancedLegalPackDashboardFilter
    implements UserFilterData
{
  private List<String> organizationFilters = new ArrayList<>();

  private List<String> applicationFilters = new ArrayList<>();

  private List<String> categoryFilters = new ArrayList<>();

  private List<String> stageTypeFilters = new ArrayList<>();

  private List<String> progressOptionsFilters = new ArrayList<>();

  public AdvancedLegalPackDashboardFilter() {
  }

  public AdvancedLegalPackDashboardFilter(
      final List<String> organizationFilters,
      final List<String> applicationFilters,
      final List<String> categoryFilters,
      final List<String> stageTypeFilters,
      final List<String> progressOptionsFilters)
  {
    this.organizationFilters = organizationFilters;
    this.applicationFilters = applicationFilters;
    this.categoryFilters = categoryFilters;
    this.stageTypeFilters = stageTypeFilters;
    this.progressOptionsFilters = progressOptionsFilters;
  }

  public List<String> getOrganizationFilters() {
    return organizationFilters;
  }

  public void setOrganizationFilters(final List<String> organizationFilters) {
    this.organizationFilters = organizationFilters;
  }

  public List<String> getApplicationFilters() {
    return applicationFilters;
  }

  public void setApplicationFilters(final List<String> applicationFilters) {
    this.applicationFilters = applicationFilters;
  }

  public List<String> getCategoryFilters() {
    return categoryFilters;
  }

  public void setCategoryFilters(final List<String> categoryFilters) {
    this.categoryFilters = categoryFilters;
  }

  public List<String> getStageTypeFilters() {
    return stageTypeFilters;
  }

  public void setStageTypeFilters(final List<String> stageTypeFilter) {
    this.stageTypeFilters = stageTypeFilter;
  }

  public List<String> getProgressOptionsFilters() {
    return progressOptionsFilters;
  }

  public void setProgressOptionsFilters(final List<String> progressOptionsFilters) {
    this.progressOptionsFilters = progressOptionsFilters;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AdvancedLegalPackDashboardFilter that = (AdvancedLegalPackDashboardFilter) o;
    return Objects.equals(getOrganizationFilters(), that.getOrganizationFilters()) &&
        Objects.equals(getApplicationFilters(), that.getApplicationFilters()) &&
        Objects.equals(getCategoryFilters(), that.getCategoryFilters()) &&
        Objects.equals(getStageTypeFilters(), that.getStageTypeFilters()) &&
        Objects.equals(getProgressOptionsFilters(), that.getProgressOptionsFilters());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getOrganizationFilters(), getApplicationFilters(), getCategoryFilters(), getStageTypeFilters(),
        getProgressOptionsFilters());
  }
}
