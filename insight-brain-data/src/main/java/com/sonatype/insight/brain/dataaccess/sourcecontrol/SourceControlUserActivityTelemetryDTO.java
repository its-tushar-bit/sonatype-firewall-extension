/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.time.LocalDate;
import jakarta.inject.Named;

/**
 * @since 1.170
 */
@Named
public class SourceControlUserActivityTelemetryDTO
{
  private String sourceControlUserActivityId;

  private String email;

  private String applicationId;

  private LocalDate commitYearMonth;

  public SourceControlUserActivityTelemetryDTO() {
  }

  public SourceControlUserActivityTelemetryDTO(
      String sourceControlUserActivityId,
      String email,
      String applicationId,
      LocalDate commitYearMonth)
  {
    this.sourceControlUserActivityId = sourceControlUserActivityId;
    this.email = email;
    this.applicationId = applicationId;
    this.commitYearMonth = commitYearMonth;
  }

  public String getSourceControlUserActivityId() {
    return sourceControlUserActivityId;
  }

  public void setSourceControlUserActivityId(final String sourceControlUserActivityId) {
    this.sourceControlUserActivityId = sourceControlUserActivityId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(final String applicationId) {
    this.applicationId = applicationId;
  }

  public LocalDate getCommitYearMonth() {
    return commitYearMonth;
  }

  public void setCommitYearMonth(final LocalDate commitYearMonth) {
    this.commitYearMonth = commitYearMonth;
  }

  @Override
  public String toString() {
    return "SourceControlUserActivityTelemetryDTO [sourceControlUserActivityId=" + sourceControlUserActivityId +
        ", email=" + email + ", applicationId=" + applicationId + ", commitYearMonth=" + commitYearMonth + "]";
  }
}
