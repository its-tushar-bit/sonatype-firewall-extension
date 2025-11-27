/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import javax.annotation.Nullable;

public interface SbomEntity
{
  /**
   * @return InputStream for reading SBOM data
   * @throws IOException if an I/O error occurs
   */
  InputStream getInputStream() throws IOException;

  /**
   * @return OutputStream for writing SBOM data
   * @throws IOException if an I/O error occurs
   */
  OutputStream getOutputStream() throws IOException;

  /**
   * Get the path to the SBOM entity.
   *
   * @return the local path to the entity, or a temporary copy if not stored locally
   */
  Path getPath();

  /**
   * Get the application ID this SBOM belongs to.
   *
   * @return application ID
   */
  @Nullable
  String getAppId();

  /**
   * @return name of the SBOM entity
   */
  String getName();

  /**
   * @return location string
   */
  String getLocation();

  /**
   * Check if the SBOM entity exists.
   *
   * @return true if the entity exists, false otherwise
   */
  boolean exists();

  /**
   * Get the class of the persistence service that handles this SBOM entity.
   *
   * @return the class of the persistence service
   */
  Class<? extends SbomPersistenceService> getSbomPersistenceServiceClass();
}
