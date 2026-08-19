/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.integration.PolicyEvaluationSummaryResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2PolicyEvaluationSummaryResourceTest
{
  private IqTestContext ctx;

  private static final String scanId = "test-scanid";

  private static final String invalidStageId = "InvalidStageId";

  private Application application;

  private HttpRequest summaryRequest(String appId, String stageTypeId) {
    return ctx.restRequest().path(PolicyEvaluationSummaryResource.RESOURCE_PATH).parameter(appId, stageTypeId);
  }

  @BeforeEach
  void setup() {
    application = ctx.tempEntity().newApplicationWithParent("test-app");
  }

  @Test
  void testGetPolicyEvaluationSummary() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    PolicyEvaluation policyEvaluation = ctx.tempEntity()
        .newPolicyEvaluation(application.getId(),
            stage.getStageTypeId(), scanId);
    Policy policy = ctx.tempEntity().newPolicy(application);
    ctx.tempEntity().newPolicyViolation(policyEvaluation, policy);

    HttpResponse response = summaryRequest(application.getId(), stage.getStageTypeId()).get();
    ctx.assertResponseStatus(200, response);

    PolicyEvaluationSummary policyEvaluationSummary = response.getBody(PolicyEvaluationSummary.class);

    assertThat(policyEvaluationSummary).isNotNull();
    assertThat(policyEvaluationSummary.getReportUrl())
        .isEqualTo("ui/links/application/" + application.getPublicId() + "/report/" + scanId);
    assertThat(policyEvaluationSummary.getAffectedComponentCount()).isEqualTo(1);
    assertThat(policyEvaluationSummary.getCriticalComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getModerateComponentCount()).isEqualTo(0);
    assertThat(policyEvaluationSummary.getSevereComponentCount()).isEqualTo(1);
  }

  @Test
  void testGetPolicyEvaluationSummary_badStage() throws Exception {
    HttpResponse response = summaryRequest(application.getId(), invalidStageId).get();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid parameter stageTypeId=" + invalidStageId + ".");
  }
}
