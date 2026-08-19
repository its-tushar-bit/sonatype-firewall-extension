/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

public class ApiSbomApplicationsHistoryMetricDTO
{
  public long totalScannedApplications;

  public long applicationsUpdatedLastYear;

  public long applicationsUpdatedLastMonth;

  public long applicationsUpdatedLastWeek;

  public ApiSbomApplicationsHistoryMetricDTO() {
  }

  public ApiSbomApplicationsHistoryMetricDTO(
      final long totalScannedApplications,
      final long applicationsUpdatedLastYear,
      final long applicationsUpdatedLastMonth,
      final long applicationsUpdatedLastWeek)
  {
    this.totalScannedApplications = totalScannedApplications;
    this.applicationsUpdatedLastYear = applicationsUpdatedLastYear;
    this.applicationsUpdatedLastMonth = applicationsUpdatedLastMonth;
    this.applicationsUpdatedLastWeek = applicationsUpdatedLastWeek;
  }

  public ApiSbomApplicationsHistoryMetricDTO(Object[] array) {
    totalScannedApplications = ((Number) array[0]).longValue();
    applicationsUpdatedLastYear = ((Number) array[1]).longValue();
    applicationsUpdatedLastMonth = ((Number) array[2]).longValue();
    applicationsUpdatedLastWeek = ((Number) array[3]).longValue();
  }
}
