/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.IOException;
import java.util.stream.Stream;

import com.sonatype.insight.brain.utils.IdValidationUtils;

public abstract class ScanPersistenceService
{
  /**
   * Get a scan entity by application ID and scan ID.
   * Validates the IDs before delegating to the concrete implementation.
   *
   * @param appId the application ID
   * @param scanId the scan ID
   * @return the scan entity
   */
  public ScanEntity getScan(String appId, String scanId) {
    IdValidationUtils.validate(appId);
    IdValidationUtils.validate(scanId);
    return doGetScan(appId, scanId);
  }

  /**
   * @param appId the application ID
   * @param scanId the scan ID
   * @return the scan entity
   */
  protected abstract ScanEntity doGetScan(String appId, String scanId);

  /**
   * Delete a scan entity.
   *
   * @param scanEntity the scan entity to delete
   * @return true if successfully deleted, false otherwise
   * @throws IOException if an I/O error occurs
   */
  public boolean deleteScan(ScanEntity scanEntity) throws IOException {
    return scanEntity.delete();
  }

  /**
   * Create a temporary scan entity for the given application.
   *
   * @param appId the application ID
   * @return the temporary scan entity
   * @throws IOException if an I/O error occurs
   */
  public abstract ScanEntity createTempScan(String appId) throws IOException;

  /**
   * Move a temporary scan entity to its permanent location.
   *
   * @param tempScanEntity the temporary scan entity
   * @param appId the application ID
   * @param scanId the scan ID
   * @throws IOException if an I/O error occurs
   */
  public abstract void moveTempScan(ScanEntity tempScanEntity, String appId, String scanId) throws IOException;

  /**
   * Get a scan entity by application ID and the full file or object name.
   *
   * @param appId the application ID
   * @param name the full file or object name
   * @return the scan entity
   */
  public abstract ScanEntity getScanByName(String appId, String name);

  /**
   * Copy a scan file from source to destination.
   *
   * @param source the source scan entity
   * @param destination the destination scan entity
   * @throws IOException if an I/O error occurs
   */
  public abstract void copyScanFile(ScanEntity source, ScanEntity destination) throws IOException;

  /**
   * Delete all scans for the given application.
   *
   * @param appId the application ID
   * @throws IOException if an I/O error occurs
   */
  public abstract void deleteScansFor(String appId) throws IOException;

  /**
   * Get a stream of all scan entities for the given application.
   *
   * @param appId the application ID
   * @return stream of scan entities
   */
  public abstract Stream<ScanEntity> allScanFilesFor(String appId);

  /**
   * Delete a specific scan by application ID and scan ID.
   *
   * @param appId the application ID
   * @param scanId the scan ID
   */
  public abstract void deleteScan(String appId, String scanId) throws IOException;

  /**
   * @return the {@link ScanEntity} class this handles.
   */
  public abstract Class<? extends ScanEntity> getScanEntityClass();
}
