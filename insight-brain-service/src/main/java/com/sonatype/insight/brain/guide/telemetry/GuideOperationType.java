/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

/**
 * Operation types for Guide usage telemetry. A subset of seaworthy's {@code CreditOperationType} — IQ
 * defines only the ones it emits, so this is not an exhaustive 1:1 mirror. {@code MCP_LOOKUP} is derived
 * (channel==MCP), never annotated.
 */
public enum GuideOperationType
{
  COMPONENT_LOOKUP("component_lookup"),
  VULNERABILITY_LOOKUP("vulnerability_lookup"),
  SECURITY_EVENT_LOOKUP("security_event_lookup"),
  /** The Guide-licensed global search API ({@code SearchApiClientImpl#globalSearch}). */
  GLOBAL_SEARCH("global_search"),
  MCP_LOOKUP("mcp_lookup");

  private final String value;

  GuideOperationType(final String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
