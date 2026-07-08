/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import static java.lang.String.format;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;

/**
 * Verifies that the process can open new connections to the databases and that those connections are writable.
 *
 * Usage: curl -u admin:admin123 http://localhost:8071/healthcheck?pretty=true
 */
@Named
@Singleton
public class NewDbConnectionOperationalCheck
    extends AbstractDbOperationalCheck
{
  private static final Logger log = LoggerFactory.getLogger(NewDbConnectionOperationalCheck.class);

  /**
   * @see <a href="https://www.postgresql.org/docs/17/runtime-config-client.html#GUC-TRANSACTION-READ-ONLY">PostgreSQL
   *      show readonly statement</a>
   */
  private static final String SHOW_TRANSACTION_READ_ONLY = "SHOW transaction_read_only";

  /**
   * @see <a href="https://www.h2database.com/html/features.html#read_only">H2 readonly statement</a>
   */
  private static final String CALL_READONLY = "CALL READONLY()";

  /**
   * @see <a href="https://www.postgresql.org/docs/17/errcodes-appendix.html">PostgreSQL Error Codes</a>
   */
  private static final String POSTGRESQL_READ_ONLY_SQL_STATE = "25006";

  /**
   * @see <a href="https://www.h2database.com/javadoc/org/h2/api/ErrorCode.html#DATABASE_IS_READ_ONLY">H2 Read Only
   *      Error Code</a>
   */
  private static final String H2_READ_ONLY_SQL_STATE = "90097";

  private static final String UPDATE_TEST_QUERY = "UPDATE %s.test_table SET name = name WHERE false";

  /**
   * Last observed connection health per managed {@link DataSource}, keyed by identity. Covers each data store's primary
   * pool plus the operational store's separate locks pool (MTIQ and clustered single-tenant Postgres). In Postgres the
   * four data stores share one primary pool, so identity keying restarts each distinct pool at most once per recovery.
   */
  private final Map<DataSource, Boolean> lastHealthyByPool = new IdentityHashMap<>();

  @Inject
  public NewDbConnectionOperationalCheck(
      final OperationalDataStore operationalDataStore,
      final DataMartDataStore dataMartDataStore,
      final AggregationDataStore aggregationDataStore,
      final ThirdPartyScansDataStore thirdPartyScansDataStore)
  {
    super("newDatabaseConnections", operationalDataStore, dataMartDataStore, aggregationDataStore,
        thirdPartyScansDataStore);
  }

  @Override
  protected void checkConnection(Health.Builder healthBuilder, DataStore dataStore) {
    if (operationalDataStore.isDatabaseInMemory()) {
      // For the in memory db (tests only), this check is not interesting.
      return;
    }

    boolean healthy = performConnectionCheck(healthBuilder, dataStore);
    drainPoolIfRecovered(dataStore, healthy);
  }

  private boolean performConnectionCheck(Health.Builder healthBuilder, DataStore dataStore) {
    String messageKey = dataStore.getID() + " database";

    try (
        BasicDataSource tempDataSource =
            (BasicDataSource) dataStore.getDataSourceProvider().createNewDataSource(dataStore.getDatabaseConfig());
        Connection tempConnection = tempDataSource.getConnection())
    {
      long start = System.currentTimeMillis();
      boolean isValidConnection = tempConnection.isValid(5 /* timeout in seconds */);
      long duration = System.currentTimeMillis() - start;

      if (!isValidConnection) {
        healthBuilder.withDetail(messageKey,
            "Cannot open new connections to the database. The connection failed after " + duration + " ms.")
            .down();
        return false;
      }

      if (isConnectionReadOnly(tempConnection, dataStore)) {
        healthBuilder.withDetail(messageKey,
            "New connections to the database are read-only. Cannot perform write operations.")
            .down();
        return false;
      }

      healthBuilder.withDetail(messageKey, "roundTripTimeInMs=" + duration);
      return true;
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      healthBuilder.withDetail(messageKey, "Cannot open new connections to the database: " + e.getMessage())
          .down(e);
      return false;
    }
  }

  /**
   * On an unhealthy-to-healthy transition for an external data store, drain and rebuild the connection pools it owns so
   * stale connections left over from a database failover are evicted immediately instead of aging out over
   * {@code maxConnectionLifetimeSeconds}. This covers the primary pool and, for the operational data store, the
   * separate
   * locks pool used by the clustered locking mechanism (MTIQ and clustered single-tenant Postgres).
   */
  // Visible for testing
  void drainPoolIfRecovered(final DataStore dataStore, final boolean nowHealthy) {
    if (dataStore.isDatabaseEmbedded()) {
      return;
    }

    restartPoolIfRecovered(dataStore, dataStore.getDataSource(), nowHealthy);

    if (dataStore instanceof OperationalDataStore operationalDataStore) {
      restartPoolIfRecovered(dataStore, operationalDataStore.getDataSourceForLocks(), nowHealthy);
    }
  }

  private void restartPoolIfRecovered(
      final DataStore dataStore,
      final DataSource dataSource,
      final boolean nowHealthy)
  {
    if (dataSource == null) {
      return;
    }

    boolean recovered;
    synchronized (lastHealthyByPool) {
      Boolean previous = lastHealthyByPool.put(dataSource, nowHealthy);
      recovered = Boolean.FALSE.equals(previous) && nowHealthy;
    }

    if (recovered) {
      restartPool(dataStore, dataSource);
    }
  }

  private void restartPool(final DataStore dataStore, final DataSource dataSource) {
    if (!(dataSource instanceof BasicDataSource pool)) {
      return;
    }

    try {
      log.info("Database connections recovered for {}; draining connection pool to evict stale connections",
          dataStore.getID());
      pool.restart();
    }
    catch (Exception e) {
      log.warn("Failed to drain connection pool for {} after health recovery; "
          + "stale connections will age out via maxConnectionLifetimeSeconds: {}", dataStore.getID(), e.getMessage(),
          e);
    }
  }

  private boolean isConnectionReadOnly(final Connection connection, final DataStore dataStore) {
    // First check: Query the database to see if it is read only
    Boolean result = isConnectionReadOnlyViaQuery(connection, dataStore);
    if (result != null) {
      return result;
    }

    // Second check: Attempt a no-op UPDATE that won't modify any rows, to verify we have write permissions.
    result = isConnectionReadOnlyViaUpdate(connection, dataStore);
    if (result != null) {
      return result;
    }
    return false;
  }

  // Visible for testing
  Boolean isConnectionReadOnlyViaQuery(final Connection connection, final DataStore dataStore) {
    return dataStore.isDatabaseEmbedded()
        ? isConnectionReadOnlyViaQueryForEmbeddedDatabase(connection, dataStore)
        : isConnectionReadOnlyViaQueryForExternalDatabase(connection, dataStore);
  }

  // Visible for testing
  Boolean isConnectionReadOnlyViaUpdate(final Connection connection, final DataStore dataStore) {
    Boolean result = null;
    try (Statement stmt = connection.createStatement()) {
      stmt.executeUpdate(format(UPDATE_TEST_QUERY, dataStore.getDatabaseSchema()));
      result = false;
    }
    catch (SQLException e) {
      // Check if this is specifically a read-only error
      String errorCode = e.getSQLState();

      // PostgreSQL: SQLState 25006 with message "cannot execute UPDATE in a read-only transaction"
      // H2: SQLState 90097 with message "The database is read only"
      boolean isReadOnlyError = false;

      if (dataStore.isDatabaseEmbedded() && H2_READ_ONLY_SQL_STATE.equals(errorCode)) {
        isReadOnlyError = true;
      }
      else if (POSTGRESQL_READ_ONLY_SQL_STATE.equals(errorCode)) {
        isReadOnlyError = true;
      }

      if (isReadOnlyError) {
        log.error("Connection is read-only for {}: {}", dataStore.getDatabaseSchema(), e.getMessage());
        result = true;
      }

      log.warn("Write test failed for {} with non-read-only error, assuming writable: {}",
          dataStore.getDatabaseSchema(), e.getMessage());
    }
    return result;
  }

  private Boolean isConnectionReadOnlyViaQueryForEmbeddedDatabase(
      final Connection connection,
      final DataStore dataStore)
  {
    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(CALL_READONLY))
    {
      if (rs.next()) {
        String readOnlyValue = rs.getString(1);

        if ("true".equalsIgnoreCase(readOnlyValue)) {
          log.error("Connection is read-only for {}: {} = {}", dataStore.getDatabaseSchema(), CALL_READONLY,
              readOnlyValue);
          return true;
        }

        return false;
      }
    }
    catch (SQLException e) {
      log.warn("Could not check {} for {}: {}", dataStore.getDatabaseSchema(), CALL_READONLY, e.getMessage());
    }

    return null;
  }

  private Boolean isConnectionReadOnlyViaQueryForExternalDatabase(
      final Connection connection,
      final DataStore dataStore)
  {
    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(SHOW_TRANSACTION_READ_ONLY))
    {
      if (rs.next()) {
        String readOnlyValue = rs.getString(1);

        if ("on".equalsIgnoreCase(readOnlyValue)) {
          log.error("Connection is read-only for {}: {} = {}", dataStore.getDatabaseSchema(),
              SHOW_TRANSACTION_READ_ONLY, readOnlyValue);
          return true;
        }

        return false;
      }
    }
    catch (SQLException e) {
      log.warn("Could not check {} for {}: {}", SHOW_TRANSACTION_READ_ONLY, dataStore.getDatabaseSchema(),
          e.getMessage());
    }

    return null;
  }
}
