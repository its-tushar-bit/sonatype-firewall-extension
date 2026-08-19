/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.fixture.h2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datasource.H2DiskDataSourceProvider;
import com.sonatype.insight.brain.db.fixture.DatabaseFixture;
import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DiskDatabaseFixture
    implements DatabaseFixture
{
  private static final Logger log = LoggerFactory.getLogger(H2DiskDatabaseFixture.class);

  private static final String DEFAULT_SETTINGS =
      "DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE";

  private final String customSettings;

  private final int maxConnections;

  private TemporaryFolder tempDir = new TemporaryFolder();

  private File databaseDir;

  private final Map<String, DatabaseConfig> databaseConfigMap = new HashMap<>();

  private H2DiskDataSourceProvider dataSourceProvider;

  private boolean reusableForSpringContext;

  public H2DiskDatabaseFixture(final H2DiskTest h2DiskTest) {
    this(h2DiskTest.maxConnections(), h2DiskTest.copyExistingDatabase(), h2DiskTest.customSettings());
  }

  public H2DiskDatabaseFixture(
      final int maxConnections,
      final String copyExistingDatabase,
      final String customSettings)
  {
    log.info("Creating new H2-disk test database");

    assertMaxConnectionsIsValid(maxConnections);
    this.maxConnections = maxConnections;
    this.customSettings = customSettings;

    try {
      tempDir.create();
      databaseDir = tempDir.newFolder("data");

      if (StringUtils.isNotEmpty(copyExistingDatabase)) {
        ClassLoader classLoader = getClass().getClassLoader();
        File source = new File(classLoader.getResource(copyExistingDatabase).getPath());
        FileUtils.copyDirectory(source, databaseDir);
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void assertMaxConnectionsIsValid(final int maxConnections) {
    if (maxConnections <= 0) {
      throw new UnsupportedOperationException(
          "Configuration Error: maxConnections configuration should be greater than 0");
    }
  }

  @Override
  public void close() throws Exception {
    log.info("Shutting down H2-disk test databases and deleting '{}'", tempDir.getRoot().getAbsolutePath());
    dataSourceProvider.shutDownDatabase();
    dataSourceProvider.closeAllDataSources();
    tempDir.delete();
  }

  @Override
  public DatabaseConfig getDatabaseConfig(final String databaseName) {
    return databaseConfigMap.computeIfAbsent(databaseName, s -> {
      final String settings = hasCustomSettings() ? customSettings : DEFAULT_SETTINGS;
      final DatabaseConfig databaseConfig = new DatabaseConfig();
      databaseConfig.setDriverClassName("org.h2.Driver");
      databaseConfig.setUrl(
          String.format("jdbc:h2:%s/%s;%s", databaseDir.getAbsolutePath(), databaseName, settings));
      databaseConfig.setUsername("sa");
      databaseConfig.setPassword("");
      databaseConfig.setMaxConnections(maxConnections);

      return databaseConfig;
    });
  }

  private boolean hasCustomSettings() {
    return StringUtils.isNotBlank(customSettings);
  }

  @Override
  public DataSourceProvider getDataSourceProvider() {
    if (dataSourceProvider == null) {
      dataSourceProvider = new H2DiskDataSourceProvider();
    }
    return dataSourceProvider;
  }

  @Override
  public Map<String, Object> getMetadata() {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put(H2DiskTest.DATABASE_PATH, databaseDir.getAbsolutePath());
    return metadata;
  }

  @Override
  public boolean isFixtureReusable() {
    return reusableForSpringContext;
  }

  public void setReusableForSpringContext(final boolean reusableForSpringContext) {
    this.reusableForSpringContext = reusableForSpringContext;
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
