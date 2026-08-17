/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.neuvector.model.Vulnerability;

/**
 * Mirrors the structure of {@link com.neuvector.model.ScanRepoReportData} so we can deserialize the
 * NeuVector-format scan output. Beyond the upstream model it exposes the {@code ContentSets} array and,
 * on each module, the pypi variant {@code qualifier}/{@code extension} (see {@link ScanModuleWithQualifier}),
 * neither of which the upstream classes carry.
 */
public class ScanRepoReportDataWithContentSets
{
  public static class ReportWithContentSets
  {
    @SerializedName("vulnerabilities")
    private Vulnerability[] vulnerabilities;

    @SerializedName("modules")
    private ScanModuleWithQualifier[] modules;

    @SerializedName("ContentSets")
    private List<String> contentSets;

    public Vulnerability[] getVulnerabilities() {
      return vulnerabilities;
    }

    public ScanModuleWithQualifier[] getModules() {
      return modules;
    }

    public List<String> getContentSets() {
      return contentSets != null ? contentSets : Collections.emptyList();
    }
  }

  @SerializedName("error_message")
  private String errorMessage;

  @SerializedName("report")
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
