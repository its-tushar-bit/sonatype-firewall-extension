/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.mcp;

import com.google.inject.AbstractModule;
import com.sonatype.insight.brain.mcp.policy.PolicyAnnotator;
import com.sonatype.insight.brain.mcp.search.SearchApiClient;

public class McpModule
    extends AbstractModule
{
  @Override
  protected void configure() {
    bind(McpServletProvider.class);
    bind(SearchApiClient.class).to(SearchApiClientImpl.class);
    bind(PolicyAnnotator.class).to(McpPolicyAnnotator.class);
  }
}
