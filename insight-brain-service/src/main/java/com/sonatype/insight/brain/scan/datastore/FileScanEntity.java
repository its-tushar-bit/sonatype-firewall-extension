/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

/**
 * File-based implementation of ScanEntity.
 * Stores scan data as files in the local file system.
 */
public record FileScanEntity(File file, String appId)
    implements ScanEntity
{
  /**
   * The assumed directory path is scan/{appId}/scan-{scanId}.xml.gz. The intent is for production code to be explicit
   * about what the appId value is so that each implementation can decide how the underlying file or objects are stored.
   * To avoid a larger refactor, this constructor is provided for testing purposes only
   */
  @Deprecated
  public FileScanEntity(final File file) {
    this(file, file.getParentFile().getName());
  }

  @Override
  public Writer getWriter() throws IOException {
    return new OutputStreamWriter(
        new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(file.toPath()), 32768)),
        StandardCharsets.UTF_8);
  }

  @Override
  public OutputStream getOutputStream() throws FileNotFoundException {
    return new FileOutputStream(file);
  }

  @Override
  public InputStream getInputStream() throws FileNotFoundException {
    return new FileInputStream(file);
  }

  @Override
  public String getLocation() {
    return file.getAbsolutePath();
  }

  @Override
  public boolean exists() {
    return file.exists() && file.isFile();
  }

  @Override
  public String getName() {
    return file.getName();
  }

  @Override
  public long getLastModifiedTime() throws IOException {
    return Files.getLastModifiedTime(file.toPath()).toMillis();
  }

  @Override
  public boolean delete() {
    return file.delete();
  }

  @Override
  public String getAppId() {
    return appId;
  }

  @Override
  public String toString() {
    return file.getAbsolutePath();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof FileScanEntity) {
      return file.equals(((FileScanEntity) obj).file);
    }
    return false;
  }
}
