/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fills in the proxy_repository_component.display_name db column.
 *
 * @since 1.152
 */
@Named
public class RepositoryComponentDisplayNameMigrator
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryComponentDisplayNameMigrator.class);

  static final String MIGRATION_ID = "repository-component-display-name";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final RepositoryDAO repositoryDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  @Inject
  public RepositoryComponentDisplayNameMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      RepositoryDAO repositoryDAO,
      ProxyRepositoryComponentDAO proxyRepositoryComponentDAO)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.repositoryDAO = repositoryDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
  }

  public void migrate() {
    long start = System.currentTimeMillis();

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Repository component display names already migrated.");
      return;
    }

    log.info("Migrating repository component display names...");

    int componentCount = 0;
    for (Repository repository : repositoryDAO.getAll()) {
      for (ProxyRepositoryComponent proxyRepositoryComponent : proxyRepositoryComponentDAO
          .getByRepositoryIdAndDisplayName(repository.getId(), null))
      {
        proxyRepositoryComponentDAO.update(proxyRepositoryComponent);
        componentCount++;
      }
    }
    migrationTrackerDAO.insert(new MigrationTracker(MIGRATION_ID));

    log.info("Migrated repository component display names for {} components in {} ms.", componentCount,
        System.currentTimeMillis() - start);
  }
}
