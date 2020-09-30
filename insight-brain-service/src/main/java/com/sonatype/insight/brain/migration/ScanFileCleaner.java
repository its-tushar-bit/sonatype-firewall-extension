/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightWork;

import io.dropwizard.lifecycle.Managed;
import org.joda.time.DateTimeConstants;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ScanFileCleaner
    implements Managed, Job
{
  private static final Logger log = LoggerFactory.getLogger(ScanFileCleaner.class);

  // Visible for testing
  static final String NAME = "ScanFileCleaner";

  // Visible for testing
  static final String MARKER_ID = "obsoletescanfiles-cleaned";

  private final InsightWork insightWork;

  private final TaskScheduler taskScheduler;

  private final MigrationTrackerDAO migrationTrackerDAO;

  public boolean disableForTesting;

  @Inject
  public ScanFileCleaner(
      InsightWork insightWork,
      TaskScheduler taskScheduler,
      MigrationTrackerDAO migrationTrackerDAO)
  {
    this.insightWork = insightWork;
    this.taskScheduler = taskScheduler;
    this.migrationTrackerDAO = migrationTrackerDAO;
  }

  @Override
  public void start() {
    if (disableForTesting) {
      return;
    }

    if (migrationTrackerDAO.isTrackerPresent(MARKER_ID)) {
      log.info("Obsolete scan files already deleted.");
      return;
    }
    Path markerFile = getObsoleteMarkerFile();
    if (Files.exists(markerFile)) {
      migrationTrackerDAO.insertTracker(MARKER_ID);
      log.info("Obsolete scan files already deleted.");
      return;
    }

    taskScheduler.scheduleOneTimeTask(ScanFileCleaner.class, NAME, LocalTime.of(23, 0));
    log.info("Scan file cleaner scheduled for {}.", taskScheduler.getNextExecutionTime(NAME));
  }

  @Override
  public void stop() {
    // noop
  }

  @Override
  public void execute(JobExecutionContext context) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      deleteScanFiles();
    }
    catch (Exception e) {
      log.error("Scan file cleaner error: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  // Visible for tests
  void deleteScanFiles() {
    long start = System.currentTimeMillis();

    log.debug("Deleting obsolete scan files...");

    int deletedFilesCount = 0;
    PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
    List<Application> apps = new ApplicationDAO().getAll();
    for (Application app : apps) {
      String appId = app.getId();

      File scanDir = insightWork.getScanDir(appId);
      if (!scanDir.isDirectory()) {
        log.info("There is no scan directory for application '{}' with ID {}.", app.getName(), appId);
        continue;
      }

      // Don't use Files.list() as that may result in a lot of file handlers being used and not released.
      // See https://stackoverflow.com/questions/36990053/
      // resource-leak-in-files-listpath-dir-when-stream-is-not-explicitly-closed
      File[] scanFiles = scanDir.listFiles();
      if (scanFiles.length == 0) {
        log.info("There are no scan files for application '{}' with ID {}.", app.getName(), appId);
        continue;
      }

      log.info("Deleting obsolete scan files for application '{}' with ID {}.", app.getName(), appId);

      Set<Path> lastPolicyEvaluationScanFiles =
          policyEvaluationDAO.getLastByApplicationIds(Collections.singleton(appId)).stream() //
              .map(PolicyEvaluation::getScanId) //
              .map(scanId -> insightWork.getScanFile(appId, scanId).toPath().toAbsolutePath()) //
              .collect(Collectors.toSet());

      for (File scanFile : scanFiles) {
        Path scanFilePath = scanFile.toPath().toAbsolutePath();

        // Don't delete files that are yonger than one hour in order to not interfere with concurrent policy
        // evaluations.
        try {
          if (!isOlderThanOneHour(scanFilePath)) {
            continue;
          }
        }
        catch (Exception e) {
          log.warn("Error accessing the last modified timestamp for scan file '{}': {}", scanFilePath, e.toString());
          continue;
        }

        // Don't delete scan files that are for the last policy evaluation for a stage.
        if (lastPolicyEvaluationScanFiles.contains(scanFilePath)) {
          continue;
        }

        // Delete the scan file.
        try {
          Files.delete(scanFilePath);
          deletedFilesCount++;
          log.info("Deleted obsolete scan file: '{}'.", scanFilePath);
        }
        catch (Exception e) {
          log.warn("Error deleting scan file '{}': {}", scanFilePath, e.toString());
        }
      }
    }

    migrationTrackerDAO.insertTracker(MARKER_ID);

    log.info("Deleted {} obsolete scan files for {} applications in {} ms.", deletedFilesCount, apps.size(),
        System.currentTimeMillis() - start);
  }

  private boolean isOlderThanOneHour(Path file) throws IOException {
    FileTime lastModifiedTime = Files.getLastModifiedTime(file);
    return System.currentTimeMillis() - lastModifiedTime.toMillis() > DateTimeConstants.MILLIS_PER_HOUR;
  }

  // Visible for tests
  Path getObsoleteMarkerFile() {
    return insightWork.getWorkDir().toPath().resolve("obsoletescanfiles-cleaned");
  }
}
