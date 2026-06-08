/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.mcp.model;

import java.util.List;

public record McpPolicyViolation(
    String policyName,
    int threatLevel,
    String actionType,
    List<String> reasons,
    boolean waived)
{
}
