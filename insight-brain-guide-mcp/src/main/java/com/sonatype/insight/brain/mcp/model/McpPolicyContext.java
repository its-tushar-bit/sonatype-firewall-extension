/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp.model;

import java.util.List;

public record McpPolicyContext(
    String applicationId,
    String stage,
    McpStageResult stageResult,
    McpStagePreview stagePreview,
    List<McpPolicyViolation> violations)
{
}
