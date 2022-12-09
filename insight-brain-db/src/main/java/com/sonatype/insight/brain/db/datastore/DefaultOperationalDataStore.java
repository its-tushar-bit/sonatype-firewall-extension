/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.SqlCallCounterMetrics;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.H2DatabaseEngine;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultOperationalDataStore
    extends AbstractDataStore
    implements OperationalDataStore
{
  private static final Logger log = LoggerFactory.getLogger(DefaultOperationalDataStore.class);

  public static final int MINIMUM_DATABASE_VERSION = 85;

  public static final int OLD_VIOLATION_MODEL_DATABASE_VERSION = 114;

  private EntityManagerFactory entityManagerFactory;

  private EntityManagerFactory entityManagerFactoryForLocks;

  private volatile boolean isInitialized = false;

  private Boolean isDatabaseEmbedded;

  public DefaultOperationalDataStore(
      final DataSourceFactory dataSourceFactory,
      final DatabaseMigrator databaseMigrator)
  {
    super(dataSourceFactory, databaseMigrator);
    // Populate the legacy class
    OperationalDataStoreProvider.setInstance(this);
  }

  @Override
  protected synchronized void init(
      DatabaseConfig databaseConfig,
      boolean migrateDatabase,
      Boolean migrateToNewViolationModel)
  {
    if (isInitialized()) {
      return;
    }

    log.info("Initializing the {} data store.", getID());
    long start = System.currentTimeMillis();

    this.databaseConfig = databaseConfig;
    dataSource = dataSourceFactory.createNewDataSource(databaseConfig, getID(), getDatabaseSchema());
    isDatabaseEmbedded = H2DatabaseEngine.class.equals(DatabaseUtil.getDatabaseEngine(dataSource).getClass());

    if (migrateDatabase) {
      migrate(migrateToNewViolationModel);
    }
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("openjpa.ConnectionFactory", dataSource);

    if (SqlCallCounterMetrics.getInstance().getJDBCListener() != null) {
      props.put("openjpa.jdbc.JDBCListeners",
          new JDBCListener[]{SqlCallCounterMetrics.getInstance().getJDBCListener()});
      log.info("Enabled JPA JDBC listener for performance testing.");
    }

    entityManagerFactory = Persistence.createEntityManagerFactory("InsightBrainODS", props);
    if (isDatabaseEmbedded) {
      entityManagerFactoryForLocks = entityManagerFactory;
    }
    else {
      // smallest pool size should still support max nesting level of locks
      int maxConnections = Math.max(5, Optional.ofNullable(databaseConfig.getMaxConnections()).orElse(50));
      BasicDataSource dataSourceForLocks = new BasicDataSource();
      dataSourceForLocks.setDriverClassName(databaseConfig.getDriverClassName());
      dataSourceForLocks.setUrl(databaseConfig.getUrl());
      dataSourceForLocks.setUsername(databaseConfig.getUsername());
      dataSourceForLocks.setPassword(databaseConfig.getPassword());
      dataSourceForLocks.setMaxTotal(maxConnections);
      dataSourceForLocks.addConnectionProperty("ApplicationName",
          DatabaseUtil.generateApplicationNameWithHost("IQ-locks"));
      props.put("openjpa.ConnectionFactory", dataSourceForLocks);
      entityManagerFactoryForLocks = Persistence.createEntityManagerFactory("InsightBrainODS", props);
    }
    isInitialized = true;

    log.info("Initialized the {} data store in {} ms.", getID(), System.currentTimeMillis() - start);
  }

  @Override
  public String getDatabaseSchema() {
    return ID;
  }

  @Override
  protected IntConsumer getUpgradeGuard(final Boolean migrateToNewViolationModel) {
    return OperationalDataStoreProvider.getUpgradeGuard(migrateToNewViolationModel);
  }

  @Override
  protected boolean isInitialized() {
    return isInitialized;
  }

  @Override
  public EntityManagerFactory getJPAEntityManagerFactory() {
    if (!isInitialized()) {
      initWithMigration(null /* databaseConfig */, false);
    }
    return entityManagerFactory;
  }

  @Override
  public void clear_ForTestsOnly() {
    super.clear_ForTestsOnly();
    entityManagerFactory = null;
    entityManagerFactoryForLocks = null;
    isInitialized = false;
    isDatabaseEmbedded = null;
  }

  @Override
  public DataSource getDataSourceWithoutInit() {
    return dataSource;
  }

  @Override
  public boolean isDatabaseInMemory() {
    return databaseConfig == null;
  }

  @Override
  public EntityManagerFactory getEntityManagerFactoryForLocks() {
    if (!isInitialized()) {
      initWithMigration(null /* databaseConfig */, false);
    }
    return entityManagerFactoryForLocks;
  }

  @Override
  public boolean isDatabaseEmbedded() {
    return isDatabaseEmbedded;
  }
}
