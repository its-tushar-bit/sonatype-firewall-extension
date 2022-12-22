/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyListDTO;
import com.sonatype.insight.brain.api.v2.service.PolicyAssertUtils;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyResourceV2Test
    extends AbstractResourceTest
{
  private final Map<String, Policy> organizationPolicyMap = new HashMap<>();

  private final Map<String, Policy> applicationPolicyMap = new HashMap<>();

  @Before
  public void setUp() {
    for (int i = 0; i < 2; i++) {
      Organization organization = tempEntity.newOrganization();
      Policy policy = tempEntity.newPolicy(organization);
      organizationPolicyMap.put(policy.getId(), policy);

      Application application = tempEntity.newApplication(organization.getId());
      policy = tempEntity.newPolicy(application);
      applicationPolicyMap.put(policy.getId(), policy);
    }
  }

  @Test
  public void testGetPolicies() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    ApiPolicyListDTO apiPolicyListDTO = response.getBody(ApiPolicyListDTO.class);

    assertThat(apiPolicyListDTO).isNotNull();
    assertThat(apiPolicyListDTO.policies).isNotNull();
    assertThat(apiPolicyListDTO.policies).hasSize(organizationPolicyMap.size() + applicationPolicyMap.size());
    PolicyAssertUtils.assertPolicies(apiPolicyListDTO.policies, organizationPolicyMap, applicationPolicyMap);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.POLICY_RESOURCE_PATH);
  }
}
