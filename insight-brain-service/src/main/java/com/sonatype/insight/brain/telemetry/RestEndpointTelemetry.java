/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

/**
 * @since 1.76
 */
public class RestEndpointTelemetry
{
  public String method;

  public String path;

  public int invocations;

  public RestEndpointTelemetry() {
    // for deserialization
  }

  public RestEndpointTelemetry(String method, String path, int invocations) {
    this.method = method;
    this.path = path;
    this.invocations = invocations;
  }

  @Override
  public String toString() {
    return "RestEndpointTelemetry [method=" + method + ", path=" + path + ", invocations=" + invocations + "]";
  }
}
