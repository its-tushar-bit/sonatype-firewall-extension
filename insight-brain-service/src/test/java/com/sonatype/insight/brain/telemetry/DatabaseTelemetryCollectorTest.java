/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import javax.inject.Inject;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseTelemetryCollectorTest
    extends AbstractComponentTest
{
  @Inject
  private DatabaseTelemetryCollector telemetryCollector;

  @Test
  public void testCollectData_TelemetryPurpose() throws Exception {
    TelemetryData telemetryData = telemetryCollector.collectData();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.DATABASE);
  }

  @Test
  public void testCollectData_OdsSizeBytes_InMemory() throws Exception {
    assertThat(telemetryCollector.collectData().getAttributes().get(DatabaseTelemetryCollector.ODS_SIZE_BYTES))
        .isNull();
  }

  @Test
  public void testCollectData_OdsSizeBytes_InFile() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    insightConfig.setSonatypeWork(tempDir.getRoot().getAbsolutePath());
    DataSourceFactory.clear_ForTestsOnly();
    try {
      OperationalDataStoreProvider.init(new DatabaseConfigProvider(insightConfig).getDatabaseConfig(DatabaseName.ods),
          false);
      String odsSizeBytes = (String) telemetryCollector.collectData().getAttributes()
          .get(DatabaseTelemetryCollector.ODS_SIZE_BYTES);
      assertThat(odsSizeBytes).isNotNull();
      assertThat(Long.valueOf(odsSizeBytes)).isGreaterThan(0);
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }
}
