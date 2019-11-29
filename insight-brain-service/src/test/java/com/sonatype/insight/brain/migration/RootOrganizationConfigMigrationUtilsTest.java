/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RootOrganizationConfigMigrationUtilsTest
    extends AbstractComponentTest
{
  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private RootOrganizationConfigMigrationUtils migrationUtils;

  @Before
  public void before() {
    migrationTrackerDAO.deleteById(RootOrganizationConfigMigrationUtils.MIGRATION_ID);
    migrationTrackerDAO.deleteById(RootOrganizationConfigMigrationUtils.MIGRATION_CONFIG_ID);
  }

  @Test
  public void testSetMigrated() {
    assertThat(migrationUtils.isMigrationScheduled()).isFalse();
    assertThat(migrationUtils.isMigrated()).isFalse();

    migrationUtils.setMigrated();

    assertThat(migrationUtils.isMigrationScheduled()).isFalse();
    assertThat(migrationUtils.isMigrated()).isTrue();
  }

  @Test
  public void testSetSourceOrganizationId_And_SetMigrated() {
    assertThat(migrationUtils.isMigrationScheduled()).isFalse();
    assertThat(migrationUtils.isMigrated()).isFalse();
    String sourceOrganizationId = "bla";

    migrationUtils.setSourceOrganizationId(sourceOrganizationId);

    assertThat(migrationUtils.getSourceOrganizationId()).isEqualTo(sourceOrganizationId);
    assertThat(migrationUtils.isMigrated()).isFalse();

    migrationUtils.setMigrated();

    assertThat(migrationUtils.isMigrationScheduled()).isTrue();
    assertThat(migrationUtils.isMigrated()).isTrue();
  }
}
