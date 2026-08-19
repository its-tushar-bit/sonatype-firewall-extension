/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

/**
 * File-based implementation of ScanEntity.
 * Stores scan data as files in the local file system.
 */
public record FileScanEntity(Path path, String appId)
    implements ScanEntity
{
  /**
   * The assumed directory path is scan/{appId}/scan-{scanId}.xml.gz. The intent is for production code to be explicit
   * about what the appId value is so that each implementation can decide how the underlying file or objects are stored.
   * To avoid a larger refactor, this constructor is provided for testing purposes only
   */
  @Deprecated
  public FileScanEntity(final Path path) {
    this(path, path.getParent().getFileName().toString());
  }

  @Override
  public Writer getWriter() throws IOException {
    return new OutputStreamWriter(
        new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(path), 32768)),
        StandardCharsets.UTF_8);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    Files.createDirectories(path.getParent());
    return Files.newOutputStream(path);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return Files.newInputStream(path);
  }

  @Override
  public String getLocation() {
    return path.toAbsolutePath().toString();
  }

  @Override
  public boolean exists() {
    return Files.exists(path) && Files.isRegularFile(path);
  }

  @Override
  public String getName() {
    return path.getFileName().toString();
  }

  @Override
  public long getLastModifiedTime() throws IOException {
    return Files.getLastModifiedTime(path).toMillis();
  }

  @Override
  public boolean delete() throws IOException {
    return Files.deleteIfExists(path);
  }

  @Override
  public String getAppId() {
    return appId;
  }

  @Override
  public Class<? extends ScanPersistenceService> getScanPersistenceServiceClass() {
    return FileScanPersistenceService.class;
  }

  @Override
  public String toString() {
    return path.toAbsolutePath().toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof FileScanEntity) {
      return path.equals(((FileScanEntity) obj).path);
    }
    return false;
  }
}
