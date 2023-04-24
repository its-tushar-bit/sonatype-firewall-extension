/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.ws.rs.Path;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.audit.AdminAuditContainerRequestFilter;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.MultiTenantAuditRecorder;
import com.sonatype.insight.brain.component.MultiTenantRepositoryIdentifiedComponentCache;
import com.sonatype.insight.brain.component.RepositoryIdentifiedComponentCache;
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
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.hds.MultiTenantTelemetryId;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.migration.MultiTenantDbMigrationCommand;
import com.sonatype.insight.brain.scheduler.MultiTenantQuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.SecurityAopModule;
import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.service.banning.BannedImplementationService;
import com.sonatype.insight.brain.service.banning.MTIQFeatureService;
import com.sonatype.insight.brain.telemetry.MultiTenantTelemetryCollectorsProvider;
import com.sonatype.insight.brain.telemetry.TelemetryCollectorsProvider;
import com.sonatype.insight.brain.tenancy.AdminTenantFilter;
import com.sonatype.insight.brain.tenancy.MultiTenantExecutorThreadPools;
import com.sonatype.insight.brain.tenancy.MultiTenantTenantManagedInitializer;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUrlFilter;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.version.MultiTenantVersionService;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.jaxrs.ComponentIdentifierParamConverterProvider;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;

public class MultiTenantInsightBrainService
    extends InsightBrainService
{
  public static final String ADMIN_BASE_PATH = "/api/*";

  private final BannedImplementationService bannedImplementationService = new BannedImplementationService();

  /**
   * Instance of the admin resources bundle needed to register Admin APIs
   */
  private final AdminResourceBundle adminResourceBundle = new AdminResourceBundle(ADMIN_BASE_PATH);

  public static void main(final String[] args) {
    TenantThreadLocal.setDefaultTenantToGlobal();

    new TenantUtil().setGlobalTenant();

    try {
      MultiTenantInsightBrainService insightBrainService = new MultiTenantInsightBrainService();

      insightBrainService.setupServerLogging(args);

      if (!validateTempDir()) {
        System.exit(1);
      }

      insightBrainService.run(args);
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
  void printVersion() {
    VersionService versionService = new MultiTenantVersionService();
    String build = versionService.getBuild();
    log.info("|------------------------------------------");
    log.info("|");
    log.info("| Initializing {} build {}", PRODUCT_NAME, build);
    log.info("|");
    log.info("|------------------------------------------");
  }

  @Override
  String getServerInstanceMessage() {
    String build = new MultiTenantVersionService().getBuild();
    return PRODUCT_NAME + " build " + build + //
        " instance ID " + INSTANCE_ID + //
        " on " + getLocalHostString() + ".";
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
    new TenantUtil().setGlobalTenant();

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
  public void initialize(final Bootstrap<InsightConfig> bootstrap) {
    super.initialize(bootstrap);
    bootstrap.addBundle(adminResourceBundle);
  }

  @Override
  protected void customize(InsightConfig configuration, Environment environment) {
    super.customize(configuration, environment);

    // Ensuring we have the same jersey configuration we have for the application context
    adminResourceBundle.jersey().register(new InsightJacksonMessageBodyProvider(environment.getObjectMapper()));
    adminResourceBundle.jersey().register(new ComponentIdentifierParamConverterProvider(environment.getObjectMapper()));
    adminResourceBundle.jersey().register(AdminAuditContainerRequestFilter.class);
    JaxRsExceptionMapper jaxRsExceptionMapper = getInstance(JaxRsExceptionMapper.class);
    adminResourceBundle.jersey().register(jaxRsExceptionMapper);

    BeanLocator locator = getInjector().getInstance(BeanLocator.class);
    addAdminApiEndpoints(locator);
  }

  private void addAdminApiEndpoints(BeanLocator locator) {
    // Unfortunately JAX-RS annotations are not a qualifier in JSR-330, so we need to check all known bindings.
    // (In practice this isn't that slow because of various caches in Sisu to optimize lookups.)
    // We could always optimize this by introducing a marker interface for injectable resources.
    //
    for (BeanEntry<Annotation, Object> resourceBeanEntry : locate(locator, Object.class)) {
      Class<?> impl = resourceBeanEntry.getImplementationClass();
      if (impl != null && (impl.isAnnotationPresent(Path.class) && impl.isAnnotationPresent(MtiqAdminEndpoint.class))) {
        try {
          Object component = resourceBeanEntry.getValue();
          adminResourceBundle.jersey().register(component);
          log.debug("Added admin REST component: {}", component);
        }
        catch (Exception e) {
          log.warn("Unable to add admin REST component: {}", impl, e);
        }
      }
    }
  }

  private void addAdminServletFilter(
      Environment env,
      Class<? extends Filter> filterType,
      String... urlPatterns)
  {
    Filter filter = getInstance(filterType);
    env.admin().addFilter(filterType.getSimpleName(), filter)
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlPatterns);
  }

  @Override
  protected void addServletFilters(Environment env) {
    addServletFilter(env, true, TenantUrlFilter.class, "/*");

    // We need to add the Header filter for the Admin endpoints before Admin Resources filter
    addAdminServletFilter(env, MultiTenantServerHeaderFilter.class, ServerHeaderFilter.URL_PATTERNS);

    // Add tenant filter for Admin resources. We need to ensure this filter is configured before the AuditFilter.
    addAdminServletFilter(env, AdminTenantFilter.class, ADMIN_BASE_PATH);

    super.addServletFilters(env, true);
  }

  @Override
  protected void addServerHeaderFilter(final Environment env) {
    addServletFilter(env, false, MultiTenantServerHeaderFilter.class, ServerHeaderFilter.URL_PATTERNS);
  }

  @Override
  protected List<Module> modules(InsightConfig config) {
    Module bindings = new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(com.sonatype.insight.jaxrs.error.ErrorResponseGenerator.class).to(ErrorResponseGenerator.class);
        bind(CsvMapper.class).toInstance(configureObjectMapper(new CsvMapper()));

        bind(GuiceShiroFilter.class);
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

  protected Module buildMultiTenantModule() {
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
        bind(TelemetryCollectorsProvider.class).to(MultiTenantTelemetryCollectorsProvider.class);

        bind(RepositoryIdentifiedComponentCache.class).to(MultiTenantRepositoryIdentifiedComponentCache.class);

        bind(FeaturesService.class).to(MTIQFeatureService.class);

        bind(AuditRecorder.class).to(MultiTenantAuditRecorder.class);

        bind(VersionService.class).to(MultiTenantVersionService.class);
      }
    };
  }

  @Override
  public Class getConfigurationClass() {
    return MultiTenantInsightConfig.class;
  }

  @Override
  protected boolean acceptComponent(Class<?> type) {
    if (bannedImplementationService.isBanned(type)) {
      return false;
    }
    return super.acceptComponent(type);
  }

  @Override
  protected Module wire(final List<Module> modules) {
    return bannedImplementationService.getBannedModule(modules);
  }
}
