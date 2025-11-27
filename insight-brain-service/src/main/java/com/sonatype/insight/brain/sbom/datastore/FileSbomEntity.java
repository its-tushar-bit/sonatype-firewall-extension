/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;

public record FileSbomEntity(Path path, @Nullable String appId, String fileName) implements SbomEntity
{
  @Override
  public InputStream getInputStream() throws IOException {
    return Files.newInputStream(path);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return Files.newOutputStream(path);
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Nullable
  @Override
  public String getAppId() {
    return appId;
  }

  @Override
  public String getName() {
    return path.getFileName().toString();
  }

  @Override
  public String getLocation() {
    return path.toAbsolutePath().toString();
  }

  @Override
  public boolean exists() {
    return Files.exists(path);
  }

  @Override
  public Class<? extends SbomPersistenceService> getSbomPersistenceServiceClass() {
    return FileSbomPersistenceService.class;
  }

  @Override
  public String toString() {
    return getLocation();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof FileSbomEntity) {
      return path.equals(((FileSbomEntity) obj).path);
    }
    return false;
  }
}
