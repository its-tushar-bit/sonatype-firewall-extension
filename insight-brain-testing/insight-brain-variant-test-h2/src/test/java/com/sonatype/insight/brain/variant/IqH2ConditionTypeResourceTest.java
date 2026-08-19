/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.policy.conditions.DeprecatedSecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.policy.ConditionTypeResource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
@IqH2Test
class IqH2ConditionTypeResourceTest
{
  private IqTestContext ctx;

  @Test
  void testGetConditionTypes() throws Exception {
    final HttpResponse response = ctx.restRequest().path(ConditionTypeResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);
    final Object[] conditionTypes = response.getBody(Object[].class);
    assertThat(conditionTypes).isNotEmpty().allSatisfy(conditionType -> {
      assertThat(conditionType).isInstanceOf(Map.class);
      assertThat(((Map<?, ?>) conditionType).get("id")).isNotNull()
          .isNotEqualTo(DeprecatedSecurityVulnerabilityConditionType.ID);
    });
  }
}
