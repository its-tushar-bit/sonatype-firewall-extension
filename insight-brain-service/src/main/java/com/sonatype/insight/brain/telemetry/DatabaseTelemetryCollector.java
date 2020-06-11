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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    if (config.isDatabaseEmbedded()) {
      attributes.put(DB_ENGINE, "h2");
      attributes.put(ODS_SIZE_BYTES, getOdsSizeBytes_EmbeddedDatabase());
    }
    else {
      attributes.put(DB_ENGINE, config.getDatabase().getType());
      attributes.put(ODS_SIZE_BYTES, getOdsSizeBytes_ExternalDatabase());
    }

    return telemetryData;
  }

  private String getOdsSizeBytes_EmbeddedDatabase() {
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

  private String getOdsSizeBytes_ExternalDatabase() {
    try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection(); //
        Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery(
          "SELECT SUM(pg_total_relation_size(quote_ident(schemaname) || '.' || quote_ident(tablename)))::BIGINT " //
              + "FROM pg_tables WHERE schemaname = '" + OperationalDataStoreProvider.ID + "'");
      resultSet.next();
      return String.valueOf(resultSet.getLong(1));
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }
}
