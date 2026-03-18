/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

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
  protected void checkConnection(ResultBuilder resultBuilder, DataStore dataStore) {
    if (operationalDataStore.isDatabaseInMemory()) {
      // For the in memory db (tests only), this check is not interesting.
      return;
    }

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
        resultBuilder.withDetail(messageKey,
            "Cannot open new connections to the database. The connection failed after " + duration + " ms.");
        resultBuilder.unhealthy();
        return;
      }

      if (isConnectionReadOnly(tempConnection, dataStore)) {
        resultBuilder.withDetail(messageKey,
            "New connections to the database are read-only. Cannot perform write operations.");
        resultBuilder.unhealthy();
        return;
      }

      resultBuilder.withDetail(messageKey, "roundTripTimeInMs=" + duration);
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      resultBuilder.withDetail(messageKey, "Cannot open new connections to the database: " + e.getMessage());
      resultBuilder.unhealthy(e);
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
