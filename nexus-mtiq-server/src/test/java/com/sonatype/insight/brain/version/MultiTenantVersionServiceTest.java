/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantVersionServiceTest
{
  @Test
  public void retrievesShortenedAndNormalBuildNumber() {
    VersionService versionService = new MultiTenantVersionService();

    String version = versionService.getVersion();
    String build = versionService.getBuild();
    try {
      versionService.setVersion("some_version");
      versionService.setBuild("dead_beef");

      String shortVersion = versionService.getShortVersion();
      String fullVersion = versionService.getFullVersion();

      assertThat(shortVersion).isEqualTo("dead_beef");
      assertThat(fullVersion).isEqualTo("dead_beef");
    }
    finally {
      versionService.setVersion(version);
      versionService.setBuild(build);
    }
  }
}
