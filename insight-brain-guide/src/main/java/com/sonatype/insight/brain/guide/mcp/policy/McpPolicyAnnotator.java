/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.mcp.policy;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.guide.mcp.model.McpPolicyCompliance;

/**
 * Evaluates a batch of components against IQ policies for a given application/owner and lifecycle
 * stage, in a single evaluation (one owner resolution, policy/waiver fetch, and Drools session for
 * the whole batch — the MCP path can submit up to {@code MAX_BATCH_SIZE} PURLs at once).
 *
 * <p>
 * Returns a map keyed by the caller's input PURL strings; each value is the slim, MCP-specific
 * projection of the API's {@code GuidePolicyCompliance} (no {@code policyId}, constraints reduced to
 * name + plain-string reasons — see {@link McpPolicyCompliance#from}). A PURL is absent from the map
 * when it has no policy result for it, and an empty map means the whole batch was soft-failed
 * (unknown owner or evaluator error).
 *
 * @throws IllegalArgumentException
 *           if {@code stage} is non-blank but is not a recognised lifecycle stage. This is a caller
 *           error rather than a soft-fail, so it is signalled by exception; callers passing untrusted
 *           stage input should validate it up front (see {@code McpServletProvider}) or catch it.
 *           An authorization failure on the resolved owner likewise propagates (callers that want to
 *           degrade to "no policy" rather than fail the response should catch it).
 */
public interface McpPolicyAnnotator
{
  Map<String, McpPolicyCompliance> evaluatePolicies(List<String> purls, String applicationId, String stage);
}
