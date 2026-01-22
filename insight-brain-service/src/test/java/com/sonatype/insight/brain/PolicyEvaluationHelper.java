/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSubStatus;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Named
public class PolicyEvaluationHelper
{
  @Inject
  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  /**
   * Wait for the policy evaluation linked to appId+statusId to finish successfully.
   * 
   * @param appId The ID of the application for which the policy evaluation was started
   * @param statusId The status ID for the requested policy evaluation (from the associated
   *          PersistedPolicyEvaluationPollingResult)
   */
  public PolicyEvaluationPollingResult awaitEvaluationCompleted(String appId, String statusId) {
    return awaitEvaluationFinished(appId, statusId, 20, TimeUnit.SECONDS, PolicyEvaluationStatus.COMPLETED);
  }

  /**
   * Wait for the policy evaluation linked to appId+statusId to finish successfully.
   * 
   * @param appId The ID of the application for which the policy evaluation was started
   * @param statusId The status ID for the requested policy evaluation (from the associated
   *          PersistedPolicyEvaluationPollingResult)
   * @param timeout How long to wait
   * @param timeUnit Time unit for the timeout
   */
  public PolicyEvaluationPollingResult awaitEvaluationCompleted(
      String appId,
      String statusId,
      long timeout,
      TimeUnit timeUnit)
  {
    return awaitEvaluationFinished(appId, statusId, timeout, timeUnit, PolicyEvaluationStatus.COMPLETED);
  }

  /**
   * Wait for the policy evaluation linked to appId+statusId to fail.
   * 
   * @param appId The ID of the application for which the policy evaluation was started
   * @param statusId The status ID for the requested policy evaluation (from the associated
   *          PersistedPolicyEvaluationPollingResult)
   */
  public PolicyEvaluationPollingResult awaitEvaluationFailed(String appId, String statusId) {
    return awaitEvaluationFinished(appId, statusId, 20, TimeUnit.SECONDS, PolicyEvaluationStatus.FAILED);
  }

  /**
   * Wait for the policy evaluation linked to appId+statusId to fail.
   * 
   * @param appId The ID of the application for which the policy evaluation was started
   * @param statusId The status ID for the requested policy evaluation (from the associated
   *          PersistedPolicyEvaluationPollingResult)
   * @param timeout How long to wait
   * @param timeUnit Time unit for the timeout
   */
  public PolicyEvaluationPollingResult awaitEvaluationFailed(
      String appId,
      String statusId,
      long timeout,
      TimeUnit timeUnit)
  {
    return awaitEvaluationFinished(appId, statusId, timeout, timeUnit, PolicyEvaluationStatus.FAILED);
  }

  private PolicyEvaluationPollingResult awaitEvaluationFinished(
      String appId,
      String statusId,
      long timeout,
      TimeUnit timeUnit,
      PolicyEvaluationStatus expectedStatus)
  {
    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        awaitEvaluationFinished(appId, statusId, timeout, timeUnit);

    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(expectedStatus);

    return policyEvaluationPollingResult;
  }

  /**
   * Wait for the policy evaluation linked to appId+statusId to finish (success or failure).
   * 
   * @param appId The ID of the application for which the policy evaluation was started
   * @param statusId The status ID for the requested policy evaluation (from the associated
   *          PersistedPolicyEvaluationPollingResult)
   */
  public PolicyEvaluationPollingResult awaitEvaluationFinished(String appId, String statusId) {
    return awaitEvaluationFinished(appId, statusId, 20, TimeUnit.SECONDS);
  }

  /**
   * Wait for the policy evaluation linked to appId+statusId to finish (success or failure).
   * 
   * @param appId The ID of the application for which the policy evaluation was started
   * @param statusId The status ID for the requested policy evaluation (from the associated
   *          PersistedPolicyEvaluationPollingResult)
   * @param timeout How long to wait
   * @param timeUnit Time unit for the timeout
   */
  public PolicyEvaluationPollingResult awaitEvaluationFinished(
      String appId,
      String statusId,
      long timeout,
      TimeUnit timeUnit)
  {
    // Waiting on not-pending ensures we don't wait too long in case the evaluation finished with an unexpected status.
    await().atMost(timeout, timeUnit)
        .until(() -> !PolicyEvaluationStatus.PENDING.equals(persistedPolicyEvaluationPollingResultDAO
            .getByApplicationIdAndStatusId(appId, statusId).getPolicyEvaluationPollingResult().getStatus()));

    return persistedPolicyEvaluationPollingResultDAO.getByApplicationIdAndStatusId(appId, statusId)
        .getPolicyEvaluationPollingResult();
  }

  /**
   * Wait for the component analysis process linked to appId and statusId to complete.
   *
   * @param appId The ID of the application for which the component analysis was started
   * @param statusId    The status ID for the component analysis process (from the associated
   *          PersistedPolicyEvaluationPollingResult)
   */
  public void awaitComponentAnalysisCompleted(final String appId, final String statusId) {
    await().atMost(20, TimeUnit.SECONDS)
        .until(() -> PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_COMPLETE
            .equals(persistedPolicyEvaluationPollingResultDAO
                .getByApplicationIdAndStatusId(appId, statusId)
                .getPolicyEvaluationPollingResult().getSubStatus()));
  }

  /**
   * Wait for the component analysis process linked to appId and statusId to fail.
   *
   * @param appId The ID of the application for which the component analysis was started
   * @param statusId    The status ID for the component analysis process (from the associated
   *          PersistedPolicyEvaluationPollingResult)
   */
  public void awaitComponentAnalysisFailed(final String appId, final String statusId) {
    await().atMost(20, TimeUnit.SECONDS)
        .until(() -> {
          PolicyEvaluationPollingResult res = persistedPolicyEvaluationPollingResultDAO
              .getByApplicationIdAndStatusId(appId, statusId)
              .getPolicyEvaluationPollingResult();
          return res.getSubStatus().equals(PolicyEvaluationSubStatus.COMPONENT_ANALYSIS_PENDING) &&
              res.getStatus().equals(PolicyEvaluationStatus.FAILED);
        });
  }
}
