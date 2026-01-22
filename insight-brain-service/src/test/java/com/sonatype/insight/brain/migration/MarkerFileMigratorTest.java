/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.openjpa.persistence.RollbackException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class MarkerFileMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private InsightWork insightWork;

  @Inject
  private MarkerFileMigrator markerFileMigrator;

  @Before
  public void before() {
    migrationTrackerDAO.deleteById(MarkerFileMigrator.MARKER_FILE_MIGRATOR_ID);
  }

  @Test
  public void testMigrate_mustNotRunIfRunPreviously() throws IOException {
    migrationTrackerDAO.insert(new MigrationTracker(MarkerFileMigrator.MARKER_FILE_MIGRATOR_ID));

    File markerFile =
        new File(insightWork.getWorkDir(), MarkerFileMigrator.POLICY_COORDINATES_CONDITION_TYPE_MARKER_FILE);
    markerFile.createNewFile();

    markerFileMigrator.migrate();

    assertThat(migrationTrackerDAO.getById(MarkerFileMigrator.POLICY_COORDINATES_CONDITION_TYPE_MARKER_FILE)).isNull();
  }

  @Test
  public void testMigrate_MustMovePolicyCoordinatesConditionTypeMarkerFileToDatabase() throws IOException {
    testMigrate(PolicyCoordinatesConditionTypeMigrator.MIGRATION_ID,
        new File(insightWork.getWorkDir(), MarkerFileMigrator.POLICY_COORDINATES_CONDITION_TYPE_MARKER_FILE));
  }

  @Test
  public void testMigrate_MustMovePolicySecurityVulnerabilityConditionTypeMarkerFileToDatabase() throws IOException {
    testMigrate(PolicySecurityVulnerabilityConditionTypeMigrator.MIGRATION_ID, new File(insightWork.getWorkDir(),
        MarkerFileMigrator.POLICY_SECURITY_VULNERABILITY_CONDITION_TYPE_MARKER_FILE));
  }

  @Test
  public void testMigrate_MustMoveProprietaryConfigMarkerFileToDatabase() throws IOException {
    testMigrate(ProprietaryConfigMigrator.MIGRATION_ID,
        new File(insightWork.getWorkDir(), MarkerFileMigrator.PROPRIETARY_CONFIG_MARKER_FILE));
  }

  @Test
  public void testMigrate_MustMoveSecurityVulnerabilityOverrideMarkerFileToDatabase() throws IOException {
    testMigrate(SecurityVulnerabilityOverrideMigrator.MIGRATION_ID,
        new File(insightWork.getAuditDir(""), MarkerFileMigrator.SECURITY_VULNERABILITY_OVERRIDE_MARKER_FILE));
  }

  @Test
  public void testMigrate_MustNotInsertTrackerIfMigrationFileDoesNotExist() {
    markerFileMigrator.migrate();
    assertThat(migrationTrackerDAO.getById(PolicyCoordinatesConditionTypeMigrator.MIGRATION_ID)).isNull();
  }

  @Test
  public void testMigrate_UsesSingleTransaction() throws IOException {
    File markerFile =
        new File(insightWork.getWorkDir(), MarkerFileMigrator.POLICY_COORDINATES_CONDITION_TYPE_MARKER_FILE);
    markerFile.createNewFile();

    markerFileMigrator = new MarkerFileMigrator(migrationTrackerDAO, insightWork);
    migrationTrackerDAO.insert(new MigrationTracker(PolicyCoordinatesConditionTypeMigrator.MIGRATION_ID));

    assertThatExceptionOfType(RollbackException.class).isThrownBy(() -> markerFileMigrator.migrate());
    assertThat(migrationTrackerDAO.getById(MarkerFileMigrator.MARKER_FILE_MIGRATOR_ID)).isNull();
  }

  private void testMigrate(String migrationTrackerId, File markerFile) throws IOException {
    migrationTrackerDAO.deleteById(migrationTrackerId);
    assertThat(migrationTrackerDAO.getById(MarkerFileMigrator.MARKER_FILE_MIGRATOR_ID)).isNull();
    assertThat(migrationTrackerDAO.getById(migrationTrackerId)).isNull();
    markerFile.getParentFile().mkdirs();
    markerFile.createNewFile();
    markerFileMigrator.migrate();
    assertThat(migrationTrackerDAO.getById(migrationTrackerId)).isNotNull();
    assertThat(markerFile).isFile();
    assertThat(migrationTrackerDAO.getById(MarkerFileMigrator.MARKER_FILE_MIGRATOR_ID)).isNotNull();
  }
}
