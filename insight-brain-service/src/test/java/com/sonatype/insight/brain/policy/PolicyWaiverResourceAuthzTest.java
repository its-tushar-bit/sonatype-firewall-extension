/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Collections;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Before;
import org.junit.Test;

public class PolicyWaiverResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  private Policy policy;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PolicyWaiverResource.RESOURCE_PATH);
  }

  @Before
  public void init() {
    policy = tempEntity.newPolicy(app);
  }

  @Test
  public void testAddPolicyWaiver() throws Exception {
    grantPermission(app.getId(), Permission.WAIVE_POLICY_VIOLATIONS);

    HttpRequest request = restRequest().body(new PolicyWaiver("hash", policy.getId(), null,
        Collections.singletonList(new ConstraintFact("id", "name", "operator")), "comment"));
    HttpResponse response = testAuthzPost(request.parameter(OwnerType.APPLICATION, app.getPublicId()));
    new PolicyWaiverDAO().delete(response.getBody(PolicyWaiver.class));

    grantPermission(org.getId(), Permission.WAIVE_POLICY_VIOLATIONS);

    request.body(new PolicyWaiver("hash", policy.getId(), null,
        Collections.singletonList(new ConstraintFact("id", "name", "operator")), "comment"));
    response = testAuthzPost(request.parameter(OwnerType.ORGANIZATION, org.getId()));
    new PolicyWaiverDAO().delete(response.getBody(PolicyWaiver.class));
  }

  @Test
  public void testGetPolicyWaiversByHash() throws Exception {
    HttpRequest request = restRequest().path("component/hash");

    grantReadPermission(app.getId());

    testAuthzGet(request.parameter(OwnerType.APPLICATION, app.getPublicId()));

    grantReadPermission(org.getId());

    testAuthzGet(request.parameter(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetApplicableContexts() throws Exception {
    grantReadPermission(app.getId());

    testAuthzGet(restRequest().path("applicable/context/{policyId}").parameter(OwnerType.APPLICATION,
        app.getPublicId(), policy.getId()));
  }
}
