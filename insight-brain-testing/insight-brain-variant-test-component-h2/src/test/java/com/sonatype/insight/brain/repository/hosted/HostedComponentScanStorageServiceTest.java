/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.scan.datastore.AbstractScanPersistenceServiceH2Test;
import com.sonatype.insight.brain.scan.datastore.FileScanPersistenceService;
import com.sonatype.insight.brain.scan.datastore.FileScanPersistenceServiceTestHelper;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceTestHelper;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ComponentH2Test
public class HostedComponentScanStorageServiceTest
    extends AbstractScanPersistenceServiceH2Test
{
  @Inject
  private InsightWork insightWork;

  private HostedComponentScanStorageService storageService;

  @BeforeEach
  public void setCluster() throws Exception {
    insightConfig.setClusterDirectory(tempDir.newFolder().getAbsolutePath());
  }

  @BeforeEach
  public void setUpStorageService() {
    setup(new FileScanPersistenceServiceTestHelper(insightWork));
    storageService = new HostedComponentScanStorageService(service);
  }

  @Test
  @Override
  public void testCorrectImplClass() {
    assertThat(service).isInstanceOf(FileScanPersistenceService.class);
  }

  @Test
  @Override
  public void testScanEntity_getLocation() {
    ScanEntity scanEntity = service.getScan("app-loc", "scan-loc");
    assertThat(scanEntity.getLocation()).contains("app-loc");
  }

  @Test
  @Override
  public void testExceptionHandlingAndCleanup() throws Exception {
    helper.saveMockScan();

    var scanEntity = service.getScan(ScanPersistenceServiceTestHelper.APPLICATION_ID,
        ScanPersistenceServiceTestHelper.SCAN_ID);
    assertThat(scanEntity.exists()).isTrue();

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
  public void storeScanFile_copiesBytesIntoTempScanEntity() throws IOException {
    byte[] content = "scan-xml-content".getBytes(StandardCharsets.UTF_8);
    File scanFile = tempDir.newFile("scan.xml.gz");
    try (FileOutputStream fos = new FileOutputStream(scanFile)) {
      fos.write(content);
    }

    ScanEntity result = storageService.storeScanFile("app-1", scanFile);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isNotBlank();
    assertThat(result.exists()).isTrue();
  }

  @Test
  public void storeScanFile_returnedEntityCanBeReadBack() throws IOException {
    File scanFile = tempDir.newFile("scan2.xml.gz");
    try (FileOutputStream fos = new FileOutputStream(scanFile)) {
      fos.write("hello-scan-content".getBytes(StandardCharsets.UTF_8));
    }

    ScanEntity stored = storageService.storeScanFile("app-2", scanFile);
    ScanEntity fetched = service.getScanByName("app-2", stored.getName());

    assertThat(fetched).isNotNull();
    assertThat(fetched.exists()).isTrue();
  }

  @Test
  public void storeScanFile_throwsWhenFileSizeExceedsLimit() throws IOException {
    File oversizedFile = tempDir.newFile("oversized.xml.gz");
    try (RandomAccessFile raf = new RandomAccessFile(oversizedFile, "rw")) {
      raf.setLength(HostedComponentScanStorageService.MAX_SCAN_FILE_SIZE_BYTES + 1);
    }

    assertThatThrownBy(() -> storageService.storeScanFile("app-3", oversizedFile))
        .isInstanceOf(ScanFileTooLargeException.class)
        .hasMessageContaining("exceeds maximum allowed size");
  }

  @Test
  public void storeScanFile_cleansUpTempEntityWhenSourceFileIsUnreadable() throws IOException {
    File scanFile = tempDir.newFile("unreadable.xml.gz");
    scanFile.delete(); // delete so FileInputStream throws

    assertThatThrownBy(() -> storageService.storeScanFile("app-4", scanFile))
        .isInstanceOf(IOException.class);

    File scanDir = insightWork.getScanDir("app-4");
    String[] remaining = scanDir.exists() ? scanDir.list() : new String[0];
    assertThat(remaining).isEmpty();
  }

  @Test
  public void storeScanFile_propagatesExceptionWhenCreateTempScanFails() throws IOException {
    File scanFile = tempDir.newFile("scan-no-dir.xml.gz");
    try (FileOutputStream fos = new FileOutputStream(scanFile)) {
      fos.write("content".getBytes(StandardCharsets.UTF_8));
    }

    File clusterDir = insightConfig.getClusterDirectory();
    File scanRootDir = new File(clusterDir, "scan");
    scanRootDir.mkdirs();
    scanRootDir.setReadOnly();

    try {
      assertThatThrownBy(() -> storageService.storeScanFile("app-5", scanFile))
          .isInstanceOf(IOException.class);

      File appScanDir = insightWork.getScanDir("app-5");
      String[] remaining = appScanDir.exists() ? appScanDir.list() : new String[0];
      assertThat(remaining).isEmpty();
    }
    finally {
      scanRootDir.setWritable(true);
    }
  }
}
