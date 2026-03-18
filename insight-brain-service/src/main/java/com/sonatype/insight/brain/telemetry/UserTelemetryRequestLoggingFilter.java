/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import ch.qos.logback.access.common.spi.IAccessEvent;
import jakarta.ws.rs.core.UriBuilder;

import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import io.dropwizard.logging.common.filter.FilterFactory;

/**
 * Based on the example at https://www.dropwizard.io/1.2.1/docs/manual/core.html#logging-filters
 *
 * @since 1.50
 */
public class UserTelemetryRequestLoggingFilter
    implements FilterFactory<IAccessEvent>
{
  private String uriToFilter =
      UriBuilder.fromPath(UserTelemetryResource.RESOURCE_PATH).path(UserTelemetryResource.EVENTS).toString();

  @Override
  public Filter<IAccessEvent> build() {
    return new Filter<>()
    {
      @Override
      public FilterReply decide(IAccessEvent event) {
        if (event.getRequestURI().contains(uriToFilter)) {
          return FilterReply.DENY;
        }
        else {
          return FilterReply.NEUTRAL;
        }
      }
    };
  }
}
