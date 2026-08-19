/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates the policy conditions for component coordinates to include 'maven' as component coordinates format.
 *
 * @since 1.22
 */
@Named
public class PolicyCoordinatesConditionTypeMigrator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyCoordinatesConditionTypeMigrator.class);

  public static final String MIGRATION_ID = "policy-coordinates-condition-type";

  private final PolicyDAO policyDAO;

  private final MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  public PolicyCoordinatesConditionTypeMigrator(
      PolicyDAO policyDAO,
      MigrationTrackerDAO migrationTrackerDAO)
  {
    this.policyDAO = policyDAO;
    this.migrationTrackerDAO = migrationTrackerDAO;
  }

  public void migrate() {
    long start = System.currentTimeMillis();

    log.debug("Migrating policy conditions for maven component coordinates...");

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.info("Policy conditions for maven coordinates already migrated.");
      return;
    }

    int numPoliciesMigrated = 0;
    try (TransactionContext tx = policyDAO.createTransactionContext()) {
      tx.begin();
      List<Policy> policies = policyDAO.getAll(tx);
      for (Policy policy : policies) {
        if (migrate(policy)) {
          numPoliciesMigrated++;
          policyDAO.update(tx, policy);
        }
      }
      migrationTrackerDAO.insertTracker(tx, MIGRATION_ID);
      tx.commit();
    }

    log.info("Migrated policy conditions for maven coordinates for {} policies in {} ms.", numPoliciesMigrated,
        System.currentTimeMillis() - start);
  }

  private boolean migrate(Policy policy) {
    boolean migrated = false;
    if (policy.getConstraints() != null) {
      for (Constraint constraint : policy.getConstraints()) {
        for (Condition condition : constraint.getConditions()) {
          if (condition.getConditionTypeId().equals(CoordinatesConditionType.ID)) {
            condition.setValue(ComponentIdentifier.FORMAT_MAVEN + ":" + condition.getValue());
            migrated = true;
          }
        }
      }
    }
    return migrated;
  }
}
