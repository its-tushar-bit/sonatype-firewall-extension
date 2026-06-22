/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.mcp.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sonatype.insight.brain.guide.api.dto.policy.GuideConstraintViolation;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyCompliance;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceLevel;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyComplianceSummary;
import com.sonatype.insight.brain.guide.api.dto.policy.GuidePolicyViolation;
import com.sonatype.insight.brain.guide.api.dto.policy.GuideViolationReason;

/**
 * Policy compliance in the MCP response — a slim projection of the API's {@link
 * GuidePolicyCompliance}. The envelope ({@code compliant}/{@code stage}/{@code ownerId}/{@code
 * summary}) is unchanged and the {@link GuidePolicyComplianceSummary} is reused verbatim, but each
 * violation is projected to the leaner {@link McpPolicyViolation} (no {@code policyId}; {@code
 * constraintViolations} reduced to {@link McpPolicyConstraint} name + plain-string reasons).
 *
 * <p>
 * The API surface keeps the full {@link GuidePolicyCompliance} shape; this slimmer shape is
 * MCP-only, tuned for an LLM consumer that wants the violation reasons without the IQ-internal
 * identifiers and nesting.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpPolicyCompliance(
    boolean compliant,
    GuidePolicyComplianceLevel complianceLevel,
    String stage,
    String ownerId,
    GuidePolicyComplianceSummary summary,
    List<McpPolicyViolation> violations)
{
  /**
   * Project a full API {@link GuidePolicyCompliance} into the slim MCP shape (badge + detail card).
   * Returns {@code null} for a {@code null} input so soft-failed evaluations stay soft-failed.
   */
  public static McpPolicyCompliance from(GuidePolicyCompliance compliance) {
    if (compliance == null) {
      return null;
    }
    List<McpPolicyViolation> violations = new ArrayList<>();
    if (compliance.violations() != null) {
      for (GuidePolicyViolation v : compliance.violations()) {
        violations.add(new McpPolicyViolation(
            v.policyName(), v.threatLevel(), v.actions(), v.waived(), v.waiver(),
            toConstraints(v.constraintViolations())));
      }
    }
    return new McpPolicyCompliance(
        compliance.compliant(), compliance.complianceLevel(), compliance.stage(), compliance.ownerId(),
        compliance.summary(), violations);
  }

  /**
   * Badge-only projection: the universal {@code compliant} flag + {@code complianceLevel}, with the
   * detail card omitted. Returned to callers lacking {@code EVALUATE_COMPONENT} (badge without
   * detail). Returns {@code null} for a {@code null} input.
   */
  public static McpPolicyCompliance badge(GuidePolicyCompliance compliance) {
    if (compliance == null) {
      return null;
    }
    return new McpPolicyCompliance(
        compliance.compliant(), compliance.complianceLevel(), null, null, null, null);
  }

  private static List<McpPolicyConstraint> toConstraints(List<GuideConstraintViolation> constraintViolations) {
    if (constraintViolations == null) {
      return null;
    }
    List<McpPolicyConstraint> constraints = new ArrayList<>();
    for (GuideConstraintViolation cv : constraintViolations) {
      constraints.add(new McpPolicyConstraint(cv.constraintName(), reasonStrings(cv.reasons())));
    }
    return constraints;
  }

  private static List<String> reasonStrings(List<GuideViolationReason> reasons) {
    if (reasons == null) {
      return null;
    }
    List<String> out = new ArrayList<>();
    for (GuideViolationReason r : reasons) {
      if (r.reason() != null) {
        out.add(r.reason());
      }
    }
    return out;
  }
}
