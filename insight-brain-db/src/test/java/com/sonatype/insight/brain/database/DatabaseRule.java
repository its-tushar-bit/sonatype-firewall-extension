/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sonatype.insight.brain.database.datasource.DataSourceProvider;
import com.sonatype.insight.brain.database.fixture.DatabaseFixture;
import com.sonatype.insight.brain.database.fixture.h2.H2DiskDatabaseFixture;
import com.sonatype.insight.brain.database.fixture.h2.H2InMemoryDatabaseFixture;
import com.sonatype.insight.brain.database.fixture.postgres.PostgresDatabaseFixture;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class DatabaseRule
    extends ExternalResource
{
  private DatabaseFixture databaseFixture;

  // TODO - future home of DatabaseConfig, DataSourceProvider, *DataStore, etc...

  @Override
  public Statement apply(final Statement base, final Description description) {
    databaseFixture = getDatabaseFixture(description);

    // TODO - init the rest of the db classes here

    return super.apply(base, description);
  }

  /**
   * Get a {@link DatabaseConfig} to access the provisioned test database
   */
  public DatabaseConfig getDatabaseConfig() {
    return databaseFixture.getDatabaseConfig();
  }

  public DataSourceProvider getDataSourceProvider() {
    return databaseFixture.getDataSourceProvider();
  }

  /**
   * Get the appropriate {@link DatabaseFixture}. The default is an {@link H2InMemoryDatabaseFixture}. Alternatively if
   * the @{@link PostgresTest} annotation is on the test then a {@link PostgresDatabaseFixture} is returned. And if a H2
   * disk based database is needed a {@link H2DiskDatabaseFixture} is returned.
   *
   * @param description
   * @return
   */
  private DatabaseFixture getDatabaseFixture(final Description description) {
    if (isPostgresTest(description)) {
      return new PostgresDatabaseFixture();
    }
    if (isH2DiskTest(description)) {
      return new H2DiskDatabaseFixture();
    }
    return new H2InMemoryDatabaseFixture();
  }

  private boolean isH2DiskTest(final Description description) {
    return description.getAnnotation(H2DiskTest.class) != null;
  }

  private boolean isPostgresTest(final Description description) {
    return description.getAnnotation(PostgresTest.class) != null;
  }

  @Override
  protected void before() throws Throwable {
  }

  @Override
  protected void after() {
    try {
      databaseFixture.close();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface PostgresTest
  {
    /**
     * By default, when a new test database is provisioned it will be migrated to the latest schema changes. Set to true
     * to suppress this and leave the database un-migrated.
     */
    boolean suppressMigrations() default false;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface H2DiskTest
  {
    /**
     * By default, when a new test database is provisioned it will be migrated to the latest schema changes. Set to true
     * to suppress this and leave the database un-migrated.
     */
    boolean suppressMigrations() default false;
  }
}
