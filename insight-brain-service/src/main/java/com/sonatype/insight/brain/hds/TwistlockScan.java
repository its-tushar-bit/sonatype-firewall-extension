/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.sonatype.insight.scan.model.ScanFileNames;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.codehaus.plexus.util.IOUtil;

/**
 * Helper class for Twistlock tar.gz scan files.
 * 
 * @since 1.24
 */
public class TwistlockScan
{
  private final File scanFile;

  public TwistlockScan(File scanFile) {
    this.scanFile = scanFile;
  }

  public String getFilesJson() {
    return getTwistlockFileContent("/files.json");
  }

  public String getScanXml() {
    try (ZipFile scanArchiveFile = new ZipFile(scanFile)) {
      ZipEntry scanArchiveEntry = scanArchiveFile.getEntry(ScanFileNames.SONATYPE_SCAN_FILENAME);
      if (scanArchiveEntry != null) {
        try (InputStream scanXmlStream = new GzipCompressorInputStream(
            scanArchiveFile.getInputStream(scanArchiveEntry))) {
          return IOUtil.toString(scanXmlStream, "UTF-8");
        }
      }
      throw new FileNotFoundException("Cannot find the Sonatype scan file in " + scanFile.getAbsolutePath());
    }
    catch (IOException e) {
      throw new RuntimeException("Error while reading " + scanFile.getAbsolutePath() + ": " + e.getMessage(), e);
    }
  }

  private String getTwistlockFileContent(String entryName) {
    try (ZipFile scanArchiveFile = new ZipFile(scanFile)) {
      ZipEntry scanArchiveEntry = scanArchiveFile.getEntry(ScanFileNames.TWISTLOCK_SCAN_FILENAME);
      if (scanArchiveEntry != null) {
        try (TarArchiveInputStream tarArchiveStream = new TarArchiveInputStream(
            new GzipCompressorInputStream(scanArchiveFile.getInputStream(scanArchiveEntry)))) {
          ArchiveEntry tarArchiveEntry = null;
          while ((tarArchiveEntry = tarArchiveStream.getNextEntry()) != null) {
            if (entryName.equals(tarArchiveEntry.getName())) {
              return IOUtil.toString(tarArchiveStream, "UTF-8");
            }
          }
          throw new FileNotFoundException("Cannot find an entry with name " + entryName);
        }
      }
      throw new FileNotFoundException("Cannot find the Twistlock scan file");
    }
    catch (IOException e) {
      throw new UncheckedIOException("Error while reading " + scanFile.getAbsolutePath() + ": " + e.getMessage(), e);
    }
  }
}
