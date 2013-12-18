/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import org.junit.Test;

public class PolicyMonitoringResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testSet() throws Exception {
    grantWritePermission(app.getId());

    PolicyMonitoring policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE);

    String url = getRestUrl(PolicyMonitoringResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzPut(url, toJson(policyMonitoring));

    grantWritePermission(org.getId());
    policyMonitoring = new PolicyMonitoring(null /* ownerId */, Stage.ID_RELEASE);

    url = getRestUrl(PolicyMonitoringResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzPut(url, toJson(policyMonitoring));
  }

  @Test
  public void testDelete() throws Exception {
    grantWritePermission(app.getId());
    createPolicyMonitoring(app.getId());
    String url = getRestUrl(PolicyMonitoringResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzDelete(url);

    grantWritePermission(org.getId());
    createPolicyMonitoring(org.getId());
    url = getRestUrl(PolicyMonitoringResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzDelete(url);
  }

  @Test
  public void testGet() throws Exception {
    grantReadPermission(app.getId());
    createPolicyMonitoring(app.getId());
    String url = getRestUrl(PolicyMonitoringResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    testAuthzGet(url);

    grantReadPermission(org.getId());
    createPolicyMonitoring(org.getId());
    url = getRestUrl(PolicyMonitoringResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    testAuthzGet(url);
  }

  private void createPolicyMonitoring(String ownerid) {
    PolicyMonitoring policyMonitoring = new PolicyMonitoring(ownerid, Stage.ID_RELEASE);
    new PolicyMonitoringDAO().insert(policyMonitoring);
  }
}
