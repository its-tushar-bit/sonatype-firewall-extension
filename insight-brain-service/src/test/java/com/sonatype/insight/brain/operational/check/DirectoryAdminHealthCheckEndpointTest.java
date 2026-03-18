/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.operational.check.AdminHealthCheckEndpoint.HealthCheckResponse;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryAdminHealthCheckEndpointTest
    extends AbstractComponentTest
{
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

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
      file = folder.newFolder();
    }
    else {
      file = folder.newFile("test");
    }
    directoryAdminHealthCheckEndpoint = new DirectoryAdminHealthCheckEndpoint("test", "test", file);
    assertThat(directoryAdminHealthCheckEndpoint.getHealthCheckResponse())
        .usingRecursiveComparison()
        .isEqualTo(expectedHealthCheckResponse);
  }
}
