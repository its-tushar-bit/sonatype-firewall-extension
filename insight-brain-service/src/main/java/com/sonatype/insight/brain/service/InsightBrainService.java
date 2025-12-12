/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.security.Provider;
import java.security.Security;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.validation.Validator;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ConfigurationUtils;
import com.sonatype.insight.brain.audit.AuditContainerRequestFilter;
import com.sonatype.insight.brain.audit.AuditFilter;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManagerProvider;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.db.DatabaseConfigProvider;
import com.sonatype.insight.brain.db.DatabaseConfigProviderFactory;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DefaultDatabaseContainer;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.DataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.filter.ThrowableHandler;
import com.sonatype.insight.brain.firewall.FirewallRedirectFilter;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.landing.IndexCacheControlFilter;
import com.sonatype.insight.brain.metrics.CustomMetrics;
import com.sonatype.insight.brain.migration.DbMigrationCommand;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ConditionValueTypes;
import com.sonatype.insight.brain.search.SearchModule;
import com.sonatype.insight.brain.search.opensearch.IndexConfigProvider;
import com.sonatype.insight.brain.search.opensearch.SingleTenantIndexConfigProvider;
import com.sonatype.insight.brain.security.AuthenticationLoggingFilter;
import com.sonatype.insight.brain.security.ContentTypeOptionsHeaderFilter;
import com.sonatype.insight.brain.security.CspHeaderFilter;
import com.sonatype.insight.brain.security.FIPSModeDetector;
import com.sonatype.insight.brain.security.HttpHeaderValidatorFilter;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.security.SecurityAopModule;
import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.shutdown.ActiveRequestCounterFilter;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.validation.DefaultSourceControlSshValidator;
import com.sonatype.insight.brain.validation.SourceControlSshValidator;
import com.sonatype.insight.brain.version.DefaultVersionService;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.jaxrs.ComponentIdentifierParamConverterProvider;
import com.sonatype.insight.jaxrs.error.JavaLangErrorHandler;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;

import com.codahale.metrics.servlets.PingServlet;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.cli.Cli;
import io.dropwizard.core.cli.Command;
import io.dropwizard.core.cli.ServerCommand;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jackson.AnnotationSensitivePropertyNamingStrategy;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import io.dropwizard.jackson.GuavaExtrasModule;
import io.dropwizard.util.JarLocation;
import io.dropwizard.web.WebBundle;
import io.dropwizard.web.conf.WebConfiguration;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

import static com.sonatype.insight.brain.security.FIPSProviderFactory.createFipsProvider;

