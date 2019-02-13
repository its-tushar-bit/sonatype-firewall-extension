/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionServiceTest
{
  private VersionService versionService;

  @Before
  public void setup() {
    versionService = new VersionService();
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
    versionService = new VersionService(new Properties());
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
}
