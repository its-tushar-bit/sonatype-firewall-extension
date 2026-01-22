/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.service.InsightConfig;

@Named
@Singleton
public class PolicyViolationConstraintFactsJsonAsyncDbMigration
    extends AbstractPolicyViolationConstraintFactsJsonAsyncDbMigration<PolicyViolation>
{
  @Inject
  public PolicyViolationConstraintFactsJsonAsyncDbMigration(
      final PolicyViolationDAO dao, final MigrationTrackerDAO migrationTrackerDAO, final InsightConfig insightConfig)
  {
    super(dao, migrationTrackerDAO, "policy violations", insightConfig);
  }
}
