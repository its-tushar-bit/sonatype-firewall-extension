/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;

import io.dropwizard.servlets.tasks.Task;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is intended to delete all data associated with ThirdPartySbomMetadata entities (sbom_metadata table rows)
 * which have had a PENDING status for at least 24 hours indicating they have failed.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class PendingSbomMetadataCleaner
    extends Task
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(PendingSbomMetadataCleaner.class);

  // Visible for testing
  static final String JOB_NAME = "PendingSbomMetadataCleanerJob";

  private static final String TASK_NAME = "PendingSbomMetadataCleanerTask";

  private static final Duration MAXIMUM_ALLOWED_HOURS_IN_PENDING_STATE = Duration.ofHours(24);

  private static final String DESCRIPTION = "Delete for all data associated with ThirdPartySbomMetadata entities " +
      "(sbom_metadata table rows) which have had a PENDING status for at least " +
      MAXIMUM_ALLOWED_HOURS_IN_PENDING_STATE + " hours.";

  // Visible for testing
  static final int EXECUTION_HOUR = 1;

  private final TaskScheduler taskScheduler;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final InsightWork insightWork;

  public boolean disableForTesting;

  @Inject
  public PendingSbomMetadataCleaner(
      TaskScheduler taskScheduler,
      ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      InsightWork insightWork,
      ThirdPartyFileDAO thirdPartyFileDAO)
  {
    super(TASK_NAME);
    this.taskScheduler = taskScheduler;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.insightWork = insightWork;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }

    taskScheduler.scheduleDailyTask(this, getTenantExecutionTime());
    Date nextExecutionTime = taskScheduler.getNextExecutionTime(this);
    log.info("Scheduled periodic {} for {}.", JOB_NAME, nextExecutionTime);
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) {
    pendingSbomMetadataCleaner();
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::pendingSbomMetadataCleaner, log, String.format("Error in %s", JOB_NAME));
  }

  private void pendingSbomMetadataCleaner() {
    log.info("Starting {} - {}", JOB_NAME, DESCRIPTION);
    long startTime = System.currentTimeMillis();

    thirdPartySbomMetadataDAO.getPendingSbomsOlderThanDuration(MAXIMUM_ALLOWED_HOURS_IN_PENDING_STATE)
        .forEach(this::deletePendingSbomMetadata);

    long duration = System.currentTimeMillis() - startTime;
    log.debug("Finished {} after {} ms.", JOB_NAME, duration);
  }

  private void deletePendingSbomMetadata(final ThirdPartySbomMetadata sbomMetadata) {
    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      thirdPartyFileDAO.delete(tx, sbomMetadata.getThirdPartyFileId());
      deleteFileIfExists(sbomMetadata);
      tx.commit();
    }
    catch (IOException e) {
      log.error("Error deleting sbom file {}, sbom cleanup will be retried later.", sbomMetadata.getFilename(), e);
    }
  }

  private void deleteFileIfExists(final ThirdPartySbomMetadata sbomMetadata) throws IOException {
    Path sbomPath =
        new File(insightWork.getSbomDir(sbomMetadata.getApplicationId()), sbomMetadata.getFilename()).toPath();
    try {
      Files.delete(sbomPath);
    }
    catch (NoSuchFileException e) {
      log.warn("Sbom file {} for app {} didn't exist when trying to delete it.", sbomMetadata.getFilename(),
          sbomMetadata.getApplicationId());
    }
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }

  private LocalTime getTenantExecutionTime() {
    Random random = new Random();
    int randomMinute = random.nextInt(60); // 0 to 59

    // Adding variable minute to avoid triggering the job at the exact same time for all tenants
    return LocalTime.of(EXECUTION_HOUR, randomMinute);
  }
}
