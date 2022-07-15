/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.security.Security;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import javax.inject.Named;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.validation.Validator;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditFilter;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.landing.IndexCacheControlFilter;
import com.sonatype.insight.brain.metrics.CustomMetrics;
import com.sonatype.insight.brain.migration.DbMigrationCommand;
import com.sonatype.insight.brain.security.AuthenticationLoggingFilter;
import com.sonatype.insight.brain.security.ContentTypeOptionsHeaderFilter;
import com.sonatype.insight.brain.security.CspHeaderFilter;
import com.sonatype.insight.brain.security.HttpHeaderValidatorFilter;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.security.SecurityAopModule;
import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.jaxrs.ComponentIdentifierParamConverterProvider;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;

import com.codahale.metrics.servlets.PingServlet;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.cli.Cli;
import io.dropwizard.cli.ServerCommand;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jackson.AnnotationSensitivePropertyNamingStrategy;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import io.dropwizard.jackson.GuavaExtrasModule;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.JarLocation;
import io.dropwizard.util.JavaVersion;
import io.dropwizard.web.WebBundle;
import io.dropwizard.web.conf.WebConfiguration;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class InsightBrainService
    extends SisuApplication<InsightConfig>
{
  private static final Logger log = LoggerFactory.getLogger(InsightBrainService.class);

  private static final String PRODUCT_NAME = "Nexus IQ Server";

  static {
    // INSIGHT-4557
    System.setProperty("java.awt.headless", "true");
    ensureBouncyCastleProviderIsLowestPreference();
  }

  public static final String BRAIN_ASSET_PATH = "/assets/";

  public static final String POLICY_ASSET_PATH = "/policy-assets/";

  private static final String INSTANCE_ID = UUID.randomUUID().toString();

  private static volatile File configFile;

  private static long startTime;

  public static void main(final String[] args) {
    try {
      setupServerLogging(args);

      if (!validateTempDir()) {
        System.exit(1);
      }

      new InsightBrainService().run(args);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
    }
  }

  static void setupServerLogging(final String... args) {
    if (args.length == 0 || "server".equals(args[0])) {
      logServerInstanceMessage("Starting " + getServerInstanceMessage());
      addShutdownLogger();
    }
  }

  private static void addShutdownLogger() {
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

  @Override
  public void run(String... arguments) throws Exception {
    startTime = System.currentTimeMillis();

    final Bootstrap<InsightConfig> bootstrap = new Bootstrap<>(this);
    bootstrap.addCommand(new DbMigrationCommand());
    bootstrap.addCommand(new ServerCommand<InsightConfig>(this)
    {
      private volatile InsightFileLock insightFileLock;

      @Override
      protected void run(Bootstrap<InsightConfig> bootstrap, Namespace namespace, InsightConfig configuration)
          throws Exception
      {
        Files.createDirectories(configuration.getSonatypeWork().toPath());
        Files.createDirectories(configuration.getClusterDirectory().toPath());
        insightFileLock = new InsightFileLock(configuration);
        insightFileLock.lock();

        MDCUsernameScope.forSystem();
        printVersion();

        String configArg = namespace.getString("file");
        InsightBrainService.configFile = new File(configArg).getAbsoluteFile();
        log.info("Configuration file: {}", InsightBrainService.configFile);
        super.run(bootstrap, namespace, configuration);
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

  @VisibleForTesting
  static void ensureBouncyCastleProviderIsLowestPreference() {
    // Adding BouncyCastleProvider here via Security.addProvider(...) ensures it gets the lowest preference position.
    // This prevents org.keycloak.saml.processing.core.util.ProvidersUtil.ensure() from adding it at a higher preference
    // position, which can cause CLM-13629 due to the IQ Server uber JAR invalidating BouncyCastleProvider by excluding 
    // its signatures

    // First remove it in case it is already added
    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    // Second add it at the lowest preference position
    Security.addProvider(new BouncyCastleProvider());
  }

  public static File getConfigFile() {
    return configFile;
  }

  @VisibleForTesting
  public static void setConfigFile(final File testConfigFile) {
    configFile = testConfigFile;
  }

  @Override
  public void run(InsightConfig configuration, Environment environment) throws Exception {
    logServerInstanceMessage("Started " + getServerInstanceMessage());

    DatabaseProvisionUtils.initializeDatabases(configuration, getDatabaseConfigProvider(configuration));

    super.run(configuration, environment);

    bootApplicationLifecycle();
  }

  // Visible for testing
  void bootApplicationLifecycle() throws Exception {
    getInstance(ApplicationLifecycle.class).boot();
  }

  private void printVersion() {
    String version = new VersionService().getLogDisplayVersion();
    log.info("|------------------------------------------");
    log.info("|");
    log.info("| Initializing {} 1 release {}", PRODUCT_NAME, version);
    log.info("|");
    log.info("|------------------------------------------");
  }

  private static String getServerInstanceMessage() {
    String version = new VersionService().getLogDisplayVersion();
    return PRODUCT_NAME + " 1 release " + version + //
        " instance ID " + INSTANCE_ID + //
        " on " + getLocalHostString() + ".";
  }

  private static void logServerInstanceMessage(String message) {
    // Log to stdout first because the standard logging may not be operational at this point.
    System.out.println(message);
    log.info(message);
  }

  private static boolean validateTempDir() {
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
    bootstrap.getObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    bootstrap.addBundle(new MultiPartBundle());

    bootstrap.addBundle(new AssetsBundle("/assets/", BRAIN_ASSET_PATH, "index.html", "assets"));

    // Legacy support for old reports
    bootstrap.addBundle(new AssetsBundle("/assets/policy/", POLICY_ASSET_PATH, "index.html", "policyAssets"));

    bootstrap.addBundle(new WebBundle<InsightConfig>() {
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
      public ConfigurationFactory<InsightConfig> create(Class<InsightConfig> klass,
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

  private <T extends ObjectMapper> T configureObjectMapper(T objectMapper) {
    // Use an object mapper mostly matching the default for Dropwizard version 1.2.2 i.e.
    // https://github.com/dropwizard/dropwizard/blob/v1.2.2/
    //   dropwizard-jackson/src/main/java/io/dropwizard/jackson/Jackson.java#L65
    // Register default modules except io.dropwizard.jackson.FuzzyEnumModule so enums using @JsonValue can be
    // deserialized without needing @JsonCreator methods
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new GuavaExtrasModule());
    objectMapper.registerModule(new JodaModule());
    if (JavaVersion.isJava8()) {
      objectMapper.registerModule(new AfterburnerModule());
    }
    objectMapper.registerModule(new ParameterNamesModule());
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new JavaTimeModule());

    // More defaults
    objectMapper.setPropertyNamingStrategy(new AnnotationSensitivePropertyNamingStrategy());
    objectMapper.setSubtypeResolver(new DiscoverableSubtypeResolver());

    return objectMapper;
  }

  protected DatabaseConfigProvider getDatabaseConfigProvider(InsightConfig insightConfig) {
    return new DatabaseConfigProvider(insightConfig);
  }

  @Override
  protected void customize(final InsightConfig config, final Environment env) {
    replaceGenericExceptionMapper(env, config);
    env.jersey().register(new InsightJacksonMessageBodyProvider(env.getObjectMapper()));
    env.jersey().register(new ComponentIdentifierParamConverterProvider(env.getObjectMapper()));
    env.servlets().addServlet(PingServlet.class.getSimpleName(), PingServlet.class)
        .addMapping(PublicApiPaths.PING_RESOURCE_PATH);

    addServletFilter(env, true, ServerHeaderFilter.class, ServerHeaderFilter.URL_PATTERNS);
    addServletFilter(env, BaseUrlFilter.class, "/*");
    addServletFilter(env, AuditFilter.class, AuditFilter.URL_PATTERNS);
    addServletFilter(env, HttpHeaderValidatorFilter.class, HttpHeaderValidatorFilter.URL_PATTERN);
    addServletFilter(env, ContentTypeOptionsHeaderFilter.class, "/*");
    addServletFilter(env, GuiceShiroFilter.class, "/*");
    addServletFilter(env, IndexCacheControlFilter.class, IndexCacheControlFilter.URL_PATTERN);
    addServletFilter(env, AuthenticationLoggingFilter.class, AuthenticationLoggingFilter.URL_PATTERN);
    addServletFilter(env, CspHeaderFilter.class, CspHeaderFilter.URL_PATTERN);
    addServletFilter(env, CspFrameHeaderFilter.class, CspFrameHeaderFilter.URL_PATTERN);

    log.debug("Headless mode: {}", java.awt.GraphicsEnvironment.isHeadless());
    log.debug("Features flags: {}", config.getFeatures());
    log.debug("Experimental features flags: {}", config.getExperimentalFeatures());
  }

  private void addServletFilter(Environment env, Class<? extends Filter> filterType, String... urlPatterns) {
    addServletFilter(env, false, filterType, urlPatterns);
  }

  private void addServletFilter(
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

  private void replaceGenericExceptionMapper(final Environment environment, InsightConfig config) {
    // Add our own mapper for exceptions.
    JaxRsExceptionMapper jaxRsExceptionMapper = getInstance(JaxRsExceptionMapper.class);
    jaxRsExceptionMapper.setExitOnFatalError(config.isExitOnFatalError());
    environment.jersey().register(jaxRsExceptionMapper);
  }

  @Override
  protected List<Module> modules(final InsightConfig config) {
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

    return Arrays.asList(bindings, authc, authz);
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
}
