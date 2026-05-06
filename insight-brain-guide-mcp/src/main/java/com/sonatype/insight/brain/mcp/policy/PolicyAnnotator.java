/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp.policy;

import com.sonatype.insight.brain.mcp.model.McpPolicyContext;

/**
 * Evaluates a component against IQ policies for a given application and lifecycle stage.
 * Returns null if applicationId is not provided or if evaluation fails (graceful degradation).
 */
public interface PolicyAnnotator
{
  McpPolicyContext evaluatePolicy(String purl, String applicationId, String stage);
}
