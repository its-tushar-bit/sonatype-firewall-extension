/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationConstraintFactsDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFactsDAOProvider;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.utils.Sha1Util;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigrationTest
    extends AbstractComponentTest
{
  private RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private MigrationTrackerDAO migrationTrackerDAO;

  private RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration
      underTest;

  @Mock
  private InsightConfig insightConfig;

  @Before
  public void setup() {
    repositoryPolicyViolationDAO = daoFactory.createRepositoryPolicyViolationDAO();
    PolicyViolationConstraintFactsDAO constraintsDAO = daoFactory.createPolicyViolationConstraintFactsDAO();
    PolicyViolationConstraintFactsDAOProvider.inject(constraintsDAO);
    migrationTrackerDAO = daoFactory.createMigrationTrackerDAO();

    when(insightConfig.isDatabaseEmbedded()).thenReturn(true);

    underTest = new RepositoryPolicyViolationConstraintFactsJsonAsyncDbMigration(
        repositoryPolicyViolationDAO, migrationTrackerDAO, insightConfig);
  }

  @After
  @Override
  public void tearDown() {
    PolicyViolationConstraintFactsDAOProvider.inject(null);
  }

  @Test
  public void testMigration_policyConstraintsJson() throws Exception {
    String constraintData = "constraint data";

    RepositoryPolicyViolation policyViolation = createRepositoryPolicyViolation(constraintData);

    // Ensure the migration tracker does not exist
    MigrationTracker migrationTracker = migrationTrackerDAO.getById(underTest.getMigrationName());
    migrationTrackerDAO.delete(migrationTracker);
    assertThat(migrationTracker).isNull();

    assertThat(repositoryPolicyViolationDAO.getById(policyViolation.getId()).getConstraintFactsJson())
        .isEqualTo(constraintData);

    underTest.runMigration();

    RepositoryPolicyViolation updatedPolicyViolation = repositoryPolicyViolationDAO.getById(policyViolation.getId());
    assertThat(updatedPolicyViolation.getConstraintFactsId()).isNotNull();
    assertThat(updatedPolicyViolation.getConstraintFactsId()).isEqualTo(Sha1Util.halfSha1(constraintData));
    assertThat(updatedPolicyViolation.getConstraintFactsJson()).isNotNull();
    assertThat(updatedPolicyViolation.getConstraintFactsJson()).isEqualTo(constraintData);

    MigrationTracker newMigrationTracker = migrationTrackerDAO.getById(underTest.getMigrationName());
    assertThat(newMigrationTracker).isNotNull();
  }

  @Test
  public void testMigration_doesNotRun_whenTrackerExists() throws Exception {
    String constraintData = "constraint data";

    RepositoryPolicyViolation policyViolation = createRepositoryPolicyViolation(constraintData);
    migrationTrackerDAO.insertTracker(underTest.getMigrationName());

    underTest.runMigration();

    RepositoryPolicyViolation policyViolationMigrated = repositoryPolicyViolationDAO.getById(policyViolation.getId());
    assertThat(policyViolationMigrated.getConstraintFactsJsonWithoutLoading()).isNotBlank();
  }

  private RepositoryPolicyViolation createRepositoryPolicyViolation(final String constraintData) {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy(repository.getParentOwnerId());

    Date now = new Date();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    ConstraintFact constraintFact = new ConstraintFact("constraint data", "constraint data", "constraint data");
    RepositoryPolicyViolation policyViolation = new RepositoryPolicyViolation(repository.getId(), "path", now,
        policy.getId(), policy.getName(), 5, PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier,
        List.of(constraintFact));

    // This simulates restoring the data to the old format where the constraint data was stored in the JSON column
    policyViolation.setConstraintFactsJson(constraintData);

    repositoryPolicyViolationDAO.insert(policyViolation);

    // Restore the constraintFactsJson to simulate the pre-migration state.
    try (TransactionContext tx = repositoryPolicyViolationDAO.createTransactionContext()) {
      tx.begin();
      policyViolation.setConstraintFactsJson(constraintData);
      RepositoryPolicyViolation entity = tx.merge(policyViolation);
      tx.persist(entity);
      tx.commit();
    }

    return policyViolation;
  }
}
