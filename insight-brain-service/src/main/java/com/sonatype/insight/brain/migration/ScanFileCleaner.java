/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.service.InsightWork;

import org.joda.time.DateTimeConstants;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class ScanFileCleaner
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(ScanFileCleaner.class);

  // Visible for testing
  public static final String NAME = "ScanFileCleaner";

  // Visible for testing
  static final String MARKER_ID = "obsoletescanfiles-cleaned";

  private static final String SCAN_FILE_CLEANER_ERROR = "Scan file cleaner error";

  private final InsightWork insightWork;

  private final TaskScheduler taskScheduler;

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationDAO applicationDAO;

  public boolean disableForTesting;

  private ScanPersistenceService scanPersistenceService;

  @Inject
  public ScanFileCleaner(
      InsightWork insightWork,
      TaskScheduler taskScheduler,
      MigrationTrackerDAO migrationTrackerDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      ApplicationDAO applicationDAO,
      ScanPersistenceService scanPersistenceService)
  {
    this.insightWork = insightWork;
    this.taskScheduler = taskScheduler;
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationDAO = applicationDAO;
    this.scanPersistenceService = scanPersistenceService;
  }

  @Override
  public void register() {
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

    taskScheduler.scheduleOneTimeTask(this, LocalTime.of(23, 0));
  }

  @Override
  public void deregister() {
    // noop
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::deleteScanFiles, log, SCAN_FILE_CLEANER_ERROR);
  }

  // Visible for tests
  void deleteScanFiles() {
    long start = System.currentTimeMillis();

    log.debug("Deleting obsolete scan files...");

    final AtomicInteger deletedFilesCount = new AtomicInteger(0);
    List<Application> apps = applicationDAO.getAll();
    for (Application app : apps) {
      String appId = app.getId();

      log.info("Deleting obsolete scan files for application '{}' with ID {}.", app.getName(), appId);

      Set<ScanEntity> lastPolicyEvaluationScanFiles =
          policyEvaluationDAO.getLastByApplicationIds(Collections.singleton(appId)).stream() //
              .map(PolicyEvaluation::getScanId) //
              .map(scanId -> scanPersistenceService.getScan(appId, scanId)) //
              .collect(Collectors.toSet());

      try (Stream<ScanEntity> scanEntityStream = scanPersistenceService.allScanFilesFor(appId)) {
        scanEntityStream.forEach(scanEntity -> {

          // Don't delete files that are younger than one hour in order to not interfere with concurrent policy
          // evaluations.
          try {
            if (!isOlderThanOneHour(scanEntity)) {
              return;
            }
          }
          catch (Exception e) {
            log.warn("Error accessing the last modified timestamp for scan file '{}': {}", scanEntity, e.toString());
            return;
          }

          // Don't delete scan files that are for the last policy evaluation for a stage.
          if (lastPolicyEvaluationScanFiles.contains(scanEntity)) {
            return;
          }

          // Delete the scan file.
          try {
            scanPersistenceService.deleteScan(scanEntity);
            deletedFilesCount.getAndIncrement();
            log.info("Deleted obsolete scan file: '{}'.", scanEntity);
          }
          catch (Exception e) {
            log.warn("Error deleting scan file '{}': {}", scanEntity, e.toString());
          }
        });
      }
    }

    if (!migrationTrackerDAO.isTrackerPresent(MARKER_ID)) {
      migrationTrackerDAO.insertTracker(MARKER_ID);
    }

    log.info("Deleted {} obsolete scan files for {} applications in {} ms.", deletedFilesCount, apps.size(),
        System.currentTimeMillis() - start);
  }

  private boolean isOlderThanOneHour(ScanEntity scanEntity) throws IOException {
    long lastModifiedTime = scanEntity.getLastModifiedTime();
    return System.currentTimeMillis() - lastModifiedTime > DateTimeConstants.MILLIS_PER_HOUR;
  }

  // Visible for tests
  Path getObsoleteMarkerFile() {
    return insightWork.getWorkDir().toPath().resolve("obsoletescanfiles-cleaned");
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
