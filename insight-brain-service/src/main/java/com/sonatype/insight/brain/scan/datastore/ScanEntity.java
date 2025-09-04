/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

/**
 * Interface for abstracting scan file operations to enable different storage backends.
 * This allows switching between file system, S3, or other cloud storage implementations.
 */
public interface ScanEntity
{
  /**
   * @return Writer for writing scan data
   * @throws IOException if an I/O error occurs
   */
  Writer getWriter() throws IOException;

  /**
   * @return OutputStream for writing scan data
   * @throws IOException if an I/O error occurs
   */
  OutputStream getOutputStream() throws IOException;

  /**
   * @return InputStream for reading scan data
   * @throws IOException if an I/O error occurs
   */
  InputStream getInputStream() throws IOException;

  /**
   * @return location string
   */
  String getLocation();

  /**
   * @return true if the scan exists, false otherwise
   */
  boolean exists();

  /**
   * @return name of the scan entity
   */
  String getName();

  /**
   * @return last modified time in milliseconds
   * @throws IOException if an I/O error occurs
   */
  long getLastModifiedTime() throws IOException;

  /**
   * @return true if successfully deleted, false otherwise
   */
  boolean delete() throws IOException;

  /**
   * Get the application ID this scan belongs to.
   *
   * @return application ID
   */
  String getAppId();

  /**
   * Get the scan ID for this scan.
   *
   * @return scan ID
   */
  default String getScanId() {
    String scanId = getName();
    scanId = scanId.substring(scanId.indexOf('-') + 1, scanId.indexOf('.'));
    return scanId;
  }

  /**
   * The primary {@link ScanPersistenceService} class that handles this.
   */
  Class<? extends ScanPersistenceService> getScanPersistenceServiceClass();
}
