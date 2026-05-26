/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlFileStorageMigratorTest
    extends AbstractComponentTest
{
  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private InsightWork insightWork;

  @Inject
  private SourceControlFileStorageMigrator sourceControlFileStorageMigrator;

  @Before
  public void before() throws IOException {
    migrationTrackerDAO.deleteById(SourceControlFileStorageMigrator.MIGRATION_ID);
    FileUtils.deleteDirectory(insightWork.getResolvedCloneDirectory());
  }

  @Test
  public void testMigrate_mustNotRunIfRunPreviously() throws Exception {
    migrationTrackerDAO.insert(new MigrationTracker(SourceControlFileStorageMigrator.MIGRATION_ID));
    File sourceControlCloneDir = insightWork.getSourceControlDir("testappid");
    Files.createDirectories(sourceControlCloneDir.toPath());
    assertThat(sourceControlCloneDir).isDirectory();

    sourceControlFileStorageMigrator.migrate();

    assertThat(sourceControlCloneDir).isDirectory();
  }

  @Test
  public void testMigrate_sourceControlDirDoesNotExist() {
    assertThat(insightWork.getResolvedCloneDirectory()).doesNotExist();

    sourceControlFileStorageMigrator.migrate();

    assertThat(migrationTrackerDAO.getById(SourceControlFileStorageMigrator.MIGRATION_ID)).isNotNull();
  }

  @Test
  public void testMigrate() throws IOException {
    assertThat(migrationTrackerDAO.getById(SourceControlFileStorageMigrator.MIGRATION_ID)).isNull();
    createSourceControlCloneDir("testappid1");
    createSourceControlCloneDir("testappid2");
    assertThat(insightWork.getResolvedCloneDirectory().list()).hasSize(2);

    sourceControlFileStorageMigrator.migrate();

    assertThat(insightWork.getResolvedCloneDirectory().list()).isEmpty();
    assertThat(migrationTrackerDAO.getById(SourceControlFileStorageMigrator.MIGRATION_ID)).isNotNull();
  }

  private void createSourceControlCloneDir(String appId) throws IOException {
    File sourceControlCloneDir = insightWork.getSourceControlDir(appId);
    Files.createDirectories(sourceControlCloneDir.toPath());
    new File(sourceControlCloneDir, "foo.txt").createNewFile();
    assertThat(sourceControlCloneDir.list()).hasSize(1);
  }
}
