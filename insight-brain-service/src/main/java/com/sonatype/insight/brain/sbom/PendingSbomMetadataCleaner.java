/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;

import io.dropwizard.servlets.tasks.Task;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is intended to delete all data associated with ThirdPartySbomMetadata entities (sbom_metadata table rows)
 * which have not reached the ACTIVE state after existing for at least 24 hours, indicating they have failed or were
 * abandoned by the user.
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

  private static final Duration MAXIMUM_ALLOWED_HOURS_IN_INACTIVE_STATE = Duration.ofHours(24);

  private static final String DESCRIPTION = "Delete all data associated with ThirdPartySbomMetadata entities " +
      "(sbom_metadata table rows) which have been inactive for at least " +
      MAXIMUM_ALLOWED_HOURS_IN_INACTIVE_STATE + " hours.";

  // Visible for testing
  static final int EXECUTION_HOUR = 1;

  private final TaskScheduler taskScheduler;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyPersistenceService thirdPartyPersistenceService;

  public boolean disableForTesting;

  @Inject
  public PendingSbomMetadataCleaner(
      TaskScheduler taskScheduler,
      ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      ThirdPartyPersistenceService thirdPartyPersistenceService)
  {
    super(TASK_NAME);
    this.taskScheduler = taskScheduler;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyPersistenceService = thirdPartyPersistenceService;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }

    taskScheduler.scheduleDailyTask(this, getTenantExecutionTime());
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) {
    execute();
  }

  @Override
  public void execute(final JobExecutionContext context) {
    execute(this::execute, log, String.format("Error in %s", JOB_NAME));
  }

  private void execute() {
    log.info("Starting {} - {}", JOB_NAME, DESCRIPTION);
    long startTime = System.currentTimeMillis();

    Date cutoffDate = new Date(Instant.now().toEpochMilli() - MAXIMUM_ALLOWED_HOURS_IN_INACTIVE_STATE.toMillis());
    List<ThirdPartySbomMetadata> toDelete = thirdPartySbomMetadataDAO.getInactiveSbomsBeforeOrAt(cutoffDate);
    for (ThirdPartySbomMetadata sbomMetadata : toDelete) {
      try {
        thirdPartyPersistenceService.deleteSbomMetadataAndAssociatedFiles(sbomMetadata);
      }
      catch (Exception e) {
        log.error("Failed to delete SBOM metadata with ID '{}' all known associated data.", sbomMetadata.getId(), e);
      }
    }
    thirdPartyPersistenceService.tryDeleteSbomTemporaryTransientFilesOlderThan(cutoffDate);

    long duration = System.currentTimeMillis() - startTime;
    log.debug("Finished {} after {} ms.", JOB_NAME, duration);
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
