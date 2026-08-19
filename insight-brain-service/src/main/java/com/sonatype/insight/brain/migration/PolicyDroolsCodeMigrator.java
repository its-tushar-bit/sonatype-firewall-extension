/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.policy.DroolsGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates Drools code for all policies.
 *
 * @since 1.12
 */
@Named
public class PolicyDroolsCodeMigrator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyDroolsCodeMigrator.class);

  private final MigrationTrackerDAO migrationTrackerDAO;

  static final String MIGRATION_ID = "policy-drools-code";

  // v2 since 1.16
  // v3 since 1.32
  // v4 since 1.50
  // v5 since 1.95
  static final int DROOLS_CODE_VERSION = 5;

  private final LabelDAO labelDAO;

  private final PolicyDAO policyDAO;

  @Inject
  public PolicyDroolsCodeMigrator(
      final MigrationTrackerDAO migrationTrackerDAO,
      final LabelDAO labelDAO,
      final PolicyDAO policyDAO)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.labelDAO = labelDAO;
    this.policyDAO = policyDAO;
  }

  void migrate() {
    MigrationTracker migrationTracker = migrationTrackerDAO.getById(MIGRATION_ID);
    int droolsCodeVersion = migrationTracker.getVersion();
    if (droolsCodeVersion < 2) {
      throw new IllegalStateException("Policy code is still at version " + droolsCodeVersion);
    }

    if (droolsCodeVersion >= DROOLS_CODE_VERSION) {
      log.debug("Policy code already generated.");
      return;
    }

    long start = System.currentTimeMillis();
    log.debug("Generating policy code...");

    List<Policy> policies = policyDAO.getAll();
    log.info("Found {} policies.", policies.size());
    for (Policy policy : policies) {
      DroolsGenerator.generate(policy, labelDAO);
      // NOTE: Due to CLM-8176, we skip validation and focus on just updating the Drools code
      policyDAO.update(policy, false);
    }

    migrationTracker.setVersion(DROOLS_CODE_VERSION);
    migrationTrackerDAO.update(migrationTracker);

    log.info("Generated policy code for {} policies in {} ms.", policies.size(), System.currentTimeMillis() - start);
  }
}
