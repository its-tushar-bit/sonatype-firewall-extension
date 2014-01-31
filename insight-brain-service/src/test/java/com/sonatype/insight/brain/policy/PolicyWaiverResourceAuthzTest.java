/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;
import com.sonatype.insight.brain.utils.IdUtils;

import com.ning.http.client.Response;
import org.junit.Before;
import org.junit.Test;

public class PolicyWaiverResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private Policy policy;

  @Before
  public void init() {
    policy = tempEntity.newPolicy(app.getId(), "Test Policy");
  }

  @Test
  public void testAddPolicyWaiver() throws Exception {
    grantWritePermission(app.getId());

    PolicyWaiver waiver = new PolicyWaiver("hash", policy.getId(), null, "comment");

    String url = getRestUrl(PolicyWaiverResource.SERVICE_PATH, IdUtils.TYPE_APPLICATION, app.getPublicId());
    Response response = testAuthzPost(url, toJson(waiver));
    waiver = fromJson(response, PolicyWaiver.class);
    new PolicyWaiverDAO().delete(waiver);

    grantWritePermission(org.getId());
    waiver = new PolicyWaiver("hash", policy.getId(), null, "comment");

    url = getRestUrl(PolicyWaiverResource.SERVICE_PATH, IdUtils.TYPE_ORGANIZATION, org.getId());
    response = testAuthzPost(url, toJson(waiver));
    waiver = fromJson(response, PolicyWaiver.class);
    new PolicyWaiverDAO().delete(waiver);
  }

  @Test
  public void testDeletePolicyWaiver() throws Exception {
    grantWritePermission(app.getId());

    PolicyWaiver waiver = tempEntity.newWaiver("hash", policy.getId(), app.getId());

    String url = getRestUrl(PolicyWaiverResource.SERVICE_PATH + "/{waiverId}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), waiver.getId());
    testAuthzDelete(url);

    grantWritePermission(org.getId());
    waiver = tempEntity.newWaiver("hash", policy.getId(), org.getId());

    url = getRestUrl(PolicyWaiverResource.SERVICE_PATH + "/{waiverId}", IdUtils.TYPE_ORGANIZATION, org.getId(),
        waiver.getId());
    testAuthzDelete(url);
  }

  @Test
  public void testGetPolicyWaiversByHash() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(PolicyWaiverResource.SERVICE_PATH + "/component/{hash}", IdUtils.TYPE_APPLICATION,
        app.getPublicId(), "hash");
    testAuthzGet(url);

    grantReadPermission(org.getId());

    url = getRestUrl(PolicyWaiverResource.SERVICE_PATH + "/component/{hash}", IdUtils.TYPE_ORGANIZATION, org.getId(),
        "hash");
    testAuthzGet(url);
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    grantReadPermission(app.getId());

    String url = getRestUrl(PolicyWaiverResource.SERVICE_PATH + "/applicable/context/{policyId}",
        IdUtils.TYPE_APPLICATION, app.getPublicId(), policy.getId());
    testAuthzGet(url);
  }
}
