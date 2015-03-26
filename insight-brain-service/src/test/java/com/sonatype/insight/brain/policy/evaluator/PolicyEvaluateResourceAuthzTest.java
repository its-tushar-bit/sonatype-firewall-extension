/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import org.junit.Test;

public class PolicyEvaluateResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testEvaluate_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);

    String scanId = "testEvaluate";
    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    // Evaluate the policy
    Stage stage = new Stage(BuildStageType.ID);
    String url = getServiceURL(app.getPublicId(), scanId);
    testAuthzPost(url, toJson(stage), 200);
  }

  @Test
  public void testEvaluate_UnauthorizedAnonymousAllowed() throws Exception {
    String scanId = "testEvaluate_UnauthorizedAnonymousAllowed";
    // Simulate that the report is available
    mockReport(scanId, "/PolicyEvaluateResourceTest/report.zip");

    // Evaluate the policy
    Stage stage = new Stage(BuildStageType.ID);
    Response response = RestAccess.post(getServiceURL(app.getPublicId(), scanId), toJson(stage));
    assertResponseStatus(200, response);
  }

  @Test
  public void testEvaluate_Unauthorized() throws Exception {
    String scanId = "testEvaluate_UnauthorizedAnonymousAllowed";

    // Evaluate the policy
    Stage stage = new Stage(BuildStageType.ID);
    Response response = RestAccess.post(getServiceURL(app.getPublicId(), scanId), toJson(stage),
        "unknownUser", "unknownPassword");
    assertResponseStatus(401, response);
  }

  private String getServiceURL(final String appId, final String scanId) {
    return getRestBaseUrl() + PolicyEvaluateResource.SERVICE_PATH.replace("{applicationPublicId}", appId) + "?scanId="
        + scanId;
  }
}
