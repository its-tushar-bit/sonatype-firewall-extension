/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import com.sonatype.insight.brain.service.Zipper;

import org.apache.commons.io.FileUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockReportDownloader
{
  private final ReportDownloader reportDownloader = mock(ReportDownloader.class);

  /**
   * Simulates that a report (based on the specified resource) exists.
   *
   * @param reportResourceName can be a report.zip file or a directory that will be zipped up into a report.
   *
   * @return A generated scan ID that can be used in subsequent calls to evaluate policies.
   */
  public String mockDownloadReport(String reportResourceName) {
    String scanId = UUID.randomUUID().toString().replace("-", "");
    mockDownloadReport(scanId, reportResourceName);
    return scanId;
  }

  /**
   * Simulates that a report (based on the specified resource) associated with the specified scan ID exists.
   *
   * @param reportResourceName can be a report.zip file or a directory that will be zipped up into a report.
   */
  public void mockDownloadReport(String scanId, String reportResourceName) {
    when(reportDownloader.downloadReport(eq(scanId), any(), anyInt(), anyInt())).then(new Answer<Boolean>()
    {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        ReportEntity reportFile = (ReportEntity) invocation.getArguments()[1];
        if (reportResourceName.endsWith(".zip")) {
          FileUtils.copyURLToFile(getClass().getResource(reportResourceName),
              ((FileApplicationReport) reportFile).getFile());
        }
        else {
          zipResourceDir(reportResourceName, ((FileApplicationReport) reportFile).getFile());
        }
        return true;
      }
    });
  }

  public ReportDownloader getMock() {
    return reportDownloader;
  }

  private void zipResourceDir(String resourceName, File reportZipFile) {
    try {
      reportZipFile.getParentFile().mkdirs();
      URL resourceUrl = getClass().getResource(resourceName);
      File resourceDir = new File(resourceUrl.toURI());
      if (!resourceDir.isDirectory()) {
        throw new RuntimeException("'" + resourceDir.getAbsolutePath() + "' is not a directory.");
      }
      Zipper.zip(resourceDir, reportZipFile);
    }
    catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
