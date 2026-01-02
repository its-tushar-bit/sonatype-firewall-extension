/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;
import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class DatabaseTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private DatabaseTelemetryCollector telemetryCollector;

  @Test
  public void testCollectData_TelemetryPurpose() {
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.DATABASE);
  }

  @Test
  public void testCollectData_OdsSizeBytes_EmbeddedDatabaseInMemory() {
    Map<String, Object> attributes = telemetryCollector.collectData().getAttributes();
    assertThat(attributes.get(DatabaseTelemetryCollector.DB_ENGINE)).isEqualTo("h2");
    assertThat(attributes.get(DatabaseTelemetryCollector.ODS_SIZE_BYTES)).isNull();
  }

  @Test
  @H2DiskTest
  public void testCollectData_OdsSizeBytes_EmbeddedDatabaseInFile() {
    Map<String, Object> attributes = telemetryCollector.collectData().getAttributes();
    assertThat(attributes.get(DatabaseTelemetryCollector.DB_ENGINE)).isEqualTo("h2");
    String odsSizeBytes = (String) attributes.get(DatabaseTelemetryCollector.ODS_SIZE_BYTES);
    assertThat(odsSizeBytes).isNotNull();
    assertThat(Long.valueOf(odsSizeBytes)).isGreaterThan(0);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testCollectData_OdsSizeBytes_ExternalDatabase() {
    Map<String, Object> attributes = telemetryCollector.collectData().getAttributes();
    assertThat(attributes.get(DatabaseTelemetryCollector.DB_ENGINE)).isEqualTo("postgresql");
    String odsSizeBytes = (String) attributes.get(DatabaseTelemetryCollector.ODS_SIZE_BYTES);
    assertThat(Long.valueOf(odsSizeBytes)).isGreaterThan(0);
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isTrue();
  }
}
