/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

/** The Guide surface a request arrived through. Mirrors the SaaS telemetry {@code channel} field. */
public enum GuideChannel
{
  UI,
  API,
  MCP
}
