/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PolicyEvaluateResourceTest
    extends AbstractResourceTest
{
  private String licenseFingerprint = "PolicyEvaluateResourceTest_LicenseFingerprint";

  private HttpRequest evaluateRequest(String appId, String scanId, Stage stage) {
    return restRequest().path(PolicyEvaluateResource.RESOURCE_PATH).query("scanId", scanId).parameter(appId)
        .body(stage);
  }

  @Test
  public void testEvaluate() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    setLicenseFingerprint(licenseFingerprint);

    tempEntity.newPolicy(app.getId(), "policy");

    // Simulate that the report is available
    String scanId = mockReport("/PolicyEvaluateResourceTest/report.zip");

    // evaluate policy
    HttpResponse response = evaluateRequest(app.getPublicId(), scanId, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);

    PolicyEvaluationResult policyEvaluationResult = response.getBody(PolicyEvaluationResult.class);
    assertEquals(7, policyEvaluationResult.getAffectedComponentCount());
    assertEquals(0, policyEvaluationResult.getCriticalComponentCount());
    assertEquals(7, policyEvaluationResult.getSevereComponentCount());
    assertEquals(0, policyEvaluationResult.getModerateComponentCount());
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertEquals(36, policyAlerts.size());
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }

    PolicyEvaluation policyEvaluation = new PolicyEvaluationDAO().getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertFalse(policyEvaluation.isReevaluation());
    assertFalse(policyEvaluation.isForObsoleteScan());
  }
}
