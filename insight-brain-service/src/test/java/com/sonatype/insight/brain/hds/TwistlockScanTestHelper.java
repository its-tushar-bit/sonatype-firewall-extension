/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import com.sonatype.insight.brain.service.Zipper;
import com.sonatype.insight.scan.model.ScanFileNames;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;

class TwistlockScanTestHelper
{
  static File createInputScanFile(TemporaryFolder tempDir, File sourceFilesDir) throws IOException {
    // Create a tar of the Twistlock scan files
    File stagingDir = tempDir.newFolder("staging");
    File twistlockScanFile = new File(stagingDir,
        ScanFileNames.TWISTLOCK_SCAN_FILENAME.substring(0, ScanFileNames.TWISTLOCK_SCAN_FILENAME.lastIndexOf('.')));
    try (TarArchiveOutputStream twistlockScanFileStream = new TarArchiveOutputStream(
        new FileOutputStream(twistlockScanFile));) {
      File twistlockFilesJsonFile = new File(sourceFilesDir, "files.json");
      TarArchiveEntry tarEntry = new TarArchiveEntry(twistlockFilesJsonFile, "/files.json");
      twistlockScanFileStream.putArchiveEntry(tarEntry);
      twistlockScanFileStream.write(FileUtils.readFileToByteArray(twistlockFilesJsonFile));
      twistlockScanFileStream.closeArchiveEntry();
    }
    // Gzip the Twistlock tar file
    File twistlockScanGzipFile = new File(stagingDir, ScanFileNames.TWISTLOCK_SCAN_FILENAME);
    try (GZIPOutputStream gzipStream = new GZIPOutputStream(new FileOutputStream(twistlockScanGzipFile))) {
      FileUtils.copyFile(twistlockScanFile, gzipStream);
    }
    twistlockScanFile.delete();

    // Gzip the Sonatype scan file
    File sonatypeScanGzipFile = new File(stagingDir, ScanFileNames.SONATYPE_SCAN_FILENAME);
    try (GZIPOutputStream gzipStream = new GZIPOutputStream(new FileOutputStream(sonatypeScanGzipFile))) {
      FileUtils.copyFile(new File(sourceFilesDir, "scan.xml"), gzipStream);
    }

    File scanZipFile = new File(tempDir.getRoot(), "scan.zip");
    Zipper.zip(stagingDir, scanZipFile);

    return scanZipFile;
  }
}
