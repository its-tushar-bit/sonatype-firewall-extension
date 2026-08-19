/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.service.InsightConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DatabaseConfigurationTest
{
  private final DatabaseContainer databaseContainer = mock(DatabaseContainer.class);

  private final DatabaseProvisioner databaseProvisioner = mock(DatabaseProvisioner.class);

  private final OperationalDataStore operationalDataStore = mock(OperationalDataStore.class);

  private final AggregationDataStore aggregationDataStore = mock(AggregationDataStore.class);

  private final DataMartDataStore dataMartDataStore = mock(DataMartDataStore.class);

  private final ThirdPartyScansDataStore thirdPartyScansDataStore = mock(ThirdPartyScansDataStore.class);

  private final TestDatabaseConfiguration configuration = new TestDatabaseConfiguration(databaseContainer);

  @BeforeEach
  public void setUp() {
    when(databaseContainer.getOperationalDataStore()).thenReturn(operationalDataStore);
    when(databaseContainer.getAggregationDataStore()).thenReturn(aggregationDataStore);
    when(databaseContainer.getDataMartDataStore()).thenReturn(dataMartDataStore);
    when(databaseContainer.getThirdPartyScansDataStore()).thenReturn(thirdPartyScansDataStore);
    when(databaseContainer.getDatabaseProvisioner()).thenReturn(databaseProvisioner);
  }

  @Test
  public void shouldRunStartupMigrationsByDefault() {
    DatabaseContainer result = configuration.databaseContainer(mock(InsightConfig.class), true);

    assertThat(result).isSameAs(databaseContainer);
    verifyDataStoresInitialized();
    verify(databaseProvisioner).initializeDatabaseWithMigration();
    verify(databaseProvisioner).validateMinimumSchemaVersion();
  }

  @Test
  public void shouldSkipStartupMigrationsWhenDisabledForCommandMode() {
    DatabaseContainer result = configuration.databaseContainer(mock(InsightConfig.class), false);

    assertThat(result).isSameAs(databaseContainer);
    verifyDataStoresInitialized();
    verify(databaseProvisioner, never()).initializeDatabaseWithMigration();
    verify(databaseProvisioner, never()).validateMinimumSchemaVersion();
  }

  private void verifyDataStoresInitialized() {
    verify(operationalDataStore).initialize();
    verify(aggregationDataStore).initialize();
    verify(dataMartDataStore).initialize();
    verify(thirdPartyScansDataStore).initialize();
  }

  private static class TestDatabaseConfiguration
      extends DatabaseConfiguration
  {
    private final DatabaseContainer databaseContainer;

    private TestDatabaseConfiguration(DatabaseContainer databaseContainer) {
      this.databaseContainer = databaseContainer;
    }

    @Override
    protected DatabaseContainer createDatabaseContainer(InsightConfig insightConfig) {
      return databaseContainer;
    }
  }
}
