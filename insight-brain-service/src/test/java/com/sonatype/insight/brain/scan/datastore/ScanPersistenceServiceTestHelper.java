/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public interface ScanPersistenceServiceTestHelper
{
  String APPLICATION_ID = "app1";

  String SCAN_ID = "scan1";

  default void saveMockScan() throws IOException {
    saveMockScan(SCAN_ID);
  }

  void saveMockScan(String scanId) throws IOException;

  default void saveEmptyMockScan() throws IOException {
    saveEmptyMockScan(SCAN_ID);
  }

  void saveEmptyMockScan(String scanId) throws IOException;

  String readDirectScanFile(String applicationId, String scanId) throws IOException;

  /**
   * Wait for file system timestamp resolution to ensure new operations have different timestamps.
   */
  void waitForNewFileTime() throws InterruptedException;

  default void assertScanContents(ScanEntity entity, String expectedXml) throws IOException {
    try (var inputStream = entity.getInputStream();
        var gzipInputStream = new java.util.zip.GZIPInputStream(inputStream))
    {
      assertThat(inputStream).isNotNull();
      byte[] entityContents = gzipInputStream.readAllBytes();
      assertThat(new String(entityContents, StandardCharsets.UTF_8)).isEqualTo(expectedXml);
    }
  }

  void assertScanExists(String applicationId, String scanId, boolean expected);

  default void assertScanExists(boolean expected) {
    assertScanExists(APPLICATION_ID, SCAN_ID, expected);
  }

  default String getSampleScanContent(String scanId) {
    return scanId + " xml content";
  }

  void cleanup() throws IOException;
}
