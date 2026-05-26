/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.db.DatabaseConfig;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Before;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(MockitoJUnitRunner.class)
abstract class AbstractNewDbOperationalCheckTest
    extends AbstractDatabaseTest
{
  private NewDbConnectionOperationalCheck databaseOperationalCheck;

  private OperationalDataStore operationalDataStore;

  private DataMartDataStore dataMartDataStore;

  private AggregationDataStore aggregationDataStore;

  private ThirdPartyScansDataStore thirdPartyScansDataStore;

  private DataStore[] allDataStores;

  @Before
  public void before() {
    operationalDataStore = spy(databaseRule.getOperationalDataStore());
    dataMartDataStore = spy(databaseRule.getDataMartDataStore());
    aggregationDataStore = spy(databaseRule.getAggregationDataStore());
    thirdPartyScansDataStore = spy(databaseRule.getThirdPartyScansDataStore());

    allDataStores =
        new DataStore[]{operationalDataStore, dataMartDataStore, aggregationDataStore, thirdPartyScansDataStore};

    databaseOperationalCheck = new NewDbConnectionOperationalCheck(operationalDataStore, dataMartDataStore,
        aggregationDataStore, thirdPartyScansDataStore);
  }

  @After
  public void resetReadOnly() {
    if (allDataStores == null) {
      return;
    }
    // Reset default_transaction_read_only for PostgreSQL databases.
    // The testExecute_Unhealthy_ReadOnly tests set this flag persistently on the database,
    // and it must be reset to avoid affecting subsequent tests.
    for (DataStore dataStore : allDataStores) {
      if (!dataStore.isDatabaseEmbedded()) {
        try (Connection connection = dataStore.getDataSource().getConnection();
            Statement statement = connection.createStatement())
        {
          statement.execute(
              "ALTER DATABASE " + connection.getCatalog() + " SET default_transaction_read_only = off;");
        }
        catch (Exception e) {
          // Best effort cleanup
        }
      }
    }
  }

  @Test
  public void testExecute_Healthy() throws Exception {
    Health result = databaseOperationalCheck.execute();
    Map<String, Object> resultDetails = result.getDetails();
    for (DataStore dataStore : allDataStores) {
      assertThat((String) resultDetails.get(dataStore.getID() + " database")).matches("^roundTripTimeInMs=\\d+$");
    }
    assertThat(result.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  public void testExecute_Unhealthy_Aggregation() throws Exception {
    testExecute_Unhealthy(aggregationDataStore);
  }

  @Test
  public void testExecute_Unhealthy_DataMart() throws Exception {
    testExecute_Unhealthy(dataMartDataStore);
  }

  @Test
  public void testExecute_Unhealthy_ODS() throws Exception {
    testExecute_Unhealthy(operationalDataStore);
  }

  @Test
  public void testExecute_Unhealthy_ThirdParty() throws Exception {
    testExecute_Unhealthy(thirdPartyScansDataStore);
  }

  @Test
  public void testExecute_Unhealthy_ReadOnly_ODS() throws Exception {
    testExecute_Unhealthy_ReadOnly(operationalDataStore, databaseOperationalCheck);
  }

  @Test
  public void testExecute_Unhealthy_ReadOnly_DataMart() throws Exception {
    testExecute_Unhealthy_ReadOnly(dataMartDataStore, databaseOperationalCheck);
  }

  @Test
  public void testExecute_Unhealthy_ReadOnly_Aggregation() throws Exception {
    testExecute_Unhealthy_ReadOnly(aggregationDataStore, databaseOperationalCheck);
  }

  @Test
  public void testExecute_Unhealthy_ReadOnly_ThirdParty() throws Exception {
    testExecute_Unhealthy_ReadOnly(thirdPartyScansDataStore, databaseOperationalCheck);
  }

  @Test
  public void testExecute_Unhealthy_ReadOnly_Fallback_ODS() throws Exception {
    NewDbConnectionOperationalCheck spy = spy(databaseOperationalCheck);
    doReturn(null).when(spy).isConnectionReadOnlyViaQuery(any(), any());
    testExecute_Unhealthy_ReadOnly(operationalDataStore, spy);
  }

  @Test
  public void testExecute_Unhealthy_ReadOnly_Fallback_DataMart() throws Exception {
    NewDbConnectionOperationalCheck spy = spy(databaseOperationalCheck);
    doReturn(null).when(spy).isConnectionReadOnlyViaQuery(any(), any());
    testExecute_Unhealthy_ReadOnly(dataMartDataStore, spy);
  }

  @Test
  public void testExecute_Unhealthy_ReadOnly_Fallback_Aggregation() throws Exception {
    NewDbConnectionOperationalCheck spy = spy(databaseOperationalCheck);
    doReturn(null).when(spy).isConnectionReadOnlyViaQuery(any(), any());
    testExecute_Unhealthy_ReadOnly(aggregationDataStore, spy);
  }

  @Test
  public void testExecute_Unhealthy_ReadOnly_Fallback_ThirdParty() throws Exception {
    NewDbConnectionOperationalCheck spy = spy(databaseOperationalCheck);
    doReturn(null).when(spy).isConnectionReadOnlyViaQuery(any(), any());
    testExecute_Unhealthy_ReadOnly(thirdPartyScansDataStore, spy);
  }

  private void testExecute_Unhealthy(DataStore unhealthyDataStore) throws Exception {
    DatabaseConfig databaseConfig = unhealthyDataStore.getDatabaseConfig();
    DataSourceProvider mockDataSourceProvider = mock();
    BasicDataSource mockDataSource = mock();
    Connection mockConnection = mock();
    when(unhealthyDataStore.getDataSourceProvider()).thenReturn(mockDataSourceProvider);
    when(mockDataSourceProvider.createNewDataSource(databaseConfig)).thenReturn(mockDataSource);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.isValid(anyInt())).thenReturn(false);

    Health result = databaseOperationalCheck.execute();
    assertThat(result.getStatus()).isEqualTo(Status.DOWN);
    Map<String, Object> resultDetails = result.getDetails();
    for (DataStore dataStore : allDataStores) {
      if (dataStore == unhealthyDataStore) {
        assertThat((String) resultDetails.get(dataStore.getID() + " database"))
            .matches("^Cannot open new connections to the database. The connection failed after \\d+ ms\\.$");
      }
      else {
        assertThat((String) resultDetails.get(dataStore.getID() + " database")).matches("^roundTripTimeInMs=\\d+$");
      }
    }
  }

  private void testExecute_Unhealthy_ReadOnly(
      DataStore unhealthyDataStore,
      NewDbConnectionOperationalCheck newDbConnectionOperationalCheck) throws Exception
  {
    if (unhealthyDataStore.isDatabaseEmbedded()) {
      setPathReadOnly(getH2DBPath(unhealthyDataStore));

      try (Connection connection = unhealthyDataStore.getDataSource().getConnection();
          Statement statement = connection.createStatement())
      {
        statement.execute("SHUTDOWN");
      }
    }
    else {
      try (Connection connection = unhealthyDataStore.getDataSource().getConnection();
          Statement statement = connection.createStatement())
      {
        statement.execute("ALTER DATABASE " + connection.getCatalog() + " SET default_transaction_read_only = on;");
      }
    }

    Health result = newDbConnectionOperationalCheck.execute();

    // Assert overall health check failed
    assertThat(result.getStatus()).isEqualTo(Status.DOWN);

    // Assert the specific datastore has the read-only error message
    Map<String, Object> resultDetails = result.getDetails();
    assertThat((String) resultDetails.get(unhealthyDataStore.getID() + " database"))
        .isEqualTo("New connections to the database are read-only. Cannot perform write operations.");
  }

  private Path getH2DBPath(final DataStore unhealthyDataStore) {
    String location = unhealthyDataStore.getDatabaseConfig().getUrl().substring("jdbc:h2:".length());
    location = location.substring(0, location.indexOf(";")) + ".h2.db";
    return Path.of(location);
  }

  private void setPathReadOnly(final Path path) {
    boolean setReadOnly = false;

    try {
      // attempt 1
      File dbFile = new File(path.toString());
      setReadOnly = dbFile.setReadOnly();
    }
    catch (Exception ignore) {
      // do nothing
    }

    try {
      // attempt 2
      Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("r--r--r--"));
      setReadOnly = true;
    }
    catch (Exception ignore) {
      // do nothing
    }

    try {
      // attempt 3
      Files.setAttribute(path, "dos:readonly", true);
      setReadOnly = true;
    }
    catch (Exception ignore) {
      // do nothing
    }

    if (!setReadOnly) {
      throw new AssertionError("Could not set read-only permissions for database " + path.toString());
    }
  }
}
