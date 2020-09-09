/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyWaiverDTOTestUtils
{
  public static void assertApiPolicyWaiverDTO(String hash,
                                              String policyId,
                                              String ownerId,
                                              String ownerName,
                                              String comment,
                                              ApiPolicyWaiverDTO actual)
  {
    assertThat(actual)
        .extracting("hash", "policyId", "scopeOwnerId", "scopeOwnerName", "comment")
        .containsExactly(hash, policyId, ownerId, ownerName, comment);
  }
}
