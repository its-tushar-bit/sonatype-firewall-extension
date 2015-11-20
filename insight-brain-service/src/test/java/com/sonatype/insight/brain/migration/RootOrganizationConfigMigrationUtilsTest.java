/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RootOrganizationConfigMigrationUtilsTest
    extends AbstractComponentTest
{
  @Inject
  private RootOrganizationConfigMigrationUtils migrationUtils;

  @Before
  public void before() {
    migrationUtils.clean();
  }

  @After
  public void after() {
    migrationUtils.clean();
  }

  @Test
  public void testIsMigrated() throws Exception {
    assertThat(migrationUtils.isMigrated(), is(false));

    migrationUtils.setMigrated();
    assertThat(migrationUtils.isMigrated(), is(true));

    migrationUtils.clean();
    assertThat(migrationUtils.isMigrationScheduled(), is(false));
  }

  @Test
  public void testSetMigrated_RemovesMigrationFile() throws Exception {
    assertThat(migrationUtils.isMigrated(), is(false));

    migrationUtils.setSourceOrganizationId("bla");
    migrationUtils.setMigrated();
    assertThat(migrationUtils.isMigrated(), is(true));
    assertThat(migrationUtils.isMigrationScheduled(), is(false));
  }

  @Test
  public void testIsMigrationScheduled() throws Exception {
    assertThat(migrationUtils.isMigrationScheduled(), is(false));

    migrationUtils.setSourceOrganizationId("bla");
    assertThat(migrationUtils.isMigrationScheduled(), is(true));
  }
}
