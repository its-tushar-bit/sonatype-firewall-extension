/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;

import com.sonatype.insight.error.exception.NotFoundException;

public interface ReportDataStore
{
  ApplicationReport downloadReport(String applicationId, String scanId, DownloadReportPostAction action)
      throws IOException, NotFoundException;

  ApplicationReport getApplicationReport(String appId, String scanId);

  ReportEntity getReportEntityByName(String applicationId, String scanId, String name) throws IOException;

  ReportPdf getReportPdf(String appId, String scanId);

  @FunctionalInterface
  interface DownloadReportPostAction
  {
    void apply(String scanId, ApplicationReport tempApplicationReport, String appId) throws IOException;
  }
}


