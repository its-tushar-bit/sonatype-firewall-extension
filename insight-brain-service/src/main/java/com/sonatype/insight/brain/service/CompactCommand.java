/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DatabaseConfigProviderFactory;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.H2DatabaseUtil;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datasource.DataSourceProviderFactory;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.error.exception.BadRequestException;

import io.dropwizard.core.cli.Cli;
import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.37
 */
public class CompactCommand
    extends ConfiguredCommand<InsightConfig>
{
  private static final Logger log = LoggerFactory.getLogger(CompactCommand.class);

  CompactCommand() {
    super("compact-db",
        "Reduces the size of the server's database by freeing empty space. It only applies to h2 databases.");
  }

  @Override
  protected void run(
      final Bootstrap<InsightConfig> bootstrap,
      final Namespace namespace,
      final InsightConfig insightConfig) throws Exception
  {
    if (!insightConfig.isDatabaseEmbedded()) {
      throw new BadRequestException("The " + getName() + " command is supported only for h2 databases.");
    }

    final DatabaseConfig databaseConfig = DatabaseConfigProviderFactory
        .createDatabaseConfigProvider(insightConfig)
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
      try (final Connection connection = dataSource.getConnection();
          final Statement statement = connection.createStatement())
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

  @Override
  public void onError(Cli cli, Namespace namespace, Throwable e) {
    log.error(e.getMessage(), e);

    super.onError(cli, namespace, e);
  }
}
