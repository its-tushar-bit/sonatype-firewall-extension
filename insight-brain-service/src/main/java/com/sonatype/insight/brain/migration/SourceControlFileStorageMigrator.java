/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.service.InsightWork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deletes all the git repositories cloned locally. The system will clone repositories again in the new location when
 * needed.
 *
 * @since 1.104
 */
@Named
public class SourceControlFileStorageMigrator
{
  private static final Logger log = LoggerFactory.getLogger(SourceControlFileStorageMigrator.class);

  static final String MIGRATION_ID = "source-control-file-storage";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final InsightWork insightWork;

  private final FileCleaner fileCleaner;

  @Inject
  public SourceControlFileStorageMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      InsightWork insightWork,
      FileCleaner fileCleaner)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.insightWork = insightWork;
    this.fileCleaner = fileCleaner;
  }

  public void migrate() {
    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.info("Source control file storage already migrated.");
      return;
    }

    long start = System.currentTimeMillis();
    log.debug("Deleting source control file storage...");

    File cloneDirectory = insightWork.getResolvedCloneDirectory();
    List<File> appCloneDirs = getAppCloneDirs(cloneDirectory);
    for (File appCloneDir : appCloneDirs) {
      log.debug("Deleting source control file storage directory: '{}'.", appCloneDir.getAbsolutePath());
      try {
        fileCleaner.delete(appCloneDir);
      }
      catch (Exception e) {
        log.error("Failed to delete source control file storage directory: '{}'. It should be manually deleted.",
            appCloneDir.getAbsolutePath(), e);
      }
    }

    migrationTrackerDAO.insertTracker(MIGRATION_ID);

    log.info("Deleted {} source control file storage directories in {} ms.", appCloneDirs.size(),
        System.currentTimeMillis() - start);
  }

  private List<File> getAppCloneDirs(File cloneDirectory) {
    List<File> dirs = new ArrayList<>();
    if (!cloneDirectory.isDirectory()) {
      return dirs;
    }

    for (File file : cloneDirectory.listFiles()) {
      if (file.isDirectory()) {
        dirs.add(file);
      }
    }
    return dirs;
  }
}
