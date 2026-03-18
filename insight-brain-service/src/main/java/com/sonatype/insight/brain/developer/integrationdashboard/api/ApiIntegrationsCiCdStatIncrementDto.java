/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard.api;

import java.util.Objects;

public class ApiIntegrationsCiCdStatIncrementDto
{
  private long dateTimeMillis;

  private int totalNumberOfApps;

  private int totalNumberOfAppsWithCiCdEnabled;

  public ApiIntegrationsCiCdStatIncrementDto(
      final long dateTimeMillis,
      final int totalNumberOfApps,
      final int totalNumberOfAppsWithCiCdEnabled)
  {
    this.dateTimeMillis = dateTimeMillis;
    this.totalNumberOfApps = totalNumberOfApps;
    this.totalNumberOfAppsWithCiCdEnabled = totalNumberOfAppsWithCiCdEnabled;
  }

  public ApiIntegrationsCiCdStatIncrementDto() {
  }

  public Long getDateTimeMillis() {
    return dateTimeMillis;
  }

  public void setDateTimeMillis(final long dateTimeMillis) {
    this.dateTimeMillis = dateTimeMillis;
  }

  public int getTotalNumberOfApps() {
    return this.totalNumberOfApps;
  }

  public void setTotalNumberOfApps(final int totalNumberOfApps) {
    this.totalNumberOfApps = totalNumberOfApps;
  }

  public Integer getTotalNumberOfAppsWithCiCdEnabled() {
    return totalNumberOfAppsWithCiCdEnabled;
  }

  public void setTotalNumberOfAppsWithCiCdEnabled(final int totalNumberOfAppsWithCiCdEnabled) {
    this.totalNumberOfAppsWithCiCdEnabled = totalNumberOfAppsWithCiCdEnabled;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ApiIntegrationsCiCdStatIncrementDto that = (ApiIntegrationsCiCdStatIncrementDto) o;

    return dateTimeMillis == that.dateTimeMillis
        && totalNumberOfApps == that.totalNumberOfApps
        && totalNumberOfAppsWithCiCdEnabled == that.totalNumberOfAppsWithCiCdEnabled;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dateTimeMillis, totalNumberOfApps, totalNumberOfAppsWithCiCdEnabled);
  }

  @Override
  public String toString() {
    return "ApiIntegrationsCiCdStatIncrementDto{" +
        "dateTimeMillis=" + dateTimeMillis +
        ", totalNumberOfApps=" + totalNumberOfApps +
        ", totalNumberOfAppsWithCiCdEnabled=" + totalNumberOfAppsWithCiCdEnabled +
        '}';
  }
}
