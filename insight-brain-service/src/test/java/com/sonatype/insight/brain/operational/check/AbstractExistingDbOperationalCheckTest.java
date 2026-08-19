/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.operational.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.MethodOrderer;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@TestMethodOrder(MethodOrderer.MethodName.class)
@ExtendWith(MockitoExtension.class)
abstract class AbstractExistingDbOperationalCheckTest
    extends AbstractDatabaseTest
{
  private ExistingDbConnectionOperationalCheck databaseOperationalCheck;

  private OperationalDataStore operationalDataStore;

  private DataMartDataStore dataMartDataStore;

  private AggregationDataStore aggregationDataStore;

  private ThirdPartyScansDataStore thirdPartyScansDataStore;

  private DataStore[] allDataStores;

  @BeforeEach
  public void before() {
    operationalDataStore = spy(databaseRule.getOperationalDataStore());
    dataMartDataStore = spy(databaseRule.getDataMartDataStore());
    aggregationDataStore = spy(databaseRule.getAggregationDataStore());
    thirdPartyScansDataStore = spy(databaseRule.getThirdPartyScansDataStore());

    allDataStores =
        new DataStore[]{operationalDataStore, dataMartDataStore, aggregationDataStore, thirdPartyScansDataStore};

    databaseOperationalCheck = new ExistingDbConnectionOperationalCheck(operationalDataStore, dataMartDataStore,
        aggregationDataStore, thirdPartyScansDataStore);
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
  public void testExecute_Unhealthy_ODS() throws Exception {
    testExecute_Unhealthy(operationalDataStore);
  }

  @Test
  public void testExecute_Unhealthy_DataMart() throws Exception {
    testExecute_Unhealthy(dataMartDataStore);
  }

  @Test
  public void testExecute_Unhealthy_Aggregation() throws Exception {
    testExecute_Unhealthy(aggregationDataStore);
  }

  @Test
  public void testExecute_Unhealthy_ThirdParty() throws Exception {
    testExecute_Unhealthy(thirdPartyScansDataStore);
  }

  private void testExecute_Unhealthy(DataStore unhealthyDataStore) throws Exception {
    DataSource mockDataSource = mock();
    Connection mockConnection = mock();
    when(unhealthyDataStore.getDataSource()).thenReturn(mockDataSource);
    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.isValid(anyInt())).thenReturn(false);

    Health result = databaseOperationalCheck.execute();
    assertThat(result.getStatus()).isEqualTo(Status.DOWN);
    Map<String, Object> resultDetails = result.getDetails();
    for (DataStore dataStore : allDataStores) {
      if (dataStore == unhealthyDataStore) {
        assertThat((String) resultDetails.get(dataStore.getID() + " database"))
            .matches("^Cannot access the database. The connection failed after \\d+ ms\\.$");
      }
      else {
        assertThat((String) resultDetails.get(dataStore.getID() + " database")).matches("^roundTripTimeInMs=\\d+$");
      }
    }
  }
}
