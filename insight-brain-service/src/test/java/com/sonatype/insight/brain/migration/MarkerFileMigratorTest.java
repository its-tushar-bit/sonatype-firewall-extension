/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;

import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.openjpa.persistence.RollbackException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class MarkerFileMigratorTest
    extends MigratorTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private InsightWork insightWork;

  private MarkerFileMigrator markerFileMigrator;

  @Before
  public void setUp() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    File workDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(workDir.getAbsolutePath());
    insightWork = new InsightWork(insightConfig);
    insightWork.getDataDir().mkdirs();
    markerFileMigrator = new MarkerFileMigrator(migrationTrackerDAO, insightWork);
  }

  @Test
  public void testMigrate() throws IOException {
    assertThat(migrationTrackerDAO.getById(MarkerFileMigrator.MARKER_FILE_MIGRATOR_ID)).isNull();
    File markerFile =
        new File(insightWork.getWorkDir(), MarkerFileMigrator.POLICY_COORDINATES_CONDITION_TYPE_MARKER_FILE);
    markerFile.createNewFile();

    markerFileMigrator.migrate();

    assertThat(migrationTrackerDAO.getById(PolicyCoordinatesConditionTypeMigrator.MIGRATION_ID)).isNotNull();
    assertThat(markerFile).isFile();
    assertThat(migrationTrackerDAO.getById(MarkerFileMigrator.MARKER_FILE_MIGRATOR_ID)).isNotNull();
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

    try {
      markerFileMigrator.migrate();
      fail("Transaction should have been rolled back!");
    }
    catch (RollbackException rollbackException) {
      // Assert Migration Tracker itself is not tracked
      assertThat(migrationTrackerDAO.getById(MarkerFileMigrator.MARKER_FILE_MIGRATOR_ID)).isNull();
    }
  }
}
