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
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
public class ApiPolicyResourceV2Test
{
  private IqTestContext ctx;

  private final Map<String, Policy> organizationPolicyMap = new HashMap<>();

  private final Map<String, Policy> applicationPolicyMap = new HashMap<>();

  private HttpRequest restRequest() {
    return ctx.restRequest().path(PublicApiPaths.POLICY_RESOURCE_PATH);
  }

  @BeforeEach
  public void setUp() {
    for (int i = 0; i < 2; i++) {
      Organization organization = ctx.tempEntity().newOrganization();
      Policy policy = ctx.tempEntity().newPolicy(organization);
      organizationPolicyMap.put(policy.getId(), policy);

      Application application = ctx.tempEntity().newApplication(organization.getId());
      policy = ctx.tempEntity().newPolicy(application);
      applicationPolicyMap.put(policy.getId(), policy);
    }
  }

  @Test
  public void testGetPolicies() throws Exception {
    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);
    ApiPolicyListDTO apiPolicyListDTO = response.getBody(ApiPolicyListDTO.class);

    assertThat(apiPolicyListDTO).isNotNull();
    assertThat(apiPolicyListDTO.policies).isNotNull();
    assertThat(apiPolicyListDTO.policies).hasSize(organizationPolicyMap.size() + applicationPolicyMap.size());
    PolicyAssertUtils.assertPolicies(apiPolicyListDTO.policies, organizationPolicyMap, applicationPolicyMap);
  }
}
