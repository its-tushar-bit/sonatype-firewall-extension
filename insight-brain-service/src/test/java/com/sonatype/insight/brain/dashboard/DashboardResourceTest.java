/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Arrays;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.PolicyViolationDTOTestUtils.assertPolicyViolationDTO;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class DashboardResourceTest
    extends AbstractResourceTest
{

  @Test
  public void testGetPolicyViolations() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");

    PolicyViolation violation = createPolicyViolation(app, buildPolicy, BuildStageType.ID);

    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH));

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(1));

    assertPolicyViolationDTO(Arrays.asList(dtos), violation, app, buildPolicy);
  }

  @Test
  public void testGetPolicyViolationsByApplicationId() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent("app1", "test application 1");
    Application app2 = tempEntity.newApplicationWithParent("app2", "test application 2");

    Policy buildPolicy = tempEntity.newPolicy(app1.getId(), "build policy");
    Policy anotherBuildPolicy = tempEntity.newPolicy(app1.getId(), "another build policy");
    Policy app2BuildPolicy = tempEntity.newPolicy(app2.getId(), "app2 build policy");

    createPolicyViolation(app1, buildPolicy, BuildStageType.ID);
    createPolicyViolation(app1, anotherBuildPolicy, BuildStageType.ID);
    PolicyViolation app2Violation = createPolicyViolation(app2, app2BuildPolicy, BuildStageType.ID);

    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?applicationPublicIds=" + app2.getPublicId());

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(1));

    assertPolicyViolationDTO(Arrays.asList(dtos), app2Violation, app2, app2BuildPolicy);
  }

  @Test
  public void testGetPolicyViolationsForNonExistantApplication() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?applicationPublicIds=noapp1&applicationPublicIds=noapp2");

    assertResponseStatus(404, response);
  }

  @Test
  public void testGetPolicyViolationsWithDifferentStageTypeIds() throws Exception {
    Application app = tempEntity.newApplicationWithParent("app1", "test application");

    Policy buildPolicy = tempEntity.newPolicy(app.getId(), "build policy");
    Policy releasePolicy = tempEntity.newPolicy(app.getId(), "release policy");

    createPolicyViolation(app, buildPolicy, BuildStageType.ID);
    PolicyViolation violationRelease = createPolicyViolation(app, releasePolicy, ReleaseStageType.ID);

    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?stageId=" + ReleaseStageType.ID);

    assertResponseStatus(200, response);
    PolicyViolationDTO[] dtos = JsonHelpers.fromJson(response.getResponseBody(), PolicyViolationDTO[].class);
    assertThat(dtos, arrayWithSize(1));
    assertEquals(dtos[0].id, violationRelease.getId());
  }

  @Test
  public void testGetPolicyViolationsWithEmptyApplicationIdsQueryParameter() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?applicationPublicIds");

    assertResponseStatus(400, response);
  }

  @Test
  public void testGetPolicyViolationsWithNonExistantStageTypeId() throws Exception {
    Response response = AuthedRestAccess.get(getRestUrl(DashboardResource.SERVICE_PATH + '/'
        + DashboardResource.GET_POLICY_VIOLATIONS_PATH)
        + "?stageId=" + "not-a-real-id");

    assertResponseStatus(400, response);
  }

  private PolicyViolation createPolicyViolation(Application app, Policy tempPolicy, String stageTypeId) {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), stageTypeId, "test scan id");
    return tempEntity.newPolicyViolation(evaluation.getId(), tempPolicy);
  }
}
