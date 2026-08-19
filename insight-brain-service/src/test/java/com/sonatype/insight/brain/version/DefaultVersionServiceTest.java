/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultVersionServiceTest
{
  private VersionService versionService;

  @BeforeEach
  public void setup() {
    versionService = new DefaultVersionService();
  }

  @Test
  public void testGetters() {
    assertThat(versionService.getProperties()).isNotNull();
    assertThat(versionService.getName()).isNotNull();
    assertThat(versionService.getBuild()).isNotNull();
    assertThat(versionService.getTimestamp()).isNotNull();
    assertThat(versionService.getVersion()).isNotNull();
    assertThat(versionService.getTag()).isNotNull();
    assertThat(versionService.getLogDisplayVersion()).isNotNull();

    // Ensure that cleared properties return null.
    versionService = new DefaultVersionService(new Properties());
    assertThat(versionService.getName()).isNull();
    assertThat(versionService.getBuild()).isNull();
    assertThat(versionService.getTimestamp()).isNull();
    assertThat(versionService.getVersion()).isNull();
    assertThat(versionService.getTag()).isNull();

    // Ensure that the default is returned when provided.
    String defaultValue = "default";
    assertThat(versionService.getName(defaultValue)).isEqualTo(defaultValue);
    assertThat(versionService.getBuild(defaultValue)).isEqualTo(defaultValue);
    assertThat(versionService.getTimestamp(defaultValue)).isEqualTo(defaultValue);
    assertThat(versionService.getVersion(defaultValue)).isEqualTo(defaultValue);
    assertThat(versionService.getTag(defaultValue)).isEqualTo(defaultValue);
  }

  @Test
  public void testGetLogDisplayVersion() {
    String version = versionService.getVersion();

    try {
      versionService.setVersion("foo");
      assertThat(versionService.getLogDisplayVersion()).isEqualTo("foo");

      versionService.setVersion("1.50.0-SNAPSHOT");
      assertThat(versionService.getLogDisplayVersion()).isEqualTo("50.0-SNAPSHOT");

      versionService.setVersion("1.50.1-01");
      assertThat(versionService.getLogDisplayVersion()).isEqualTo("50.1-01");

      versionService.setVersion("50.0-SNAPSHOT");
      assertThat(versionService.getLogDisplayVersion()).isEqualTo("50.0-SNAPSHOT");

      versionService.setVersion("50.1-01");
      assertThat(versionService.getLogDisplayVersion()).isEqualTo("50.1-01");
    }
    finally {
      // set version back to what it was before the test
      versionService.setVersion(version);
    }
  }

  @Test
  public void testCompare() {
    // Version to compare against
    final String targetVersion = "1.180.0-min";
    final String olderVersion = "1.176.0";
    final String newerVersion = "1.182.0";
    final String snapshotVersion = targetVersion + "-SNAPSHOT";

    assertThat(versionService.compare(olderVersion, targetVersion)).isEqualTo(-1);
    assertThat(versionService.compare(newerVersion, targetVersion)).isEqualTo(1);
    assertThat(versionService.compare(targetVersion, targetVersion)).isZero();
    assertThat(versionService.compare(snapshotVersion, targetVersion)).isEqualTo(1);
  }
}
