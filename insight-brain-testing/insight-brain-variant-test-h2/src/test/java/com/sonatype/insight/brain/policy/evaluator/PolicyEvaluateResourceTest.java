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
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in {@code PolicyEvaluateResource}'s own package (rather than {@code com.sonatype.insight.brain.variant}) and
 * named exactly like the legacy {@code PolicyEvaluateResourceTest} because its mock report fixture is resolved via
 * {@code getClass().getSimpleName()} (see the {@code /PolicyEvaluateResourceTest/report} classpath resource).
 */
@IqH2Test
class PolicyEvaluateResourceTest
{
  private IqTestContext ctx;

  private PolicyDAO policyDAO;

  private PolicyEvaluationDAO policyEvaluationDAO;

  @BeforeEach
  void setUp() {
    policyDAO = ctx.lookup(PolicyDAO.class);
    policyEvaluationDAO = ctx.lookup(PolicyEvaluationDAO.class);
  }

  private HttpRequest evaluateRequest(String appId, String scanId, Stage stage) {
    return ctx.restRequest()
        .path(PolicyEvaluateResource.RESOURCE_PATH)
        .query("scanId", scanId)
        .parameter(appId)
        .body(stage);
  }

  private String mockReport(String resourceName) {
    String scanId = TemporaryEntity.uuid();
    ctx.mockReport(scanId, resourceName);
    return scanId;
  }

  @Test
  void testEvaluate() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();

    Policy policy = ctx.tempEntity().newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ctx.createScanFile(app.getId(), scanId);

    // evaluate policy
    HttpResponse response = evaluateRequest(app.getPublicId(), scanId, new Stage(Stage.ID_BUILD)).post();
    ctx.assertResponseStatus(200, response);

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

    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByOwnerIdAndScanId(app.getId(), scanId);
    assertThat(policyEvaluation.getScanTriggerType()).isEqualTo(ScanTriggerType.UNKNOWN);
    assertThat(policyEvaluation.isReevaluation()).isFalse();
    assertThat(policyEvaluation.isForObsoleteScan()).isFalse();
    assertThat(policyEvaluationResult.getAlerts()).isNotEmpty();
    PolicyAlert alert = policyEvaluationResult.getAlerts().get(0);
    assertThat(alert.getActions().get(0).getActionTypeId()).isEqualTo(Action.ID_FAIL);
  }

  @Test
  void testEvaluate_MissingEnforcementFeature() throws Exception {
    ctx.setMissingFeature(LicensedFeature.ENFORCEMENT);

    Application app = ctx.tempEntity().newApplicationWithParent();

    Policy policy = ctx.tempEntity().newPolicy(app);
    policy.setAction(BuildStageType.ID, Action.ID_FAIL);
    policyDAO.update(policy);

    // Simulate that the report is available
    String scanId = mockReport("/" + getClass().getSimpleName() + "/report");
    ctx.createScanFile(app.getId(), scanId);

    // evaluate policy
    HttpResponse response = evaluateRequest(app.getPublicId(), scanId, new Stage(Stage.ID_BUILD)).post();
    ctx.assertResponseStatus(200, response);

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
