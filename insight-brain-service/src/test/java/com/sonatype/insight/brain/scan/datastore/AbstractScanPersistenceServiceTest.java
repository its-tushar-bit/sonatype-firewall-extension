/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceTestHelper.APPLICATION_ID;
import static com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceTestHelper.SCAN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public abstract class AbstractScanPersistenceServiceTest
    extends AbstractComponentTest
{
  private static final Set<String> BAD_SCAN_IDS = Set.of(
      "foo/../bar",
      "foo\\..\\bar",
      "..",
      ".",
      "../bar",
      "bar/..",
      "/",
      "/foo",
      "C:\\foo",
      "..\\foo",
      "foo//..",
      ".\\foo",
      "foo//.",
      "./foo",
      "foo/.",
      "\\",
      "\\foo",
      " foo",
      "foo ",
      "foo/ bar",
      "foo/bar ",
      "foo/bar /",
      "foo/bar /baz",
      "  "
  );

  @Inject
  protected InsightConfig insightConfig;

  protected ScanPersistenceService service;

  protected ScanPersistenceServiceTestHelper helper;

  /**
   * Should be called in @Before by subclasses to specify the service and helper. Prior to this call, the appropriate
   * configs in InsightConfig should be set up to ensure that lookup(ScanPersistenceService.class) returns the expected
   * service implementation
   */
  protected void setup(ScanPersistenceServiceTestHelper helper) {
    this.service = lookup(ScanPersistenceService.class);
    this.helper = helper;
  }

  @Test
  public abstract void testCorrectImplClass();

  @Test
  public abstract void testScanEntity_getLocation();

  @Test
  public void testGetScan_exists() throws Exception {
    helper.saveMockScan();

    var scanEntity = service.getScan(APPLICATION_ID, SCAN_ID);
    assertThat(scanEntity.getName()).isEqualTo("scan-" + SCAN_ID + ".xml.gz");
    assertThat(scanEntity.exists()).isTrue();
    assertThat(scanEntity.getAppId()).isEqualTo(APPLICATION_ID);
    var oldTime = scanEntity.getLastModifiedTime();
    assertThat(oldTime).isGreaterThan(0);

    helper.assertScanContents(scanEntity, helper.getSampleScanContent(SCAN_ID));
  }

  @Test
  public void testGetScan_notExists() throws Exception {
    var scanEntity = service.getScan(APPLICATION_ID, "nonexistent");
    assertThat(scanEntity.getName()).isEqualTo("scan-nonexistent.xml.gz");
    assertThat(scanEntity.exists()).isFalse();
    assertThat(scanEntity.getAppId()).isEqualTo(APPLICATION_ID);
    assertThatThrownBy(scanEntity::getLastModifiedTime).isInstanceOf(IOException.class);
    assertThatThrownBy(scanEntity::getInputStream).isInstanceOf(IOException.class);
  }

  @Test
  public void testCreateTempScan() throws Exception {
    var tempScan = service.createTempScan(APPLICATION_ID);
    assertThat(tempScan.getName()).startsWith("temp-");
    assertThat(tempScan.getName()).endsWith(".xml.gz");
    assertThat(tempScan.getAppId()).isEqualTo(APPLICATION_ID);
    assertThat(tempScan.exists()).isFalse();

    // Temp scan should have a valid location
    assertThat(tempScan.getLocation()).isNotEmpty();
    assertThat(tempScan.getLocation()).contains(APPLICATION_ID);
  }

  @Test
  public void testMoveTempScan() throws Exception {
    var tempScan = service.createTempScan(APPLICATION_ID);
    String testContent = "moved scan content";

    try (var writer = tempScan.getWriter()) {
      writer.write(testContent);
    }

    String targetScanId = "moved-scan";
    service.moveTempScan(tempScan, APPLICATION_ID, targetScanId);

    // Temp scan should no longer exist
    assertThat(tempScan.exists()).isFalse();

    // Target scan should exist with the content
    var targetScan = service.getScan(APPLICATION_ID, targetScanId);
    assertThat(targetScan.exists()).isTrue();
    helper.assertScanContents(targetScan, testContent);
  }

  @Test
  public void testDeleteScan() throws Exception {
    helper.saveMockScan();
    var scanEntity = service.getScan(APPLICATION_ID, SCAN_ID);
    assertThat(scanEntity.exists()).isTrue();

    boolean deleted = service.deleteScan(scanEntity);
    assertThat(deleted).isTrue();
    assertThat(scanEntity.exists()).isFalse();
  }

  @Test
  public void testDeleteScanById() throws Exception {
    helper.saveMockScan();
    helper.assertScanExists(true);

    service.deleteScan(APPLICATION_ID, SCAN_ID);
    helper.assertScanExists(false);
  }

  @Test
  public void testDeleteScansFor() throws Exception {
    helper.saveMockScan("scan1");
    helper.saveMockScan("scan2");
    helper.saveMockScan("scan3");

    helper.assertScanExists(APPLICATION_ID, "scan1", true);
    helper.assertScanExists(APPLICATION_ID, "scan2", true);
    helper.assertScanExists(APPLICATION_ID, "scan3", true);

    service.deleteScansFor(APPLICATION_ID);

    helper.assertScanExists(APPLICATION_ID, "scan1", false);
    helper.assertScanExists(APPLICATION_ID, "scan2", false);
    helper.assertScanExists(APPLICATION_ID, "scan3", false);
  }

  @Test
  public void testAllScanFilesFor() throws Exception {
    helper.saveMockScan("scan1");
    helper.saveMockScan("scan2");
    helper.saveMockScan("scan3");

    try (var stream = service.allScanFilesFor(APPLICATION_ID)) {
      var scans = stream.toList();
      assertThat(scans).hasSize(3);
      assertThat(scans).allMatch(scan -> scan.getAppId().equals(APPLICATION_ID));
      assertThat(scans).extracting(ScanEntity::getName)
          .containsExactlyInAnyOrder("scan-scan1.xml.gz", "scan-scan2.xml.gz", "scan-scan3.xml.gz");
    }
  }

  @Test
  public void testAllScanFilesFor_noScans() throws Exception {
    try (var stream = service.allScanFilesFor("nonexistent-app")) {
      var scans = stream.toList();
      assertThat(scans).isEmpty();
    }
  }

  @Test
  public void testCopyScanFile() throws Exception {
    helper.saveMockScan("source-scan");

    var sourceEntity = service.getScan(APPLICATION_ID, "source-scan");
    var destinationEntity = service.getScan(APPLICATION_ID, "dest-scan");

    assertThat(sourceEntity.exists()).isTrue();
    assertThat(destinationEntity.exists()).isFalse();

    service.copyScanFile(sourceEntity, destinationEntity);

    assertThat(destinationEntity.exists()).isTrue();
    assertThat(sourceEntity.exists()).isTrue();

    helper.assertScanContents(destinationEntity, helper.getSampleScanContent("source-scan"));
  }

  @Test
  public void testScanEntity_inputOutput() throws Exception {
    // Create temp scan first to ensure directory structure exists
    var tempScan = service.createTempScan(APPLICATION_ID);
    String testContent = "input output test content";

    // Write using Writer (with GZIP compression)
    try (var writer = tempScan.getWriter()) {
      writer.write(testContent);
    }

    assertThat(tempScan.exists()).isTrue();
    helper.assertScanContents(tempScan, testContent);
  }

  @Test
  public void testScanEntity_invalidNames() throws Exception {
    AtomicInteger count = new AtomicInteger(0);

    assertThat(BAD_SCAN_IDS).allSatisfy(scanId -> {
      String appId = "app" + count.getAndIncrement();
      assertThatThrownBy(() -> service.getScan(appId, scanId))
          .isInstanceOf(BadRequestException.class);
    });

    Set<String> validScanIds = Set.of(
        "",
        "12345",
        "valid-scan-id",
        "scan_with_underscores",
        "scan-with-dashes"
    );

    assertThat(validScanIds).allSatisfy(scanId -> {
      String appId = "app" + count.getAndIncrement();
      assertThatCode(() -> service.getScan(appId, scanId))
          .doesNotThrowAnyException();
    });
  }

  @Test
  @Category(SlowTest.class)
  public void testScanEntity_getLastModifiedTime() throws Exception {
    helper.saveMockScan();
    long startTime = System.currentTimeMillis();
    helper.waitForNewFileTime();

    var scanEntity = service.getScan(APPLICATION_ID, SCAN_ID);
    long modifiedTime = scanEntity.getLastModifiedTime();

    assertThat(modifiedTime).isGreaterThan(0);
    assertThat(modifiedTime).isLessThanOrEqualTo(startTime);
  }

  /**
   * Test error handling and cleanup during operations
   */
  @Test
  public abstract void testExceptionHandlingAndCleanup() throws Exception;

  @Test
  public void testGetScanId() {
    ScanEntity scanEntity = service.getScan(APPLICATION_ID, SCAN_ID);

    assertThat(scanEntity.getScanId()).isEqualTo(SCAN_ID);
  }
}
