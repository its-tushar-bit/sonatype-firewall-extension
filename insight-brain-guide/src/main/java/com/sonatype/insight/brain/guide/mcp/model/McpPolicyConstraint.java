/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.mcp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single constraint within an MCP {@link McpPolicyViolation}. The slim MCP shape keeps the
 * constraint's human-readable {@code constraintName} and its reasons as plain strings, dropping the
 * API shape's {@code constraintId} and the per-reason structured {@code reference} (the triggering
 * value, e.g. a CVE id, remains inline in the reason text). Grouping reasons under their named
 * constraint preserves which reasons belong together when a policy has more than one constraint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpPolicyConstraint(
    String constraintName,
    List<String> reasons)
{
}
