/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class UserTelemetryRequestLoggingFilterTest
{
  @Test
  public void shouldSkipTelemetryEventsPath() {
    UserTelemetryRequestLoggingFilter filter = new UserTelemetryRequestLoggingFilter();

    assertThat(filter.shouldSkip("/rest/user-telemetry/events")).isTrue();
    assertThat(filter.shouldSkip("/rest/user-telemetry/events/path/to/pendo")).isTrue();
    assertThat(filter.shouldSkip("/api/v2/applications")).isFalse();
    assertThat(filter.shouldSkip("/api/debug/rest/user-telemetry/events-copy")).isFalse();
  }
}
