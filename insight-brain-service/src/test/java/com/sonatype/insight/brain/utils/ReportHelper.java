/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import com.sonatype.insight.brain.service.Zipper;

import org.junit.rules.TemporaryFolder;

public class ReportHelper
{
  /**
   * Create zipped report given report dir
   *
   * @param reportResourceName resource directory with unzipped report
   * @param tempDir            directory to put zipped report
   * @return URL to zipped report
   */
  public static URL zipReport(String reportResourceName, TemporaryFolder tempDir) {
    URL reportResourceUrl = ReportHelper.class.getResource(reportResourceName);
    if (!reportResourceName.endsWith(".zip")) {
      return zipResourceDir(reportResourceUrl, tempDir);
    }
    else {
      return reportResourceUrl;
    }
  }

  private static URL zipResourceDir(URL resourceDirUrl, TemporaryFolder tempDir) {
    try {
      File resourceDir = new File(resourceDirUrl.toURI());
      if (!resourceDir.isDirectory()) {
        throw new RuntimeException("'" + resourceDir.getAbsolutePath() + "' is not a directory.");
      }
      File reportZipFile = new File(tempDir.getRoot(), "MockReport-" + UUID.randomUUID() + ".zip");
      Zipper.zip(resourceDir, reportZipFile);
      return reportZipFile.toURI().toURL();
    }
    catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
