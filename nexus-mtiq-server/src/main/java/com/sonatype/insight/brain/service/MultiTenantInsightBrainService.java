/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.ws.rs.Path;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.admin.authorization.JwtHttpAuthorizationFilter;
import com.sonatype.insight.brain.audit.AdminAuditContainerRequestFilter;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManagerProvider;
import com.sonatype.insight.brain.datadog.DatadogInterceptor;
import com.sonatype.insight.brain.db.DatabaseConfigProvider;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.MultiTenantDatabaseConfigProvider;
import com.sonatype.insight.brain.db.MultiTenantDatabaseContainer;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.health.ServerBootHealthCheck;
import com.sonatype.insight.brain.mcp.McpModule;
import com.sonatype.insight.brain.migration.MigrateTenantsCommand;
import com.sonatype.insight.brain.migration.MultiTenantDbMigrationCommand;
import com.sonatype.insight.brain.search.SearchModule;
import com.sonatype.insight.brain.security.SecurityAopModule;
import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.service.banning.rest.BlockEndpointsContainerRequestFilter;
import com.sonatype.insight.brain.service.modules.ApiServiceBindingsModule;
import com.sonatype.insight.brain.service.modules.AuthenticationModule;
import com.sonatype.insight.brain.service.modules.ComponentModule;
import com.sonatype.insight.brain.service.modules.CoreServiceModule;
import com.sonatype.insight.brain.service.modules.DashboardModule;
import com.sonatype.insight.brain.service.modules.DataAccessModule;
import com.sonatype.insight.brain.service.modules.FirewallModule;
import com.sonatype.insight.brain.service.modules.IntegrationModule;
import com.sonatype.insight.brain.service.modules.MigrationModule;
import com.sonatype.insight.brain.service.modules.MtiqOnlyAuthModule;
import com.sonatype.insight.brain.service.modules.MtiqOnlyModule;
import com.sonatype.insight.brain.service.modules.OperationalModule;
import com.sonatype.insight.brain.service.modules.OrganizationModule;
import com.sonatype.insight.brain.service.modules.PolicyModule;
import com.sonatype.insight.brain.service.modules.ProductLicenseModule;
import com.sonatype.insight.brain.service.modules.RepositoryModule;
import com.sonatype.insight.brain.service.modules.ScannerModule;
import com.sonatype.insight.brain.service.modules.SonatypeLicensingModule;
import com.sonatype.insight.brain.service.modules.TelemetryModule;
import com.sonatype.insight.brain.shutdown.ActiveRequestCounterFilter;
import com.sonatype.insight.brain.tenancy.AdminTasksTenantFilter;
import com.sonatype.insight.brain.tenancy.AdminTenantFilter;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUrlFilter;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.version.MultiTenantVersionService;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.jaxrs.ComponentIdentifierParamConverterProvider;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import io.dropwizard.core.cli.Command;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.apache.shiro.guice.web.GuiceShiroFilter;
import ru.vyarus.dropwizard.guice.GuiceBundle;
import ru.vyarus.dropwizard.guice.module.installer.feature.jersey.ResourceInstaller;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

