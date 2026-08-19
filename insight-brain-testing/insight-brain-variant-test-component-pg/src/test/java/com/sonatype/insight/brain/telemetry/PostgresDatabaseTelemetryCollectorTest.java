/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentPgTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL ODS-size telemetry assertion relocated from {@code DatabaseTelemetryCollectorTest} (CLM-45235). The
 * in-memory / on-disk H2 coverage stays in the origin {@code DatabaseTelemetryCollectorTest}.
 */
@ComponentPgTest
public class PostgresDatabaseTelemetryCollectorTest
    extends AbstractComponentPgTest
{
  @Inject
  private DatabaseTelemetryCollector telemetryCollector;

  @Test
  public void testCollectData_OdsSizeBytes_ExternalDatabase() {
    Map<String, Object> attributes = telemetryCollector.collectData().getAttributes();
    assertThat(attributes.get(DatabaseTelemetryCollector.DB_ENGINE)).isEqualTo("postgresql");
    String odsSizeBytes = (String) attributes.get(DatabaseTelemetryCollector.ODS_SIZE_BYTES);
    assertThat(Long.valueOf(odsSizeBytes)).isGreaterThan(0);
  }
}
