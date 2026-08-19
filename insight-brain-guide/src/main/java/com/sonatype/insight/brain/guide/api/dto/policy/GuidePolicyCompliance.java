/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto.policy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuidePolicyCompliance(
    boolean compliant,
    GuidePolicyComplianceLevel complianceLevel,
    String stage,
    String ownerId,
    GuidePolicyComplianceSummary summary,
    List<GuidePolicyViolation> violations)
{
  /**
   * Badge-only compliance: the universal {@code compliant} flag plus the {@code complianceLevel}
   * that drives the green/amber/red badge. The detail card ({@code stage}/{@code ownerId}/{@code
   * summary}/{@code violations}) is left null and omitted by the class-level {@link
   * JsonInclude.Include#NON_NULL}, so this serializes to {@code
   * {"compliant":<bool>,"complianceLevel":"<LEVEL>"}}.
   *
   * <p>
   * Used for list/badge surfaces and for callers without {@code EVALUATE_COMPONENT} (who see the
   * badge but not the card). {@code compliant} is derived as {@code level != FAIL}.
   */
  public static GuidePolicyCompliance badge(GuidePolicyComplianceLevel complianceLevel) {
    return new GuidePolicyCompliance(
        complianceLevel != GuidePolicyComplianceLevel.FAIL, complianceLevel, null, null, null, null);
  }
}
