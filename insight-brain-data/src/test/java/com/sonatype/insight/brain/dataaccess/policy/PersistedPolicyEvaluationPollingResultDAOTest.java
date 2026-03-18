/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistedPolicyEvaluationPollingResultDAOTest
    extends AbstractDbDAOTest
{
  private PersistedPolicyEvaluationPollingResultDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createPersistedPolicyEvaluationPollingResultDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    String statusId = TemporaryEntity.uuid();
    PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setReason("reason");
    PersistedPolicyEvaluationPollingResult expected =
        new PersistedPolicyEvaluationPollingResult(application.getId(), statusId, policyEvaluationPollingResult);
    dao.insert(expected);
    assertThat(expected.getId()).isNotNull();

    // Read
    assertThat(dao.getByApplicationIdAndStatusId(application.getId(), statusId)).usingRecursiveComparison()
        .usingOverriddenEquals()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringCollectionOrder()
        .isEqualTo(expected);

    // Update
    policyEvaluationPollingResult.setReason("other reason");
    expected.setPolicyEvaluationPollingResult(policyEvaluationPollingResult);
    dao.update(expected);
    assertThat(dao.getByApplicationIdAndStatusId(expected.getApplicationId(), expected.getStatusId()))
        .usingRecursiveComparison()
        .usingOverriddenEquals()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringCollectionOrder()
        .isEqualTo(expected);

    // Delete
    dao.delete(expected);
    assertThat(dao.getByApplicationIdAndStatusId(expected.getApplicationId(), expected.getStatusId())).isNull();
  }

  @Test
  public void testDeleteBeforeOrOn() {
    long now = System.currentTimeMillis();
    PersistedPolicyEvaluationPollingResult result1 = new PersistedPolicyEvaluationPollingResult(application.getId(),
        TemporaryEntity.uuid(), new PolicyEvaluationPollingResult());
    result1.setCreateTime(new Date(now - 1));
    dao.insert(result1);
    PersistedPolicyEvaluationPollingResult result2 = new PersistedPolicyEvaluationPollingResult(application.getId(),
        TemporaryEntity.uuid(), new PolicyEvaluationPollingResult());
    result2.setCreateTime(new Date(now));
    dao.insert(result2);
    PersistedPolicyEvaluationPollingResult result3 = new PersistedPolicyEvaluationPollingResult(application.getId(),
        TemporaryEntity.uuid(), new PolicyEvaluationPollingResult());
    result3.setCreateTime(new Date(now + 1));
    dao.insert(result3);

    dao.deleteBeforeOrOn(result2.getCreateTime());

    assertThat(dao.getByApplicationIdAndStatusId(result1.getApplicationId(), result1.getStatusId())).isNull();
    assertThat(dao.getByApplicationIdAndStatusId(result2.getApplicationId(), result2.getStatusId())).isNull();
    assertThat(dao.getByApplicationIdAndStatusId(result3.getApplicationId(), result3.getStatusId())).isNotNull();
  }

  @Test
  public void testDeleteAll() {
    PersistedPolicyEvaluationPollingResult result1 = new PersistedPolicyEvaluationPollingResult(application.getId(),
        TemporaryEntity.uuid(), new PolicyEvaluationPollingResult());
    dao.insert(result1);
    PersistedPolicyEvaluationPollingResult result2 = new PersistedPolicyEvaluationPollingResult(application.getId(),
        TemporaryEntity.uuid(), new PolicyEvaluationPollingResult());
    dao.insert(result2);

    dao.deleteAll();

    assertThat(dao.getByApplicationIdAndStatusId(result1.getApplicationId(), result1.getStatusId())).isNull();
    assertThat(dao.getByApplicationIdAndStatusId(result2.getApplicationId(), result2.getStatusId())).isNull();
  }
}
