/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.mcp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sonatype.insight.brain.guide.api.dto.policy.GuideWaiverInfo;

/**
 * A single policy violation in the MCP response — the slim projection of the API's
 * {@code GuidePolicyViolation}. Drops the API shape's {@code policyId} and replaces the nested
 * {@code constraintViolations} (with their {@code constraintId} and structured reason references)
 * with {@link McpPolicyConstraint} entries that keep only the constraint name and plain-string
 * reasons. {@code actions} are the lowercase IQ action ids (e.g. {@code "fail"}, {@code "warn"});
 * {@code waiver} is present only when the violation is waived.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpPolicyViolation(
    String policyName,
    int threatLevel,
    List<String> actions,
    boolean waived,
    GuideWaiverInfo waiver,
    List<McpPolicyConstraint> constraints)
{
}
