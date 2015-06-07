/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.HttpRequest;
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

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PolicyWaiverResource.SERVICE_PATH);
  }

  @Before
  public void init() {
    policy = tempEntity.newPolicy(app.getId(), "Test Policy");
  }

  @Test
  public void testAddPolicyWaiver() throws Exception {
    grantWritePermission(app.getId());

    HttpRequest request = restRequest().body(new PolicyWaiver("hash", policy.getId(), null, "comment"));
    Response response = testAuthzPost(request.parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()));
    new PolicyWaiverDAO().delete(fromJson(response, PolicyWaiver.class));

    grantWritePermission(org.getId());

    request.body(new PolicyWaiver("hash", policy.getId(), null, "comment"));
    response = testAuthzPost(request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId()));
    new PolicyWaiverDAO().delete(fromJson(response, PolicyWaiver.class));
  }

  @Test
  public void testDeletePolicyWaiver() throws Exception {
    HttpRequest request = restRequest().path("{waiverId}");

    grantWritePermission(app.getId());
    PolicyWaiver waiver = tempEntity.newWaiver("hash", policy.getId(), app.getId());
    testAuthzDelete(request.parameter(IdUtils.TYPE_APPLICATION, app.getPublicId(), waiver.getId()));

    grantWritePermission(org.getId());
    waiver = tempEntity.newWaiver("hash", policy.getId(), org.getId());
    testAuthzDelete(request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId(), waiver.getId()));
  }

  @Test
  public void testGetPolicyWaiversByHash() throws Exception {
    HttpRequest request = restRequest().path("component/hash");

    grantReadPermission(app.getId());

    testAuthzGet(request.parameter(IdUtils.TYPE_APPLICATION, app.getPublicId()));

    grantReadPermission(org.getId());

    testAuthzGet(request.parameter(IdUtils.TYPE_ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    grantReadPermission(app.getId());

    testAuthzGet(restRequest().path("applicable/context/{policyId}").parameter(IdUtils.TYPE_APPLICATION,
        app.getPublicId(), policy.getId()));
  }
}
