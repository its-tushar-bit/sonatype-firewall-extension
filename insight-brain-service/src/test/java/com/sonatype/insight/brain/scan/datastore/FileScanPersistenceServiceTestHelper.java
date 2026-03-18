/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import com.sonatype.insight.brain.service.InsightWork;

public class FileScanPersistenceServiceTestHelper
    implements ScanPersistenceServiceTestHelper
{
  private final InsightWork insightWork;

  public FileScanPersistenceServiceTestHelper(
      final InsightWork insightWork)
  {
    this.insightWork = insightWork;
  }

  @Override
  public void saveMockScan(String scanId) throws IOException {
    Path scanFile = getScanFile(APPLICATION_ID, scanId);
    Files.createDirectories(scanFile.getParent());

    writeCompressedScanContent(scanFile, getSampleScanContent(scanId));
  }

  @Override
  public void saveEmptyMockScan(String scanId) throws IOException {
    Path scanFile = getScanFile(APPLICATION_ID, scanId);
    Files.createDirectories(scanFile.getParent());

    writeCompressedScanContent(scanFile, "empty scan");
  }

  @Override
  public String readDirectScanFile(String applicationId, String scanId) throws IOException {
    Path scanFile = getScanFile(applicationId, scanId);
    if (!Files.exists(scanFile)) {
      return null;
    }

    // Read GZIP compressed content and decompress it
    try (var fileInputStream = Files.newInputStream(scanFile);
        var gzipInputStream = new java.util.zip.GZIPInputStream(fileInputStream))
    {
      byte[] decompressedContent = gzipInputStream.readAllBytes();
      return new String(decompressedContent, StandardCharsets.UTF_8);
    }
  }

  @Override
  public void waitForNewFileTime() throws InterruptedException {
    Thread.sleep(2000);
  }

  @Override
  public void assertScanExists(String applicationId, String scanId, boolean expected) {
    Path scanFile = getScanFile(applicationId, scanId);
    boolean exists = Files.exists(scanFile) && Files.isRegularFile(scanFile);

    if (expected != exists) {
      throw new AssertionError(
          "Expected scan %s/%s to %s, but it %s".formatted(
              applicationId, scanId,
              expected ? "exist" : "not exist",
              exists ? "exists" : "does not exist"));
    }
  }

  @Override
  public void cleanup() throws IOException {
    File scanDir = insightWork.getScanDir(APPLICATION_ID);
    if (scanDir != null && scanDir.exists()) {
      deleteRecursively(scanDir.toPath());
    }
  }

  private Path getScanFile(String applicationId, String scanId) {
    String scanFileName = "scan-" + scanId + ".xml.gz";
    return insightWork.getScanDir(applicationId).toPath().resolve(scanFileName);
  }

  private void writeCompressedScanContent(Path file, String content) throws IOException {
    try (var fileOut = Files.newOutputStream(file);
        var gzipOut = new GZIPOutputStream(fileOut);
        var writer = new OutputStreamWriter(gzipOut, StandardCharsets.UTF_8))
    {
      writer.write(content);
    }
  }

  private void deleteRecursively(Path path) throws IOException {
    try (Stream<Path> walk = Files.walk(path)) {
      walk.sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .peek(System.out::println)
          .forEach(File::delete);
    }
  }
}
