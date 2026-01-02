/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Before;
import org.junit.Test;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
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
