/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantDataStoreTest
    extends AbstractMultiTenantDatabaseTest
{
  @Test
  public void testSameDataSource() {
    assertThat(databaseRule.getOperationalDataStore().getDataSource())
        .isSameAs(databaseRule.getAggregationDataStore().getDataSource())
        .isSameAs(databaseRule.getDataMartDataStore().getDataSource())
        .isSameAs(databaseRule.getThirdPartyScansDataStore().getDataSource());
  }

  @Test
  public void testOdsSpecialProperties() {
    assertThat(databaseRule.getOperationalDataStore().isDatabaseInMemory()).isFalse();
    assertThat(databaseRule.getOperationalDataStore().isDatabaseEmbedded()).isFalse();
  }

  @Test
  public void testGlobalSchema() {
    testAsGlobalTenant(g -> {
      assertThat(databaseRule.getOperationalDataStore().getDatabaseSchema()).isEqualTo("global");
      assertThat(databaseRule.getAggregationDataStore().getDatabaseSchema()).isEqualTo("global");
      assertThat(databaseRule.getDataMartDataStore().getDatabaseSchema()).isEqualTo("global");
      assertThat(databaseRule.getThirdPartyScansDataStore().getDatabaseSchema()).isEqualTo("global");
    });
  }

  @Test
  public void testTenantSchema() {

    testAsNewTenant(t -> {
      assertThat(databaseRule.getOperationalDataStore().getDatabaseSchema()).isEqualTo(t.databaseSchema);
      assertThat(databaseRule.getAggregationDataStore().getDatabaseSchema()).isEqualTo(t.databaseSchema);
      assertThat(databaseRule.getThirdPartyScansDataStore().getDatabaseSchema()).isEqualTo(t.databaseSchema);

      // datamart is ALWAYS global
      assertThat(databaseRule.getDataMartDataStore().getDatabaseSchema()).isEqualTo("global");
    });
  }

  @Test
  public void testDataSourceID() {
    assertThat(databaseRule.getOperationalDataStore().getID()).isEqualTo(OperationalDataStore.ID);
    assertThat(databaseRule.getAggregationDataStore().getID()).isEqualTo(AggregationDataStore.ID);
    assertThat(databaseRule.getDataMartDataStore().getID()).isEqualTo(DataMartDataStore.ID);
    assertThat(databaseRule.getThirdPartyScansDataStore().getID()).isEqualTo(ThirdPartyScansDataStore.ID);
  }
}
