/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Typed model of the Dropwizard {@code server.requestLog} section. These are the only keys Dropwizard's request-log
 * factories accept: {@code type} (classic / logback-access / external), {@code timeZone}, the polymorphic list of
 * {@code appenders}, and {@code enabled} (external only). Modelled (rather than a raw map) so the section is validated
 * like the rest of the server config.
 */
public class RequestLogConfig
{
  @JsonProperty
  String type;

  @JsonProperty
  String timeZone;

  @JsonProperty
  List<Map<String, Object>> appenders;

  // Modelled only so a 'type: external' request log (ExternalRequestLogFactory's sole field) parses under strict
  // conversion instead of failing startup. 'external' (which pre-Spring routed request logs through SLF4J) is not
  // supported here - no request log is installed for it (see RequestLoggingConfiguration).
  @JsonProperty
  Boolean enabled;
}
