/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.violation;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationConstraintFactsDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFactsDAOProvider;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PolicyViolationConstraintFactsJsonAsyncDbMigrationTest
    extends AbstractComponentTest
{
  private PolicyViolationDAO policyViolationDAO;

  private PolicyViolationConstraintFactsDAO constraintsDAO;

  private MigrationTrackerDAO migrationTrackerDAO;

  private PolicyViolationConstraintFactsJsonAsyncDbMigration underTest;

  @Inject
  private ClusterLockManager clusterLockManager;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private InsightConfig insightConfig;

  @Before
  public void setup() {
    policyViolationDAO = daoFactory.createPolicyViolationDAO();
    constraintsDAO = daoFactory.createPolicyViolationConstraintFactsDAO();
    PolicyViolationConstraintFactsDAOProvider.inject(constraintsDAO);
    migrationTrackerDAO = daoFactory.createMigrationTrackerDAO();

    when(insightConfig.isDatabaseEmbedded()).thenReturn(true);

    underTest = new PolicyViolationConstraintFactsJsonAsyncDbMigration(taskScheduler, policyViolationDAO,
        migrationTrackerDAO, constraintsDAO, insightConfig, clusterLockManager);
  }

  @After
  @Override
  public void tearDown() {
    PolicyViolationConstraintFactsDAOProvider.inject(null);
  }

  @Test
  public void testSchedulesJob_onRegister() {
    underTest.register();

    verify(taskScheduler).scheduleOneTimeTask(underTest);
  }

  @Test
  public void testMigration_policyConstraintsJson() throws Exception {
    String constraintData = "[{constraint data}]";

    PolicyViolation policyViolation = createPolicyViolation(constraintData);

    // Ensure the migration tracker does not exist
    MigrationTracker migrationTracker = migrationTrackerDAO.getById(underTest.getJobName());
    migrationTrackerDAO.delete(migrationTracker);
    assertThat(migrationTracker).isNull();

    assertThat(policyViolationDAO.getById(policyViolation.getId()).getConstraintFactsJson()).isEqualTo(constraintData);

    underTest.execute(null);

    PolicyViolation updatedPolicyViolation = policyViolationDAO.getById(policyViolation.getId());
    assertThat(updatedPolicyViolation.getConstraintFactsId()).isEqualTo(Sha1Util.halfSha1(constraintData));
    assertThat(updatedPolicyViolation.getConstraintFactsJson()).isEqualTo(constraintData);

    MigrationTracker newMigrationTracker = migrationTrackerDAO.getById(underTest.getJobName());
    assertThat(newMigrationTracker).isNotNull();
  }

  @Test
  public void testMigration_doesNotRun_whenTrackerExists() throws Exception {
    String constraintData = "[{constraint data}]";

    PolicyViolation policyViolation = createPolicyViolation(constraintData);
    migrationTrackerDAO.insertTracker(underTest.getJobName());

    underTest.execute(null);

    PolicyViolation policyViolationMigrated = policyViolationDAO.getById(policyViolation.getId());
    assertThat(policyViolationMigrated.getConstraintFactsJsonWithoutLoading()).isNotBlank();
  }

  @Test
  public void testMigration_doesNotRun_whenClusterLocked() throws Exception {
    String constraintData = "[{constraint data}]";

    PolicyViolation policyViolation = createPolicyViolation(constraintData);

    try (ClusterLock clusterLock = clusterLockManager.createForAsyncDbMigration(underTest.getJobName())) {
      clusterLock.lock();
      underTest.execute(null);
    }
    finally {
      clusterLockManager.deleteForAsyncDbMigration(underTest.getJobName());
    }

    PolicyViolation policyViolationMigrated = policyViolationDAO.getById(policyViolation.getId());
    assertThat(policyViolationMigrated.getConstraintFactsJsonWithoutLoading()).isNotBlank();
  }

  private PolicyViolation createPolicyViolation(final String constraintData) {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID,
        "PolicyViolationDAOTestScanId");
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1",
        "Version1");
    PolicyViolation policyViolation = new PolicyViolation(policyEvaluation, policy.getId(), policy.getName(), 5,
        PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, constraintData, "filename");

    policyViolation.setConstraintFactsJson(constraintData);

    policyViolationDAO.insert(policyViolation);

    // Reset the ID column to match the pre-migration state
    try (TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();
      policyViolation.setConstraintFactsId(null);
      policyViolation.setConstraintFactsJson(constraintData);
      PolicyViolation entity = tx.merge(policyViolation);
      tx.persist(entity);
      tx.commit();
    }

    return policyViolation;
  }
}
