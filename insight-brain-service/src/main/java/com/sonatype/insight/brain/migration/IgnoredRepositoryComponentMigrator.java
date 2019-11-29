/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.repository.RepositoryComponentDeleteService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Motivation and related discussion: CLM-14021
/**
 * This migrator deletes all repository components (together with related policy violations, policy violation waivers
 * and labels) that should have been ignored either by Repository Manager/Insight Brain Server.
 *
 * @since 1.79
 */
public class IgnoredRepositoryComponentMigrator
{
  private static final Logger log = LoggerFactory.getLogger(IgnoredRepositoryComponentMigrator.class);

  static final String MIGRATION_ID = "ignored-repository-components";

  private final RepositoryComponentDeleteService repositoryComponentDeleteService;

  private final RepositoryDAO repositoryDAO;

  private final MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  public IgnoredRepositoryComponentMigrator(
      RepositoryComponentDeleteService repositoryComponentDeleteService,
      RepositoryDAO repositoryDAO,
      MigrationTrackerDAO migrationTrackerDAO)
  {
    this.repositoryComponentDeleteService = repositoryComponentDeleteService;
    this.repositoryDAO = repositoryDAO;
    this.migrationTrackerDAO = migrationTrackerDAO;
  }

  public void migrate() {
    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Ignored repository components already deleted.");
      return;
    }

    long start = System.currentTimeMillis();
    log.debug("Deleting ignored repository components...");

    List<Repository> repositories = repositoryDAO.getAll();
    repositories.forEach(repositoryComponentDeleteService::deleteUnknownIgnoredComponents);
    migrationTrackerDAO.insert(new MigrationTracker(MIGRATION_ID));
    log.info("Deleted ignored repository components for {} repositories in {} ms.", repositories.size(),
        System.currentTimeMillis() - start);
  }
}
