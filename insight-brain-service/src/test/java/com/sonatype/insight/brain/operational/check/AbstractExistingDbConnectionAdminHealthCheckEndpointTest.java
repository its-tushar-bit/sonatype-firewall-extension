/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import java.sql.Connection;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.operational.check.AdminHealthCheckEndpoint.HealthCheckResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.MethodOrderer;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.MethodName.class)
@ExtendWith(MockitoExtension.class)
abstract class AbstractExistingDbConnectionAdminHealthCheckEndpointTest
    extends AbstractDatabaseTest
{
  private ExistingDbConnectionAdminHealthCheckEndpoint databaseAdminHealthCheckEndpoint;

  private ExistingDbConnectionOperationalCheck databaseOperationalCheck;

  private OperationalDataStore operationalDataStore;

  private DataMartDataStore dataMartDataStore;

  private AggregationDataStore aggregationDataStore;

  private ThirdPartyScansDataStore thirdPartyScansDataStore;

  private DataStore[] allDataStores;

  @BeforeEach
  public void setUp() {
    operationalDataStore = spy(databaseRule.getOperationalDataStore());
    dataMartDataStore = spy(databaseRule.getDataMartDataStore());
    aggregationDataStore = spy(databaseRule.getAggregationDataStore());
    thirdPartyScansDataStore = spy(databaseRule.getThirdPartyScansDataStore());
    allDataStores =
        new DataStore[]{operationalDataStore, dataMartDataStore, aggregationDataStore, thirdPartyScansDataStore};

    databaseOperationalCheck = new ExistingDbConnectionOperationalCheck(operationalDataStore, dataMartDataStore,
        aggregationDataStore, thirdPartyScansDataStore);
    databaseAdminHealthCheckEndpoint = new ExistingDbConnectionAdminHealthCheckEndpoint(databaseOperationalCheck);
  }

  @Test
  public void testGetName() {
    assertThat(databaseAdminHealthCheckEndpoint.getName()).isEqualTo("Database");
  }

  @Test
  public void testGetPath() {
    assertThat(databaseAdminHealthCheckEndpoint.getPath()).isEqualTo("/healthcheck/database");
  }

  @Test
  public void testIsHealthy_Healthy() {
    assertThat(databaseAdminHealthCheckEndpoint.getHealthCheckResponse())
        .usingRecursiveComparison()
        .isEqualTo(new HealthCheckResponse(true));
  }

  @Test
  public void testIsHealthy_Unhealthy_ODS() throws Exception {
    testExecute_Unhealthy(operationalDataStore);
  }

  @Test
  public void testIsHealthy_Unhealthy_ThirdParty() throws Exception {
    testExecute_Unhealthy(thirdPartyScansDataStore);
  }

  @Test
  public void testIsHealthy_Unhealthy_Aggregate() throws Exception {
    testExecute_Unhealthy(aggregationDataStore);
  }

  @Test
  public void testIsHealthy_Unhealthy_Datamart() throws Exception {
    testExecute_Unhealthy(dataMartDataStore);
  }

  private void testExecute_Unhealthy(DataStore unhealthyDataStore) throws Exception {
    DataSource mockDataSource = mock();
    Connection mockConnection = mock();
    when(unhealthyDataStore.getDataSource()).thenReturn(mockDataSource);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.isValid(anyInt())).thenReturn(false);

    HealthCheckResponse response = databaseAdminHealthCheckEndpoint.getHealthCheckResponse();
    assertThat(response.isHealthy()).isFalse();
    for (DataStore dataStore : allDataStores) {
      if (dataStore == unhealthyDataStore) {
        assertThat(response.getContent()).matches("^.*" + dataStore.getID()
            + " database=Cannot access the database\\. The connection failed after \\d+ ms\\..*$");
      }
      else {
        assertThat(response.getContent()).matches("^.*" + dataStore.getID() + " database=roundTripTimeInMs=\\d+.*$");
      }
    }
    assertThat(response.getContent()).matches("^.*roundTripTimeInMs=\\d+.*$");
  }
}
