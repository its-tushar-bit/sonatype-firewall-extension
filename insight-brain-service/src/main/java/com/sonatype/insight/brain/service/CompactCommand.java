/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.db.DatabaseConfigProviderFactory;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.H2DatabaseUtil;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datasource.DataSourceProviderFactory;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.spring.InsightBrainCompatibilityCommand;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.37
 */
@Named
public class CompactCommand
    implements InsightBrainCompatibilityCommand
{
  public static final String NAME = "compact-db";

  public static final String DESCRIPTION =
      "Reduces the size of the server's database by freeing empty space. It only applies to h2 databases.";

  private static final Logger log = LoggerFactory.getLogger(CompactCommand.class);

  private final InsightConfig insightConfig;

  @Inject
  public CompactCommand(InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public void run(String... args) throws Exception {
    run(insightConfig);
  }

  public void run(final InsightConfig runtimeConfig) throws Exception {
    if (!runtimeConfig.isDatabaseEmbedded()) {
      throw new BadRequestException("The " + getName() + " command is supported only for h2 databases.");
    }

    final DatabaseConfig databaseConfig = DatabaseConfigProviderFactory
        .createDatabaseConfigProvider(runtimeConfig)
        .getDatabaseConfig(DatabaseName.ods);
    final DataSourceProvider dataSourceProvider =
        DataSourceProviderFactory.createDataSourceProvider(H2DatabaseEngine.INSTANCE);
    final DataSource dataSource = dataSourceProvider.getDataSource(databaseConfig, OperationalDataStore.ID);

    final Path databaseFile = Paths.get(H2DatabaseUtil.getDatabasePath(databaseConfig).getAbsolutePath() + ".h2.db");
    try {
      final long originalSize = Files.size(databaseFile);
      log.info("Compacting {}", databaseFile);
      log.info("This might take a while, please be patient.");
      final long startTime = System.currentTimeMillis();
      try (Connection connection = dataSource.getConnection();
          Statement statement = connection.createStatement())
      {
        statement.execute("SHUTDOWN COMPACT");
      }
      final long newSize = Files.size(databaseFile);
      final BigDecimal percentChange = new BigDecimal(100 - newSize * 100.0d / originalSize)
          .setScale(2, RoundingMode.HALF_EVEN);
      log.info("Successfully compacted {} from {} bytes to {} bytes (reduced by {}%) in {} ms", databaseFile,
          originalSize, newSize, percentChange, System.currentTimeMillis() - startTime);
    }
    catch (Exception e) {
      log.error("Failed to compact {}", databaseFile, e);
      throw e;
    }
  }
}
