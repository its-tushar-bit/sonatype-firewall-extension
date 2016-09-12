/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class TestReportUtil
{
  /**
   * Creates a report zip in the specified location using the json content provided as an argument. Uses the most
   * current version of the static report files.
   * 
   * @param reportContentPath the directory containing the json files to be added to the report zip
   * @param testReport the destination report zip, usually from a temp folder
   * @return returns testReport for convenience
   */
  public static File setupReport(String reportContentPath, File testReport) throws Exception {
    return setupReport("/canned-reports/static-report-53f5de7037a8cc019196816dc317b42fe4e593a4.zip", reportContentPath,
        testReport);
  }

  /**
   * Creates a report zip in the specified location using the json content provided as an argument. Uses the specified
   * version of the static report files.
   * 
   * @param reportContentPath the directory containing the json files to be added to the report zip
   * @param testReport the destination report zip, usually from a temp folder
   * @return returns testReport for convenience
   */
  public static File setupReport(String staticReportPath, String reportContentPath, File testReport) throws Exception {
    try (InputStream in = TestReportUtil.class.getResource(staticReportPath).openStream()) {
      Files.copy(in, testReport.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    Zipper.zip(new File(TestReportUtil.class.getResource(reportContentPath).toURI()), testReport);

    return testReport;
  }
}
