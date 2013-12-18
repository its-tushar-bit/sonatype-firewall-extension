/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PolicyMonitoringResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testCRUD_Application() throws Exception {
    String appPublicId = "PolicyMonitoringResourceTest_AppId";
    Application application = createApplication(appPublicId);

    testCRUD(IdUtils.TYPE_APPLICATION, appPublicId, application.getId());
  }

  @Test
  public void testCRUD_Organization() throws Exception {
    Organization organization = createOrganization("PolicyMonitoringResourceTest");

    testCRUD(IdUtils.TYPE_ORGANIZATION, organization.getId(), organization.getId());
  }

  private void testCRUD(String ownerType, String ownerPublicId, String ownerId) throws Exception {
    // Create
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE);
    Response response = AuthedRestAccess.put(getServiceURL(ownerType, ownerPublicId),
        JsonHelpers.asJson(policyMonitoring));
    assertResponseStatus(200, response);
    policyMonitoring = JsonHelpers.fromJson(response.getResponseBody(), PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_RELEASE, policyMonitoring);

    // Get
    response = AuthedRestAccess.get(getServiceURL(ownerType, ownerPublicId));
    assertResponseStatus(200, response);
    policyMonitoring = JsonHelpers.fromJson(response.getResponseBody(), PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_RELEASE, policyMonitoring);

    // Update
    policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_BUILD);
    response = AuthedRestAccess.put(getServiceURL(ownerType, ownerPublicId), JsonHelpers.asJson(policyMonitoring));
    assertResponseStatus(200, response);
    policyMonitoring = JsonHelpers.fromJson(response.getResponseBody(), PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_BUILD, policyMonitoring);

    // Get
    response = AuthedRestAccess.get(getServiceURL(ownerType, ownerPublicId));
    assertResponseStatus(200, response);
    policyMonitoring = JsonHelpers.fromJson(response.getResponseBody(), PolicyMonitoring.class);
    assertPolicyMonitoring(ownerId, Stage.ID_BUILD, policyMonitoring);

    // Delete
    response = AuthedRestAccess.delete(getServiceURL(ownerType, ownerPublicId));
    assertResponseStatus(204, response);

    // Get
    response = AuthedRestAccess.get(getServiceURL(ownerType, ownerPublicId));
    assertResponseStatus(204, response);
  }

  @Test
  public void testDelete_NotSet_Organization() throws Exception {
    Organization organization = createOrganization("PolicyMonitoringResourceTest");
    Response response = AuthedRestAccess.delete(getServiceURL(IdUtils.TYPE_ORGANIZATION, organization.getId()));
    assertResponseStatus(404, response);
    assertThat(response.getResponseBody(), is("Policy monitoring was not set for owner id " + organization.getId()));
  }

  @Test
  public void testDelete_NotSet_Application() throws Exception {
    String appPublicId = "PolicyMonitoringResourceTest_AppId";
    Application application = createApplication(appPublicId);
    Response response = AuthedRestAccess.delete(getServiceURL(IdUtils.TYPE_APPLICATION, appPublicId));
    assertResponseStatus(404, response);
    assertThat(response.getResponseBody(), is("Policy monitoring was not set for owner id " + application.getId()));
  }

  private void assertPolicyMonitoring(String ownerId, String stageTypeId, PolicyMonitoring actual) {
    assertEquals(ownerId, actual.getOwnerId());
    assertEquals(stageTypeId, actual.getStageTypeId());
  }

  private String getServiceURL(String ownerType, String ownerId) {
    return getRestUrl(PolicyMonitoringResource.SERVICE_PATH, ownerType, ownerId);
  }
}
