/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.StaticInjectionTestHelper;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManagerProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.search.SearchIndexRule;
import com.sonatype.insight.brain.search.SearchModule;
import com.sonatype.insight.brain.service.DbBasedModule;
import com.sonatype.insight.brain.service.SisuApplication;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.eclipse.sisu.launch.InjectedTest;
import org.eclipse.sisu.space.SpaceModule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.experimental.categories.Category;

/**
 * Handles creation of the four data store classes for tests. The {@link DatabaseContainerRule} is a junit rule
 * to create the instances and inject them into the legacy *Provider classes. The {@link DataStoreTestModule} binds
 * those instances so Guice can inject as needed. Ultimately any test that accesses a datastore needs to extend this
 * base class.
 *
 * <B>IMPORTANT</B> - If you override {@link #configure(Binder)}, make sure to call `super.configure(binder)` to get
 * database support
 */
@Category(SlowTest.class)
public abstract class BrainInjectedTest
    extends InjectedTest
{
  /**
   * Note: As this will be the child class of the test, the database rule must be executed first. This is very
   * important as we need the data stores initialized first, in particular ahead of other rules like
   * {@link TemporaryEntity}
   */
  @Rule(order = 1)
  public DatabaseContainerRule databaseContainerRule = DatabaseContainerRule.getInstance(BrainInjectedTest.class);

  @Rule(order = 2)
  public SearchIndexRule searchIndexRule = SearchIndexRule.getInstance(BrainInjectedTest.class);

  @Rule(order = 3)
  public TemporaryEntity tempEntity = createTemporaryEntity();

  /** You should only use this `daoFactory` when you override the `configure` method and you need to crate DAOs there.
   * Otherwise, always prefer the use of the `@Inject` annotation to inject the DAOs you need for your test */
  protected DAOFactory daoFactory;

  @BeforeClass
  public static void disableWaitToCloseOldClients() {
    HdsClient.waitToCloseOldClients = false;
  }

  @Before
  @Override
  public void setUp()
      throws Exception
  {
    // Re-inject classes that have static dependencies
    daoFactory = new TestDAOFactory(databaseContainerRule);
    StaticInjectionTestHelper.inject(daoFactory);

    String sisuUrlCaches = System.getProperty("sisu.url.caches");
    if (sisuUrlCaches == null) {
      System.setProperty("sisu.url.caches", "true");
    }
    super.setUp();
  }

  @Override
  public SpaceModule spaceModule() {
    return SisuApplication.getSpaceModule();
  }

  /**
   * Important: If you override this method be sure to call `super.configure` if you need database support. Also,
   * if you need to create DAOs on the override, please use the `daoFactory` provided by this class instead of using
   * the `@Inject` annotation.
   */
  @Override
  public void configure(final Binder binder) {
    binder.install(new DataStoreTestModule());
    binder.install(new DbBasedModule(() -> databaseContainerRule.getDatabaseContainer()));
    binder.install(new SearchModule(() -> searchIndexRule.getSearchConfig()));
  }

  private class DataStoreTestModule
      implements Module
  {
    @Override
    public void configure(final Binder binder) {
      // Use toProvider instead of toInstance to ensure we always get the current DataStore instance
      // This is critical when database fixtures switch types (H2 <-> Postgres) - the old DataStore
      // instances must be released to prevent memory leaks of JDBCConfigurationImpl
      binder.bind(OperationalDataStore.class).toProvider(() -> databaseContainerRule.getOperationalDataStore());
      binder.bind(AggregationDataStore.class).toProvider(() -> databaseContainerRule.getAggregationDataStore());
      binder.bind(DataMartDataStore.class).toProvider(() -> databaseContainerRule.getDataMartDataStore());
      binder.bind(ThirdPartyScansDataStore.class).toProvider(() -> databaseContainerRule.getThirdPartyScansDataStore());
      binder.bind(DataStoreProvider.class).toInstance(databaseContainerRule.getDatabaseContainer());
      binder.bind(ClusterLockManager.class).toProvider(ClusterLockManagerProvider.class);
    }
  }

  /**
   * Creates a new instance of {@link TemporaryEntity}. This method is protected to allow subclasses to override the
   * creation logic if needed, for example, to insert any dependencies or configurations used by the
   * {@link TemporaryEntity}
   *
   * @return a new instance of {@link TemporaryEntity}.
   */
  protected TemporaryEntity createTemporaryEntity() {
    return new TemporaryEntity(databaseContainerRule);
  }
}
