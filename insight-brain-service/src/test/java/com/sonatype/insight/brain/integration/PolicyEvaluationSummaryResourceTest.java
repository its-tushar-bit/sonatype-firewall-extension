/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PolicyEvaluationSummaryResourceTest
    extends AbstractResourceTest
{
  private static final String scanId = "test-scanid";

  private static final String invalidStageId = "InvalidStageId";

  private Application application;

  private HttpRequest summaryRequest(String appId, String stageTypeId) {
    return restRequest().path(PolicyEvaluationSummaryResource.RESOURCE_PATH).parameter(appId, stageTypeId);
  }

  @Before
  public void setup() {
    application = tempEntity.newApplicationWithParent("test-app");
  }

  @Test
  public void testGetPolicyEvaluationSummary() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), stage.getStageTypeId(),
        scanId);
    Policy policy = tempEntity.newPolicy(application);
    tempEntity.newPolicyViolation(policyEvaluation, policy);

    HttpResponse response = summaryRequest(application.getId(), stage.getStageTypeId()).get();
    assertResponseStatus(200, response);

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
  public void testGetPolicyEvaluationSummary_badStage() throws Exception {
    HttpResponse response = summaryRequest(application.getId(), invalidStageId).get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Invalid parameter stageTypeId=" + invalidStageId + ".");
  }
}
