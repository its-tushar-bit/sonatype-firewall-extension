/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Motivation and related discussion: CLM-14021

/**
 * Deletes all repository components (together with related policy violations, policy violation waivers and labels) that
 * should have been ignored either by Repository Manager/Insight Brain Server.
 *
 * @since 1.80
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class IgnoredRepositoryComponentCleaner
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(IgnoredRepositoryComponentCleaner.class);

  static final String MIGRATION_ID = "ignored-repository-components";

  // Visible for testing
  static final String TASK_NAME = "IgnoredRepositoryComponentCleaner";

  private final RepositoryComponentDeleteService repositoryComponentDeleteService;

  private final RepositoryDAO repositoryDAO;

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final TaskScheduler taskScheduler;

  @Inject
  public IgnoredRepositoryComponentCleaner(
      RepositoryComponentDeleteService repositoryComponentDeleteService,
      RepositoryDAO repositoryDAO,
      MigrationTrackerDAO migrationTrackerDAO,
      TaskScheduler taskScheduler)
  {
    this.repositoryComponentDeleteService = repositoryComponentDeleteService;
    this.repositoryDAO = repositoryDAO;
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void register() {
    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Ignored repository components already deleted.");
      return;
    }
    taskScheduler.scheduleOneTimeTask(this);
  }

  @Override
  public void deregister() {
    // noop
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    execute(this::doDeleteIgnoredRepositoryComponents, log,
        "Failed to delete ignored repository components, will retry upon next server start");
  }

  // Visible for testing
  void doDeleteIgnoredRepositoryComponents() {
    long start = System.currentTimeMillis();
    log.debug("Deleting ignored repository components...");
    List<Repository> repositories = repositoryDAO.getAll();
    repositories.forEach(repositoryComponentDeleteService::deleteUnknownIgnoredComponents);
    migrationTrackerDAO.insert(new MigrationTracker(MIGRATION_ID));
    log.info("Deleted ignored repository components for {} repositories in {} ms.", repositories.size(),
        System.currentTimeMillis() - start);
  }
  
  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
