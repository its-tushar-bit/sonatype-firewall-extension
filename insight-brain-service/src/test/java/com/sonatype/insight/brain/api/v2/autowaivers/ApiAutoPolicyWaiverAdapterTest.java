/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import java.util.Date;

import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiAutoPolicyWaiverAdapterTest
{
  @Test
  public void testConvertToDTO_nullValue() {
    final ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = ApiAutoPolicyWaiverAdapter.convertToDTO(null);
    assertThat(apiAutoPolicyWaiverDTO).isNull();
  }

  @Test
  public void testConvertToDTO() {
    final AutoPolicyWaiver autoPolicyWaiver = new AutoPolicyWaiver();
    autoPolicyWaiver.setId("waiverId");
    autoPolicyWaiver.setOwnerId("ownerId");
    autoPolicyWaiver.setThreatLevel(7);
    autoPolicyWaiver.setReachability(true);
    autoPolicyWaiver.setPathForward(false);
    autoPolicyWaiver.setCreatorId("creatorId");
    autoPolicyWaiver.setCreatorName("creatorName");
    autoPolicyWaiver.setCreateTime(new Date());
    autoPolicyWaiver.setScopesOperatorAny(false);

    ApiAutoPolicyWaiverDTO apiAutoPolicyWaiverDTO = ApiAutoPolicyWaiverAdapter.convertToDTO(autoPolicyWaiver);
    assertThat(apiAutoPolicyWaiverDTO).isNotNull();
    assertThat(apiAutoPolicyWaiverDTO.autoPolicyWaiverId).isEqualTo(autoPolicyWaiver.getId());
    assertThat(apiAutoPolicyWaiverDTO.ownerId).isEqualTo(autoPolicyWaiver.getOwnerId());
    assertThat(apiAutoPolicyWaiverDTO.threatLevel).isEqualTo(autoPolicyWaiver.getThreatLevel());
    assertThat(apiAutoPolicyWaiverDTO.reachability).isEqualTo(autoPolicyWaiver.hasReachability());
    assertThat(apiAutoPolicyWaiverDTO.pathForward).isEqualTo(autoPolicyWaiver.hasPathForward());
    assertThat(apiAutoPolicyWaiverDTO.creatorId).isEqualTo(autoPolicyWaiver.getCreatorId());
    assertThat(apiAutoPolicyWaiverDTO.creatorName).isEqualTo(autoPolicyWaiver.getCreatorName());
    assertThat(apiAutoPolicyWaiverDTO.createTime).isEqualTo(autoPolicyWaiver.getCreateTime());
    assertThat(apiAutoPolicyWaiverDTO.scopesOperatorAny).isEqualTo(autoPolicyWaiver.getScopesOperatorAny());
  }
}
