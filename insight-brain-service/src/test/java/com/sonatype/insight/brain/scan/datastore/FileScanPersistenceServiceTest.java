/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan.datastore;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FileScanPersistenceServiceTest
    extends AbstractComponentTest
{
  private static final String APP_ID = "appId";

  private static final String OTHER_APP_ID = "otherAppId";

  private static final String OTHER_SCAN_ID = "otherScanId";

  @Inject
  private FileScanPersistenceService fileScanPersistenceService;

  @Test
  public void testCopyScanFile() throws Exception {
    ScanEntity source = fileScanPersistenceService.createTempScan(APP_ID);
    createScan(source);
    ScanEntity destination = fileScanPersistenceService.getScan(OTHER_APP_ID, OTHER_SCAN_ID);
    assertThat(destination.exists()).isFalse();

    fileScanPersistenceService.copyScanFile(source, destination);

    assertThat(readScan(source)).isEqualTo(readScan(destination));
  }

  @Test
  public void testMoveTempScan() throws Exception {
    ScanEntity source = fileScanPersistenceService.createTempScan(APP_ID);
    createScan(source);
    ScanEntity destination = fileScanPersistenceService.getScan(OTHER_APP_ID, OTHER_SCAN_ID);
    assertThat(destination.exists()).isFalse();
    String content = readScan(source);

    fileScanPersistenceService.moveTempScan(source, OTHER_APP_ID, OTHER_SCAN_ID);

    assertThat(source.exists()).isFalse();
    assertThat(readScan(destination)).isEqualTo(content);
  }

  private void createScan(final ScanEntity scanEntity) throws Exception {
    assertThat(scanEntity.exists()).isFalse();
    try (OutputStream outputStream = scanEntity.getOutputStream()) {
      outputStream.write(TemporaryEntity.uuid().getBytes(StandardCharsets.UTF_8));
    }
    assertThat(scanEntity.exists()).isTrue();
  }

  private String readScan(final ScanEntity scanEntity) throws Exception {
    byte[] bytes;
    try (InputStream inputStream = scanEntity.getInputStream()) {
      bytes = inputStream.readAllBytes();
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
