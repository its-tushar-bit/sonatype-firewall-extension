/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import org.junit.Test;

public class PolicyEvaluateResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PolicyEvaluateResource.RESOURCE_PATH);
  }

  private HttpRequest evalRequest(String scanId, Stage stage) {
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
    testAuthzPost(evalRequest(scanId, stage));
  }

  @Test
  public void testEvaluate_UnauthorizedAnonymousNotAllowed() throws Exception {
    String scanId = "testEvaluate_UnauthorizedAnonymousNotAllowed";

    // Evaluate the policy
    Stage stage = new Stage(BuildStageType.ID);
    HttpResponse response = evalRequest(scanId, stage).post();
    assertResponseStatus(401, response);
  }

  @Test
  @ManualServerInit
  public void testEvaluate_UnauthorizedAnonymousAllowed_AnonymousClientAccessAllowed() throws Exception {
    initServer(new Configurator() {
      @Override
      public void configure(final InsightConfig config) {
        config.setAnonymousClientAccessAllowed(true);
      }
    });
    String scanId = "testEvaluate_UnauthorizedAnonymousAllowed";
    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    // Evaluate the policy
    Stage stage = new Stage(BuildStageType.ID);
    HttpResponse response = evalRequest(scanId, stage).anon().post();
    assertResponseStatus(200, response);
  }

  @Test
  public void testEvaluate_Unauthorized() throws Exception {
    String scanId = "testEvaluate_UnauthorizedAnonymousAllowed";

    // Evaluate the policy
    Stage stage = new Stage(BuildStageType.ID);
    HttpResponse response = evalRequest(scanId, stage).auth("unknownUser", "unknownPassword").post();
    assertResponseStatus(401, response);
  }
}
