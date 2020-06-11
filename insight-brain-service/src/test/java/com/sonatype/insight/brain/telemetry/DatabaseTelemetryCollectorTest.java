/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;

import javax.inject.Inject;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.DatabaseConfig;
import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.postgres.PostgresServer;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private DatabaseTelemetryCollector telemetryCollector;

  @Inject
  private InsightConfig insightConfig;

  @Test
  public void testCollectData_TelemetryPurpose() throws Exception {
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.DATABASE);
  }

  @Test
  public void testCollectData_OdsSizeBytes_EmbeddedDatabaseInMemory() throws Exception {
    Map<String, Object> attributes = telemetryCollector.collectData().getAttributes();
    assertThat(attributes.get(DatabaseTelemetryCollector.DB_ENGINE)).isEqualTo("h2");
    assertThat(attributes.get(DatabaseTelemetryCollector.ODS_SIZE_BYTES)).isNull();
  }

  @Test
  public void testCollectData_OdsSizeBytes_EmbeddedDatabaseInFile() throws Exception {
    insightConfig.setSonatypeWork(tempDir.getRoot().getAbsolutePath());
    DataSourceFactory.clear_ForTestsOnly();
    try {
      OperationalDataStoreProvider.init(new DatabaseConfigProvider(insightConfig).getDatabaseConfig(DatabaseName.ods),
          false);
      Map<String, Object> attributes = telemetryCollector.collectData().getAttributes();
      assertThat(attributes.get(DatabaseTelemetryCollector.DB_ENGINE)).isEqualTo("h2");
      String odsSizeBytes = (String) attributes.get(DatabaseTelemetryCollector.ODS_SIZE_BYTES);
      assertThat(odsSizeBytes).isNotNull();
      assertThat(Long.valueOf(odsSizeBytes)).isGreaterThan(0);
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testCollectData_OdsSizeBytes_ExternalDatabase() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      DatabaseConfig databaseConfig = new DatabaseConfig();
      databaseConfig.setType("postgresql");
      insightConfig.setDatabase(databaseConfig);
      // Create a postgres ODS database
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);

      Map<String, Object> attributes = telemetryCollector.collectData().getAttributes();
      assertThat(attributes.get(DatabaseTelemetryCollector.DB_ENGINE)).isEqualTo("postgresql");
      String odsSizeBytes = (String) attributes.get(DatabaseTelemetryCollector.ODS_SIZE_BYTES);
      assertThat(Long.valueOf(odsSizeBytes)).isGreaterThan(0);
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testCollectData_DbEngine_DatabaseConfigNull() {
    insightConfig.setDatabase(null);
    Map<String, Object> attributes = telemetryCollector.collectData().getAttributes();
    assertThat(attributes.get(DatabaseTelemetryCollector.DB_ENGINE)).isEqualTo("h2");
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryCollector.isClusterTelemetry()).isTrue();
  }
}
