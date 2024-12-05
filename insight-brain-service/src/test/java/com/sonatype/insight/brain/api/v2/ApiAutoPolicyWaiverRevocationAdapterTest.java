/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;

import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationResponseDTO;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiAutoPolicyWaiverRevocationAdapterTest
{
  @Test
  public void testConvertToDTO_nullValue() {
    final ApiAutoPolicyWaiverRevocationResponseDTO apiAutoPolicyWaiverRevocationDTO =
        ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(null);
    assertThat(apiAutoPolicyWaiverRevocationDTO).isNull();
  }

  @Test
  public void testConvertToDTO() {
    final AutoPolicyWaiverRevocation autoPolicyWaiverRevocation = new AutoPolicyWaiverRevocation();
    autoPolicyWaiverRevocation.setId("waiverRevocationId");
    autoPolicyWaiverRevocation.setOwnerId("ownerId");
    autoPolicyWaiverRevocation.setCreatorId("creatorId");
    autoPolicyWaiverRevocation.setCreatorName("creatorName");
    autoPolicyWaiverRevocation.setCreateTime(new Date());
    autoPolicyWaiverRevocation.setAutoPolicyWaiverId("waiverId");
    autoPolicyWaiverRevocation.setHash("hash");
    autoPolicyWaiverRevocation.setAssociatedPackageUrl("pkg:maven/org.apache.commons/commons-lang3@3.9");
    autoPolicyWaiverRevocation.setScanId("scanId");
    autoPolicyWaiverRevocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.EXACT_COMPONENT);
    autoPolicyWaiverRevocation.setPolicyViolationId("fakeViolationId");
    autoPolicyWaiverRevocation.setThreatLevel(8);
    autoPolicyWaiverRevocation.setPolicyName("policyName");
    autoPolicyWaiverRevocation.setComponentDisplayName("componentDisplayName");
    autoPolicyWaiverRevocation.setVulnerabilityIdentifiers("vulnerabilityIdentifiers");
    autoPolicyWaiverRevocation.setPolicyId("policyId");

    ApiAutoPolicyWaiverRevocationResponseDTO apiAutoPolicyWaiverRevocationDTO =
        ApiAutoPolicyWaiverRevocationAdapter.convertToDTO(autoPolicyWaiverRevocation);
    assertThat(apiAutoPolicyWaiverRevocationDTO).isNotNull();
    assertThat(apiAutoPolicyWaiverRevocationDTO.autoPolicyWaiverRevocationId).isEqualTo(
        autoPolicyWaiverRevocation.getId());
    assertThat(apiAutoPolicyWaiverRevocationDTO.ownerId).isEqualTo(autoPolicyWaiverRevocation.getOwnerId());
    assertThat(apiAutoPolicyWaiverRevocationDTO.creatorId).isEqualTo(autoPolicyWaiverRevocation.getCreatorId());
    assertThat(apiAutoPolicyWaiverRevocationDTO.creatorName).isEqualTo(autoPolicyWaiverRevocation.getCreatorName());
    assertThat(apiAutoPolicyWaiverRevocationDTO.createTime).isEqualTo(autoPolicyWaiverRevocation.getCreateTime());
    assertThat(apiAutoPolicyWaiverRevocationDTO.autoPolicyWaiverId).isEqualTo(
        autoPolicyWaiverRevocation.getAutoPolicyWaiverId());
    assertThat(apiAutoPolicyWaiverRevocationDTO.hash).isEqualTo(autoPolicyWaiverRevocation.getHash());
    assertThat(apiAutoPolicyWaiverRevocationDTO.scanId).isEqualTo(autoPolicyWaiverRevocation.getScanId());
    assertThat(apiAutoPolicyWaiverRevocationDTO.componentMatchStrategy).isEqualTo(
        autoPolicyWaiverRevocation.getComponentMatchStrategy());
    assertThat(apiAutoPolicyWaiverRevocationDTO.policyViolationId).isEqualTo(
        autoPolicyWaiverRevocation.getPolicyViolationId());
    assertThat(apiAutoPolicyWaiverRevocationDTO.threatLevel).isEqualTo(autoPolicyWaiverRevocation.getThreatLevel());
    assertThat(apiAutoPolicyWaiverRevocationDTO.policyName).isEqualTo(autoPolicyWaiverRevocation.getPolicyName());
    assertThat(apiAutoPolicyWaiverRevocationDTO.componentDisplayName).isEqualTo(
        autoPolicyWaiverRevocation.getComponentDisplayName());
    assertThat(apiAutoPolicyWaiverRevocationDTO.vulnerabilityIdentifiers).isEqualTo(
        autoPolicyWaiverRevocation.getVulnerabilityIdentifiers());
    assertThat(apiAutoPolicyWaiverRevocationDTO.policyId).isEqualTo(autoPolicyWaiverRevocation.getPolicyId());
  }
}
