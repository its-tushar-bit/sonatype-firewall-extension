/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.MultiTenantAggregationDataStore;
import com.sonatype.insight.brain.db.MultiTenantDataMartDataStore;
import com.sonatype.insight.brain.db.MultiTenantDataSourceFactory;
import com.sonatype.insight.brain.db.MultiTenantDatabaseConfigProvider;
import com.sonatype.insight.brain.db.MultiTenantOperationalDataStore;
import com.sonatype.insight.brain.db.MultiTenantThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.hds.MultiTenantTelemetryId;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.migration.MultiTenantDbMigrationCommand;
import com.sonatype.insight.brain.scheduler.MultiTenantQuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.SecurityAopModule;
import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.tenancy.MultiTenantExecutorThreadPools;
import com.sonatype.insight.brain.tenancy.MultiTenantTenantManagedInitializer;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantUrlFilter;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Environment;

public class MultiTenantInsightBrainService
    extends InsightBrainService
{
  /**
   * SisuApplication#addManaged loads Managed beans outside the normal flow but does not respect the @Priority
   * annotation however it does call SisuApplication#acceptComponent to check whether the component should be loaded or
   * not. We make use of that functionality here to prevent Default* (i.e. on-prem implmentations) being loaded in MTIQ
   * and causing conflicting behaviour.
   */
  private static final List<Class> BANNED_IMPLEMENTATIONS =
      Arrays.asList(new Class[]{DefaultTenantManagedInitializer.class});

  public static void main(final String[] args) {
    TenantManager.initGlobalTenant();

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
  protected DatabaseContainer createDatabaseContainer() {
    MultiTenantDataSourceFactory multiTenantDataSourceFactory = new MultiTenantDataSourceFactory();

    DatabaseMigrator databaseMigrator = new DatabaseMigrator(multiTenantDataSourceFactory);

    OperationalDataStore operationalDataStore =
        new MultiTenantOperationalDataStore(multiTenantDataSourceFactory, databaseMigrator);
    AggregationDataStore aggregationDataStore =
        new MultiTenantAggregationDataStore(multiTenantDataSourceFactory, databaseMigrator);
    DataMartDataStore dataMartDataStore =
        new MultiTenantDataMartDataStore(multiTenantDataSourceFactory, databaseMigrator);
    ThirdPartyScansDataStore thirdPartyScansDataStore =
        new MultiTenantThirdPartyScansDataStore(multiTenantDataSourceFactory, databaseMigrator);

    DatabaseProvisionUtils databaseProvisionUtils =
        new DatabaseProvisionUtils(operationalDataStore, aggregationDataStore, dataMartDataStore,
            thirdPartyScansDataStore);

    return new DatabaseContainer(multiTenantDataSourceFactory, databaseProvisionUtils);
  }

  @Override
  protected DatabaseConfigProvider getDatabaseConfigProvider(final InsightConfig insightConfig) {
    return new MultiTenantDatabaseConfigProvider(insightConfig);
  }

  @Override
  public void run(String... arguments) throws Exception {
    TenantManager.initGlobalTenant();

    super.run(arguments);
  }

  @Override
  public void run(InsightConfig configuration, Environment environment) throws Exception {
    // The MTIQ has additional control over the 'locks' DataSource object. The configuration for this comes from a
    // custom property defined in MultiTenantInsightConfig which we then need to set into the factory.
    MultiTenantDataSourceFactory dataSourceFactory =
        (MultiTenantDataSourceFactory) databaseContainer.getDataSourceFactory();
    dataSourceFactory.setInsightConfig((MultiTenantInsightConfig) configuration);

    super.run(configuration, environment);
  }

  @Override
  protected void addServletFilters(Environment env) {
    addServletFilter(env, TenantUrlFilter.class, "/*");

    super.addServletFilters(env);
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
        bind(ThirdPartyScansDataStore.class).toInstance(ThirdPartyScansProvider.getInstance());
        bind(DatabaseConfigProvider.class).toInstance(getDatabaseConfigProvider(config));
      }
    };

    return Arrays.asList(bindings, authc, authz, dbModule, buildMultiTenantModule());
  }

  @Override
  protected Command createDbMigrationCommand(final DatabaseProvisionUtils databaseProvisionUtils) {
    return new MultiTenantDbMigrationCommand(databaseProvisionUtils);
  }

  private Module buildMultiTenantModule() {
    return new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(ExecutorThreadPools.class).to(MultiTenantExecutorThreadPools.class);
        requestStaticInjection(ExecutorThreadPools.class);

        bind(TenantManagedInitializer.class).to(MultiTenantTenantManagedInitializer.class).in(Singleton.class);
        bind(DatabaseProvisionUtils.class).toInstance(databaseContainer.getDatabaseProvisionUtils());

        bind(ApplicationLifecycle.class).to(MultiTenantApplicationLifecycle.class);
        bind(InsightConfig.class).to(MultiTenantInsightConfig.class);

        bind(QuartzJobStoreTX.class).to(MultiTenantQuartzJobStoreTX.class);
        bind(TaskScheduler.class).to(MultiTenantTaskScheduler.class);

        bind(TelemetryId.class).to(MultiTenantTelemetryId.class);
      }
    };
  }

  @Override
  public Class getConfigurationClass() {
    return MultiTenantInsightConfig.class;
  }

  @Override
  protected boolean acceptComponent(Class<?> type) {
    if (BANNED_IMPLEMENTATIONS.contains(type)) {
      return false;
    }

    return super.acceptComponent(type);
  }
}
