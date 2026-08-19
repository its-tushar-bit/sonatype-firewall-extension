/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.health.ServerBootHealthCheck;
import com.sonatype.insight.brain.spring.DropwizardConfigBootstrap;
import com.sonatype.insight.brain.spring.LaunchConfigurationResolver;
import org.springframework.boot.WebApplicationType;
import com.sonatype.insight.brain.spring.config.NamedBeanRegistrationConfiguration;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.version.MultiTenantVersionService;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.core.DefaultModelStore;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxModelFactory;
import org.spdx.storage.simple.InMemSpdxStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Main entry point for Nexus Multi-Tenant IQ Server.
 * Migrated from Dropwizard to Spring Boot.
 *
 * Note: Spring Security auto-configuration is excluded because
 * this application uses Apache Shiro for authentication/authorization.
 */
@SpringBootApplication
@ComponentScan(
    basePackages = {
      "com.sonatype.insight.brain",
      "com.sonatype.insight.jaxrs"
    },
    includeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ANNOTATION,
          classes = Named.class)
    },
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.CUSTOM,
          classes = MtiqComponentScanExclusionFilter.class)
    })
@Import({
  NamedBeanRegistrationConfiguration.class,
  MultiTenantDataAccessConfiguration.class
})
public class MultiTenantInsightBrainService
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantInsightBrainService.class);

  private static final String PRODUCT_NAME = "Nexus Multi-Tenant IQ Server";

  private static final String MULTI_TENANT_SERVER_NAME = "MTIQ Server";

  private static final String MULTI_TENANT_BATCH_NAME = "MTIQ Server (Batch Mode)";

  private static final String INSTANCE_ID = UUID.randomUUID().toString().substring(0, 8);

  public static void main(String[] args) throws Exception {
    // WARNING: No code that uses tenancy should be added before this line.
    TenantThreadLocal.setDefaultTenantToGlobal();
    assertRunningAsGlobalTenant();

    SecurityProviderBootstrap.ensureBouncyCastleProviderIsLowestPreference();

    new TenantUtil().setGlobalTenant();

    try {
      printVersion();

      if (!validateTempDir()) {
        System.exit(1);
      }

      MultiTenantCommandDispatcher commandDispatcher = new MultiTenantCommandDispatcher();
      if (commandDispatcher.handles(args)) {
        commandDispatcher.dispatch(args);
        return;
      }

      SpringApplication app = new SpringApplication(MultiTenantInsightBrainService.class);
      app.setKeepAlive(true);
      // Ensure Spring Boot runs as a servlet web application
      app.setWebApplicationType(WebApplicationType.SERVLET);

      LaunchConfigurationResolver.LaunchConfiguration launchConfiguration =
          LaunchConfigurationResolver.resolve(args);

      // MTIQ intentionally overrides the following single-tenant beans with @Primary:
      // - jerseyResourceRegistry: MTIQ splits resources into main + admin ResourceConfig
      // - resourceConfig: MTIQ uses its own ResourceConfig bean (mtiqMainResourceConfig)
      // - jerseyFilter: MTIQ registers its own selective Jersey filter
      // - auditContainerRequestFilter: MTIQ provides its own audit filter
      // - insightJacksonMessageBodyProvider: MTIQ uses MTIQ-specific ObjectMapper
      // - componentIdentifierParamConverterProvider: MTIQ uses MTIQ-specific ObjectMapper
      // - taskScheduler: MTIQ uses MultiTenantTaskScheduler
      // - productLicense: MTIQ uses MultiTenantProductLicense
      // - multiTenantJwkProvider: MTIQ declares its own @Primary JWK provider
      File configFile = new File(launchConfiguration.configFilePath()).getAbsoluteFile();
      app.setDefaultProperties(Map.of(
          "spring.main.keep-alive", "true",
          "sonatype.mtiq.enabled", "true",
          "config.file", configFile.getAbsolutePath(),
          "config.class", MultiTenantInsightConfig.class.getName(),
          "config.file.implicitDefault", Boolean.toString(launchConfiguration.implicitDefaultConfigFile())));

      DropwizardConfigBootstrap.configure(app, launchConfiguration.configFilePath(), MultiTenantInsightConfig.class,
          launchConfiguration.implicitDefaultConfigFile());

      // Add listener for application ready to mark server as booted
      app.addListeners((ApplicationListener<ApplicationReadyEvent>) event -> {
        ServerBootHealthCheck.fullyBooted();
        log.info(getServerInstanceMessage());
      });

      SpdxModelFactory.init();
      DefaultModelStore.initialize(new InMemSpdxStore(), "https://spdx.org/spdxdocs/default", new ModelCopyManager());

      app.run(args);

    }
    catch (Throwable t) {
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
    }
  }

  private static void assertRunningAsGlobalTenant() {
    if (!new TenantUtil().isGlobalTenant()) {
      System.err.println(
          "Fatal error: Expecting to run as GLOBAL tenant, but found tenant: " + TenantThreadLocal.getTenant());
      System.exit(10);
    }
  }

  private static void printVersion() {
    VersionService versionService = new MultiTenantVersionService();
    String build = versionService.getBuild();
    log.info("|------------------------------------------");
    log.info("|");
    log.info("| Initializing {} build {}", PRODUCT_NAME, build);
    log.info("|");
    log.info("|------------------------------------------");
  }

  private static String getServerInstanceMessage() {
    String build = new MultiTenantVersionService().getBuild();
    String name = new TenantUtil().isMtiqBatchMode() ? MULTI_TENANT_BATCH_NAME : MULTI_TENANT_SERVER_NAME;
    return name + " build " + build + " instance ID " + INSTANCE_ID + " on " + getLocalHostString() + ".";
  }

  private static String getLocalHostString() {
    try {
      return InetAddress.getLocalHost().toString();
    }
    catch (UnknownHostException e) {
      return "unknown";
    }
  }

  static boolean validateTempDir() {
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
}
