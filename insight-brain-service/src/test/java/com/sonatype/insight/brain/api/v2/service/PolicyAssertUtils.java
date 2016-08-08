/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyOwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class PolicyAssertUtils
{
  public static void assertPolicies(List<ApiPolicyDTO> actualPolicyList,
                                    Map<String, Policy> expectedOrgPolicies,
                                    Map<String, Policy> expectedAppPolicies)
  {
    for (ApiPolicyDTO actualPolicy : actualPolicyList) {
      if (expectedAppPolicies.get(actualPolicy.id) != null) {
        assertPolicy(actualPolicy, expectedAppPolicies.get(actualPolicy.id), ApiPolicyOwnerType.APPLICATION);
      }
      else if (expectedOrgPolicies.get(actualPolicy.id) != null) {
        assertPolicy(actualPolicy, expectedOrgPolicies.get(actualPolicy.id), ApiPolicyOwnerType.ORGANIZATION);
      }
      else {
        assertThat("Policy " + actualPolicy + " not found", false);
      }
    }
  }

  public static void assertPolicy(ApiPolicyDTO actualPolicy, Policy expectedPolicy, ApiPolicyOwnerType ownerType) {
    assertThat(actualPolicy, notNullValue());
    assertThat(actualPolicy.id, is(expectedPolicy.getId()));
    assertThat(actualPolicy.name, is(expectedPolicy.getName()));
    assertThat(actualPolicy.ownerId, is(expectedPolicy.getOwnerId()));
    assertThat(actualPolicy.ownerType, is(ownerType));
    assertThat(actualPolicy.threatLevel, is(expectedPolicy.getThreatLevel()));
    assertThat(PolicyThreatCategory.getByName(actualPolicy.policyType), is(expectedPolicy.getThreatCategory()));
  }
}
