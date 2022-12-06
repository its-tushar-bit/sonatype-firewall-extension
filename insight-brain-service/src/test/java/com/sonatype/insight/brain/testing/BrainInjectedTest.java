/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing; // note: this class is in this package only because SetUpModule is package-private

import java.util.Properties;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultAggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultDataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore;
import com.sonatype.insight.brain.db.datastore.DefaultThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Module;
import org.eclipse.sisu.launch.InjectedTest;
import org.eclipse.sisu.wire.ParameterKeys;
import org.eclipse.sisu.wire.WireModule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Handles creation of the four data store classes for tests. The {@link DataStoreRule} is a junit rule to create the
 * instances and inject them into the legacy *Provider classes. The {@link DataStoreTestModule} binds those instances so
 * Guice can inject as needed. Ultimately any test that accesses a datastore needs to extend this base class.
 */
public class BrainInjectedTest
    extends InjectedTest
{
  /**
   * Note: As this will be the child class of the test, the {@link DataStoreRule} is executed first. This is very
   * important as we need the data stores initialized first, in particular ahead of other rules like
   * {@link TemporaryEntity}
   */
  @Rule(order = 1)
  public DataStoreRule dataStoreRule = new DataStoreRule();

  @Before
  @Override
  public void setUp()
      throws Exception
  {
    String sisuUrlCaches = System.getProperty("sisu.url.caches");
    if (sisuUrlCaches == null) {
      System.setProperty("sisu.url.caches", "true");
    }
    Guice.createInjector(new WireModule(new DataStoreTestModule(), new SetUpModule(), spaceModule()));
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
      operationalDataStore = new DefaultOperationalDataStore();
      aggregationDataStore = new DefaultAggregationDataStore();
      dataMartDataStore = new DefaultDataMartDataStore();
      thirdPartyScansDataStore = new DefaultThirdPartyScansDataStore();

      OperationalDataStoreProvider.setInstance(operationalDataStore);
      AggregationDataStoreProvider.setInstance(aggregationDataStore);
      DatamartProvider.setInstance(dataMartDataStore);
      ThirdPartyScansProvider.setInstance(thirdPartyScansDataStore);

      return super.apply(base, description);
    }
  }

  /**
   * Duplicated from {@link InjectedTest}. The inner class there is unfortunately marked package-private. So we either
   * put this class in the `org.eclipse.sisu.launch` package, or duplicate the SetUpModule inner class.
   * <a href="https://github.com/eclipse/sisu.inject/issues/70">Created sisu.inject issue #70</a>
   */
  final class SetUpModule
      implements Module
  {
    @Override
    public void configure(final Binder binder) {
      binder.install(BrainInjectedTest.this);

      final Properties properties = new Properties();
      properties.put("basedir", getBasedir());
      BrainInjectedTest.this.configure(properties);

      binder.bind(ParameterKeys.PROPERTIES).toInstance(properties);

      binder.requestInjection(BrainInjectedTest.this);
    }
  }
}
