/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import java.io.IOException;
import java.time.Instant;
import jakarta.annotation.Nullable;

import com.sonatype.insight.brain.utils.IdValidationUtils;

/**
 * Service for managing the persistence of SBOM entities.
 * This service provides three distinct types of storage for SBOMs:
 * <p>
 * Permanent: SBOMs are saved indefinitely and associated with a specific application ID.
 * These can be retrieved at any time using the application ID and filename.
 * <p>
 * Temporary: SBOMs are saved in a temporary location with the original file name
 * and an optional prefix for additional namespacing. This allows retrieving the SBOM
 * later without keeping a reference to the SbomEntity object.
 * <p>
 * Transient: SBOMs are saved in a temporary location with a generated name.
 * If the reference to the SbomEntity is lost, the file cannot be retrieved later.
 */
public abstract class SbomPersistenceService
{
  /**
   * Get a permanent SBOM entity by application ID and fileName.
   *
   * @param appId the application ID
   * @param fileName the file name
   * @return the SBOM entity
   */
  public SbomEntity getPermanentSbom(String appId, String fileName) {
    IdValidationUtils.validate(appId);
    return doGetSbom(appId, fileName);
  }

  /**
   * Implementation method for retrieving a permanent SBOM entity.
   *
   * @param appId the application ID
   * @param fileName the file name
   * @return the SBOM entity
   */
  public abstract SbomEntity doGetSbom(String appId, String fileName);

  /**
   * Get a persistent temporary SBOM entity by file name.
   *
   * @param fileName the file name
   * @param prefix optional prefix that is added for additional namespacing
   * @return the persistent temporary SBOM entity
   */
  public abstract SbomEntity getTemporarySbom(String fileName, @Nullable String prefix);

  /**
   * Get a transient SBOM entity for the given file name. The entity returned by this method is aimed to be used
   * for a short period of time, such as during a scan or analysis, and it is expected to be deleted after use.
   *
   * @param fileName the file name
   * @return the transient SBOM entity
   * @throws IOException if an I/O error occurs
   */
  public abstract SbomEntity getTransientSbom(String fileName) throws IOException;

  /**
   * Save a SBOM entity to a persistent temporary location.
   *
   * @param sbomEntity the SBOM entity to save
   * @param fileName the file name where the SBOM will be saved
   * @param prefix optional prefix that is added for additional namespacing
   * @return the temporary SBOM entity
   * @throws IOException if an I/O error occurs
   */
  public abstract SbomEntity saveTemporarySbom(
      final SbomEntity sbomEntity,
      final String fileName,
      @Nullable final String prefix) throws IOException;

  /**
   * Delete a permanent SBOM entity.
   *
   * @param sbomEntity the SBOM entity to delete
   * @throws IOException if an I/O error occurs
   */
  public abstract void deleteSbom(SbomEntity sbomEntity) throws IOException;

  /**
   * Delete a specific permanent SBOM by application ID and file name.
   *
   * @param appId the application ID
   * @param fileName the file name
   * @throws IOException if an I/O error occurs
   */
  public abstract void deleteSbom(String appId, String fileName) throws IOException;

  /**
   * Delete all SBOMs associated with a specific application ID.
   *
   * @param appId the application ID
   * @throws IOException if an I/O error occurs
   */
  public abstract void deleteSbomsFor(final String appId) throws IOException;

  /**
   * Delete all SBOMs that are older than the specified instant.
   *
   * @param instant the instant before which SBOMs should be deleted.
   * @throws IOException if an I/O error occurs
   */
  public abstract void deleteTransientSbomsOlderThan(Instant instant) throws IOException;

  public abstract void moveSbomEntity(final SbomEntity from, final SbomEntity to) throws IOException;
}
