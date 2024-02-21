/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.io.File;
import java.nio.file.Files;

public class DirectoryAdminHealthCheckEndpoint
    implements AdminHealthCheckEndpoint
{
  private final String name;

  private final String path;

  private final File directory;

  public DirectoryAdminHealthCheckEndpoint(String name, String path, File directory) {
    this.name = name;
    this.path = path;
    this.directory = directory;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public HealthCheckResponse getHealthCheckResponse() {
    if (!directory.isDirectory() || !Files.isWritable(directory.toPath())) {
      return new HealthCheckResponse(false, directory.getName() + " is not a valid directory");
    }
    return new HealthCheckResponse(true);
  }
}