public class InsightBrainService
    extends SisuApplication<InsightConfig>
{
  protected static final Logger log = LoggerFactory.getLogger(InsightBrainService.class);

  static final String PRODUCT_NAME = "Nexus IQ Server";

  static {
    // INSIGHT-4557
    System.setProperty("java.awt.headless", "true");
    ensureBouncyCastleProviderIsLowestPreference();
  }

  public static final String BRAIN_ASSET_PATH = "/assets/";

  public static final String POLICY_ASSET_PATH = "/policy-assets/";

  static final String INSTANCE_ID = UUID.randomUUID().toString();

  // Visible for testing
  static final String SISU_URL_CACHES = "sisu.url.caches";

  private static volatile File configFile;

  private static long startTime;

  // DatabaseContainer for the main 'ServerCommand' application (NOT for other commands like DbMigrationCommand)
  protected DatabaseContainer databaseContainer;

  private static void assertRunningAsSingleTenant() {
    if (!new TenantUtil().isSingleTenant()) {
      System.err.println(
          "Fatal error: Expecting to run as SINGLE tenant, but found tenant: " + TenantThreadLocal.getTenant());
      System.exit(10);
    }
  }

  private static void logInfoIfFIPSModeIsEnabled() {
    if (FIPSModeDetector.isEnabled()) {
      log.info("FIPS mode is enabled for Nexus IQ Server.");
    }
  }

  public static void main(final String[] args) {
    assertRunningAsSingleTenant();

    try {
      InsightBrainService insightBrainService = new InsightBrainService();

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

  // Visible for testing
  static void setSisuUrlCachesToTrueIfNotSet() {
    String sisuUrlCaches = System.getProperty(SISU_URL_CACHES);
    if (sisuUrlCaches == null) {
      System.setProperty(SISU_URL_CACHES, "true");
    }
  }

  void setupServerLogging(final String... args) {
    if (args.length == 0 || "server".equals(args[0])) {
      logServerInstanceMessage("Starting " + getServerInstanceMessage());
      addShutdownLogger();
    }
  }

  void addShutdownLogger() {
    Runtime.getRuntime().addShutdownHook(new Thread("Shutdown Logger")
    {
      @Override
      public void run() {
        String formattedStartTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        // The Support team required "Uptime" in this log message in https://issues.sonatype.org/browse/CLM-14607,
        // so it should not be removed or changed to something else.
        logServerInstanceMessage("Stopping " + getServerInstanceMessage() + " Uptime: " + formattedStartTime);
      }
    });
  }

  // First `run` is the DropWizard entry point
  @Override
  public void run(String... arguments) throws Exception {
    startTime = System.currentTimeMillis();
    setSisuUrlCachesToTrueIfNotSet();

    final Bootstrap<InsightConfig> bootstrap = new Bootstrap<>(this);
    bootstrap.addCommand(createDbMigrationCommand());
    // Note the DropWizard 'ServerCommand' is special for the main http application and passes 'this' in. For a main
    // server start the `#run(InsightConfig, Environment)` method is called next.
    bootstrap.addCommand(new ServerCommand<>(this)
    {
      private volatile InsightFileLock insightFileLock;

      // Second `run` is the entry point for the `ServerCommand` which is the main http server
      @Override
      protected void run(Bootstrap<InsightConfig> bootstrap, Namespace namespace, InsightConfig insightConfig)
          throws Exception
      {
        Files.createDirectories(insightConfig.getSonatypeWork().toPath());
        Files.createDirectories(insightConfig.getClusterDirectory().toPath());
        insightFileLock = new InsightFileLock(insightConfig);
        insightFileLock.lock();

        MDCUsernameScope.forSystem();
        printVersion();
        logInfoIfFIPSModeIsEnabled();

        // Note DatabaseContainer is created within the DropWizard 'ServerCommand#run' (see also DbMigrationCommand)
        databaseContainer = createAndInitDatabaseContainer(insightConfig);

        String configArg = namespace.getString("file");
        InsightBrainService.configFile = new File(configArg).getAbsoluteFile();
        log.info("Configuration file: {}", InsightBrainService.configFile);
        super.run(bootstrap, namespace, insightConfig);
      }

      @Override
      public void onError(Cli cli, Namespace namespace, Throwable t) {
        // throw up to let our main() method do the desired error logging/handling
        throw new IllegalStateException("Fatal error trying to start server", t);
      }

      @Override
      protected void cleanup() {
        if (insightFileLock != null) {
          insightFileLock.release();
        }
        super.cleanup();
      }
    });
    initialize(bootstrap);
    CustomMetrics.registerMetrics(bootstrap.getMetricRegistry());

    final Cli cli = new Cli(new JarLocation(this.getClass()), bootstrap, System.out, System.err);
    cli.run(arguments);
  }

  protected Command createDbMigrationCommand() {
    return new DbMigrationCommand();
  }

  @VisibleForTesting
  static void ensureBouncyCastleProviderIsLowestPreference() {
    if (FIPSModeDetector.isEnabled()) {
      // Remove the BouncyCastleProvider if it is already present.
      // To only support the BouncyCastleFipsProvider and BouncyCastleJsseProvider.
      Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);

      // Adding BouncyCastleProviderFips here via Security.addProvider(...) ensures it gets the
      // lowest preference position (1) in the list of security providers.
      // This is important because the BouncyCastleProviderFips provider is used for FIPS mode
      // operations and we want to ensure that it is the first provider in the list.

      loadFipsProvider();
    }

    else {
      // Adding BouncyCastleProvider here via Security.addProvider(...) ensures it gets the lowest preference position.
      // This prevents org.keycloak.saml.processing.core.util.ProvidersUtil.ensure() from adding it
      // at a higher preference position, which can cause CLM-13629 due to the IQ Server uber JAR
      // invalidating BouncyCastleProvider by excluding
      // its signatures

      // First remove it in case it is already added
      Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
      // Second add it at the lowest preference position
      loadNonFipsProvider();
    }
  }

  public static File getConfigFile() {
    return configFile;
  }

  @VisibleForTesting
  public static void setConfigFile(final File testConfigFile) {
    configFile = testConfigFile;
  }

  // Third `run` method is for the main HTTP server `Application` itself
  @Override
  public void run(InsightConfig configuration, Environment environment) throws Exception {
    logServerInstanceMessage("Started " + getServerInstanceMessage());

    // Configure gzip to exclude text/csv for streaming endpoints
    configureGzipExclusions(environment);

    super.run(configuration, environment);

    bootApplicationLifecycle();
  }

  /**
   * Configures Jetty's GzipHandler to exclude the CSV streaming endpoint from compression.
   * This is critical for streaming CSV endpoints with keep-alive, as gzip compression
   * buffers the entire response before compressing, defeating the streaming mechanism.
   */
  private void configureGzipExclusions(Environment environment) {
    environment.lifecycle().addServerLifecycleListener(server -> {
      Handler rootHandler = server.getHandler();
      log.debug("Searching for GzipHandler in handler tree, root handler type: {}",
          rootHandler.getClass().getName());

      GzipHandler gzipHandler = findGzipHandler(rootHandler);

      if (gzipHandler != null) {
        gzipHandler.addExcludedPaths("/api/v2/componentSearch/downloadComponentSearchReport");
        log.info("Added CSV streaming endpoint to gzip path exclusions for streaming support");
      }
      else {
        log.warn("GzipHandler not found in handler tree - streaming endpoint compression may cause issues. " +
            "Root handler type: {}", rootHandler.getClass().getName());
      }
    });
  }

  private GzipHandler findGzipHandler(Handler handler) {
    if (handler == null) {
      return null;
    }

    log.debug("Checking handler type: {}", handler.getClass().getName());

    if (handler instanceof GzipHandler) {
      return (GzipHandler) handler;
    }

    // Check wrapped handler
    if (handler instanceof HandlerWrapper handlerWrapper) {
      GzipHandler found = findGzipHandler(handlerWrapper.getHandler());
      if (found != null) {
        return found;
      }
    }

    // Check handler collections
    if (handler instanceof HandlerCollection handlerCollection) {
      for (Handler child : handlerCollection.getHandlers()) {
        GzipHandler found = findGzipHandler(child);
        if (found != null) {
          return found;
        }
      }
    }

    return null;
  }

  public DatabaseContainer createDatabaseContainer(final InsightConfig insightConfig) {
    return new DefaultDatabaseContainer(insightConfig);
  }

  private DatabaseContainer createAndInitDatabaseContainer(InsightConfig configuration) {
    DatabaseContainer databaseContainer = createDatabaseContainer(configuration);

    DatabaseProvisioner databaseProvisioner = databaseContainer.getDatabaseProvisioner();
    databaseProvisioner.initializeDatabaseWithMigration();
    databaseProvisioner.validateMinimumSchemaVersion();

    return databaseContainer;
  }

  // Visible for testing
  void bootApplicationLifecycle() throws Exception {
    getInstance(ApplicationLifecycle.class).boot();
  }

  void printVersion() {
    VersionService versionService = new DefaultVersionService();
    String version = versionService.getLogDisplayVersion();
    String build = versionService.getBuild();
    log.info("|------------------------------------------");
    log.info("|");
    log.info("| Initializing {} 1 release {} build {}", PRODUCT_NAME, version, build);
    log.info("|");
    log.info("|------------------------------------------");
  }

  String getServerInstanceMessage() {
    String version = new DefaultVersionService().getLogDisplayVersion();
    return PRODUCT_NAME + " 1 release " + version + //
        " instance ID " + INSTANCE_ID + //
        " on " + getLocalHostString() + ".";
  }

  private static void logServerInstanceMessage(String message) {
    // Log to stdout first because the standard logging may not be operational at this point.
    System.out.println(message);
    log.info(message);
  }

  protected static boolean validateTempDir() {
    // Ensure that temp directory can be written to. If not, exit and log reason.
    String tmp = System.getProperty("java.io.tmpdir");
    try {
      File dir = new File(tmp);

      if (!dir.exists()) {
        if (dir.mkdirs()) {
          log.info("Created temporary folder: {}", dir.getAbsolutePath());
        }
      }
      else if (!dir.isDirectory()) {
        log.error("It appears that the temporary location is not a folder. Please ensure that {} is a folder "
            + "or specify another folder by adding -Djava.io.tmpdir=<writeable-folder> to the command line "
            + "used for launching the server.", dir.getAbsolutePath());
        return false;
      }

      // Ensure we can actually create and delete a new temp file
      File file = Files.createTempFile("clm-server-launcher", ".tmp").toFile();
      try {
        new FileCleaner().delete(file);
      }
      catch (FileDeletionException fde) {
        log.error("The server is not able to delete from the temporary folder. Please ensure server has access to {} "
            + "or specify another folder by adding -Djava.io.tmpdir=<writeable-folder> to the command line "
            + "used for launching the server.", dir.getAbsolutePath());
        return false;
      }
    }
    catch (IOException ex) {
      log.error("The server is not able to write to the temporary folder. Please ensure server has access to {} "
          + "or specify another folder by adding -Djava.io.tmpdir=<writeable-folder> to the command line "
          + "used for launching the server.", tmp);
      log.debug("Unable to validate temporary folder", ex);
      return false;
    }
    return true;
  }

  @Override
  public void initialize(final Bootstrap<InsightConfig> bootstrap) {
    super.initialize(bootstrap);
    configureObjectMapperDeserializationFeature(bootstrap.getObjectMapper());

    bootstrap.addBundle(new MultiPartBundle());

    bootstrap.addBundle(new AssetsBundle("/assets/", BRAIN_ASSET_PATH, "index.html", "assets"));

    // Legacy support for old reports
    bootstrap.addBundle(new AssetsBundle("/assets/policy/", POLICY_ASSET_PATH, "index.html", "policyAssets"));

    bootstrap.addBundle(new WebBundle<InsightConfig>()
    {
      @Override
      public WebConfiguration getWebConfiguration(final InsightConfig configuration) {
        return configuration.getWebConfiguration();
      }
    });

    bootstrap.setObjectMapper(configureObjectMapper(new ObjectMapper()));

    bootstrap.addCommand(new CompactCommand());
    bootstrap.addCommand(new ExportEmbeddedDatabaseCommand());
    bootstrap.addCommand(new ResetAdminCommand());

    bootstrap.setConfigurationFactoryFactory(new DefaultConfigurationFactoryFactory<InsightConfig>()
    {
      @Override
      public ConfigurationFactory<InsightConfig> create(
          Class<InsightConfig> klass,
          Validator validator,
          ObjectMapper objectMapper,
          String propertyPrefix)
      {
        return new InsightConfigurationFactory(klass, validator, configureObjectMapper(objectMapper.copy()),
            propertyPrefix);
      }

      @Override
      protected ObjectMapper configureObjectMapper(ObjectMapper objectMapper) {
        super.configureObjectMapper(objectMapper);

        configureObjectMapperDeserializationFeature(objectMapper);

        // Workaround to let us detect an unset syslog log format
        objectMapper.registerModule(new InsightSyslogAppenderFactory.Module());

        // Workaround to let us set different defaults in the core HTTP configuration
        objectMapper.registerModule(new InsightHttpConnectorFactory.Module());
        objectMapper.registerModule(new InsightHttpsConnectorFactory.Module());
        objectMapper.registerModule(new InsightDefaultServerFactory.Module());

        return objectMapper;
      }
    });

    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
        new EnvironmentVariableSubstitutor(false, true)));
  }

  protected void configureObjectMapperDeserializationFeature(ObjectMapper objectMapper) {
    objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  protected <T extends ObjectMapper> T configureObjectMapper(T objectMapper) {
    // Use an object mapper mostly matching the default for Dropwizard version 1.2.2 i.e.
    // https://github.com/dropwizard/dropwizard/blob/v1.2.2/
    //   dropwizard-jackson/src/main/java/io/dropwizard/jackson/Jackson.java#L65
    // Register default modules except io.dropwizard.jackson.FuzzyEnumModule so enums using @JsonValue can be
    // deserialized without needing @JsonCreator methods
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new GuavaExtrasModule());
    objectMapper.registerModule(new JodaModule());
    objectMapper.registerModule(new ParameterNamesModule());
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new JavaTimeModule());

    // More defaults
    objectMapper.setPropertyNamingStrategy(new AnnotationSensitivePropertyNamingStrategy());
    objectMapper.setSubtypeResolver(new DiscoverableSubtypeResolver());

    return objectMapper;
  }

  protected DatabaseConfigProvider getDatabaseConfigProvider(InsightConfig insightConfig) {
    return DatabaseConfigProviderFactory.createDatabaseConfigProvider(insightConfig);
  }

  @Override
  protected void customize(final InsightConfig config, final Environment env) {
    super.customize(config, env);

    replaceGenericExceptionMapper(env);

    // This provider comes from HDS and does not have the necessary annotations for automatic injection,
    // so register it manually
    env.jersey().register(new ComponentIdentifierParamConverterProvider(env.getObjectMapper()));

    // Most jersey components are injected automatically by dropwizard-guicey. However it seems to be unable
    // to correctly handle @Context injections on @Providers. So for that case, we register them here. Note that
    // even doing it manually, jersey won't do the @Context injection if you provide it a class, it only seems to
    // work with an instance. This means that classes registered this way are effectively singletons
    Injector injector = getInjector();
    AuditContainerRequestFilter auditContainerRequestFilter = new AuditContainerRequestFilter(
        injector.getInstance(ApplicationDAO.class),
        injector.getInstance(OrganizationDAO.class),
        injector.getInstance(RepositoryDAO.class),
        injector.getInstance(RepositoryManagerDAO.class)
    );
    getInjector().injectMembers(auditContainerRequestFilter);

    addThrowableHandlers(env);

    env.jersey().register(auditContainerRequestFilter);

    env.servlets().addServlet(PingServlet.class.getSimpleName(), PingServlet.class)
        .addMapping(PublicApiPaths.PING_RESOURCE_PATH);

    addServletFilters(env);

    log.debug("Headless mode: {}", java.awt.GraphicsEnvironment.isHeadless());
    log.debug("Features flags: {}", config.getFeatures());
  }

  private void addThrowableHandlers(final Environment env) {
    ThrowableHandler applicationThrowableHandler = getInstance(ThrowableHandler.class);
    applicationThrowableHandler.setHandler(env.getApplicationContext().getHandler());
    env.getApplicationContext().setHandler(applicationThrowableHandler);

    ThrowableHandler adminThrowableHandler = getInstance(ThrowableHandler.class);
    adminThrowableHandler.setHandler(env.getAdminContext().getHandler());
    env.getAdminContext().setHandler(adminThrowableHandler);
  }

  protected void addServletFilters(Environment env) {
    addServletFilter(env, true, ActiveRequestCounterFilter.class, "/*");
    addServletFilters(env, false);
  }

  protected void addServerHeaderFilter(Environment env) {
    addServletFilter(env, true, ServerHeaderFilter.class, ServerHeaderFilter.URL_PATTERNS);
  }

  protected void addServletFilters(Environment env, boolean attachToAdminApi) {
    addServerHeaderFilter(env);
    addServletFilter(env, attachToAdminApi, BaseUrlFilter.class, "/*");
    addServletFilter(env, attachToAdminApi, AuditFilter.class, AuditFilter.URL_PATTERNS);
    addServletFilter(env, attachToAdminApi, HttpHeaderValidatorFilter.class, HttpHeaderValidatorFilter.URL_PATTERN);
    addServletFilter(env, attachToAdminApi, ContentTypeOptionsHeaderFilter.class, "/*");
    addServletFilter(env, GuiceShiroFilter.class, "/*");
    addServletFilter(env, IndexCacheControlFilter.class, IndexCacheControlFilter.URL_PATTERN);
    addServletFilter(env, AuthenticationLoggingFilter.class, AuthenticationLoggingFilter.URL_PATTERN);
    addServletFilter(env, CspHeaderFilter.class, CspHeaderFilter.URL_PATTERN);
    addServletFilter(env, CspFrameHeaderFilter.class, CspFrameHeaderFilter.URL_PATTERN);
    addServletFilter(env, FirewallRedirectFilter.class, "/*");
  }

  protected void addServletFilter(Environment env, Class<? extends Filter> filterType, String... urlPatterns) {
    addServletFilter(env, false, filterType, urlPatterns);
  }

  protected void addServletFilter(
      Environment env,
      boolean includeAdmin,
      Class<? extends Filter> filterType,
      String... urlPatterns)
  {
    Filter filter = getInstance(filterType);
    env.servlets().addFilter(filterType.getSimpleName(), filter)
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlPatterns);
    if (includeAdmin) {
      env.admin().addFilter(filterType.getSimpleName(), filter)
          .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urlPatterns);
    }
  }

  private void replaceGenericExceptionMapper(final Environment environment) {
    // Add our own mapper for exceptions.
    JaxRsExceptionMapper jaxRsExceptionMapper = getInstance(JaxRsExceptionMapper.class);
    JavaLangErrorHandler errorHandler = getInstance(JavaLangErrorHandler.class);
    errorHandler.setExitOnFatalErrorSupplier(() ->
        SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.isEnabled()
    );
    environment.jersey().register(jaxRsExceptionMapper);
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
        bind(ExecutorThreadPools.class).to(DefaultExecutorThreadPools.class);

        requestStaticInjection(ExecutorThreadPools.class);
        requestStaticInjection(ConditionTypes.class);
        requestStaticInjection(ConditionValueTypes.class);
        requestStaticInjection(ConfigurationUtils.class);
        requestStaticInjection(ComponentDetailsLoader.class);
        requestStaticInjection(SystemConfigurationPropertyFeature.class);

        bind(ApplicationLifecycle.class).to(DefaultApplicationLifecycle.class);

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
      public void configure() {
        bind(OperationalDataStore.class).toInstance(databaseContainer.getOperationalDataStore());
        bind(AggregationDataStore.class).toInstance(databaseContainer.getAggregationDataStore());
        bind(DataMartDataStore.class).toInstance(databaseContainer.getDataMartDataStore());
        bind(ThirdPartyScansDataStore.class).toInstance(databaseContainer.getThirdPartyScansDataStore());
        bind(DataStoreProvider.class).toInstance(databaseContainer);
        bind(DatabaseConfigProvider.class).toInstance(getDatabaseConfigProvider(configuration()));
        bind(ClusterLockManager.class).toProvider(ClusterLockManagerProvider.class);
        bind(SourceControlSshValidator.class).to(DefaultSourceControlSshValidator.class);
        bind(IndexConfigProvider.class).to(SingleTenantIndexConfigProvider.class);
      }
    });

    modules.addAll(baseModules());
    // Set up bindings based on which database is used.
    modules.add(new DbBasedModule(() -> databaseContainer));
    // Set up bindings based on which search index is used.
    modules.add(new SearchModule());

    return modules;
  }

  public static String getInstanceId() {
    return INSTANCE_ID;
  }

  public static String getLocalHostString() {
    try {
      InetAddress localHost = InetAddress.getLocalHost();
      if (localHost == null) {
        log.error("InetAddress.getLocalHost() returned null.");
        return null;
      }

      return "hostname " + localHost.getHostName() + " (IP address " + localHost.getHostAddress() + ")";
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  private static Provider createNonFipsProvider() {
    try {
      URL bouncyCastleJarUrl = BouncyCastleProvider.class.getProtectionDomain().getCodeSource().getLocation();
      URLClassLoader bouncyCastleClassLoader = new URLClassLoader(new URL[]{bouncyCastleJarUrl}, null);

      Class<?> providerClass = bouncyCastleClassLoader.loadClass("org.bouncycastle.jce.provider.BouncyCastleProvider");
      return (Provider) providerClass.getConstructor().newInstance();
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed to create non-FIPS provider", e);
    }
  }

  private static void loadNonFipsProvider() {
    Provider provider = createNonFipsProvider();
    Security.addProvider(provider);
  }

  private static void loadFipsProvider() {
    Provider fipsProvider = createFipsProvider();
    Security.addProvider(fipsProvider);
  }
}
