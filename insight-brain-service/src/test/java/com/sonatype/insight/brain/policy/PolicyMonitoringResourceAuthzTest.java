/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Test;

public class PolicyMonitoringResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PolicyMonitoringResource.SERVICE_PATH);
  }

  @Test
  public void testSet() throws Exception {
    HttpRequest request = restRequest().body(new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE));

    grantWritePermission(app.getId());
    testAuthzPut(request.parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()));

    grantWritePermission(org.getId());
    testAuthzPut(request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId()));
  }

  @Test
  public void testDelete() throws Exception {
    grantWritePermission(app.getId());
    createPolicyMonitoring(app.getId());
    testAuthzDelete(restRequest().parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()));

    grantWritePermission(org.getId());
    createPolicyMonitoring(org.getId());
    testAuthzDelete(restRequest().parameter(IdUtils.TYPE_ORGANIZATION, org.getId()));
  }

  @Test
  public void testGet() throws Exception {
    grantReadPermission(app.getId());
    createPolicyMonitoring(app.getId());
    testAuthzGet(restRequest().parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()));

    grantReadPermission(org.getId());
    createPolicyMonitoring(org.getId());
    testAuthzGet(restRequest().parameter(IdUtils.TYPE_ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetApplicable() throws Exception {
    HttpRequest request = restRequest().path("applicable");

    grantReadPermission(app.getId());
    createPolicyMonitoring(app.getId());
    testAuthzGet(request.parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()));

    grantReadPermission(org.getId());
    createPolicyMonitoring(org.getId());
    testAuthzGet(request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId()));
  }

  private void createPolicyMonitoring(String ownerid) {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerid, Stage.ID_RELEASE);
    tempEntity.newPolicyMonitoring(policyMonitoring);
  }
}
