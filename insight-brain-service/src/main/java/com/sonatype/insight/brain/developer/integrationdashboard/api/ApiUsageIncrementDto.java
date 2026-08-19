/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard.api;

import java.util.Objects;

import com.sonatype.insight.brain.model.ApplicationCountHistory;

public class ApiUsageIncrementDto
{
  private long dateTimeMillis;

  private int totalNumberOfApps;

  private int totalNumberOfAppsWithScmEnabled;

  private int totalNumberOfPolicyActionFailuresByAppCount;

  private int totalNumberOfWaivers;

  private long meanTimeToRemediateMs;

  private int totalNumberOfAppsUsingCiCd;

  public ApiUsageIncrementDto(
      final long dateTimeMillis,
      final int totalNumberOfApps,
      final int totalNumberOfAppsWithScmEnabled,
      final int totalNumberOfPolicyActionFailuresByAppCount,
      final int totalNumberOfWaivers,
      final long meanTimeToRemediateMs,
      final int totalNumberOfAppsUsingCiCd)
  {
    this.dateTimeMillis = dateTimeMillis;
    this.totalNumberOfApps = totalNumberOfApps;
    this.totalNumberOfAppsWithScmEnabled = totalNumberOfAppsWithScmEnabled;
    this.totalNumberOfPolicyActionFailuresByAppCount = totalNumberOfPolicyActionFailuresByAppCount;
    this.totalNumberOfWaivers = totalNumberOfWaivers;
    this.meanTimeToRemediateMs = meanTimeToRemediateMs;
    this.totalNumberOfAppsUsingCiCd = totalNumberOfAppsUsingCiCd;
  }

  public ApiUsageIncrementDto() {
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

  public int getTotalNumberOfPolicyActionFailuresByAppCount() {
    return totalNumberOfPolicyActionFailuresByAppCount;
  }

  public void setTotalNumberOfPolicyActionFailuresByAppCount(final int totalNumberOfPolicyActionFailuresByAppCount) {
    this.totalNumberOfPolicyActionFailuresByAppCount = totalNumberOfPolicyActionFailuresByAppCount;
  }

  public int getTotalNumberOfWaivers() {
    return totalNumberOfWaivers;
  }

  public void setTotalNumberOfWaivers(final int totalNumberOfWaivers) {
    this.totalNumberOfWaivers = totalNumberOfWaivers;
  }

  public long getMeanTimeToRemediateMs() {
    return meanTimeToRemediateMs;
  }

  public void setMeanTimeToRemediateMs(final long meanTimeToRemediateMs) {
    this.meanTimeToRemediateMs = meanTimeToRemediateMs;
  }

  public int getTotalNumberOfAppsUsingCiCd() {
    return totalNumberOfAppsUsingCiCd;
  }

  public void setTotalNumberOfAppsUsingCiCd(final int totalNumberOfAppsUsingCiCd) {
    this.totalNumberOfAppsUsingCiCd = totalNumberOfAppsUsingCiCd;
  }

  public static ApiUsageIncrementDto fromApplicationHistoryCount(
      final long dateTimeMillis,
      final int totalNumberOfAppsUsingCiCd,
      final ApplicationCountHistory applicationCountHistory)
  {
    return new ApiUsageIncrementDto(
        dateTimeMillis,
        applicationCountHistory.getApplicationCount(),
        applicationCountHistory.getScmFeedbackEnabledCount(),
        applicationCountHistory.getPolicyActionFailuresByAppCount(),
        applicationCountHistory.getWaiversCount(),
        applicationCountHistory.getMeanTimeToRemediateMs(),
        totalNumberOfAppsUsingCiCd);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ApiUsageIncrementDto that = (ApiUsageIncrementDto) o;

    return dateTimeMillis == that.dateTimeMillis && totalNumberOfApps == that.totalNumberOfApps &&
        totalNumberOfAppsWithScmEnabled == that.totalNumberOfAppsWithScmEnabled &&
        totalNumberOfPolicyActionFailuresByAppCount == that.totalNumberOfPolicyActionFailuresByAppCount &&
        totalNumberOfWaivers == that.totalNumberOfWaivers && meanTimeToRemediateMs == that.meanTimeToRemediateMs &&
        totalNumberOfAppsUsingCiCd == that.totalNumberOfAppsUsingCiCd;
  }

  @Override
  public int hashCode() {
    return Objects.hash(dateTimeMillis, totalNumberOfApps, totalNumberOfAppsWithScmEnabled,
        totalNumberOfPolicyActionFailuresByAppCount,
        totalNumberOfWaivers,
        meanTimeToRemediateMs,
        totalNumberOfAppsUsingCiCd);
  }

  @Override
  public String toString() {
    return "ApiUsageIncrementDto{" +
        "dateTimeMillis=" + dateTimeMillis +
        ", totalNumberOfApps=" + totalNumberOfApps +
        ", totalNumberOfAppsWithScmEnabled=" + totalNumberOfAppsWithScmEnabled +
        ", totalNumberOfPolicyActionFailuresByAppCount=" + totalNumberOfPolicyActionFailuresByAppCount +
        ", totalNumberOfWaivers=" + totalNumberOfWaivers +
        ", meanTimeToRemediateMs=" + meanTimeToRemediateMs +
        ", totalNumberOfAppsUsingCiCd=" + totalNumberOfAppsUsingCiCd +
        '}';
  }
}
