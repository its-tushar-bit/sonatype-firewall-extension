/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.Date;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLegacyViolationAdapterTest
{
  @Test
  public void convert_copiesAllFieldsIncludingLegacyViolationTime() {
    Date legacyTime = new Date(1_700_000_000_000L);
    Date openTime = new Date(1_600_000_000_000L);

    PolicyViolation pv = new PolicyViolation();
    pv.setId("pv-1");
    pv.setPolicyId("policy-1");
    pv.setPolicyName("Security-High");
    pv.setThreatLevel(8);
    pv.setOpenTime(openTime);
    pv.setLegacyViolationTime(legacyTime);
    pv.setConstraintFacts(Collections.singletonList(new ConstraintFact()));

    ApiPolicyViolationDTOV2 dto = ApiLegacyViolationAdapter.convert(pv);

    assertThat(dto.policyViolationId).isEqualTo("pv-1");
    assertThat(dto.policyId).isEqualTo("policy-1");
    assertThat(dto.policyName).isEqualTo("Security-High");
    assertThat(dto.threatLevel).isEqualTo(8);
    assertThat(dto.openTime).isEqualTo(openTime);
    assertThat(dto.waiveTime).isNull();
    assertThat(dto.legacyViolationTime).isEqualTo(legacyTime);
    assertThat(dto.constraintViolations).isNotNull();
  }

  @Test
  public void convert_handlesNullWaiveTime() {
    PolicyViolation pv = new PolicyViolation();
    pv.setLegacyViolationTime(new Date());
    pv.setConstraintFacts(Collections.singletonList(new ConstraintFact()));

    ApiPolicyViolationDTOV2 dto = ApiLegacyViolationAdapter.convert(pv);

    assertThat(dto.waiveTime).isNull();
  }
}
