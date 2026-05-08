/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.Sha1Util;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigrationTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private OperationalDataStore operationalDataStore;

  @Inject
  private RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration underTest;

  @Test
  public void testMigration() throws Exception {
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);
    String constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    RepositoryPolicyViolation policyViolation = createPolicyViolation(constraintFacts);

    // Ensure the migration tracker does not exist
    MigrationTracker migrationTracker = migrationTrackerDAO.getById(underTest.getMigrationName());
    if (migrationTracker != null) {
      migrationTrackerDAO.delete(migrationTracker);
    }
    assertThat(migrationTrackerDAO.getById(underTest.getMigrationName())).isNull();

    underTest.runMigration();

    RepositoryPolicyViolation migratedPolicyViolation = repositoryPolicyViolationDAO.getById(policyViolation.getId());
    assertThat(migratedPolicyViolation.getConstraintFactsId()).isEqualTo(Sha1Util.halfSha1(constraintFactsJson));
    assertThat(migratedPolicyViolation.getDeprecatedConstraintFactsJson()).isNull();
    repositoryPolicyViolationDAO.loadConstraintFacts(Collections.singletonList(migratedPolicyViolation));
    assertThat(migratedPolicyViolation.getConstraintFactsJson()).isEqualTo(constraintFactsJson);

    MigrationTracker newMigrationTracker = migrationTrackerDAO.getById(underTest.getMigrationName());
    assertThat(newMigrationTracker).isNotNull();
  }

  @Test
  public void testMigration_doesNotRun_whenTrackerExists() throws Exception {
    List<ConstraintFact> constraintFacts = createConstraintFacts(1);
    RepositoryPolicyViolation policyViolation = createPolicyViolation(constraintFacts);
    if (!migrationTrackerDAO.isTrackerPresent(underTest.getMigrationName())) {
      migrationTrackerDAO.insertTracker(underTest.getMigrationName());
    }

    underTest.runMigration();

    RepositoryPolicyViolation policyViolationMigrated = repositoryPolicyViolationDAO.getById(policyViolation.getId());
    assertThat(policyViolationMigrated.getDeprecatedConstraintFactsJson()).isNotBlank();
  }

  private List<ConstraintFact> createConstraintFacts(int count) {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ConstraintFact constraintFact = new ConstraintFact(UUID.randomUUID().toString(), "constraintName " + i, "and");
      ConditionFact conditionFact = new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID,
          0 /* conditionIndex */, "some summary", "some reason");
      conditionFact.setTriggerJson("some trigger");
      constraintFact.addConditionFact(conditionFact);
      constraintFacts.add(constraintFact);
    }
    return constraintFacts;
  }

  private RepositoryPolicyViolation createPolicyViolation(
      final List<ConstraintFact> constraintFacts) throws SQLException
  {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    Date now = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "Version1", "", "jar");

    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repository.getId(), "path", now,
        policy.getId(), policy.getName(), 5, PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier,
        constraintFacts);

    repositoryPolicyViolationDAO.insert(policyViolation);

    // Sanity check
    assertThat(policyViolation.getConstraintFactsJson()).isNotBlank();

    // Restore the pre-migration state.
    String updateQuery = "UPDATE " + operationalDataStore.getDatabaseSchema() + ".repository_policy_violation"
        + " SET constraint_facts_id = NULL, constraint_facts_json = ? WHERE repository_policy_violation_id = ?";
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        PreparedStatement updateStmt = connection.prepareStatement(updateQuery))
    {
      updateStmt.setString(1, policyViolation.getConstraintFactsJson());
      updateStmt.setString(2, policyViolation.getId());
      updateStmt.execute();
    }

    // Sanity check
    RepositoryPolicyViolation persistedPolicyViolation = repositoryPolicyViolationDAO.getById(policyViolation.getId());
    assertThat(persistedPolicyViolation.getDeprecatedConstraintFactsJson())
        .isEqualTo(policyViolation.getConstraintFactsJson());
    assertThat(persistedPolicyViolation.getConstraintFactsId()).isNull();

    return policyViolation;
  }
}
