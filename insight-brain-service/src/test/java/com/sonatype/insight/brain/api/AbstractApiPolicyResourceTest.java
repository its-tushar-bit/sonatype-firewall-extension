/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.api.v1.dto.ApiPolicyListDTO;
import com.sonatype.insight.brain.api.v1.service.PolicyAssertUtils;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public abstract class AbstractApiPolicyResourceTest
    extends AbstractResourceTest
{
  private Map<String, Policy> organizationPolicyMap = new HashMap<>();

  private Map<String, Policy> applicationPolicyMap = new HashMap<>();

  abstract protected String getServiceURL();

  @Before
  public void setUp() throws Exception {
    for (int i = 0; i < 2; i++) {
      Organization organization = tempEntity.newOrganization();
      Policy policy = tempEntity.newPolicy(organization.getId(), organization.getName() + "-policy");
      organizationPolicyMap.put(policy.getId(), policy);

      Application application = tempEntity.newApplication(organization.getId());
      policy = tempEntity.newPolicy(application.getId(), application.getName() + "-policy");
      applicationPolicyMap.put(policy.getId(), policy);
    }
  }

  @Test
  public void testGetPolicies() throws Exception {
    Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    ApiPolicyListDTO apiPolicyListDTO = JsonHelpers.fromJson(response.getResponseBody(), ApiPolicyListDTO.class);

    assertThat(apiPolicyListDTO, notNullValue());
    assertThat(apiPolicyListDTO.policies, notNullValue());
    assertThat(apiPolicyListDTO.policies.size(), is(organizationPolicyMap.size() + applicationPolicyMap.size()));
    PolicyAssertUtils.assertPolicies(apiPolicyListDTO.policies, organizationPolicyMap, applicationPolicyMap);
  }
}
