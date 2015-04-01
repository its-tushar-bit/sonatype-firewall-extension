/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class PolicyEvaluationSummaryResourceTest
    extends AbstractResourceTest
{
  private static final String scanId = "test-scanid";

  private static final String invalidStageId = "InvalidStageId";

  private Application application;

  @Before
  public void setup() {
    application = tempEntity.newApplicationWithParent("test-app");
  }

  @Test
  public void testGetPolicyEvaluationSummary() throws Exception {
    Stage stage = new Stage(Stage.ID_BUILD);

    PolicyEvaluation policyEvaluation = tempEntity
        .newPolicyEvaluation(application.getId(), stage.getStageTypeId(), scanId);
    Policy policy = tempEntity.newPolicy(application.getId(), "test-policy");
    tempEntity.newPolicyViolation(policyEvaluation, policy);


    Response response = AuthedRestAccess.get(getServiceURL(application.getId(), stage.getStageTypeId()));
    assertResponseStatus(200, response);

    PolicyEvaluationSummary policyEvaluationSummary = fromJson(response, PolicyEvaluationSummary.class);

    assertThat(policyEvaluationSummary, notNullValue());
    assertThat(policyEvaluationSummary.getReportUrl(),
        is("ui/links/application/" + application.getPublicId() + "/report/" + scanId));
    assertThat(policyEvaluationSummary.getAffectedComponentCount(), is(1));
    assertThat(policyEvaluationSummary.getCriticalComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getModerateComponentCount(), is(0));
    assertThat(policyEvaluationSummary.getSevereComponentCount(), is(1));
  }

  @Test
  public void testGetPolicyEvaluationSummary_badStage() throws Exception {
    Response response = AuthedRestAccess.get(getServiceURL(application.getId(), invalidStageId));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Invalid parameter stageTypeId=" + invalidStageId + "."));
  }

  private String getServiceURL(final String appId, final String stageTypeId) {
    return getRestBaseUrl() + PolicyEvaluationSummaryResource.SERVICE_PATH.replace("{applicationId}", appId)
        .replace("{stageTypeId}", stageTypeId);
  }
}
