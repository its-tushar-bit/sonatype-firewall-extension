/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.policy.evaluator.PolicyMonitor;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphCacheLoader;
import com.sonatype.insight.brain.releasegraph.ReleaseGraphKey;
import com.sonatype.insight.brain.saas.DefaultLicenseDataUpdater;
import com.sonatype.insight.brain.security.CLMShiroAopModule;
import com.sonatype.insight.brain.security.CLMShiroModule;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.error.JaxRsExceptionMapper;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.server.impl.resource.SingletonFactory;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jersey.LoggingExceptionMapper;
import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class InsightBrainService
    extends SisuService<InsightConfig>
{
  private static final Logger log = LoggerFactory.getLogger(InsightBrainService.class);

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

  public static void main(final String[] args) throws Exception {
    JavaRuntimeChecker.checkJreIsSupported();

    if (!validateTempDir()) {
      System.exit(1);
    }

    new InsightBrainService().run(args.length > 0 ? args : new String[] { "server" });
  }

  @Override
  public void run(InsightConfig configuration, Environment environment) throws Exception {
    super.run(configuration, environment);

    LicenseDataUpdater.setUpdater(getInjector().getInstance(DefaultLicenseDataUpdater.class));

    LicenseOverrideMigrator LicenseOverrideMigrator = getInjector().getInstance(LicenseOverrideMigrator.class);
    LicenseOverrideMigrator.migrate();

    configurePolicyMonitoring(environment, configuration.getPolicyMonitoringHour());
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
      if (!file.delete()) {
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

    // workaround to let us set different defaults in the core HTTP configuration
    bootstrap.getObjectMapperFactory().registerModule(new HttpConfig.Module());
  }

  protected DatabaseConfig getDatabaseConfig(File databaseDir, String databaseName) {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    databaseConfig.setUrl("jdbc:h2:" + databaseDir.getAbsolutePath() + '/' + databaseName
        + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
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

    env.addFilter(getInjector().getInstance(GuiceShiroFilter.class), "/*");

    log.info("Server base URL: {}", config.getBaseUrl());
    log.debug("SaaS address: {}", config.getSaasAddress());
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
    environment.addProvider(new JaxRsExceptionMapper(new ErrorResponseGenerator()));
  }

  @Override
  protected List<Module> modules(final InsightConfig config) {
    // NOTE: The ReleaseGraphCacheLoader indirectly uses the ApplicationDAO so we better setup the DB before
    File databaseDir = new File(config.getSonatypeWork(), "data");
    DatabaseConfig dmDatabaseConfig = getDatabaseConfig(databaseDir, "dm");
    DatamartProvider.init(dmDatabaseConfig);
    DatabaseConfig odsDatabaseConfig = getDatabaseConfig(databaseDir, "ods");
    OperationalDataStoreProvider.init(odsDatabaseConfig);

    return Arrays.<Module> asList(new AbstractModule()
    {
      @Override
      protected void configure() {
        final LoadingCache<ReleaseGraphKey, byte[]> cache = CacheBuilder.newBuilder()
            .maximumSize(config.getReleaseGraphCacheSize()).build(new ReleaseGraphCacheLoader());
        bind(new TypeLiteral<LoadingCache<ReleaseGraphKey, byte[]>>()
        {
        }).toInstance(cache);
      }
    }, new CLMShiroModule(), new CLMShiroAopModule());
  }

  /**
   * Configure PolicyMonitor to run once a day at a specified hour.
   * 
   * @since 1.7.1
   */
  protected void configurePolicyMonitoring(final Environment environment, final int policyMonitoringHour) {
    DateTime dateTime = determineNextExecutionTime(policyMonitoringHour);
    log.info("Scheduling Policy Monitor execution for {}", dateTime);
    ScheduledExecutorService scheduledExecutorService = environment.managedScheduledExecutorService(
        "policyMonitoring-%d", 1);
    final PolicyMonitor policyMonitor = getInjector().getInstance(PolicyMonitor.class);
    ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(new Runnable()
    {
      @Override
      public void run() {
        log.info("Triggering scheduled execution of Policy Monitor");
        policyMonitor.run();
        log.info("Next Policy Monitor execution scheduled for {}", determineNextExecutionTime(policyMonitoringHour));
      }
    }, TimeUnit.MILLISECONDS.toMinutes(dateTime.getMillis() - System.currentTimeMillis()), 1440, TimeUnit.MINUTES);
    log.info("First execution of Policy Monitor will happen in {} minutes", scheduledFuture.getDelay(TimeUnit.MINUTES));
  }

  private DateTime determineNextExecutionTime(final int policyMonitoringHour) {
    DateTime dateTime = new DateTime().withHourOfDay(policyMonitoringHour).withMinuteOfHour(0).withSecondOfMinute(0)
        .withMillisOfSecond(0);
    // set for tomorrow if this time has already passed today
    if (dateTime.isBeforeNow()) {
      dateTime = dateTime.plusDays(1);
    }
    return dateTime;
  }
}
