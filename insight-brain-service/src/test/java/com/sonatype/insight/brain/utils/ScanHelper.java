/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import com.sonatype.insight.brain.service.InsightWork;

public class ScanHelper
{
  public static File createDummyScanFile(InsightWork insightWork, String appId, String scanId) {
    File scanFile = insightWork.getScanFile(appId, scanId);
    try {
      Files.createDirectories(scanFile.getParentFile().toPath());
      Files.write(scanFile.toPath(), new byte[0]);
      return scanFile;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
