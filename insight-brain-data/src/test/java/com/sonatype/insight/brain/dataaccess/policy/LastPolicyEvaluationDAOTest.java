/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.LastPolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LastPolicyEvaluationDAOTest
    extends AbstractDbDAOTest
{
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private SearchIndexChangeDAO searchIndexChangeDAO;

  private LastPolicyEvaluationDAO dao;

  private PolicyEvaluationDAO peDao;

  @Before
  @Override
  public void setup() {
    super.setup();
    systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    searchIndexChangeDAO = daoFactory.createSearchIndexChangeDAO();
    dao = daoFactory.createLastPolicyEvaluationDAO();
    peDao = daoFactory.createPolicyEvaluationDAO();
  }

  @Test
  public void testCRUD() {

    final String stageTypeId = ReleaseStageType.ID;
    final String scanId = "LastPolicyEvaluationDAOTest";

    // Create (as part of a policy eval)
    final PolicyEvaluation eval = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId);

    // Read
    final LastPolicyEvaluation policyEvaluation = dao.getById(eval.getId());
    assertThat(policyEvaluation.getId()).isEqualTo(eval.getId());
    assertThat(policyEvaluation.getApplicationId()).isEqualTo(application.getId());
    assertThat(policyEvaluation.getStageTypeId()).isEqualTo(stageTypeId);

    // Update is not allowed
    assertThatThrownBy(() -> dao.update(policyEvaluation)).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("The LastPolicyEvaluation table does not support update operations");

    // Delete
    dao.delete(policyEvaluation);

    LastPolicyEvaluation readPolicyEvaluation2 = dao.getById(eval.getId());
    assertThat(readPolicyEvaluation2).isNull();
  }

  @Test
  public void testAddingAndDeletingWorksProperly() {
    final String stageTypeId = ReleaseStageType.ID;
    final String scanId = "LastPolicyEvaluationDAOTest";
    final Date eval1Date = new Date();

    // put one guy in, he should be first
    final PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId, eval1Date);
    final LastPolicyEvaluation firstPolicyEvaluation =
        dao.getByApplicationIdAndStageTypeId(application.getId(), stageTypeId);
    assertThat(firstPolicyEvaluation.getId()).isEqualTo(eval1.getId());

    // put in a newer guy, he should be the newest now
    final Date eval2Date = new Date(eval1Date.getTime() + 10);
    final PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId, eval2Date);
    final LastPolicyEvaluation secondPolicyEvaluation = dao
        .getByApplicationIdAndStageTypeId(application.getId(), stageTypeId);
    assertThat(secondPolicyEvaluation.getId()).isEqualTo(eval2.getId());

    // put a guy in the middle (timewise), he should not change who the newest is
    final Date eval3Date = new Date(eval1Date.getTime() + 5);
    final PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(application.getId(), stageTypeId, scanId, eval3Date);
    final LastPolicyEvaluation thirdPolicyEvaluation =
        dao.getByApplicationIdAndStageTypeId(application.getId(), stageTypeId);
    assertThat(thirdPolicyEvaluation.getId()).isEqualTo(eval2.getId());

    // delete the newest guy, should now be the middle guy
    peDao.delete(eval2);
    final LastPolicyEvaluation fourthPolicyEvaluation = dao
        .getByApplicationIdAndStageTypeId(application.getId(), stageTypeId);
    assertThat(fourthPolicyEvaluation.getId()).isEqualTo(eval3.getId());

    // delete currently newest guy, should now be the first guy
    peDao.delete(eval3);
    final LastPolicyEvaluation fifthPolicyEvaluation =
        dao.getByApplicationIdAndStageTypeId(application.getId(), stageTypeId);
    assertThat(fifthPolicyEvaluation.getId()).isEqualTo(eval1.getId());
  }

  @Test
  public void testInsert_RecordSearchIndexChange() {
    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    PolicyEvaluation eval =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, "scanId", new Date());

    List<SearchIndexChange> searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.LAST_POLICY_EVALUATION);
    assertThat(searchIndexChanges.get(0).getChangeData())
        .isEqualTo(eval.getApplicationId() + ':' + eval.getStageTypeId());
  }

  @Test
  public void testInsert_RecordSearchIndexChange_SkipForProxyStage() {
    systemConfigurationPropertyDAO
        .update(new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    tempEntity.newPolicyEvaluation(application.getId(), ProxyStageType.ID, "scanId", new Date());
    assertThat(searchIndexChangeDAO.getAll()).isEmpty();
  }
}
