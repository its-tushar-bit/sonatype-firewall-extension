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
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.service.InsightConfig;

@Named
@Singleton
public class RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration
    extends AbstractPolicyViolationConstraintFactsJsonAsyncDbMigration<RepositoryPolicyViolation>
{
  @Inject
  public RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration(
      final RepositoryPolicyViolationDAO dao, final MigrationTrackerDAO migrationTrackerDAO, final InsightConfig config)
  {
    super(dao, migrationTrackerDAO, "repository policy violations", config);
  }
}
