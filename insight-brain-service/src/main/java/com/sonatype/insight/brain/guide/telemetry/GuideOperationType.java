/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

/**
 * Operation types mirroring seaworthy CreditOperationType. {@code MCP_LOOKUP} is derived (channel==MCP), never
 * annotated.
 */
public enum GuideOperationType
{
  COMPONENT_LOOKUP("component_lookup"),
  VULNERABILITY_LOOKUP("vulnerability_lookup"),
  SECURITY_EVENT_LOOKUP("security_event_lookup"),
  /** The Guide-licensed global search API ({@code SearchApiClientImpl#globalSearch}). */
  GLOBAL_SEARCH("global_search"),
  /**
   * The catalog-federation leg of Nexus One global search. Base functionality available with any valid
   * IQ license, so its HDS traffic is reported under its own operation type: downstream credit and
   * usage analytics can separate it from {@link #GLOBAL_SEARCH}, which is Guide-licensed usage.
   */
  CATALOG_FEDERATION_SEARCH("catalog_federation_search"),
  MCP_LOOKUP("mcp_lookup");

  private final String value;

  GuideOperationType(final String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
