/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpVersionDetails(
    String version,
    boolean endOfLife,
    McpVulnerabilities vulnerabilities,
    Set<String> licenses,
    Long catalogDate,
    boolean malicious,
    McpPolicyContext policyCompliance)
{
}
