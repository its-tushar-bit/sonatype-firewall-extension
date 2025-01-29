/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.ws.rs.Path;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.admin.authorization.JwtHttpAuthorizationFilter;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkAuth0Provider;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkLocalProvider;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkProvider;
import com.sonatype.insight.brain.api.admin.service.MtiqScmNodeProcessor;
import com.sonatype.insight.brain.api.admin.service.MultiTenantActiveRequestCounterFilter;
import com.sonatype.insight.brain.api.v2.service.ConfigurationUtils;
import com.sonatype.insight.brain.audit.AdminAuditContainerRequestFilter;
import com.sonatype.insight.brain.aws.credentials.MtiqAwsCredentialsProvider;
import com.sonatype.insight.brain.configuration.webhook.WebhookService;
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
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.git.BranchMonitorExecutor;
import com.sonatype.insight.brain.git.MultiTenantDefaultBranchMonitorExecutor;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.MultiTenantTelemetryId;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.health.ServerBootHealthCheck;
import com.sonatype.insight.brain.micrometer.MultiTenantMeterRegistryProvider;
import com.sonatype.insight.brain.migration.MigrateTenantsCommand;
import com.sonatype.insight.brain.migration.MultiTenantDbMigrationCommand;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ConditionValueTypes;
import com.sonatype.insight.brain.product.license.DefaultProductLicense;
import com.sonatype.insight.brain.product.license.MultiTenantProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.scheduler.MultiTenantQuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.DefaultEncryptionKeyStore;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.MultiTenantEncryptionKeyStore;
import com.sonatype.insight.brain.security.MultiTenantSsoUserService;
import com.sonatype.insight.brain.security.SecurityAopModule;
import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.UserDirectory;
import com.sonatype.insight.brain.service.banning.BannedImplementationService;
import com.sonatype.insight.brain.service.banning.MTIQFeatureService;
import com.sonatype.insight.brain.service.banning.rest.BlockEndpointsContainerRequestFilter;
import com.sonatype.insight.brain.shutdown.ActiveRequestCounterFilter;
import com.sonatype.insight.brain.telemetry.MultiTenantTelemetryCollectorsProvider;
import com.sonatype.insight.brain.telemetry.TelemetryCollectorsProvider;
import com.sonatype.insight.brain.tenancy.AdminTasksTenantFilter;
import com.sonatype.insight.brain.tenancy.AdminTenantFilter;
import com.sonatype.insight.brain.tenancy.MultiTenantExecutorThreadPools;
import com.sonatype.insight.brain.tenancy.MultiTenantTenantManagedInitializer;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUrlFilter;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.users.MultiTenantUserDirectory;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.validation.MtiqSourceControlSshValidator;
import com.sonatype.insight.brain.validation.SourceControlSshValidator;
import com.sonatype.insight.brain.version.MultiTenantVersionService;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.jaxrs.ComponentIdentifierParamConverterProvider;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import io.dropwizard.core.cli.Command;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;
import ru.vyarus.dropwizard.guice.GuiceBundle;
import ru.vyarus.dropwizard.guice.module.installer.feature.jersey.ResourceInstaller;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public class MultiTenantInsightBrainService
    extends InsightBrainService
{
  private static final String MULTI_TENANT_SERVER_NAME = "MTIQ Server";

  private static final String MULTI_TENANT_BATCH_NAME = "MTIQ Server (Batch Mode)";

  public static final String ADMIN_BASE_PATH = "/api/*";

  public static final String NXIQ_ENABLE_LOCAL_JWK_PROVIDER_ENV_VAR = "NXIQ_ENABLE_LOCAL_JWK_PROVIDER";

  private final BannedImplementationService bannedImplementationService = new BannedImplementationService();

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
    environment.jersey().register(new BlockEndpointsContainerRequestFilter());

    // Ensuring we have the same jersey configuration we have for the application context
    adminResourceBundle.jersey().register(new InsightJacksonMessageBodyProvider(environment.getObjectMapper()));
    adminResourceBundle.jersey().register(new ComponentIdentifierParamConverterProvider(environment.getObjectMapper()));
    adminResourceBundle.jersey().register(new AdminAuditContainerRequestFilter());
    JaxRsExceptionMapper jaxRsExceptionMapper = getInstance(JaxRsExceptionMapper.class);
    adminResourceBundle.jersey().register(jaxRsExceptionMapper);

    BeanLocator locator = getInjector().getInstance(BeanLocator.class);
    addAdminApiEndpoints(locator);
  }

  private void addAdminApiEndpoints(BeanLocator locator) {
    // Unfortunately JAX-RS annotations are not a qualifier in JSR-330, so we need to check all known bindings.
    // (In practice this isn't that slow because of various caches in Sisu to optimize lookups.)
    // We could always optimize this by introducing a marker interface for injectable resources.
    for (BeanEntry<Annotation, Object> resourceBeanEntry : locate(locator, Object.class)) {
      Class<?> impl = resourceBeanEntry.getImplementationClass();
      if (impl != null && (impl.isAnnotationPresent(Path.class) && impl.isAnnotationPresent(MtiqAdminEndpoint.class))) {
        try {
          Object component = resourceBeanEntry.getValue();
          adminResourceBundle.jersey().register(component);
          log.debug("Added admin REST component: {}", component);
        }
        catch (Exception e) {
          log.error("Unable to add admin REST component: {}", impl, e);
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
    modules.add(new DropwizardAwareModule<InsightConfig>()
    {
      @Override
      protected void configure() {
        bind(OperationalDataStore.class).toInstance(databaseContainer.getOperationalDataStore());
        bind(AggregationDataStore.class).toInstance(databaseContainer.getAggregationDataStore());
        bind(DataMartDataStore.class).toInstance(databaseContainer.getDataMartDataStore());
        bind(ThirdPartyScansDataStore.class).toInstance(databaseContainer.getThirdPartyScansDataStore());
        bind(DataStoreProvider.class).toInstance(databaseContainer);
        bind(ClusterLockManager.class).toProvider(ClusterLockManagerProvider.class);
        bind(DatabaseConfigProvider.class).toInstance(getDatabaseConfigProvider(configuration()));
      }
    });

    modules.add(buildMultiTenantModule());

    modules.addAll(baseModules());

    // Set up bindings based on which database is used.
    modules.add(new DbBasedModule(() -> databaseContainer));

    return modules;
  }

  @Override
  protected Command createDbMigrationCommand() {
    return new MultiTenantDbMigrationCommand();
  }

  protected Module buildMultiTenantModule() {
    return new DropwizardAwareModule<InsightConfig>()
    {
      @Override
      protected void configure() {
        bind(ExecutorThreadPools.class).to(MultiTenantExecutorThreadPools.class);

        requestStaticInjection(ExecutorThreadPools.class);
        requestStaticInjection(ConditionTypes.class);
        requestStaticInjection(ConditionValueTypes.class);
        requestStaticInjection(ConfigurationUtils.class);
        requestStaticInjection(ComponentDetailsLoader.class);
        requestStaticInjection(SystemConfigurationPropertyFeature.class);

        bind(TenantManagedInitializer.class).to(MultiTenantTenantManagedInitializer.class).in(Singleton.class);

        bind(DatabaseProvisioner.class).toInstance(databaseContainer.getDatabaseProvisioner());

        bind(ApplicationLifecycle.class).to(DefaultApplicationLifecycle.class);

        bind(QuartzJobStoreTX.class).to(MultiTenantQuartzJobStoreTX.class);
        bind(TaskScheduler.class).to(MultiTenantTaskScheduler.class);

        bind(TelemetryId.class).to(MultiTenantTelemetryId.class);
        bind(TelemetryCollectorsProvider.class).to(MultiTenantTelemetryCollectorsProvider.class);

        bind(FeaturesService.class).to(MTIQFeatureService.class);

        bind(AwsCredentialsProvider.class).toProvider(MtiqAwsCredentialsProvider.class);

        bind(VersionService.class).to(MultiTenantVersionService.class);

        bind(MultiTenantJwkProvider.class).toInstance(getMultitenantJwkProvider(configuration()));

        bind(UserDirectory.class).to(MultiTenantUserDirectory.class);

        bind(InsightMail.class).to(MultiTenantInsightMail.class);

        bind(BranchMonitorExecutor.class).to(MultiTenantDefaultBranchMonitorExecutor.class);

        bind(SourceControlSshValidator.class).to(MtiqSourceControlSshValidator.class);

        bind(ActiveRequestCounterFilter.class).to(MultiTenantActiveRequestCounterFilter.class);

        bind(MeterRegistry.class).toProvider(MultiTenantMeterRegistryProvider.class);

        bind(SsoUserService.class).to(MultiTenantSsoUserService.class);

        bind(WebhookService.class).to(MultiTenantWebhookService.class);

        bind(ProductLicense.class).to(MultiTenantProductLicense.class);
        bind(DefaultProductLicense.class).to(MultiTenantProductLicense.class);
        bind(ScmNodeProcessor.class).to(MtiqScmNodeProcessor.class);

        List<Class<?>> extraToBan = new ArrayList<>();
        if (((MultiTenantInsightConfig) configuration()).isUsingDefaultEncryptionKeyStore()) {
          bind(EncryptionKeyStore.class).to(DefaultEncryptionKeyStore.class).in(Singleton.class);
          bind(MultiTenantEncryptionKeyStore.class).toProvider(Providers.of(null));
          extraToBan.add(MultiTenantEncryptionKeyStore.class);
        }
        else {
          bind(EncryptionKeyStore.class).to(MultiTenantEncryptionKeyStore.class).in(Singleton.class);
          bind(DefaultEncryptionKeyStore.class).toProvider(Providers.of(null));
          extraToBan.add(DefaultEncryptionKeyStore.class);
        }
        bannedImplementationService.setupBannedClasses(extraToBan.toArray(new Class[0]));
      }
    };
  }

  protected MultiTenantJwkProvider getMultitenantJwkProvider(final InsightConfig insightConfig) {
    boolean localJwkProviderEnabled = Boolean.parseBoolean(System.getenv().get(NXIQ_ENABLE_LOCAL_JWK_PROVIDER_ENV_VAR));

    if (localJwkProviderEnabled) {
      return new MultiTenantJwkLocalProvider();
    }
    return new MultiTenantJwkAuth0Provider((MultiTenantInsightConfig) insightConfig);
  }

  @Override
  public Class getConfigurationClass() {
    return MultiTenantInsightConfig.class;
  }

  @Override
  protected DropwizardAwareModule wire(final List<Module> modules) {
    return bannedImplementationService.getBannedModule(modules);
  }

  @Override
  protected GuiceBundle.Builder customizeGuiceBundle(GuiceBundle.Builder builder) {
    return builder
        .disableInstallers(ResourceInstaller.class)
        .installers(MtiqResourceInstaller.class);
  }
}
