/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.122
 */
@Named
public class InternalSourceControlPolicyEvaluationsConfigMigrator
{
  private static final Logger log = LoggerFactory.getLogger(InternalSourceControlPolicyEvaluationsConfigMigrator.class);

  static final String MIGRATION_ID = "internal-source-control-policy-evaluations-config";

  private final InsightConfig insightConfig;

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final SourceControlDAO sourceControlDAO;

  @Inject
  public InternalSourceControlPolicyEvaluationsConfigMigrator(
      InsightConfig insightConfig,
      MigrationTrackerDAO migrationTrackerDAO,
      SourceControlDAO sourceControlDAO)
  {
    this.insightConfig = insightConfig;
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.sourceControlDAO = sourceControlDAO;
  }

  public void migrate() {
    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Internal source control policy evaluations configuration already migrated.");
      return;
    }

    SourceControl rootOrgSourceControl = sourceControlDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    if (null == rootOrgSourceControl) {
      migrationTrackerDAO.insert(new MigrationTracker(MIGRATION_ID));
      return;
    }

    try (TransactionContext txn = sourceControlDAO.createTransactionContext()) {
      txn.begin();

      @SuppressWarnings("deprecation")
      boolean internalSourceControlPolicyEvaluationsEnabled =
          insightConfig.isFeatureEnabled(Feature.INTERNAL_SOURCE_CONTROL_POLICY_EVALUATIONS);
      rootOrgSourceControl.setSourceControlScansEnabled(internalSourceControlPolicyEvaluationsEnabled);
      sourceControlDAO.update(txn, rootOrgSourceControl);
      migrationTrackerDAO.insert(txn, new MigrationTracker(MIGRATION_ID));

      txn.commit();

      log.debug("Internal source control policy evaluations configuration migrated from config.yml "
          + "to the root org source control configuration.");
    }
  }
}
