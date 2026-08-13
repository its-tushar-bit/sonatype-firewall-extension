/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link PolicyEvaluationDAOTest} (CLM-45228).
 */
@PostgresTest
public class PolicyEvaluationDAOPgTest
    extends AbstractDbDAOTest
{
  private PolicyEvaluationDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createPolicyEvaluationDAO();
  }

  @Test
  public void testGetLastByApplicationIdsAndStageIds_InOperatorOptimizationForPostgres() {
    testGetLastByApplicationIdsAndStageIds_InOperatorOptimization(false);
  }

  private void testGetLastByApplicationIdsAndStageIds_InOperatorOptimization(boolean isEmbeddedDb) {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());

    Date time1 = new Date();
    Application application2 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, "scanId1", time1);
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId2", time1);
    tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scanId3", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scanId4", time2);

    int inOperatorThreshold = isEmbeddedDb
        ? PolicyEvaluationDAO.H2_IN_OPERATOR_THRESHOLD
        : PolicyEvaluationDAO.POSTGRES_IN_OPERATOR_THRESHOLD;
    Set<String> appIds = new LinkedHashSet<>();
    while (appIds.size() < inOperatorThreshold) {
      appIds.add(TemporaryEntity.uuid());
    }
    appIds.add(application.getId());
    List<PolicyEvaluation> policyEvaluations = dao.getLastByOwnerIdsAndStageIds(appIds,
        Collections.singleton(BuildStageType.ID));
    assertThat(policyEvaluations).hasSize(1);
    PolicyEvaluation policyEvaluation = policyEvaluations.get(0);
    assertThat(policyEvaluation.getId()).isEqualTo(pe2.getId());
    assertThat(policyEvaluation.getTime()).isEqualTo(time2);
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
  }

  @Test
  public void testGetLastByApplicationIds_InOperatorOptimizationForPostgres() {
    testGetLastByApplicationIds_InOperatorOptimization(false);
  }

  private void testGetLastByApplicationIds_InOperatorOptimization(boolean isDatabaseEmbedded) {
    organization = tempEntity.newOrganization();
    application = tempEntity.newApplication(organization.getId());

    String stageTypeId = ReleaseStageType.ID;
    Date time1 = new Date();
    Application application2 = tempEntity.newApplication(organization.getId());
    tempEntity.newPolicyEvaluation(application2.getId(), stageTypeId, "scanId1", time1);
    tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId2", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    PolicyEvaluation pe2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, "scanId3", time2);

    Set<String> appIds = new LinkedHashSet<>();
    int threshold = isDatabaseEmbedded
        ? PolicyEvaluationDAO.H2_IN_OPERATOR_THRESHOLD
        : PolicyEvaluationDAO.POSTGRES_IN_OPERATOR_THRESHOLD;

    while (appIds.size() < threshold) {
      appIds.add(TemporaryEntity.uuid());
    }
    appIds.add(application.getId());
    List<PolicyEvaluation> policyEvaluations = dao.getLastByOwnerIds(appIds);
    assertThat(policyEvaluations).hasSize(1);
    PolicyEvaluation policyEvaluation = policyEvaluations.get(0);
    assertThat(policyEvaluation.getId()).isEqualTo(pe2.getId());
    assertThat(policyEvaluation.getTime()).isEqualTo(time2);
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
  }
}
