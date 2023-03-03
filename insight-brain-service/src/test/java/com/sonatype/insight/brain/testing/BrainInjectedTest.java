/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultAggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultDataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.hds.DefaultHdsClient;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Handles creation of the four data store classes for tests. The {@link DataStoreRule} is a junit rule to create the
 * instances and inject them into the legacy *Provider classes. The {@link DataStoreTestModule} binds those instances so
 * Guice can inject as needed. Ultimately any test that accesses a datastore needs to extend this base class.
 *
 * <B>IMPORTANT</B> - If you override {@link #configure(Binder)}, make sure to call `super.configure(binder)` to get
 * database support
 */
public abstract class BrainInjectedTest
    extends InjectedTest
{
  /**
   * Note: As this will be the child class of the test, the {@link DataStoreRule} is executed first. This is very
   * important as we need the data stores initialized first, in particular ahead of other rules like
   * {@link TemporaryEntity}
   */
  @Rule(order = 1)
  public DataStoreRule dataStoreRule = new DataStoreRule();

  @BeforeClass
  public static void disableWaitToCloseOldClients() {
    DefaultHdsClient.waitToCloseOldClients = false;
  }

  @Before
  @Override
  public void setUp()
      throws Exception
  {
    String sisuUrlCaches = System.getProperty("sisu.url.caches");
    if (sisuUrlCaches == null) {
      System.setProperty("sisu.url.caches", "true");
    }
    super.setUp();
  }

  /**
   * Important: If you override this method be sure to call `super.configure` if you need database support
   */
  @Override
  public void configure(final Binder binder) {
    binder.install(new DataStoreTestModule());
  }

  private class DataStoreTestModule
      implements Module
  {
    @Override
    public void configure(final Binder binder) {
      binder.bind(OperationalDataStore.class).toInstance(dataStoreRule.operationalDataStore);
      binder.bind(AggregationDataStore.class).toInstance(dataStoreRule.aggregationDataStore);
      binder.bind(DataMartDataStore.class).toInstance(dataStoreRule.dataMartDataStore);
      binder.bind(ThirdPartyScansDataStore.class).toInstance(dataStoreRule.thirdPartyScansDataStore);
    }
  }

  private static class DataStoreRule
      extends ExternalResource
  {
    private OperationalDataStore operationalDataStore;

    private AggregationDataStore aggregationDataStore;

    private DataMartDataStore dataMartDataStore;

    private ThirdPartyScansDataStore thirdPartyScansDataStore;

    @Override
    public Statement apply(final Statement base, final Description description) {
      operationalDataStore = new DefaultOperationalDataStore(new DataSourceFactory(), new DatabaseMigrator());
      aggregationDataStore = new DefaultAggregationDataStore(new DataSourceFactory(), new DatabaseMigrator());
      dataMartDataStore = new DefaultDataMartDataStore(new DataSourceFactory(), new DatabaseMigrator());
      thirdPartyScansDataStore = new DefaultThirdPartyScansDataStore(new DataSourceFactory(), new DatabaseMigrator());
      return super.apply(base, description);
    }
  }
}
