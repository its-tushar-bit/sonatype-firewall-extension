/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;

import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.MultiTenantAggregationDataStore;
import com.sonatype.insight.brain.db.MultiTenantDataMartDataStore;
import com.sonatype.insight.brain.db.MultiTenantDataSourceFactory;
import com.sonatype.insight.brain.db.MultiTenantOperationalDataStore;
import com.sonatype.insight.brain.db.MultiTenantThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class MultiTenantDatabaseTestRule
    extends ExternalResource
{
  public MultiTenantDataSourceFactory multiTenantDataSourceFactory;

  public DatabaseMigrator databaseMigrator;

  public OperationalDataStore operationalDataStore;

  public AggregationDataStore aggregationDataStore;

  public DataMartDataStore dataMartDataStore;

  public ThirdPartyScansDataStore thirdPartyScansDataStore;

  public DatabaseProvisionUtils databaseProvisionUtils;

  public InsightConfig insightConfig;

  public PostgresServer postgresServer;

  private boolean initializeDatabase = true;

  @Override
  public Statement apply(final Statement base, final Description description) {
    DatabaseTest databaseTest = isAnnotatedWith(description, DatabaseTest.class);
    if (databaseTest != null) {
      this.initializeDatabase = databaseTest.initializeDatabase();
    }

    return super.apply(base, description);
  }

  @Override
  protected void before() {
    TenantManager.initGlobalTenant();

    multiTenantDataSourceFactory = new MultiTenantDataSourceFactory();

    databaseMigrator = new DatabaseMigrator(multiTenantDataSourceFactory);

    operationalDataStore = new MultiTenantOperationalDataStore(multiTenantDataSourceFactory, databaseMigrator);
    aggregationDataStore = new MultiTenantAggregationDataStore(multiTenantDataSourceFactory, databaseMigrator);
    dataMartDataStore = new MultiTenantDataMartDataStore(multiTenantDataSourceFactory, databaseMigrator);
    thirdPartyScansDataStore = new MultiTenantThirdPartyScansDataStore(multiTenantDataSourceFactory, databaseMigrator);

    databaseProvisionUtils = new DatabaseProvisionUtils(operationalDataStore, aggregationDataStore, dataMartDataStore,
        thirdPartyScansDataStore);

    postgresServer = new PostgresServer();

    insightConfig = new InsightConfig();
    insightConfig.setDatabase(getPostgresDatabaseConfig(postgresServer.getDatabaseConfig()));

    if (initializeDatabase) {
      initializeDatabase();
    }
  }

  @Override
  protected void after() {
    postgresServer.close();
  }

  public DatabaseConfig getDatabaseConfig() {
    return postgresServer.getDatabaseConfig();
  }

  public void initializeDatabase() {
    DatabaseConfigProvider databaseConfigProvider = new DatabaseConfigProvider(insightConfig);
    databaseProvisionUtils.initializeDatabases(insightConfig, databaseConfigProvider);
  }

  private com.sonatype.insight.brain.service.DatabaseConfig getPostgresDatabaseConfig(DatabaseConfig postgresConfig) {
    com.sonatype.insight.brain.service.DatabaseConfig result =
        new com.sonatype.insight.brain.service.DatabaseConfig();
    result.setType("postgresql");
    URI uri = URI.create(postgresConfig.getUrl().substring("jdbc:postgresql:".length()));
    result.setHostname(uri.getHost());
    result.setPort(uri.getPort());
    result.setName(uri.getPath().substring(1));
    result.setUsername(postgresConfig.getUsername());
    result.setPassword(postgresConfig.getPassword());
    return result;
  }

  private static <T extends Annotation> T isAnnotatedWith(final Description description, Class<T> clazz) {
    // is method annotated
    T annotation = description.getAnnotation(clazz);
    if (annotation != null) {
      return annotation;
    }

    // return class level annotation or null if not present on the class
    return description.getTestClass().getAnnotation(clazz);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.TYPE, ElementType.METHOD})
  public @interface DatabaseTest
  {
    boolean initializeDatabase() default true;
  }
}
