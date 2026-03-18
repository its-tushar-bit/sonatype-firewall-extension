/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;

import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyWaiverDTOTestUtils
{
  public static void assertApiPolicyWaiverDTO(
      String hash,
      String policyId,
      String ownerId,
      String ownerName,
      String comment,
      String policyViolationId,
      final Date expiryTime,
      String creatorId,
      String creatorName,
      ComponentMatcherStrategyForWaiver matcherStrategy,
      String associatedPackageUrl,
      String reasonText,
      String policyWaiverReasonId,
      ApiPolicyWaiverDTO actual)
  {
    assertThat(actual.hash).isEqualTo(hash);
    assertThat(actual.policyId).isEqualTo(policyId);
    assertThat(actual.scopeOwnerId).isEqualTo(ownerId);
    assertThat(actual.scopeOwnerName).isEqualTo(ownerName);
    assertThat(actual.comment).isEqualTo(comment);
    assertThat(actual.expiryTime).isEqualTo(expiryTime);
    assertThat(actual.policyViolationId).isEqualTo(policyViolationId);
    assertThat(actual.creatorId).isNotNull();
    assertThat(actual.creatorId).isEqualTo(creatorId);
    assertThat(actual.matcherStrategy).isEqualTo(matcherStrategy);
    assertThat(actual.creatorName).isEqualTo(creatorName);
    assertThat(actual.associatedPackageUrl).isEqualTo(associatedPackageUrl);
    assertThat(actual.reasonText).isEqualTo(reasonText);
    assertThat(actual.policyWaiverReasonId).isEqualTo(policyWaiverReasonId);
  }
}
