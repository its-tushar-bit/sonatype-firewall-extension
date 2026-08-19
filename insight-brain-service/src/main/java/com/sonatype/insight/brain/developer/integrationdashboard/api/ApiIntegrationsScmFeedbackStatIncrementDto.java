/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard.api;

import java.util.Objects;

public class ApiIntegrationsScmFeedbackStatIncrementDto
{
  private long dateTimeMillis;

  private int totalNumberOfApps;

  private int totalNumberOfAppsWithScmEnabled;

  public ApiIntegrationsScmFeedbackStatIncrementDto(
      final long dateTimeMillis,
      final int totalNumberOfApps,
      final int totalNumberOfAppsWithScmEnabled)
  {
    this.dateTimeMillis = dateTimeMillis;
    this.totalNumberOfApps = totalNumberOfApps;
    this.totalNumberOfAppsWithScmEnabled = totalNumberOfAppsWithScmEnabled;
  }

  public long getDateTimeMillis() {
    return dateTimeMillis;
  }

  public void setDateTimeMillis(final long dateTimeMillis) {
    this.dateTimeMillis = dateTimeMillis;
  }

  public int getTotalNumberOfApps() {
    return totalNumberOfApps;
  }

  public void setTotalNumberOfApps(final int totalNumberOfApps) {
    this.totalNumberOfApps = totalNumberOfApps;
  }

  public int getTotalNumberOfAppsWithScmEnabled() {
    return totalNumberOfAppsWithScmEnabled;
  }

  public void setTotalNumberOfAppsWithScmEnabled(final int totalNumberOfAppsWithScmEnabled) {
    this.totalNumberOfAppsWithScmEnabled = totalNumberOfAppsWithScmEnabled;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ApiIntegrationsScmFeedbackStatIncrementDto that = (ApiIntegrationsScmFeedbackStatIncrementDto) o;

    return dateTimeMillis == that.dateTimeMillis && totalNumberOfApps == that.totalNumberOfApps &&
        totalNumberOfAppsWithScmEnabled == that.totalNumberOfAppsWithScmEnabled;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dateTimeMillis, totalNumberOfApps, totalNumberOfAppsWithScmEnabled);
  }

  @Override
  public String toString() {
    return "ApiIntegrationsScmFeedbackStatIncrementDto{" +
        "dateTimeMillis=" + dateTimeMillis +
        ", totalNumberOfApps=" + totalNumberOfApps +
        ", totalNumberOfAppsWithScmEnabled=" + totalNumberOfAppsWithScmEnabled +
        '}';
  }
}
