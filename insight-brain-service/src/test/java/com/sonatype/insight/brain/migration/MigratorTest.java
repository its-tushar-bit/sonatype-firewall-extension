/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;

import org.junit.After;
import org.junit.Before;

public class MigratorTest
{
  final MigrationTrackerDAO migrationTrackerDAO = new MigrationTrackerDAO();

  final RootOrganizationConfigMigrationUtils migrationUtils =
      new RootOrganizationConfigMigrationUtils(migrationTrackerDAO);

  @Before
  public void cleanupMigrationTrackersBefore() {
    migrationTrackerDAO.deleteAll();
  }

  @After
  public void cleanupMigrationTrackersAfter() {
    migrationTrackerDAO.deleteAll();
    migrationUtils.setMigrated();
  }
}
