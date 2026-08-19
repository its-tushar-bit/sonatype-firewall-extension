/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;

public class SbomApplicationListSummaryDTO
{
  private List<SbomApplicationSummaryDTO> applications;

  private long totalCount;

  public SbomApplicationListSummaryDTO() {
  }

  public SbomApplicationListSummaryDTO(final List<SbomApplicationSummaryDTO> applications) {
    this.applications = applications;
  }

  public List<SbomApplicationSummaryDTO> getApplications() {
    return applications;
  }

  public void setApplications(List<SbomApplicationSummaryDTO> applications) {
    this.applications = applications;
  }

  public long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(long totalCount) {
    this.totalCount = totalCount;
  }
}
