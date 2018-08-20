/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class VersionServiceTest
{

  private VersionService versionService;

  @Before
  public void setup() {
    versionService = new VersionService();
  }

  @Test
  public void testGetters() {
    assertNotNull(versionService.getProperties());
    assertNotNull(versionService.getName());
    assertNotNull(versionService.getBuild());
    assertNotNull(versionService.getTimestamp());
    assertNotNull(versionService.getVersion());
    assertNotNull(versionService.getTag());
    assertNotNull(versionService.getLogDisplayVersion());

    // Ensure that cleared properties return null.
    versionService = new VersionService(new Properties());
    assertNull(versionService.getName());
    assertNull(versionService.getBuild());
    assertNull(versionService.getTimestamp());
    assertNull(versionService.getVersion());
    assertNull(versionService.getTag());

    // Ensure that the default is returned when provided.
    String defaultValue = "default";
    assertThat(versionService.getName(defaultValue), is(defaultValue));
    assertThat(versionService.getBuild(defaultValue), is(defaultValue));
    assertThat(versionService.getTimestamp(defaultValue), is(defaultValue));
    assertThat(versionService.getVersion(defaultValue), is(defaultValue));
    assertThat(versionService.getTag(defaultValue), is(defaultValue));
  }

  @Test
  public void testGetLogDisplayVersion() {
    versionService.setVersion("foo");
    assertThat(versionService.getLogDisplayVersion(), is("foo"));

    versionService.setVersion("1.50.0-SNAPSHOT");
    assertThat(versionService.getLogDisplayVersion(), is("50.0-SNAPSHOT"));

    versionService.setVersion("1.50.1-01");
    assertThat(versionService.getLogDisplayVersion(), is("50.1-01"));

    versionService.setVersion("50.0-SNAPSHOT");
    assertThat(versionService.getLogDisplayVersion(), is("50.0-SNAPSHOT"));

    versionService.setVersion("50.1-01");
    assertThat(versionService.getLogDisplayVersion(), is("50.1-01"));
  }
}
