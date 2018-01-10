/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Named;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.eventbus.EventBusConfig;
import com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater;
import com.sonatype.insight.brain.landing.IndexCacheControlFilter;
import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.organization.SampleDataCreator;
import com.sonatype.insight.brain.security.AuthenticationLoggingFilter;
import com.sonatype.insight.brain.security.HttpHeaderValidatorFilter;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.security.SecurityAopModule;
import com.sonatype.insight.brain.security.SecurityModule;
import com.sonatype.insight.brain.security.TraceMethodBlockFilter;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.server.impl.resource.SingletonFactory;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.cli.Cli;
import com.yammer.dropwizard.cli.ServerCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jersey.LoggingExceptionMapper;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class InsightBrainService
    extends SisuService<InsightConfig>
{
  private static final Logger log = LoggerFactory.getLogger(InsightBrainService.class);

  private static final String PRODUCT_NAME = "Nexus IQ Server";

  static {
    // INSIGHT-4557
    System.setProperty("java.awt.headless", "true");
  }

  public static final String BRAIN_ASSET_PATH = "/assets/";

  public static final String POLICY_ASSET_PATH = "/policy-assets/";

  private static final String INSTANCE_ID = UUID.randomUUID().toString();

  private static volatile File configFile;

  public static void main(final String[] args) {
    try {
      setupServerLogging(args);
      JavaRuntimeChecker.checkJreIsSupported();

      if (!validateTempDir()) {
        System.exit(1);
      }

      new InsightBrainService().runFromArguments(args.length > 0 ? args : new String[] { "server" });
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
      printInstanceId("Starting");
      addShutdownLogger();
    }
  }

  private static void addShutdownLogger() {
    Runtime.getRuntime().addShutdownHook(new Thread("Shutdown Logger")
    {
      @Override
      public void run() {
        printInstanceId("Stopping");
      }
    });
  }

  /**
   * This is meant to override {@link #run(String[])} from the base class which is unfortunately {@code final} .
   */
  public void runFromArguments(String[] arguments) throws Exception {
    final Bootstrap<InsightConfig> bootstrap = new Bootstrap<>(this);
    bootstrap.addCommand(new ServerCommand<InsightConfig>(this)
    {
      @Override
      protected void run(Environment environment, Namespace namespace, InsightConfig configuration) throws Exception {
        String configArg = namespace.getString("file");
        InsightBrainService.configFile = configArg != null ? new File(configArg) : null;
        log.info("Configuration file: {}",
            InsightBrainService.configFile != null ? InsightBrainService.configFile.getAbsolutePath() : "(none)");
        super.run(environment, namespace, configuration);
      }
    });
    initialize(bootstrap);
    final Cli cli = new Cli(this.getClass(), bootstrap);
    cli.run(arguments);
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
    MDCUsernameScope.forSystem();

    printInstanceId("Started");
    printVersion();

    initializeDatabases(configuration);

    super.run(configuration, environment);

    LicenseDataUpdater.setUpdater(getInstance(DefaultLicenseDataUpdater.class));

    getInstance(DataMigrator.class).migrate();

    // This call must come after the DataMigrator. Specifically, the RootOrganizationConfigMigrator as the sample data
    // will interfere with its decision to determine a fresh install and mistakenly trigger the root org migration.
    SampleDataCreator.createSampleData(configuration);

    new Thread("Startup license data updater")
    {
      @Override
      public void run() {
        try {
          LicenseDataUpdater.update();
        }
        catch (Exception e) {
          log.info("Failed to retrieve license data from Sonatype HDS");
          log.debug("Failed to retrieve license data from Sonatype HDS", e);
        }
      }
    }.start();
  }

  private void printVersion() {
    String version = new VersionService().getVersion("Unknown");
    log.info("|------------------------------------------");
    log.info("|");
    log.info("| Initializing {} {}", PRODUCT_NAME, version);
    log.info("|");
    log.info("|------------------------------------------");
  }

  private static void printInstanceId(String messagePrefix) {
    String message = messagePrefix + " " + PRODUCT_NAME + " instance ID " + INSTANCE_ID + " on " + getLocalHostString()
        + ".";
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
        log.error(
            "It appears that the temporary location is not a folder. Please ensure that {} is a folder "
                + "or specify another folder by adding -Djava.io.tmpdir=<writeable-folder> to the command line used for launching "
                + "the server.", dir.getAbsolutePath());
        return false;
      }

      // Ensure we can actually create and delete a new temp file
      File file = File.createTempFile("clm-server-launcher", ".tmp");
      try {
        new FileCleaner().delete(file);
      }
      catch (FileDeletionException fde) {
        log.error(
            "The server is not able to delete from the temporary folder. Please ensure server has access to {} "
                + "or specify another folder by adding -Djava.io.tmpdir=<writeable-folder> to the command line used for launching "
                + "the server.", dir.getAbsolutePath());
        return false;
      }
    }
    catch (IOException ex) {
      log.error(
          "The server is not able to write to the temporary folder. Please ensure server has access to {} "
              + "or specify another folder by adding -Djava.io.tmpdir=<writeable-folder> to the command line used for launching "
              + "the server.", tmp);
      log.debug("Unable to validate temporary folder", ex);
      return false;
    }
    return true;
  }

  @Override
  public void initialize(final Bootstrap<InsightConfig> bootstrap) {
    bootstrap.addBundle(new AssetsBundle("/assets/", BRAIN_ASSET_PATH, "index.html"));

    // Legacy support for old reports
    bootstrap.addBundle(new AssetsBundle("/assets/policy/", POLICY_ASSET_PATH, "index.html"));

    // workaround to let us set different defaults in the core HTTP configuration
    bootstrap.getObjectMapperFactory().registerModule(new HttpConfig.Module());

    bootstrap.addCommand(new CompactCommand());
  }

  protected DatabaseConfig getDatabaseConfig(DatabaseConfigProvider databaseConfigProvider, DatabaseName databaseName)
  {
    return databaseConfigProvider.getDatabaseConfig(databaseName);
  }

  @Override
  protected void customize(final InsightConfig config, final Environment env) {
    replaceGenericExceptionMapper(env, config);

    config.getSonatypeWork().mkdirs();

    env.enableJerseyFeature(ResourceConfig.FEATURE_CANONICALIZE_URI_PATH);
    env.enableJerseyFeature(ResourceConfig.FEATURE_NORMALIZE_URI);
    env.setJerseyProperty(ResourceConfig.PROPERTY_DEFAULT_RESOURCE_COMPONENT_PROVIDER_FACTORY_CLASS,
        SingletonFactory.class);

    env.addFilter(getInstance(HttpHeaderValidatorFilter.class), HttpHeaderValidatorFilter.URL_PATTERN);
    env.addFilter(getInstance(TraceMethodBlockFilter.class), TraceMethodBlockFilter.URL_PATTERN);
    env.addFilter(getInstance(GuiceShiroFilter.class), "/*");
    env.addFilter(getInstance(IndexCacheControlFilter.class), IndexCacheControlFilter.URL_PATTERN);
    env.addFilter(getInstance(AuthenticationLoggingFilter.class), AuthenticationLoggingFilter.URL_PATTERN);

    if (config.isForceBaseUrl()) {
      log.error("DEPRECATION NOTICE: Forcing use of server base URL: {}, any 'X-Forwarded-*' headers will be " +
          "ignored. More information at http://links.sonatype.com/products/clm/docs/base-url", config.getBaseUrl());
    }
    else {
      log.info("Server base URL: {}", config.getBaseUrl());
    }
    log.debug("HDS URL: {}", config.getHdsUrl());
    log.debug("Headless mode: {}", java.awt.GraphicsEnvironment.isHeadless());
  }

  // Copied from IdeScanService
  private void replaceGenericExceptionMapper(final Environment environment, InsightConfig config) {
    // DW has an exception mapper that turns exceptions into 500. Boo for us.
    // Remove it so that our mapper will always be used to handle exceptions.
    final Set<Object> singletons = environment.getJerseyResourceConfig().getSingletons();
    for (Object candidate : singletons) {
      if (candidate instanceof LoggingExceptionMapper) {
        log.debug("Removing LoggingExceptionMapper");
        singletons.remove(candidate);
        break;
      }
    }

    // Add our own mapper for exceptions.
    JaxRsExceptionMapper jaxRsExceptionMapper = getInstance(JaxRsExceptionMapper.class);
    jaxRsExceptionMapper.setExitOnFatalError(config.isExitOnFatalError());
    environment.addProvider(jaxRsExceptionMapper);
  }

  private void initializeDatabases(final InsightConfig config) {
    DatabaseConfigProvider databaseConfigProvider = new DatabaseConfigProvider(config);

    // NOTE: The ODS can refuse upgrade if the existing schema is too old. So initialize&upgrade it first to avoid
    // upgrading the other databases if the ODS fails and a previous server version must be run first instead.
    DatabaseConfig odsDatabaseConfig = getDatabaseConfig(databaseConfigProvider, DatabaseName.ods);
    OperationalDataStoreProvider.init(odsDatabaseConfig);

    DatabaseConfig dmDatabaseConfig = getDatabaseConfig(databaseConfigProvider, DatabaseName.dm);
    DatamartProvider.init(dmDatabaseConfig);

    DatabaseConfig aggregationDatabaseConfig = getDatabaseConfig(databaseConfigProvider, DatabaseName.aggregation);
    AggregationDataStoreProvider.init(aggregationDatabaseConfig);

    // Create the default LTGs on the root organization (must be called after the database is initialized)
    new LicenseThreatGroupDAO().createDefaultLicenseThreatGroups();
  }

  @Override
  protected List<Module> modules(final InsightConfig config) {
    Module bindings = new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(com.sonatype.insight.jaxrs.error.ErrorResponseGenerator.class).to(ErrorResponseGenerator.class);
        bind(EventBusConfig.class).toInstance(config.getEventBusConfig());
      }
    };
    Module authc = new SecurityModule(config);
    Module authz = new SecurityAopModule(config.isAnonymousClientAccessAllowed());

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
