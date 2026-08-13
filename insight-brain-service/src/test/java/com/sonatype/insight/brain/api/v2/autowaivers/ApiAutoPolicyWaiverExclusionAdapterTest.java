/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import java.util.Date;

import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionResponseDTO;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiAutoPolicyWaiverExclusionAdapterTest
{
  @Test
  public void testConvertToDTO_nullValue() {
    final ApiAutoPolicyWaiverExclusionResponseDTO apiAutoPolicyWaiverExclusionDTO =
        ApiAutoPolicyWaiverExclusionAdapter.convertToDTO(null);
    assertThat(apiAutoPolicyWaiverExclusionDTO).isNull();
  }

  @Test
  public void testConvertToDTO() {
    final AutoPolicyWaiverExclusion autoPolicyWaiverExclusion = new AutoPolicyWaiverExclusion();
    autoPolicyWaiverExclusion.setId("waiverExclusionId");
    autoPolicyWaiverExclusion.setOwnerId("ownerId");
    autoPolicyWaiverExclusion.setCreatorId("creatorId");
    autoPolicyWaiverExclusion.setCreatorName("creatorName");
    autoPolicyWaiverExclusion.setCreateTime(new Date());
    autoPolicyWaiverExclusion.setAutoPolicyWaiverId("waiverId");
    autoPolicyWaiverExclusion.setHash("hash");
    autoPolicyWaiverExclusion.setAssociatedPackageUrl("pkg:maven/org.apache.commons/commons-lang3@3.9");
    autoPolicyWaiverExclusion.setScanId("scanId");
    autoPolicyWaiverExclusion.setComponentMatchStrategy(ComponentMatcherStrategyForExclusion.EXACT_COMPONENT);
    autoPolicyWaiverExclusion.setPolicyViolationId("fakeViolationId");
    autoPolicyWaiverExclusion.setThreatLevel(8);
    autoPolicyWaiverExclusion.setPolicyName("policyName");
    autoPolicyWaiverExclusion.setComponentDisplayName("componentDisplayName");
    autoPolicyWaiverExclusion.setVulnerabilityIdentifiers("vulnerabilityIdentifiers");
    autoPolicyWaiverExclusion.setPolicyId("policyId");

    ApiAutoPolicyWaiverExclusionResponseDTO apiAutoPolicyWaiverExclusionDTO =
        ApiAutoPolicyWaiverExclusionAdapter.convertToDTO(autoPolicyWaiverExclusion);
    assertThat(apiAutoPolicyWaiverExclusionDTO).isNotNull();
    assertThat(apiAutoPolicyWaiverExclusionDTO.autoPolicyWaiverExclusionId).isEqualTo(
        autoPolicyWaiverExclusion.getId());
    assertThat(apiAutoPolicyWaiverExclusionDTO.ownerId).isEqualTo(autoPolicyWaiverExclusion.getOwnerId());
    assertThat(apiAutoPolicyWaiverExclusionDTO.creatorId).isEqualTo(autoPolicyWaiverExclusion.getCreatorId());
    assertThat(apiAutoPolicyWaiverExclusionDTO.creatorName).isEqualTo(autoPolicyWaiverExclusion.getCreatorName());
    assertThat(apiAutoPolicyWaiverExclusionDTO.createTime).isEqualTo(autoPolicyWaiverExclusion.getCreateTime());
    assertThat(apiAutoPolicyWaiverExclusionDTO.autoPolicyWaiverId).isEqualTo(
        autoPolicyWaiverExclusion.getAutoPolicyWaiverId());
    assertThat(apiAutoPolicyWaiverExclusionDTO.hash).isEqualTo(autoPolicyWaiverExclusion.getHash());
    assertThat(apiAutoPolicyWaiverExclusionDTO.scanId).isEqualTo(autoPolicyWaiverExclusion.getScanId());
    assertThat(apiAutoPolicyWaiverExclusionDTO.componentMatchStrategy).isEqualTo(
        autoPolicyWaiverExclusion.getComponentMatchStrategy());
    assertThat(apiAutoPolicyWaiverExclusionDTO.policyViolationId).isEqualTo(
        autoPolicyWaiverExclusion.getPolicyViolationId());
    assertThat(apiAutoPolicyWaiverExclusionDTO.threatLevel).isEqualTo(autoPolicyWaiverExclusion.getThreatLevel());
    assertThat(apiAutoPolicyWaiverExclusionDTO.policyName).isEqualTo(autoPolicyWaiverExclusion.getPolicyName());
    assertThat(apiAutoPolicyWaiverExclusionDTO.componentDisplayName).isEqualTo(
        autoPolicyWaiverExclusion.getComponentDisplayName());
    assertThat(apiAutoPolicyWaiverExclusionDTO.vulnerabilityIdentifiers).isEqualTo(
        autoPolicyWaiverExclusion.getVulnerabilityIdentifiers());
    assertThat(apiAutoPolicyWaiverExclusionDTO.policyId).isEqualTo(autoPolicyWaiverExclusion.getPolicyId());
  }
}
