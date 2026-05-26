/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class DropwizardLoggingConfig
{
  @JsonProperty
  Object level;

  @JsonProperty
  Map<String, Object> loggers;

  @JsonProperty
  List<Map<String, Object>> appenders;

  @Deprecated
  @JsonProperty
  Boolean additive;
}
