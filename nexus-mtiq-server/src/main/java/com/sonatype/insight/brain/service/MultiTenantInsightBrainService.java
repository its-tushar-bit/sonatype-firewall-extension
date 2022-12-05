/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Arrays;
import java.util.List;

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
import com.sonatype.insight.brain.security.SecurityAopModule;
import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.tenancy.MultiTenantQuartzJobInitializer;
import com.sonatype.insight.brain.tenancy.MultiTenantExecutorThreadPools;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantUrlFilter;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;

public class MultiTenantInsightBrainService
    extends InsightBrainService
{
  public static void main(final String[] args) {
    try {
      setupServerLogging(args);

      if (!validateTempDir()) {
        System.exit(1);
      }

      new MultiTenantInsightBrainService().run(args);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
    }
  }

  @Override
  protected DatabaseProvisionUtils createDatabaseProvisionUtils() {
    // TODO MTIQ: Using default data stores so that we can run and test but this should use the MultiTenant* variants
    OperationalDataStore operationalDataStore = new DefaultOperationalDataStore();
    AggregationDataStore aggregationDataStore = new DefaultAggregationDataStore();
    DataMartDataStore dataMartDataStore = new DefaultDataMartDataStore();
    ThirdPartyScansDataStore thirdPartyScansDataStore = new DefaultThirdPartyScansDataStore();

    // Populate the legacy classes
    OperationalDataStoreProvider.setInstance(operationalDataStore);
    AggregationDataStoreProvider.setInstance(aggregationDataStore);
    DatamartProvider.setInstance(dataMartDataStore);
    ThirdPartyScansProvider.setInstance(thirdPartyScansDataStore);

    return new DatabaseProvisionUtils(operationalDataStore, aggregationDataStore, dataMartDataStore,
        thirdPartyScansDataStore);
  }

  @Override
  public void run(String... arguments) throws Exception {
    TenantManager.initGlobalTenant();

    super.run(arguments);
  }

  @Override
  protected void customize(InsightConfig config, Environment env) {
    super.customize(config, env);

    addServletFilter(env, TenantUrlFilter.class, "/*");
  }

  @Override
  protected List<Module> modules(InsightConfig config) {
    Module bindings = new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(com.sonatype.insight.jaxrs.error.ErrorResponseGenerator.class).to(ErrorResponseGenerator.class);
        bind(CsvMapper.class).toInstance(configureObjectMapper(new CsvMapper()));
      }
    };

    Module authc = new SecurityModule();
    Module authz = new SecurityAopModule();
    Module dbModule = new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(OperationalDataStore.class).toInstance(OperationalDataStoreProvider.getInstance());
        bind(AggregationDataStore.class).toInstance(AggregationDataStoreProvider.getInstance());
        bind(DataMartDataStore.class).toInstance(DatamartProvider.getInstance());
        bind(ThirdPartyScansDataStore.class).toInstance(ThirdPartyScansProvider.getInstance());
      }
    };

    return Arrays.asList(bindings, authc, authz, dbModule, buildMultiTenantModule());
  }

  private Module buildMultiTenantModule() {
    return new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(ExecutorThreadPools.class).to(MultiTenantExecutorThreadPools.class);
        requestStaticInjection(ExecutorThreadPools.class);

        bind(QuartzJobInitializer.class).to(MultiTenantQuartzJobInitializer.class).in(Singleton.class);
        bind(DatabaseProvisionUtils.class).toInstance(databaseProvisionUtils);
      }
    };
  }
}
