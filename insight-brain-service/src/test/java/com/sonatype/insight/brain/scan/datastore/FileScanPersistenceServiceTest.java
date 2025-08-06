/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.service.InsightWork;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceTestHelper.APPLICATION_ID;
import static com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceTestHelper.SCAN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Category(SlowTest.class)
public class FileScanPersistenceServiceTest
    extends AbstractScanPersistenceServiceTest
{
  private static final String OTHER_APP_ID = "otherAppId";

  private static final String OTHER_SCAN_ID = "otherScanId";

  @Inject
  private InsightWork insightWork;

  @Before
  public void setCluster() throws Exception {
    insightConfig.setClusterDirectory(tempDir.newFolder().getAbsolutePath());
  }

  @Before
  public void setup() {
    var helper = new FileScanPersistenceServiceTestHelper(insightWork);
    setup(helper);
  }

  @Test
  @Override
  public void testCorrectImplClass() {
    assertThat(service).isInstanceOf(FileScanPersistenceService.class);
  }

  @Test
  public void testGetScanLocation() {
    String suffix = FileSystems.getDefault().getSeparator().equals("\\") ?
        "\\scan\\" + APPLICATION_ID + "\\scan-" + SCAN_ID + ".xml.gz" :
        "/scan/" + APPLICATION_ID + "/scan-" + SCAN_ID + ".xml.gz";

    var scanEntity = service.getScan(APPLICATION_ID, SCAN_ID);
    assertThat(scanEntity.getLocation()).isEqualTo(
        insightConfig.getClusterDirectory().toString() + suffix
    );
  }

  @Test
  @Override
  public void testScanEntity_getLocation() {
    String suffix = FileSystems.getDefault().getSeparator().equals("\\") ?
        "\\scan\\" + APPLICATION_ID + "\\scan-" + SCAN_ID + ".xml.gz" :
        "/scan/" + APPLICATION_ID + "/scan-" + SCAN_ID + ".xml.gz";

    var scanEntity = service.getScan(APPLICATION_ID, SCAN_ID);
    String expectedLocation = insightConfig.getClusterDirectory().toString() + suffix;
    assertThat(scanEntity.getLocation()).isEqualTo(expectedLocation);
  }

  @Test
  @Override
  public void testExceptionHandlingAndCleanup() throws Exception {
    helper.saveMockScan();

    var scanEntity = service.getScan(APPLICATION_ID, SCAN_ID);
    assertThat(scanEntity.exists()).isTrue();

    // Make the file read-only to simulate permission issues
    File scanFile = new File(scanEntity.getLocation());
    scanFile.setReadOnly();

    try {
      assertThatThrownBy(() -> {
        try (var writer = scanEntity.getWriter()) {
          writer.write("<scan>test</scan>");
        }
      }).isInstanceOf(IOException.class);
    }
    finally {
      scanFile.setWritable(true);
    }
  }

  @Test
  @Override
  public void testCopyScanFile() throws Exception {
    ScanEntity source = service.createTempScan(APPLICATION_ID);
    createScan(source);
    ScanEntity destination = service.getScan(OTHER_APP_ID, OTHER_SCAN_ID);
    assertThat(destination.exists()).isFalse();

    service.copyScanFile(source, destination);

    assertThat(readScan(source)).isEqualTo(readScan(destination));
  }

  @Test
  @Override
  public void testMoveTempScan() throws Exception {
    ScanEntity source = service.createTempScan(APPLICATION_ID);
    createScan(source);
    ScanEntity destination = service.getScan(OTHER_APP_ID, OTHER_SCAN_ID);
    assertThat(destination.exists()).isFalse();
    String content = readScan(source);

    service.moveTempScan(source, OTHER_APP_ID, OTHER_SCAN_ID);

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
