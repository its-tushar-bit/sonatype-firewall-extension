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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.security.SystemRunnable;
import com.sonatype.insight.brain.service.InsightWork;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ScanFileCleaner
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(ScanFileCleaner.class);

  private final Path markerFile;

  private final InsightWork insightWork;

  private ScheduledExecutorService executor;

  public boolean disableForTesting;

  @Inject
  public ScanFileCleaner(InsightWork insightWork) {
    this.insightWork = insightWork;
    markerFile = insightWork.getWorkDir().toPath().resolve("obsoletescanfiles-cleaned");
  }

  ScheduledExecutorService newExecutor() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("ScanFileCleaner").setDaemon(true).build();
    return new ScheduledThreadPoolExecutor(1, threadFactory);
  }

  private DateTime determineExecutionTime() {
    // Schedule for 11 pm.
    DateTime dateTime =
        new DateTime().withHourOfDay(23).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
    // Set for tomorrow if this time has already passed today.
    if (dateTime.isBeforeNow()) {
      dateTime = dateTime.plusDays(1);
    }
    return dateTime;
  }

  @Override
  public void start() {
    if (disableForTesting) {
      return;
    }

    if (Files.exists(markerFile)) {
      log.info("Obsolete scan files already deleted.");
      return;
    }

    DateTime dateTime = determineExecutionTime();
    executor = newExecutor();
    long delay = dateTime.getMillis() - System.currentTimeMillis();
    executor.schedule(new SystemRunnable(() -> {
      try {
        ScanFileCleaner.this.deleteScanFiles();
      }
      catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }), delay, TimeUnit.MILLISECONDS);
    log.info("Scan file cleaner scheduled for {}.", dateTime);
  }

  @Override
  public void stop() {
    if (executor != null) {
      executor.shutdown();
      executor = null;
      log.info("Scan file cleaner stopped.");
    }
  }

  // Visible for tests
  void deleteScanFiles() throws IOException {
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

    Files.createFile(markerFile);

    log.info("Deleted {} obsolete scan files for {} applications in {} ms.", deletedFilesCount, apps.size(),
        System.currentTimeMillis() - start);
  }

  private boolean isOlderThanOneHour(Path file) throws IOException {
    FileTime lastModifiedTime = Files.getLastModifiedTime(file);
    return System.currentTimeMillis() - lastModifiedTime.toMillis() > DateTimeConstants.MILLIS_PER_HOUR;
  }

  Path getMarkerFile() {
    return markerFile;
  }
}
