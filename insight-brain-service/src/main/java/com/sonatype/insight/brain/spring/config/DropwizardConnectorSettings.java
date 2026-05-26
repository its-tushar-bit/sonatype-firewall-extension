/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import java.time.Duration;
import org.springframework.core.env.Environment;

record DropwizardConnectorSettings(Duration applicationIdleTimeout, Duration adminIdleTimeout)
{
  static final String APPLICATION_IDLE_TIMEOUT_PROPERTY =
      "sonatype.dropwizard.application-connector.idle-timeout";

  static final String ADMIN_IDLE_TIMEOUT_PROPERTY =
      "sonatype.dropwizard.admin-connector.idle-timeout";

  static DropwizardConnectorSettings from(Environment environment) {
    return new DropwizardConnectorSettings(
        environment.getProperty(APPLICATION_IDLE_TIMEOUT_PROPERTY, Duration.class),
        environment.getProperty(ADMIN_IDLE_TIMEOUT_PROPERTY, Duration.class));
  }

  static Duration parseIdleTimeout(Object value, String connectorListName) {
    if (value == null) {
      return null;
    }
    try {
      return DropwizardDurationParser.parse(String.valueOf(value));
    }
    catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Unsupported legacy connector idleTimeout for " + connectorListName + ": " + value, e);
    }
  }
}
