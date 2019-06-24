/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.model.MigrationTracker;

/**
 * @since 1.18
 */
@Named
@Singleton
public class RootOrganizationConfigMigrationUtils
{
  public static final String MIGRATION_ID = "root-organization";

  public static final String MIGRATION_CONFIG_ID = "root-organization-config";

  private final MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  public RootOrganizationConfigMigrationUtils(MigrationTrackerDAO migrationTrackerDAO) {
    this.migrationTrackerDAO = migrationTrackerDAO;
  }

  public boolean isMigrated() {
    return migrationTrackerDAO.isTrackerPresent(MIGRATION_ID);
  }

  public boolean isMigrationScheduled() {
    return migrationTrackerDAO.isTrackerPresent(MIGRATION_CONFIG_ID);
  }

  public void setSourceOrganizationId(String orgId) {
    MigrationTracker migrationTracker = new MigrationTracker(MIGRATION_CONFIG_ID);
    migrationTracker.setConfiguration(orgId);
    migrationTrackerDAO.insert(migrationTracker);
  }

  public void setMigrated() {
    if (!isMigrated()) {
      migrationTrackerDAO.insert(new MigrationTracker(MIGRATION_ID));
    }
  }

  public String getSourceOrganizationId() {
    MigrationTracker migrationTracker = migrationTrackerDAO.getById(MIGRATION_CONFIG_ID);
    return migrationTracker == null ? null : migrationTracker.getConfiguration();
  }
}
