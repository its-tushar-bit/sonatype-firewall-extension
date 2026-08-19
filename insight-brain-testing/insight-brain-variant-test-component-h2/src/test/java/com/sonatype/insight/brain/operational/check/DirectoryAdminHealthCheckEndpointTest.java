/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.sonatype.insight.brain.operational.check.AdminHealthCheckEndpoint.HealthCheckResponse;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class DirectoryAdminHealthCheckEndpointTest
    extends AbstractComponentH2Test
{
  @TempDir
  public File folder;

  private DirectoryAdminHealthCheckEndpoint directoryAdminHealthCheckEndpoint;

  @Test
  public void testIsHealthy() throws IOException {
    testIsHealthy(true, new HealthCheckResponse(true));
  }

  @Test
  public void testIsHealthy_InvalidDirectory() throws IOException {
    testIsHealthy(false, new HealthCheckResponse(false, "test is not a valid directory"));
  }

  private void testIsHealthy(
      boolean validDirectory,
      HealthCheckResponse expectedHealthCheckResponse) throws IOException
  {
    File file;
    if (validDirectory) {
      file = Files.createDirectory(folder.toPath().resolve("dir")).toFile();
    }
    else {
      file = Files.createFile(folder.toPath().resolve("test")).toFile();
    }
    directoryAdminHealthCheckEndpoint = new DirectoryAdminHealthCheckEndpoint("test", "test", file);
    assertThat(directoryAdminHealthCheckEndpoint.getHealthCheckResponse())
        .usingRecursiveComparison()
        .isEqualTo(expectedHealthCheckResponse);
  }
}
