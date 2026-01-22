/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;

@Named
public class PolicyEvaluationPollingResultUtils
{
  private final PolicyEvaluationUtil policyEvaluationUtil;

  private final ErrorResponseGenerator errorResponseGenerator;

  private final PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  @Inject
  public PolicyEvaluationPollingResultUtils(
      PolicyEvaluationUtil policyEvaluationUtil,
      ErrorResponseGenerator errorResponseGenerator,
      PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO)
  {
    this.policyEvaluationUtil = policyEvaluationUtil;
    this.errorResponseGenerator = errorResponseGenerator;
    this.persistedPolicyEvaluationPollingResultDAO = persistedPolicyEvaluationPollingResultDAO;
  }

  public PolicyEvaluationPollingResult handleException(String appId, String statusId, Exception e) {
    try {
      String errorMessage = errorResponseGenerator.mapExceptionAndLog(e).getMessageBody();

      PersistedPolicyEvaluationPollingResult persistedPolicyEvaluationPollingResult =
          policyEvaluationUtil.createPersistedPolicyEvaluationPollingResultIfNeeded(appId, statusId);
      PolicyEvaluationPollingResult policyEvaluationPollingResult =
          persistedPolicyEvaluationPollingResult.getPolicyEvaluationPollingResult();

      // Don't overwrite failed status
      if (!PolicyEvaluationStatus.FAILED.equals(policyEvaluationPollingResult.getStatus())) {
        policyEvaluationPollingResult.setStatus(PolicyEvaluationStatus.FAILED);
        policyEvaluationPollingResult.setReason(errorMessage);
        persistedPolicyEvaluationPollingResult.setPolicyEvaluationPollingResult(policyEvaluationPollingResult);
        persistedPolicyEvaluationPollingResultDAO.update(persistedPolicyEvaluationPollingResult);
      }

      return policyEvaluationPollingResult;
    }
    catch (Exception e1) {
      e.addSuppressed(e1);
      throw new RuntimeException(e);
    }
  }
}
