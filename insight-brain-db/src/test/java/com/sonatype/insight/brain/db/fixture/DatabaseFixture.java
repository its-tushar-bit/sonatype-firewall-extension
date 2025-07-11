/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.fixture;

import java.nio.file.Path;

import com.sonatype.insight.brain.common.test.InsightTestFixture;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.rule.DatabaseRule;
import com.sonatype.insight.db.DatabaseConfig;

/**
 * Test fixtures for the database. Direct use is normally not required, use {@link DatabaseRule}.
 */
public interface DatabaseFixture
    extends InsightTestFixture
{
  /**
   * Return an IQ {@link DatabaseConfig} object for the given database name
   */
  DatabaseConfig getDatabaseConfig(final String databaseName);

  /**
   * Return a {@link DataSourceProvider} for this database fixture
   *
   * @return
   */
  DataSourceProvider getDataSourceProvider();

  /**
   * Load the given SQL file into the database
   */
  void loadSqlDump(Path sqlFile);

  /**
   * Dump and return the given schema as a string
   */
  String dumpSchema(String schema);
}
