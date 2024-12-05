/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class FileReport implements Report
{
  private final File file;

  public FileReport(final File file) {
    this.file = file;
  }

  public File getFile() {
    return file;
  }

  @Override
  public boolean exists() {
    return file.exists();
  }

  @Override
  public void deleteIfExists() throws IOException {
    Files.deleteIfExists(file.toPath());
  }

  @Override
  public boolean canCreate() {
    return !file.isFile() || file.length() == 0;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return new FileOutputStream(file);
  }

  @Override
  public String getLocation() {
    return file.getAbsolutePath();
  }

  @Override
  public long length() {
    return file.length();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new FileInputStream(file);
  }
}
