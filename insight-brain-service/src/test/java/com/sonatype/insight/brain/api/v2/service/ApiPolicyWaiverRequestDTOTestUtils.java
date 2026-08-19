/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverRequestDTO;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyWaiverRequestDTOTestUtils
{
  public static void assertPolicyWaiverRequestDTO(
      ApiPolicyWaiverRequestDTO policyWaiverRequestDTO,
      PolicyWaiverRequest policyWaiverRequest)
  {
    assertThat(policyWaiverRequestDTO.policyWaiverRequestId).isEqualTo(policyWaiverRequest.getId());
    assertThat(policyWaiverRequestDTO.scopeOwnerId).isEqualTo(policyWaiverRequest.getOwnerId());
    assertThat(policyWaiverRequestDTO.hash).isEqualTo(policyWaiverRequest.getHash());
    assertThat(policyWaiverRequestDTO.comment).isEqualTo(policyWaiverRequest.getComment());
    assertThat(policyWaiverRequestDTO.policyId).isEqualTo(policyWaiverRequest.getPolicyId());
    assertThat(policyWaiverRequestDTO.requestTime).isEqualTo(policyWaiverRequest.getRequestTime());
    assertThat(policyWaiverRequestDTO.expiryTime).isEqualTo(policyWaiverRequest.getExpiryTime());
    assertThat(policyWaiverRequestDTO.requesterId).isEqualTo(policyWaiverRequest.getRequesterId());
    assertThat(policyWaiverRequestDTO.requesterName).isEqualTo(policyWaiverRequest.getRequesterName());
    assertThat(policyWaiverRequestDTO.constraintFactsJson).isEqualTo(policyWaiverRequest.getConstraintFactsJson());
    assertThat(policyWaiverRequestDTO.matcherStrategy).isEqualTo(policyWaiverRequest.getComponentMatchStrategy());
    assertThat(policyWaiverRequestDTO.associatedPackageUrl).isEqualTo(policyWaiverRequest.getAssociatedPackageUrl());
    assertThat(policyWaiverRequestDTO.policyWaiverReasonId).isEqualTo(policyWaiverRequest.getWaiverReasonId());
    assertThat(policyWaiverRequestDTO.expireWhenRemediationAvailable)
        .isEqualTo(policyWaiverRequest.isExpireWhenRemediationAvailable());
    assertThat(policyWaiverRequestDTO.policyViolationId).isEqualTo(policyWaiverRequest.getPolicyViolationId());
    assertThat(policyWaiverRequestDTO.status).isEqualTo(policyWaiverRequest.getStatus().name());
  }
}
