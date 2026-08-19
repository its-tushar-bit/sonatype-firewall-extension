/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;

public class ReportTestUtils
{
  public static void createReportFile(
      String appId,
      String scanId,
      File reportFile,
      InsightWork insightWork) throws IOException
  {
    FileUtils.copyFile(reportFile, insightWork.getReportFile(appId, scanId));
  }

  public static File zipReportDir(String reportResourceName, TemporaryFolder tempDir) throws URISyntaxException {
    return Paths.get(ReportHelper.zipReport(reportResourceName, tempDir).toURI()).toFile();
  }
}
