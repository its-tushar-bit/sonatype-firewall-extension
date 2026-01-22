/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.122
 */
@Named
public class PullRequestCommentingConfigMigrator
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestCommentingConfigMigrator.class);

  static final String MIGRATION_ID = "pull-request-commenting-config";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final SourceControlDAO sourceControlDAO;

  @Inject
  public PullRequestCommentingConfigMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      SourceControlDAO sourceControlDAO)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.sourceControlDAO = sourceControlDAO;
  }

  public void migrate() {
    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Pull request commenting configuration already migrated.");
      return;
    }

    SourceControl rootOrgSourceControl = sourceControlDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    if (null == rootOrgSourceControl) {
      migrationTrackerDAO.insert(new MigrationTracker(MIGRATION_ID));
      return;
    }

    try (TransactionContext txn = sourceControlDAO.createTransactionContext()) {
      txn.begin();

      boolean isPullRequestCommentingEnabled = SystemConfigurationPropertyFeature.PR_COMMENTING.isEnabled();
      rootOrgSourceControl.setPullRequestCommentingEnabled(isPullRequestCommentingEnabled);
      sourceControlDAO.update(txn, rootOrgSourceControl);
      migrationTrackerDAO.insert(txn, new MigrationTracker(MIGRATION_ID));

      txn.commit();

      log.debug("Pull request commenting config migrated from config.yml to the root org source control config");
    }
  }
}
