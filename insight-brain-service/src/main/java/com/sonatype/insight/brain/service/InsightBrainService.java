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
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.landing.IndexCacheControlFilter;
import com.sonatype.insight.brain.migration.DataMigrator;
import com.sonatype.insight.brain.saas.DefaultLicenseDataUpdater;
import com.sonatype.insight.brain.security.AuthenticationLoggingFilter;
import com.sonatype.insight.brain.security.CLMShiroAopModule;
import com.sonatype.insight.brain.security.CLMShiroModule;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.security.TraceMethodBlockFilter;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.jaxrs.error.JaxRsExceptionMapper;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.server.impl.resource.SingletonFactory;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jersey.LoggingExceptionMapper;
import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class InsightBrainService
    extends SisuService<InsightConfig>
{
  private static final Logger log = LoggerFactory.getLogger(InsightBrainService.class);

  private static final String PRODUCT_NAME = "Sonatype CLM";

  static {
    // INSIGHT-4557
    System.setProperty("java.awt.headless", "true");
  }

  public static final String APPLICATION_ASSET_PATH = "/application-assets/";

  public static final String BRAIN_ASSET_PATH = "/assets/";

  public static final String POLICY_ASSET_PATH = "/policy-assets/";

  public static final String ORGANIZATION_ASSET_PATH = "/organization-assets/";

  public static final String CONFIGURATION_ASSET_PATH = "/configuration-assets/";

  public static final String SECURITY_ASSET_PATH = "/security-assets/";

  public static final String CIP_ASSET_PATH = "/cip/";

  public static final String REPORT_ASSET_PATH = "/report-assets/";

  public static final String DASHBOARD_ASSET_PATH = "/dashboard-assets/";

  public static final String ABOUT_ASSET_PATH = "/about-assets/";

  private static final String INSTANCE_ID = UUID.randomUUID().toString();

  public static void main(final String[] args) {
    try {
      printInstanceId("Starting");
      addShutdownLogger();
      JavaRuntimeChecker.checkJreIsSupported();
      JavaXXMaxPermSizeChecker.check();

      if (!validateTempDir()) {
        System.exit(1);
      }

      new InsightBrainService().run(args.length > 0 ? args : new String[] { "server" });
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
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

  @Override
  public void run(InsightConfig configuration, Environment environment) throws Exception {
    MDCUsernameScope.forSystem();

    printInstanceId("Started");
    printVersion();

    super.run(configuration, environment);

    LicenseDataUpdater.setUpdater(getInstance(DefaultLicenseDataUpdater.class));

    getInstance(DataMigrator.class).migrate();

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
    bootstrap.addBundle(new AssetsBundle("/assets/application/", APPLICATION_ASSET_PATH, "index.html"));
    bootstrap.addBundle(new AssetsBundle("/assets/assets/", BRAIN_ASSET_PATH, "index.html"));
    bootstrap.addBundle(new AssetsBundle("/assets/policy/", POLICY_ASSET_PATH, "index.html"));
    bootstrap.addBundle(new AssetsBundle("/assets/organization/", ORGANIZATION_ASSET_PATH));
    bootstrap.addBundle(new AssetsBundle("/assets/configuration/", CONFIGURATION_ASSET_PATH));
    bootstrap.addBundle(new AssetsBundle("/assets/cip/", CIP_ASSET_PATH));
    bootstrap.addBundle(new AssetsBundle("/assets/security/", SECURITY_ASSET_PATH));
    bootstrap.addBundle(new AssetsBundle("/assets/report/", REPORT_ASSET_PATH));
    bootstrap.addBundle(new AssetsBundle("/assets/dashboard/", DASHBOARD_ASSET_PATH));
    bootstrap.addBundle(new AssetsBundle("/assets/about/", ABOUT_ASSET_PATH, "index.html"));

    bootstrap.addBundle(new AssetsBundle("/assets-new/application/", "/new" + APPLICATION_ASSET_PATH, "index.html"));
    bootstrap.addBundle(new AssetsBundle("/assets-new/assets/", "/new" + BRAIN_ASSET_PATH, "index.html"));
    bootstrap.addBundle(new AssetsBundle("/assets-new/policy/", "/new" + POLICY_ASSET_PATH, "index.html"));
    bootstrap.addBundle(new AssetsBundle("/assets-new/organization/", "/new" + ORGANIZATION_ASSET_PATH));
    bootstrap.addBundle(new AssetsBundle("/assets-new/configuration/", "/new" + CONFIGURATION_ASSET_PATH));
    bootstrap.addBundle(new AssetsBundle("/assets-new/cip/", "/new" + CIP_ASSET_PATH));
    bootstrap.addBundle(new AssetsBundle("/assets-new/security/", "/new" + SECURITY_ASSET_PATH));
    bootstrap.addBundle(new AssetsBundle("/assets-new/report/", "/new" + REPORT_ASSET_PATH));
    bootstrap.addBundle(new AssetsBundle("/assets-new/dashboard/", "/new" + DASHBOARD_ASSET_PATH));
    bootstrap.addBundle(new AssetsBundle("/assets-new/about/", "/new" + ABOUT_ASSET_PATH, "index.html"));

    // workaround to let us set different defaults in the core HTTP configuration
    bootstrap.getObjectMapperFactory().registerModule(new HttpConfig.Module());
  }

  protected DatabaseConfig getDatabaseConfig(File databaseDir, String databaseName, Long cacheSizeInBytes,
      String additionalDBParams)
  {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    StringBuilder urlBuilder = new StringBuilder()
        .append("jdbc:h2:")
        .append(databaseDir.getAbsolutePath())
        .append('/')
        .append(databaseName)
        .append(";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
    if (cacheSizeInBytes != null) {
      urlBuilder.append(";CACHE_SIZE=").append(cacheSizeInBytes / 1024);
    }
    if (additionalDBParams != null) {
      urlBuilder.append(";").append(additionalDBParams);
    }
    databaseConfig.setUrl(urlBuilder.toString());
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    databaseConfig.setMaxConnections(50);
    return databaseConfig;
  }

  @Override
  protected void customize(final InsightConfig config, final Environment env) {
    replaceGenericExceptionMapper(env);

    config.getSonatypeWork().mkdirs();

    env.enableJerseyFeature(ResourceConfig.FEATURE_CANONICALIZE_URI_PATH);
    env.enableJerseyFeature(ResourceConfig.FEATURE_NORMALIZE_URI);
    env.setJerseyProperty(ResourceConfig.PROPERTY_DEFAULT_RESOURCE_COMPONENT_PROVIDER_FACTORY_CLASS,
        SingletonFactory.class);

    env.addFilter(getInstance(TraceMethodBlockFilter.class), TraceMethodBlockFilter.URL_PATTERN);
    env.addFilter(getInstance(GuiceShiroFilter.class), "/*");
    env.addFilter(getInstance(IndexCacheControlFilter.class), IndexCacheControlFilter.URL_PATTERN);
    env.addFilter(getInstance(AuthenticationLoggingFilter.class), AuthenticationLoggingFilter.URL_PATTERN);

    log.info("Server base URL: {}", config.getBaseUrl());
    log.debug("HDS address: {}", config.getSaasAddress());
    log.debug("Headless mode: {}", java.awt.GraphicsEnvironment.isHeadless());
  }

  // Copied from IdeScanService
  private void replaceGenericExceptionMapper(final Environment environment) {
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
    environment.addProvider(getInstance(JaxRsExceptionMapper.class));
  }

  @Override
  protected List<Module> modules(final InsightConfig config) {
    // NOTE: The ReleaseGraphCacheLoader indirectly uses the ApplicationDAO so we better setup the DB before
    File databaseDir = new File(config.getSonatypeWork(), "data");
    DatabaseConfig dmDatabaseConfig = getDatabaseConfig(databaseDir, "dm", null, config.getAdditionalDBParams());
    DatamartProvider.init(dmDatabaseConfig);
    // NOTE: H2 uses previous setting if not set in URL, so be explicit about the default size
    long dbCacheSizeInBytes = 16L * 1024 * 1024;
    if (config.getDbCacheSizePercent() != null) {
      dbCacheSizeInBytes = Runtime.getRuntime().maxMemory() * config.getDbCacheSizePercent() / 100;
    }
    DatabaseConfig odsDatabaseConfig = getDatabaseConfig(databaseDir, "ods", dbCacheSizeInBytes,
        config.getAdditionalDBParams());
    OperationalDataStoreProvider.init(odsDatabaseConfig);

    Module bindings = new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(com.sonatype.insight.jaxrs.error.ErrorResponseGenerator.class).to(ErrorResponseGenerator.class);
      }
    };
    Module authc = new CLMShiroModule(config);
    Module authz = new CLMShiroAopModule(config.isAnonymousClientAccessAllowed());
    return Arrays.asList(bindings, authc, authz);
  }

  private static String getLocalHostString() {
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
