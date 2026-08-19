/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles persisting scan files received from NXRM hosted repositories to local file storage.
 * Accepts a pre-formed scan.xml.gz file and stores it using the scan persistence infrastructure.
 */
@Named
@Singleton
public class HostedComponentScanStorageService
{
  private static final Logger log = LoggerFactory.getLogger(HostedComponentScanStorageService.class);

  static final long MAX_SCAN_FILE_SIZE_BYTES = 100L * 1024 * 1024; // 100 MB

  private final ScanPersistenceService scanPersistenceService;

  @Inject
  public HostedComponentScanStorageService(final ScanPersistenceService scanPersistenceService) {
    this.scanPersistenceService = scanPersistenceService;
  }

  /**
   * Stores a scan file received from NXRM into a temporary scan entity.
   *
   * @param appId the application ID this scan belongs to
   * @param scanFile the pre-formed scan.xml.gz file received from NXRM
   * @return the {@link ScanEntity} representing the stored temp scan
   * @throws IOException if file storage fails
   */
  public ScanEntity storeScanFile(final String appId, final File scanFile) throws IOException {
    long fileSize = scanFile.length();
    if (fileSize > MAX_SCAN_FILE_SIZE_BYTES) {
      throw new ScanFileTooLargeException("Scan file size " + fileSize + " bytes exceeds maximum allowed size of "
          + MAX_SCAN_FILE_SIZE_BYTES + " bytes");
    }
    log.debug("Storing scan file for appId={}, sourceFile={}", appId, scanFile.getName());
    ScanEntity tempScanEntity = scanPersistenceService.createTempScan(appId);
    try (InputStream in = new FileInputStream(scanFile);
        OutputStream out = tempScanEntity.getOutputStream())
    {
      IOUtils.copy(in, out);
    }
    catch (IOException e) {
      try {
        scanPersistenceService.deleteScan(tempScanEntity);
      }
      catch (IOException deleteEx) {
        log.warn("Failed to clean up temp scan entity appId={}, scanName={} after copy failure",
            appId, tempScanEntity.getName(), deleteEx);
      }
      throw e;
    }
    log.debug("Stored scan file for appId={} as tempScan={}", appId, tempScanEntity.getName());
    return tempScanEntity;
  }
}
