/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.neuvector.model.ScanRepoReport;

/**
 * Mirrors the structure of {@link com.neuvector.model.ScanRepoReportData} so we can deserialize the
 * NeuVector-format scan output once while also exposing the {@code ContentSets} array in the report,
 * which the upstream class does not model.
 */
public class ScanRepoReportDataWithContentSets
{
  public static class ReportWithContentSets
      extends ScanRepoReport
  {
    @SerializedName("ContentSets")
    private List<String> contentSets;

    public List<String> getContentSets() {
      return contentSets != null ? contentSets : Collections.emptyList();
    }
  }

  @SerializedName("error_message")
  private String errorMessage;

  private ReportWithContentSets report;

  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Returns the report, or an empty report if the scan response had no {@code report} field
   * (e.g. an error-only response). Never returns null so callers can chain calls without null
   * guards.
   */
  public ReportWithContentSets getReport() {
    return report != null ? report : new ReportWithContentSets();
  }

  public List<String> getContentSets() {
    return getReport().getContentSets();
  }
}
