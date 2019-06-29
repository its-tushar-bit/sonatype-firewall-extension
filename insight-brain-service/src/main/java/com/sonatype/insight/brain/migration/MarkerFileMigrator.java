/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class MarkerFileMigrator
{
  private static final Logger log = LoggerFactory.getLogger(MarkerFileMigrator.class);

  static final String MARKER_FILE_MIGRATOR_ID = "marker-files";

  static final String POLICY_COORDINATES_CONDITION_TYPE_MARKER_FILE = "policycoordinatesconditiontype-migrated";

  static final String POLICY_SECURITY_VULNERABILITY_CONDITION_TYPE_MARKER_FILE =
      "policysecurityvulnerabilityconditiontype-migrated";

  static final String PROPRIETARY_CONFIG_MARKER_FILE = "proprietaryconfig-migrated";

  static final String SECURITY_VULNERABILITY_OVERRIDE_MARKER_FILE = "svoverrides-migrated";

  static final String ROOT_ORGANIZATION_CONFIG_MARKER_FILE = "rootorganizationconfig-migrated";

  static final String ROOT_ORGANIZATION_CONFIG_FILE = "rootorganizationconfig-migration";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final InsightWork insightWork;

  @Inject
  public MarkerFileMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      InsightWork insightWork)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.insightWork = insightWork;
  }

  public void migrate() {
    if (migrationTrackerDAO.isTrackerPresent(MARKER_FILE_MIGRATOR_ID)) {
      log.info("Marker files already migrated.");
      return;
    }

    try (TransactionContext tx = migrationTrackerDAO.createTransactionContext()) {
      tx.begin();

      migrateOne(new File(insightWork.getWorkDir(), POLICY_COORDINATES_CONDITION_TYPE_MARKER_FILE),
          PolicyCoordinatesConditionTypeMigrator.MIGRATION_ID, tx);
      migrateOne(new File(insightWork.getWorkDir(), POLICY_SECURITY_VULNERABILITY_CONDITION_TYPE_MARKER_FILE),
          PolicySecurityVulnerabilityConditionTypeMigrator.MIGRATION_ID, tx);
      migrateOne(new File(insightWork.getWorkDir(), PROPRIETARY_CONFIG_MARKER_FILE),
          ProprietaryConfigMigrator.MIGRATION_ID, tx);
      migrateOne(new File(insightWork.getAuditDir(""), SECURITY_VULNERABILITY_OVERRIDE_MARKER_FILE),
          SecurityVulnerabilityOverrideMigrator.MIGRATION_ID, tx);
      migrateRootOrganizationMarkerOrConfig(tx);

      // Track `this` so it does not run again
      migrationTrackerDAO.insertTracker(tx, MARKER_FILE_MIGRATOR_ID);
      tx.commit();
    }
  }

  private void migrateOne(File markerFile, String migrationId, TransactionContext tx) {
    if (markerFile.exists()) {
      migrationTrackerDAO.insertTracker(tx, migrationId);
      log.info("Migration state moved to database for: {}", migrationId);
    }
  }

  private void migrateRootOrganizationMarkerOrConfig(TransactionContext tx) {
    File markerFile = new File(insightWork.getWorkDir(), ROOT_ORGANIZATION_CONFIG_MARKER_FILE);
    File configFile = new File(insightWork.getWorkDir(), ROOT_ORGANIZATION_CONFIG_FILE);
    if (markerFile.exists()) {
      migrationTrackerDAO.insertTracker(tx, RootOrganizationConfigMigrationUtils.MIGRATION_ID);
    }
    else if (configFile.exists()) {
      MigrationTracker migrationTracker =
          new MigrationTracker(RootOrganizationConfigMigrationUtils.MIGRATION_CONFIG_ID);
      String sourceOrganizationId;
      try {
        sourceOrganizationId = FileUtils.fileRead(configFile);
      }
      catch (IOException e) {
        throw new UncheckedIOException(
            "Cannot load the source organization ID from file: " + configFile.getAbsolutePath(), e);
      }
      migrationTracker.setConfiguration(sourceOrganizationId);
      migrationTrackerDAO.insert(tx, migrationTracker);
    }
    log.info("Migration state moved to database for: {}", RootOrganizationConfigMigrationUtils.MIGRATION_ID);
  }
}
