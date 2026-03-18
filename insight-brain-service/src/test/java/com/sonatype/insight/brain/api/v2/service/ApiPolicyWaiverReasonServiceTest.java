/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverReasonDTO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyWaiverReasonServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiPolicyWaiverReasonService apiPolicyWaiverReasonService;

  @Test
  public void testGetAllPolicyWaiverReasons_shouldReturnAllPolicyWaiverReasons() {
    final String givenSomeOtherReason = TemporaryEntity.uuid();
    final PolicyWaiverReason additionalWaiverReason = tempEntity.newWaiverReason("user", givenSomeOtherReason);

    final var results = apiPolicyWaiverReasonService.getAllPolicyWaiverReasons();

    assertThat(results).containsExactlyInAnyOrder(
        new ApiPolicyWaiverReasonDTO("9b704ef5bc064fc29d7fe08a251ee9a6", "system", "Acknowledged violation"),
        new ApiPolicyWaiverReasonDTO("ab704ef5bc064fc29d7fe08a251ee9aa", "system", "Evaluating component"),
        new ApiPolicyWaiverReasonDTO("42069f58114f4df8b435a40a415d2835", "system", "Mitigated externally"),
        new ApiPolicyWaiverReasonDTO("39984de3d6e64f508df82b4cbfd72f70", "system", "No upgrade path"),
        new ApiPolicyWaiverReasonDTO("f6990a32cd8d4ea78853ca829d948927", "system", "Not exploitable"),
        new ApiPolicyWaiverReasonDTO("19bbf1a7d591497698ab3172461d971a", "system", "Not reachable"),
        new ApiPolicyWaiverReasonDTO("3446e70e60e04676a90131f3dea9bdb5", "system", "Researching"),
        new ApiPolicyWaiverReasonDTO("c991ef95866d4903ad0c6c217ac47c07", "system", "Other"),
        ApiPolicyWaiverReasonDTO.fromPolicyWaiverReason(additionalWaiverReason));
  }
}
