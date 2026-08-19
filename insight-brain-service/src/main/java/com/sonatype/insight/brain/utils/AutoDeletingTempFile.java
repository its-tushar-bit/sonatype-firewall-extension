/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AutoDeletingTempFile
    implements AutoCloseable
{
  private final Path file;

  public AutoDeletingTempFile() throws IOException {
    file = Files.createTempFile(null, null);
  }

  public AutoDeletingTempFile(Path dir, String extension) throws IOException {
    file = Files.createTempFile(dir, null, extension == null ? null : "." + extension);
  }

  public AutoDeletingTempFile(Path dir, String prefix, String extension) throws IOException {
    file = Files.createTempFile(dir, prefix, extension == null ? null : "." + extension);
  }

  public Path getPath() {
    return file;
  }

  @Override
  public void close() throws IOException {
    Files.deleteIfExists(file);
  }

  @Override
  public String toString() {
    return file.toString();
  }
}
