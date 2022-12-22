/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

import com.sonatype.insight.scan.model.ScanFileNames;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;

class ThirdPartyScanTestHelper
{
  static File createInputScanFile(TemporaryFolder tempDir, File sourceFilesDir) throws IOException {
    File stagingDir = tempDir.newFolder("staging");

    // Gzip the Third Party scan file
    File sonatypeScanGzipFile = new File(stagingDir, ScanFileNames.SONATYPE_SCAN_FILENAME);
    try (GZIPOutputStream gzipStream = new GZIPOutputStream(Files.newOutputStream(sonatypeScanGzipFile.toPath()))) {
      FileUtils.copyFile(new File(sourceFilesDir, "scan.xml"), gzipStream);
    }
    return sonatypeScanGzipFile;
  }
}
