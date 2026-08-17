/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyOwnerType;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyAdapterTest
    extends AbstractDataTest
{
  @Test
  public void testConvert_SinglePolicy() {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization);
    ApiPolicyDTO policyDTO = ApiPolicyAdapter.convert(policy, ApiPolicyOwnerType.ORGANIZATION);
    PolicyAssertUtils.assertPolicy(policyDTO, policy, ApiPolicyOwnerType.ORGANIZATION);
  }

  @Test
  public void testConvertList_ListOfPolicies() {
    Map<String, Policy> orgPolicyMap = new HashMap<>();
    Organization organization = tempEntity.newOrganization();
    Policy policy1 = tempEntity.newPolicy(organization);
    Policy policy2 = tempEntity.newPolicy(organization);
    orgPolicyMap.put(policy1.getId(), policy1);
    orgPolicyMap.put(policy2.getId(), policy2);

    List<ApiPolicyDTO> policyDTOList = ApiPolicyAdapter.convert(orgPolicyMap.values(), ApiPolicyOwnerType.ORGANIZATION);
    assertThat(policyDTOList).hasSize(2);
    PolicyAssertUtils.assertPolicies(policyDTOList, orgPolicyMap, Collections.emptyMap());
  }
}
