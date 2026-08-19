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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.H2DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.db.PostgresDatabaseEngine;
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
  public static final String ODS_SIZE_BYTES = "ods_size_bytes";

  public static final String DB_ENGINE = "db_engine";

  private final OperationalDataStore operationalDataStore;

  @Inject
  public DatabaseTelemetryCollector(final OperationalDataStore operationalDataStore) {
    this.operationalDataStore = operationalDataStore;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.DATABASE);
    Map<String, Object> attributes = telemetryData.getAttributes();

    if (operationalDataStore.isDatabaseEmbedded()) {
      attributes.put(DB_ENGINE, H2DatabaseEngine.INSTANCE.getId());
      attributes.put(ODS_SIZE_BYTES, getOdsSizeBytes_EmbeddedDatabase());
    }
    else {
      attributes.put(DB_ENGINE, PostgresDatabaseEngine.INSTANCE.getId());
      attributes.put(ODS_SIZE_BYTES, getOdsSizeBytes_ExternalDatabase());
    }

    return telemetryData;
  }

  private String getOdsSizeBytes_EmbeddedDatabase() {
    try {
      if (operationalDataStore.isDatabaseInMemory()) {
        return null;
      }
      return String.valueOf(Files.size(
          Paths.get(H2DatabaseUtil.getDatabasePath(operationalDataStore.getDatabaseConfig()) + ".h2.db")));
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String getOdsSizeBytes_ExternalDatabase() {
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT SUM(pg_total_relation_size(quote_ident(schemaname) || '.' || quote_ident(tablename)))::BIGINT "
                + "FROM pg_tables WHERE schemaname = ?"))
    {
      statement.setString(1, operationalDataStore.getDatabaseSchema());
      ResultSet resultSet = statement.executeQuery();
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
