/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.fixture.h2;

import java.nio.file.Path;

import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datasource.H2InMemoryTestDataSourceProvider;
import com.sonatype.insight.brain.db.fixture.DatabaseFixture;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2InMemoryTest;
import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note that we are using H2 in-memory databases for testing. See the <a
 * href="http://www.h2database.com/html/features.html#in_memory_databases">H2 in-memory docs</a> for more info.
 */
public class H2InMemoryDatabaseFixture
    implements DatabaseFixture
{
  private static final Logger log = LoggerFactory.getLogger(H2InMemoryDatabaseFixture.class);

  private static final String DEFAULT_SETTINGS = "DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1";

  private final DatabaseConfig databaseConfig;

  private H2InMemoryTestDataSourceProvider dataSourceProvider;

  private boolean useTemporaryDatabase;

  public H2InMemoryDatabaseFixture(final H2InMemoryTest h2InMemoryTest) {
    log.info("Creating new H2 in-memory test database");

    this.useTemporaryDatabase = useTemporalDatabase(h2InMemoryTest);
    if (useTemporaryDatabase) {
      // Database created with custom settings should not be re-used, so creating this DB with a different name.
      // This DB will be closed once the close() method is called
      // This NOT re-usable DB will have the name tempInMemoryDatabase
      String customSettings =
          StringUtils.isBlank(h2InMemoryTest.customSettings()) ? DEFAULT_SETTINGS : h2InMemoryTest.customSettings();

      databaseConfig = createDBConfig("tempInMemoryDatabase", customSettings);
    }
    else {
      // Database created with default settings will be re-used on different tests, note the DB_CLOSE_DELAY=-1.
      // That re-usable DB will have the name sharedInMemoryDatabase
      databaseConfig = createDBConfig("sharedInMemoryDatabase", DEFAULT_SETTINGS);
    }
  }

  private boolean useTemporalDatabase(final H2InMemoryTest h2InMemoryTest) {
    return h2InMemoryTest != null && (StringUtils.isNotBlank(h2InMemoryTest.customSettings()) ||
        h2InMemoryTest.cleanDatabase() || h2InMemoryTest.suppressMigrations());
  }

  private DatabaseConfig createDBConfig(String name, String customSettings) {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setUrl(String.format("jdbc:h2:mem:%s;%s", name, customSettings));
    databaseConfig.setDriverClassName("org.h2.Driver");
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    return databaseConfig;
  }

  @Override
  public void close() throws Exception {
    log.info("Destroying H2 in-memory test database");

    // We only shut down the in memory DB if it has custom settings or if a clean DB was requested
    if (useTemporaryDatabase) {
      dataSourceProvider.shutDownDatabase();
    }
  }

  @Override
  public DatabaseConfig getDatabaseConfig(final String databaseName /* unused */) {
    return databaseConfig;
  }

  @Override
  public DataSourceProvider getDataSourceProvider() {
    if (dataSourceProvider == null) {
      dataSourceProvider = new H2InMemoryTestDataSourceProvider();
    }
    return dataSourceProvider;
  }

  @Override
  public boolean isFixtureReusable() {
    return !useTemporaryDatabase;
  }

  @Override
  public void loadSqlDump(final Path sqlFile) {
    // Implemented for MTIQ tests. As of writing H2 does not require but in the future we can implement if need be.
    throw new UnsupportedOperationException("`loadSqlDump` not implemented in H2DiskDatabaseFixture");
  }

  @Override
  public String dumpSchema(final String schema) {
    // Implemented for MTIQ tests. As of writing H2 does not require but in the future we can implement if need be.
    throw new UnsupportedOperationException("`dumpSchema` not implemented in H2DiskDatabaseFixture");
  }
}
