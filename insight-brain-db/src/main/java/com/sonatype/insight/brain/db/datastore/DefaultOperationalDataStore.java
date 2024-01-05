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

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.DbApplicationNameGenerator;
import com.sonatype.insight.brain.db.SqlCallCounterMetrics;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.db.DatabaseConfig;

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

  public DefaultOperationalDataStore(final DataSourceProvider dataSourceProvider, final DatabaseConfig databaseConfig) {
    super(dataSourceProvider, databaseConfig);
  }

  @Override
  public synchronized void initialize() {
    if (isInitialized()) {
      return;
    }

    log.info("Initializing the {} data store.", getID());
    long start = System.currentTimeMillis();

    dataSource = dataSourceProvider.getDataSource(databaseConfig, getID());
    isDataStoreNew = !DatabaseUtil.schemaExists(dataSource, getDatabaseSchema());

    isDatabaseEmbedded = DatabaseUtil.isDatabaseEmbedded(databaseConfig);

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
          new DbApplicationNameGenerator().generateApplicationNameWithHost("IQ-locks"));
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
  public IntConsumer getUpgradeGuard(final Boolean migrateToNewViolationModel) {
    return currentVersion -> {
      if (currentVersion < MINIMUM_DATABASE_VERSION) {
        throw new UnsupportedOperationException(String.format(
            "Cannot migrate %s database, this requires version %s at minimum, but you have version %s.\n"
                + "Please upgrade to Nexus IQ Server version 1.16 before upgrading to this version.",
            OperationalDataStore.ID, MINIMUM_DATABASE_VERSION, currentVersion));
      }
      if (currentVersion <= OLD_VIOLATION_MODEL_DATABASE_VERSION && !migrateToNewViolationModel) {
        log.error("|------------------------------------------");
        log.error("|");
        log.error("| Upgrade requires consent to proceed.");
        log.error("| For detailed instructions, see");
        log.error("| https://links.sonatype.com/products/clm/doc/upgrade/1.45");
        log.error("|");
        log.error("|------------------------------------------");
        throw new UnsupportedOperationException("Consent to upgrade has not been given.");
      }
    };
  }

  @Override
  protected boolean isInitialized() {
    return isInitialized;
  }

  @Override
  public EntityManagerFactory getJPAEntityManagerFactory() {
    return entityManagerFactory;
  }

  @Override
  public DataSource getDataSourceWithoutInit() {
    return dataSource;
  }

  @Override
  public boolean isDatabaseInMemory() {
    return DatabaseUtil.isInMemoryDatabase(databaseConfig);
  }

  @Override
  public EntityManagerFactory getEntityManagerFactoryForLocks() {
    return entityManagerFactoryForLocks;
  }

  @Override
  public boolean isDatabaseEmbedded() {
    return isDatabaseEmbedded;
  }
}
