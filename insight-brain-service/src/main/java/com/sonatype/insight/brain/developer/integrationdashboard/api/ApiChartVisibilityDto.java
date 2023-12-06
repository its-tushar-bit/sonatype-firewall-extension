/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard.api;

import java.util.Objects;

public class ApiChartVisibilityDto
{
  private boolean isUsageOverTimeChartsShown;

  public ApiChartVisibilityDto(final boolean isUsageOverTimeChartsShown) {
    this.isUsageOverTimeChartsShown = isUsageOverTimeChartsShown;
  }

  public ApiChartVisibilityDto() {}

  public boolean isUsageOverTimeChartsShown() {
    return isUsageOverTimeChartsShown;
  }

  public void setUsageOverTimeChartsShown(final boolean usageOverTimeChartsShown) {
    isUsageOverTimeChartsShown = usageOverTimeChartsShown;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ApiChartVisibilityDto that = (ApiChartVisibilityDto) o;

    return isUsageOverTimeChartsShown == that.isUsageOverTimeChartsShown;
  }

  @Override
  public int hashCode() {
    return Objects.hash(isUsageOverTimeChartsShown);
  }

  @Override
  public String toString() {
    return "ApiChartVisibilityDto{" +
        "isUsageOverTimeChartsShown=" + isUsageOverTimeChartsShown +
        '}';
  }
}
