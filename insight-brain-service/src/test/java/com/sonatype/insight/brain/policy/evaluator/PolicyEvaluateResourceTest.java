/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class PolicyEvaluateResourceTest
    extends AbstractResourceTest
{
  private PolicyDAO policyDAO;

  private PolicyEvaluationDAO policyEvaluationDAO;

  @Before
  public void setUp() {
    policyDAO = lookup(PolicyDAO.class);
    policyEvaluationDAO = lookup(PolicyEvaluationDAO.class);
  }

  private HttpRequest evaluateRequest(String appId, String scanId, Stage stage) {
    return restRequest().path(PolicyEvaluateResource.RESOURCE_PATH)
        .query("scanId", scanId)
        .parameter(appId)
        .body(stage);
  }

  @Test
  public void testEvaluate() throws Exception {
    Application app = tempEntity.newApplicationWithParent();

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    createScanFile(app.getId(), scanId);

    // evaluate policy
    HttpResponse response = evaluateRequest(app.getPublicId(), scanId, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);

    PolicyEvaluationResult policyEvaluationResult = response.getBody(PolicyEvaluationResult.class);
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    List<PolicyAlert> policyAlerts = policyEvaluationResult.getAlerts();
    assertThat(policyAlerts).hasSize(36);
    for (PolicyAlert policyAlert : policyAlerts) {
      AbstractPolicyEvaluationTest.assertFactCounts(1, 1, policyAlert);
    }

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation.getScanTriggerType()).isEqualTo(ScanTriggerType.UNKNOWN);
    assertThat(policyEvaluation.isReevaluation()).isFalse();
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
    assertThat(policyEvaluationResult.getAlerts()).isNotEmpty();
    PolicyAlert alert = policyEvaluationResult.getAlerts().get(0);
    assertThat(alert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_FAIL);
  }

  @Test
  public void testEvaluate_MissingEnforcementFeature() throws Exception {
    setMissingFeature(LicensedFeature.ENFORCEMENT);

    Application app = tempEntity.newApplicationWithParent();

    Policy policy = tempEntity.newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    createScanFile(app.getId(), scanId);

    // evaluate policy
    HttpResponse response = evaluateRequest(app.getPublicId(), scanId, new Stage(Stage.ID_BUILD)).post();
    assertResponseStatus(200, response);

    PolicyEvaluationResult policyEvaluationResult = response.getBody(PolicyEvaluationResult.class);
    assertThat(policyEvaluationResult.getAffectedComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getSevereComponentCount()).isEqualTo(7);
    assertThat(policyEvaluationResult.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationResult.getAlerts()).isNotEmpty();
    PolicyAlert alert = policyEvaluationResult.getAlerts().get(0);
    assertThat(alert.getActions()).isEmpty();
  }
}
