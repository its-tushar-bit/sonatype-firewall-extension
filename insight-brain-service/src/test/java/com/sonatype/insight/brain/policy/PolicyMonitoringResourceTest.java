/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.policy.PolicyMonitoringResource.ApplicablePolicyMonitors;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PolicyMonitoringResourceTest
    extends AbstractResourceTest
{
  private HttpRequest restRequest(String ownerType, String ownerId) {
    return restRequest().path(PolicyMonitoringResource.SERVICE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  public void testCRUD_Application() throws Exception {
    String appPublicId = "PolicyMonitoringResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);

    testCRUD(IdUtils.TYPE_APPLICATION, appPublicId, application.getId());
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("PolicyMonitoringResourceTest");

    testCRUD(IdUtils.TYPE_ORGANIZATION, organization.getId(), organization.getId());
  }

  private void testCRUD(String ownerType, String ownerPublicId, String ownerId) throws Exception {
    // Create
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE);
    Response response = restRequest(ownerType, ownerPublicId).body(policyMonitoring).put();
    assertResponseStatus(200, response);
    policyMonitoring = fromJson(response, PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_RELEASE, policyMonitoring);

    // Get
    response = restRequest(ownerType, ownerPublicId).get();
    assertResponseStatus(200, response);
    policyMonitoring = fromJson(response, PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_RELEASE, policyMonitoring);

    // Update
    policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_BUILD);
    response = restRequest(ownerType, ownerPublicId).body(policyMonitoring).put();
    assertResponseStatus(200, response);
    policyMonitoring = fromJson(response, PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_BUILD, policyMonitoring);

    // Get
    response = restRequest(ownerType, ownerPublicId).get();
    assertResponseStatus(200, response);
    policyMonitoring = fromJson(response, PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_BUILD, policyMonitoring);

    // Delete
    response = restRequest(ownerType, ownerPublicId).delete();
    assertResponseStatus(204, response);

    // Get
    response = restRequest(ownerType, ownerPublicId).get();
    assertResponseStatus(204, response);
  }

  @Test
  public void testDelete_NotSet_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization("PolicyMonitoringResourceTest");
    Response response = restRequest(IdUtils.TYPE_ORGANIZATION, organization.getId()).delete();
    assertResponseStatus(404, response);
    assertThat(response.getResponseBody(), is("Policy monitoring was not set for owner ID " + organization.getId() + "."));
  }

  @Test
  public void testDelete_NotSet_Application() throws Exception {
    String appPublicId = "PolicyMonitoringResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(appPublicId);
    Response response = restRequest(IdUtils.TYPE_APPLICATION, appPublicId).delete();
    assertResponseStatus(404, response);
    assertThat(response.getResponseBody(), is("Policy monitoring was not set for owner ID " + application.getId() + "."));
  }

  @Test
  public void testGetApplicablePolicyMonitoring() throws Exception {
    Organization organization = tempEntity.newOrganization("testGetApplicablePolicyMonitoringOrgId");
    Application application = tempEntity.newApplication("testGetApplicablePolicyMonitoringAppId",
        "testGetApplicablePolicyMonitoringAppId", organization.getId());

    //no Policy Monitoring set
    Response response = restRequest(IdUtils.TYPE_APPLICATION, application.getPublicId()).path("applicable").get();
    ApplicablePolicyMonitors applicablePolicyMonitors = fromJson(response, ApplicablePolicyMonitors.class);
    assertThat(applicablePolicyMonitors.appPolicyMonitor, nullValue());
    assertThat(applicablePolicyMonitors.orgPolicyMonitor, nullValue());
    response = restRequest(IdUtils.TYPE_ORGANIZATION, organization.getId()).path("applicable").get();
    applicablePolicyMonitors = fromJson(response, ApplicablePolicyMonitors.class);
    assertThat(applicablePolicyMonitors.appPolicyMonitor, nullValue());
    assertThat(applicablePolicyMonitors.orgPolicyMonitor, nullValue());

    //Org only Policy Monitoring set
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE);
    restRequest(IdUtils.TYPE_ORGANIZATION, organization.getId()).body(policyMonitoring).put();

    response = restRequest(IdUtils.TYPE_APPLICATION, application.getPublicId()).path("applicable").get();
    applicablePolicyMonitors = fromJson(response, ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.appPolicyMonitor, nullValue());
    assertThat(applicablePolicyMonitors.orgPolicyMonitor, notNullValue());
    assertThat(applicablePolicyMonitors.orgPolicyMonitor.getStageTypeId(), is(Stage.ID_RELEASE));

    //App and Org both have Policy Monitoring set
    policyMonitoring.setStageTypeId(Stage.ID_OPERATE);
    restRequest(IdUtils.TYPE_APPLICATION, application.getPublicId()).body(policyMonitoring).put();
    response = restRequest(IdUtils.TYPE_APPLICATION, application.getPublicId()).path("applicable").get();
    applicablePolicyMonitors = fromJson(response, ApplicablePolicyMonitors.class);

    assertThat(applicablePolicyMonitors.appPolicyMonitor, notNullValue());
    assertThat(applicablePolicyMonitors.appPolicyMonitor.getStageTypeId(), is(Stage.ID_OPERATE));
    assertThat(applicablePolicyMonitors.orgPolicyMonitor, notNullValue());
    assertThat(applicablePolicyMonitors.orgPolicyMonitor.getStageTypeId(), is(Stage.ID_RELEASE));

    //sanity check the Org information
    response = restRequest(IdUtils.TYPE_ORGANIZATION, organization.getId()).path("applicable").get();
    applicablePolicyMonitors = fromJson(response, ApplicablePolicyMonitors.class);
    assertThat(applicablePolicyMonitors.appPolicyMonitor, nullValue());
    assertThat(applicablePolicyMonitors.orgPolicyMonitor, notNullValue());
    assertThat(applicablePolicyMonitors.orgPolicyMonitor.getStageTypeId(), is(Stage.ID_RELEASE));
  }

  private void assertPolicyMonitoring(String ownerId, String stageTypeId, PolicyMonitoring actual) {
    assertEquals(ownerId, actual.getOwnerId());
    assertEquals(stageTypeId, actual.getStageTypeId());
  }
}
