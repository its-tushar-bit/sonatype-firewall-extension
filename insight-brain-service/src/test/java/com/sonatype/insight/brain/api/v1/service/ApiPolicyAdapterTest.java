/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sonatype.insight.brain.api.v1.dto.ApiPolicyDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiPolicyOwnerType;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class ApiPolicyAdapterTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private ApiPolicyAdapter apiPolicyAdapter = new ApiPolicyAdapter();

  @Test
  public void testConvert() {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId(), UUID.randomUUID().toString(), "test-policy-");
    ApiPolicyDTO policyDTO = apiPolicyAdapter.convert(policy, ApiPolicyOwnerType.ORGANIZATION);
    PolicyAssertUtils.assertPolicy(policyDTO, policy, ApiPolicyOwnerType.ORGANIZATION);
  }

  @Test
  public void testConvertList() {
    Map<String, Policy> orgPolicyMap = new HashMap<>();
    Organization organization = tempEntity.newOrganization();
    Policy policy1 = tempEntity.newPolicy(organization.getId(), UUID.randomUUID().toString(), "test-policy-1");
    Policy policy2 = tempEntity.newPolicy(organization.getId(), UUID.randomUUID().toString(), "test-policy-2");
    orgPolicyMap.put(policy1.getId(), policy1);
    orgPolicyMap.put(policy2.getId(), policy2);

    List<ApiPolicyDTO> policyDTOList = apiPolicyAdapter.convert(orgPolicyMap.values(), ApiPolicyOwnerType.ORGANIZATION);
    assertThat(policyDTOList, hasSize(2));
    PolicyAssertUtils.assertPolicies(policyDTOList, orgPolicyMap, Collections.<String, Policy> emptyMap());
  }
}
