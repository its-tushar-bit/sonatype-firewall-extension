/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.List;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.evaluator.AbstractPolicyEvaluationTest;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.scan.model.ClientScanType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationEvaluationResourceTest
    extends AbstractResourceTest
{
  private HttpRequest evaluateWithPollingRequest(IntegrationType integrationType,
                                                 String applicationPublicId,
                                                 String stageId)
  {
    return restRequest()
        .path(ApplicationEvaluationResource.RESOURCE_PATH, ApplicationEvaluationResource.EVALUATE_PATH)
        .query("scanType", ClientScanType.SONATYPE).parameter(applicationPublicId, integrationType, stageId);
  }

  private HttpRequest pollEvaluationResultRequest(String appId, String statusId) {
    return restRequest().path(ApplicationEvaluationResource.RESOURCE_PATH, ApplicationEvaluationResource.STATUS_PATH)
        .parameter(appId, statusId);
  }

  @Test
  public void testEvaluateWithPolling() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    // evaluate policy
    HttpResponse response =
        evaluateWithPollingRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID).post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getStatusId()).isNotNull();
  }

  @Test
  public void testPollEvaluationResult() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    new PolicyDAO().update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/ApplicationEvaluationResourceTest/report.zip");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId(scanId);

    mockScanReceipt(scanReceipt);

    // evaluate policy
    HttpResponse response =
        evaluateWithPollingRequest(IntegrationType.CLI, app.getPublicId(), BuildStageType.ID).post();
    assertResponseStatus(200, response);

    PolicyEvaluationReceipt receipt = response.getBody(PolicyEvaluationReceipt.class);
    assertThat(receipt).isNotNull();
    assertThat(receipt.getStatusId()).isNotNull();

    PolicyEvaluationPollingResult policyEvaluationPollingResult =
        getPolicyEvaluationPollingResult(app.getPublicId(), receipt.getStatusId());

    assertThat(policyEvaluationPollingResult.getStatus()).isEqualTo(PolicyEvaluationStatus.COMPLETED);
    assertThat(policyEvaluationPollingResult.getReason()).isNull();
    assertThat(policyEvaluationPollingResult.getResult()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt()).isNotNull();
    assertThat(policyEvaluationPollingResult.getScanReceipt().getScanId()).isEqualTo(scanReceipt.getScanId());

    PolicyEvaluationResult policyEvaluationResult = policyEvaluationPollingResult.getResult();
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts).hasSize(36);
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
      assertThat(policyAlert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_FAIL);
    }

    PolicyEvaluation policyEvaluation = new PolicyEvaluationDAO().getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation.isReevaluation()).isFalse();
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
  }

  private PolicyEvaluationPollingResult getPolicyEvaluationPollingResult(String applicationPublicId, String statusId)
      throws Exception
  {
    long endTime = System.currentTimeMillis() + 10000;

    while (System.currentTimeMillis() < endTime) {
      HttpResponse response = pollEvaluationResultRequest(applicationPublicId, statusId).get();
      assertResponseStatus(200, response);
      PolicyEvaluationPollingResult policyEvaluationPollingResult =
          response.getBody(PolicyEvaluationPollingResult.class);
      if (policyEvaluationPollingResult.getStatus().equals(PolicyEvaluationStatus.COMPLETED)) {
        return policyEvaluationPollingResult;
      }
      Thread.sleep(500);
    }
    throw new RuntimeException("Evaluation did not complete within the expected 10 seconds to get the polling result.");
  }
}
