/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import com.ning.http.client.Response;
import org.junit.Test;

public class PolicyEvaluateResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PolicyEvaluateResource.SERVICE_PATH);
  }

  private HttpRequest evalRequest(String appId, String scanId, Stage stage) {
    return restRequest().body(stage).parameter(app.getPublicId()).query("scanId", scanId);
  }

  @Test
  public void testEvaluate_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    String scanId = "testEvaluate";
    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    // Evaluate the policy
    Stage stage = new Stage(BuildStageType.ID);
    testAuthzPost(evalRequest(app.getPublicId(), scanId, stage), 200);
  }

  @Test
  public void testEvaluate_UnauthorizedAnonymousAllowed() throws Exception {
    String scanId = "testEvaluate_UnauthorizedAnonymousAllowed";
    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    // Evaluate the policy
    Stage stage = new Stage(BuildStageType.ID);
    Response response = evalRequest(app.getPublicId(), scanId, stage).post();
    assertResponseStatus(200, response);
  }

  @Test
  public void testEvaluate_Unauthorized() throws Exception {
    String scanId = "testEvaluate_UnauthorizedAnonymousAllowed";

    // Evaluate the policy
    Stage stage = new Stage(BuildStageType.ID);
    Response response = evalRequest(app.getPublicId(), scanId, stage).auth("unknownUser", "unknownPassword").post();
    assertResponseStatus(401, response);
  }
}
