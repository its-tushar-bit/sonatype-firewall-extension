/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Map;

/**
 * Resource to capture data from external services that interact with the IQ server.
 *
 * @since 1.133
 */
public interface ApiExternalTelemetryResourceV2
{
  void postExternalTelemetry(String userAgent, Map<String, String> telemetryValues);
}
