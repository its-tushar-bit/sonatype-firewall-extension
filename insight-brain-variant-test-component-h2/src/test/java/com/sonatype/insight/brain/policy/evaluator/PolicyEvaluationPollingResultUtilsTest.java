/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class PolicyEvaluationPollingResultUtilsTest
    extends AbstractComponentH2Test
{
  @Inject
  private PolicyEvaluationPollingResultUtils policyEvaluationPollingResultUtils;

  @Inject
  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  @Test
  public void testHandleException() {
    Application app = tempEntity.newApplicationWithParent();
    String statusId = "testStatusId";
    Exception exception = new Exception("test exception");
    createPersistedPolicyEvaluationPollingResult(app.getId(), statusId, PolicyEvaluationStatus.PENDING, null);
    policyEvaluationPollingResultUtils.handleException(app.getId(), statusId, exception);

    PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        persistedPolicyEvaluationPollingResultDAO.getByApplicationIdAndStatusId(app.getId(), statusId);
    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.FAILED);
    assertThat(policyEvaluationPollingResult.getReason()).startsWith("Internal Server Error (ID ");
  }

  @Test
  public void testHandleException_DoesNotChangeIfAlreadyFailed() {
    Application app = tempEntity.newApplicationWithParent();
    String statusId = "testStatusId";
    Exception exception = new Exception("test exception");
    String reason = "test reason";
    createPersistedPolicyEvaluationPollingResult(app.getId(), statusId, PolicyEvaluationStatus.FAILED, reason);
    policyEvaluationPollingResultUtils.handleException(app.getId(), statusId, exception);

    PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        persistedPolicyEvaluationPollingResultDAO.getByApplicationIdAndStatusId(app.getId(), statusId);
    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.FAILED);
    assertThat(policyEvaluationPollingResult.getReason()).isEqualTo(reason);
  }

  @Test
  public void testHandleException_Handles_Null_PersistedPolicyEvaluationPollingResult() {
    Application app = tempEntity.newApplicationWithParent();
    String statusId = "testStatusId";
    Exception exception = new Exception("test exception");

    policyEvaluationPollingResultUtils.handleException(app.getId(), statusId, exception);

    PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        persistedPolicyEvaluationPollingResultDAO.getByApplicationIdAndStatusId(app.getId(), statusId);
    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult();
    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.FAILED);
    assertThat(policyEvaluationPollingResult.getReason()).startsWith("Internal Server Error (ID ");
  }

  private PersistedPolicyEvaluationPollingResult createPersistedPolicyEvaluationPollingResult(
      String appId,
      String statusId,
      PolicyEvaluationStatus policyEvaluationStatus,
      String reason)
  {
    PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setStatus(policyEvaluationStatus);
    policyEvaluationPollingResult.setReason(reason);
    PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
        new PersistedPolicyEvaluationPollingResult(appId, statusId, policyEvaluationPollingResult);
    persistedPolicyEvaluationPollingResultDAO.insert(persistedPolicyEvaluationPollingResult);
    return persistedPolicyEvaluationPollingResult;
  }
}
