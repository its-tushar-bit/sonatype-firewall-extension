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

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(migrationUtils.isMigrated()).isFalse();

    migrationUtils.setMigrated();
    assertThat(migrationUtils.isMigrated()).isTrue();

    migrationUtils.clean();
    assertThat(migrationUtils.isMigrationScheduled()).isFalse();
  }

  @Test
  public void testSetMigrated_RemovesMigrationFile() throws Exception {
    assertThat(migrationUtils.isMigrated()).isFalse();

    migrationUtils.setSourceOrganizationId("bla");
    migrationUtils.setMigrated();
    assertThat(migrationUtils.isMigrated()).isTrue();
    assertThat(migrationUtils.isMigrationScheduled()).isFalse();
  }

  @Test
  public void testIsMigrationScheduled() throws Exception {
    assertThat(migrationUtils.isMigrationScheduled()).isFalse();

    migrationUtils.setSourceOrganizationId("bla");
    assertThat(migrationUtils.isMigrationScheduled()).isTrue();
  }
}