public class MultiTenantInsightBrainService
    extends InsightBrainService
{
  private static final String MULTI_TENANT_SERVER_NAME = "MTIQ Server";

  private static final String MULTI_TENANT_BATCH_NAME = "MTIQ Server (Batch Mode)";

  public static final String ADMIN_BASE_PATH = "/api/*";

  /**
   * Instance of the admin resources bundle needed to register Admin APIs
   */
  private final AdminResourceBundle adminResourceBundle = new AdminResourceBundle(ADMIN_BASE_PATH);

  private static void assertRunningAsGlobalTenant() {
    if (!new TenantUtil().isGlobalTenant()) {
      System.err.println(
          "Fatal error: Expecting to run as GLOBAL tenant, but found tenant: " + TenantThreadLocal.getTenant());
      System.exit(10);
    }
  }

  public static void main(final String[] args) {
    // WARNING: No code that uses tenancy should be added before this line. Even if it doesn't touch tenancy, it's still
    // better to avoid adding code before this line.
    TenantThreadLocal.setDefaultTenantToGlobal();
    assertRunningAsGlobalTenant();

    datadog.trace.api.GlobalTracer.get().addTraceInterceptor(new DatadogInterceptor());

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
    String name = new TenantUtil().isMtiqBatchMode() ? MULTI_TENANT_BATCH_NAME : MULTI_TENANT_SERVER_NAME;
    return name + " build " + build + " instance ID " + INSTANCE_ID + " on " + getLocalHostString() + ".";
  }

  @Override
  public DatabaseContainer createDatabaseContainer(final InsightConfig insightConfig) {
    return new MultiTenantDatabaseContainer((MultiTenantInsightConfig) insightConfig);
  }

  @Override
  protected DatabaseConfigProvider getDatabaseConfigProvider(final InsightConfig insightConfig) {
    return new MultiTenantDatabaseConfigProvider(insightConfig);
  }

  @Override
  public void run(String... arguments) throws Exception {
    new TenantUtil().setGlobalTenant();

    super.run(arguments);
    ServerBootHealthCheck.fullyBooted();
  }

  @Override
  public void initialize(final Bootstrap<InsightConfig> bootstrap) {
    super.initialize(bootstrap);
    bootstrap.addCommand(new MigrateTenantsCommand());
    bootstrap.addBundle(adminResourceBundle);
  }

  @Override
  protected void configureObjectMapperDeserializationFeature(ObjectMapper objectMapper) {
    // Disable for MTIQ, allow unknown properties in Insight Config
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @Override
  protected void customize(InsightConfig configuration, Environment environment) {
    super.customize(configuration, environment);

    // Most jersey components are injected automatically by dropwizard-guicey. However it seems to be unable
    // to correctly handle @Context injections on @Providers. So for that case, we register them here. Note that
    // even doing it manually, jersey won't do the @Context injection if you provide it a class, it only seems to
    // work with an instance. This means that classes registered this way are effectively singletons
    //
    // Register the filter classes directly and let dropwizard-guicey's Jersey integration handle the
    // Provider<ResourceInfo> injection via its JerseyComponentProvider bridge.
    environment.jersey().register(BlockEndpointsContainerRequestFilter.class);

    // Ensuring we have the same jersey configuration we have for the application context
    adminResourceBundle.jersey().register(new InsightJacksonMessageBodyProvider(environment.getObjectMapper()));
    adminResourceBundle.jersey().register(new ComponentIdentifierParamConverterProvider(environment.getObjectMapper()));
    adminResourceBundle.jersey().register(AdminAuditContainerRequestFilter.class);
    JaxRsExceptionMapper jaxRsExceptionMapper = getInstance(JaxRsExceptionMapper.class);
    adminResourceBundle.jersey().register(jaxRsExceptionMapper);

    addAdminApiEndpoints();
  }

  private void addAdminApiEndpoints() {
    // Use pure Guice to discover admin endpoints
    // Get all bindings from the injector and check for MtiqAdminEndpoint annotation
    Injector injector = getInjector();

    for (com.google.inject.Binding<?> binding : injector.getAllBindings().values()) {
      com.google.inject.Key<?> key = binding.getKey();
      Class<?> type = key.getTypeLiteral().getRawType();

      // Check if this class has both @Path and @MtiqAdminEndpoint annotations
      if (type.isAnnotationPresent(Path.class) && type.isAnnotationPresent(MtiqAdminEndpoint.class)) {
        try {
          Object component = injector.getInstance(key);
          adminResourceBundle.jersey().register(component);
          log.debug("Added admin REST component: {}", component);
        }
        catch (Exception e) {
          log.error("Unable to add admin REST component: {}", type, e);
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
    env.admin()
        .addFilter(filterType.getSimpleName(), filter)
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlPatterns);
  }

  @Override
  protected void addServletFilters(Environment env) {
    addServletFilter(env, true, ActiveRequestCounterFilter.class, "/*");

    addServletFilter(env, true, TenantUrlFilter.class, "/*");

    // We need to add the Header filter for the Admin endpoints before Admin Resources filter
    addAdminServletFilter(env, MultiTenantServerHeaderFilter.class, ServerHeaderFilter.URL_PATTERNS);

    // Add tenant filter for Admin resources. We need to ensure this filter is configured before the AuditFilter.
    addAdminServletFilter(env, AdminTenantFilter.class, ADMIN_BASE_PATH);

    // Add tenant filter for admin tasks api. We need to ensure this filter is configured after the AdminTenantFilter.
    addAdminServletFilter(env, AdminTasksTenantFilter.class, "/api/admin/tenants/*", "/tasks/*");

    // Add Authorization filter
    addAdminServletFilter(env, JwtHttpAuthorizationFilter.class, ADMIN_BASE_PATH);

    super.addServletFilters(env, true);
  }

  @Override
  protected void addServerHeaderFilter(final Environment env) {
    addServletFilter(env, false, MultiTenantServerHeaderFilter.class, ServerHeaderFilter.URL_PATTERNS);
  }

  @Override
  protected List<Module> modules() {
    List<Module> modules = new ArrayList<>();

    // Ensure URL rewriting filter is registered before other modules' filters, so they will act on the rewritten URLs
    modules.add(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        bind(PlatformContextFilter.class).in(Singleton.class);
        filter(List.of(PlatformContextFilter.URL_PATTERNS)).through(PlatformContextFilter.class);
      }
    });

    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(com.sonatype.insight.jaxrs.error.ErrorResponseGenerator.class).to(ErrorResponseGenerator.class);
        bind(CsvMapper.class).toInstance(configureObjectMapper(new CsvMapper()));

        bind(GuiceShiroFilter.class);

        // This binding is referenced by a class present in sonatype-licensing that we don't actually use.
        // For unclear reasons, since the switch to dropwizard-guicey leaving this binding null has prevented
        // the server from starting. A proper solution cound not be found, so just fill it in with a dummy value
        bind(File.class).annotatedWith(Names.named("licensing.access.file")).toInstance(new File("workaround"));
      }
    });

    modules.add(new SecurityModule());
    modules.add(new SecurityAopModule());
    modules.add(new SearchModule());
    modules.add(new DropwizardAwareModule<InsightConfig>()
    {
      @Override
      protected void configure() {
        bind(OperationalDataStore.class).toInstance(databaseContainer.getOperationalDataStore());
        bind(AggregationDataStore.class).toInstance(databaseContainer.getAggregationDataStore());
        bind(DataMartDataStore.class).toInstance(databaseContainer.getDataMartDataStore());
        bind(ThirdPartyScansDataStore.class).toInstance(databaseContainer.getThirdPartyScansDataStore());
        bind(DataStoreProvider.class).toInstance(databaseContainer);
        // Bind ClusterLockManagerProvider so it can be injected, then use it as a provider
        bind(ClusterLockManagerProvider.class);
        bind(ClusterLockManager.class).toProvider(new com.google.inject.Provider<ClusterLockManager>()
        {
          @Inject
          ClusterLockManagerProvider provider;

          @Override
          public ClusterLockManager get() {
            return provider.get();
          }
        });
        bind(DatabaseConfigProvider.class).toInstance(getDatabaseConfigProvider(configuration()));

        // MTIQ-specific bindings that need access to configuration or databaseContainer
        bind(DatabaseProvisioner.class).toInstance(databaseContainer.getDatabaseProvisioner());
      }
    });

    modules.addAll(baseModules());

    // Set up bindings based on which database is used.
    modules.add(new DbBasedModule(() -> databaseContainer));

    return modules;
  }

  @Override
  protected Command createDbMigrationCommand() {
    return new MultiTenantDbMigrationCommand();
  }

  @Override
  protected List<Module> getAppModules() {
    List<Module> modules = new ArrayList<>();

    modules.add(new ApiServiceBindingsModule());
    modules.add(new ComponentModule());
    modules.add(new CoreServiceModule());
    modules.add(new DashboardModule());
    modules.add(new DataAccessModule());
    modules.add(new FirewallModule());
    modules.add(new IntegrationModule());
    modules.add(new MtiqOnlyModule());
    modules.add(new MtiqOnlyAuthModule());
    modules.add(new MigrationModule());
    modules.add(new OperationalModule());
    modules.add(new OrganizationModule());
    modules.add(new PolicyModule());
    modules.add(new ProductLicenseModule());
    modules.add(new SonatypeLicensingModule());
    modules.add(new RepositoryModule());
    modules.add(new ScannerModule());
    modules.add(new AuthenticationModule());
    modules.add(new TelemetryModule());
    modules.add(new McpModule());

    return modules;
  }

  @Override
  public Class getConfigurationClass() {
    return MultiTenantInsightConfig.class;
  }

  @Override
  protected GuiceBundle.Builder customizeGuiceBundle(GuiceBundle.Builder builder) {
    return builder
        .disableInstallers(ResourceInstaller.class)
        .installers(MtiqResourceInstaller.class);
  }
}
