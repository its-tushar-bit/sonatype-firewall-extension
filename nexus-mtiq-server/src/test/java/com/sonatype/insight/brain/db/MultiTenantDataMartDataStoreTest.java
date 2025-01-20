/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.EntityManagerFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantDataMartDataStoreTest
    extends AbstractMultiTenantDatabaseTest
{
  @Test
  public void testDataMart_schemaIsGlobal() {
    testAsNewTenant(t -> {
      assertThat(databaseRule.getDataMartDataStore().getDatabaseSchema()).isEqualTo("global");
    });
  }

  @Test
  public void testDataMart_entityManagerFactoryInstanceIsUsedForAllTenants() {
    AtomicReference<EntityManagerFactory> globalFactory = new AtomicReference<>();

    testAsGlobalTenant(g -> {
      globalFactory.set(databaseRule.getDataMartDataStore().getJPAEntityManagerFactory());
    });
    testAsNewTenant(t -> {
      assertThat(databaseRule.getDataMartDataStore().getJPAEntityManagerFactory()).isSameAs(globalFactory.get());
    });
    testAsNewTenant(t -> {
      assertThat(databaseRule.getDataMartDataStore().getJPAEntityManagerFactory()).isSameAs(globalFactory.get());
    });
  }
}
