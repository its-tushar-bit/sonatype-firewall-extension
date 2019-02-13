/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.service.InsightWork;
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

  static final String MARKER_FILE_NAME = "policycoordinatesconditiontype-migrated";

  private final InsightWork insightWork;

  private final PolicyDAO policyDAO;

  @Inject
  public PolicyCoordinatesConditionTypeMigrator(InsightWork insightWork, PolicyDAO policyDAO) {
    this.insightWork = insightWork;
    this.policyDAO = policyDAO;
  }

  public void migrate() throws IOException {
    long start = System.currentTimeMillis();

    log.debug("Migrating policy conditions for maven component coordinates...");

    File markerFile = new File(insightWork.getWorkDir(), MARKER_FILE_NAME);
    if (markerFile.exists()) {
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
      tx.commit();
    }
    markerFile.createNewFile();

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
