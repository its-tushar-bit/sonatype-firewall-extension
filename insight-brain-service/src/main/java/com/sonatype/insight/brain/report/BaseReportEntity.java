/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Representation of an individual, persisted file that makes up an application report (such as bom.json). Note that
 * an instance of this class does not guarantee that the file actually exists. Current file existence can be checked
 * with the exists() method.
 *
 * Contrast with ReportEntry, which contains the _contents_ of such a file in memory.
 */
public interface BaseReportEntity
{
  /**
   * @return whether the file exists in the underlying storage
   * @throws IOException if there is an error reading the file's metadata
   */
  boolean exists() throws IOException;

  /**
   * @param source the source of metadata to use
   * @return whether the file exists in the underlying storage
   * @throws IOException if there is an error reading the file's metadata
   * @implNote The default implementation ignores the {@code source} parameter and always fetches
   *           fresh metadata from the underlying storage (equivalent to {@link MetadataSource#FETCH}).
   *           Implementations that support caching should override this method.
   */
  default boolean exists(MetadataSource source) throws IOException {
    return exists();
  }

  /**
   * @return the last modified time of the file
   * @throws IOException if the file does not exist, or if there is an error reading the file's metadata
   */
  long getTime() throws IOException;

  /**
   * @param source the source of metadata to use
   * @return the last modified time of the file
   * @throws IOException if the file does not exist, or if there is an error reading the file's metadata
   * @implNote The default implementation ignores the {@code source} parameter and always fetches
   *           fresh metadata from the underlying storage (equivalent to {@link MetadataSource#FETCH}).
   *           Implementations that support caching should override this method.
   */
  default long getTime(MetadataSource source) throws IOException {
    return getTime();
  }

  /**
   * @return the length of the file in bytes
   * @throws IOException if the file does not exist, or if there is an error reading the file's metadata
   */
  long length() throws IOException;

  /**
   * @param source the source of metadata to use
   * @return the length of the file in bytes
   * @throws IOException if the file does not exist, or if there is an error reading the file's metadata
   * @implNote The default implementation ignores the {@code source} parameter and always fetches
   *           fresh metadata from the underlying storage (equivalent to {@link MetadataSource#FETCH}).
   *           Implementations that support caching should override this method.
   */
  default long length(MetadataSource source) throws IOException {
    return length();
  }

  /**
   * @param metadataAttributes the minimal metadata attributes to retrieve
   * @return at least the requested metadata for the report entity or empty if it does not exist
   * @throws IOException if there is an error reading the file's metadata
   */
  Optional<Metadata> getMetadata(MetadataAttribute... metadataAttributes) throws IOException;

  /**
   * @param source the source of metadata to use
   * @param metadataAttributes the minimal metadata attributes to retrieve
   * @return at least the requested metadata for the report entity or empty if it does not exist
   * @throws IOException if there is an error reading the file's metadata
   * @implNote The default implementation ignores the {@code source} parameter and always fetches
   *           fresh metadata from the underlying storage (equivalent to {@link MetadataSource#FETCH}).
   *           Implementations that support caching should override this method.
   */
  default Optional<Metadata> getMetadata(
      MetadataSource source,
      MetadataAttribute... metadataAttributes) throws IOException
  {
    return getMetadata(metadataAttributes);
  }

  /**
   * This method is intended to allow the cached metadata to be set in cases where we know something about it from other
   * sources, e.g., if we've just successfully copied the file from one location to another, then we know it exists
   *
   * @param metadata the metadata to set, null should clear the cached metadata
   */
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  default void setMetadata(Optional<Metadata> metadata) {
    // no-op by default
  }

  /**
   * @return an OutputStream that can be used to write to the file. If the file already exists, it will be overwritten.
   *         If it doesn't already exist, it will be created.
   */
  OutputStream getOutputStream() throws IOException;

  /**
   * @return an InputStream that can be used to read from the file. If the file does not exist, an IOException will be
   *         thrown.
   */
  InputStream getInputStream() throws IOException;

  /**
   * The primary {@link LifecycleReportPersistenceService} class that handles this.
   */
  Class<? extends LifecycleReportPersistenceService> getLifecycleReportPersistenceServiceClass();
}
