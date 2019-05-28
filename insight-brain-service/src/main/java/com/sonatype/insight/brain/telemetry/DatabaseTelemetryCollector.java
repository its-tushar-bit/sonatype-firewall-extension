/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.db.H2DatabaseUtil;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

/**
 * @since 1.53
 */
@Named
@Singleton
public class DatabaseTelemetryCollector
    implements TelemetryCollector
{
  private final InsightConfig config;

  public static final String ODS_SIZE_BYTES = "ods_size_bytes";

  public static final String DB_ENGINE = "db_engine";

  @Inject
  public DatabaseTelemetryCollector(final InsightConfig config) {
    this.config = config;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.DATABASE);
    Map<String, Object> attributes = telemetryData.getAttributes();

    if (config.getDatabase() == null) {
      attributes.put(DB_ENGINE, "h2");
      attributes.put(ODS_SIZE_BYTES, getOdsSizeBytes());
    }
    else {
      attributes.put(DB_ENGINE, config.getDatabase().getType());
    }

    return telemetryData;
  }

  private String getOdsSizeBytes() {
    try {
      if (OperationalDataStoreProvider.isDatabaseInMemory()) {
        return null;
      }
      return String.valueOf(Files.size(
          Paths.get(H2DatabaseUtil.getDatabasePath(OperationalDataStoreProvider.getDatabaseConfig()) + ".h2.db")));
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
