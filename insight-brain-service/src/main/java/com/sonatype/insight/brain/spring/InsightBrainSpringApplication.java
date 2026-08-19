/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.service.ApplicationLifecycle;
import com.sonatype.insight.brain.service.SecurityProviderBootstrap;
import com.sonatype.insight.brain.spring.config.AdminCompatibilityConfiguration;
import com.sonatype.insight.brain.spring.config.DropwizardManagementConnectorConfiguration;
import com.sonatype.insight.brain.spring.config.NamedBeanRegistrationConfiguration;
import com.sonatype.insight.brain.spring.config.SingleTenantAdminFilterConfiguration;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.core.DefaultModelStore;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxModelFactory;
import org.spdx.storage.simple.InMemSpdxStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * Main entry point for Nexus IQ Server.
 * Migrated from Dropwizard to Spring Boot.
 *
 * Note: Spring Security auto-configuration is excluded because
 * this application uses Apache Shiro for authentication/authorization.
 */
@SpringBootApplication
@ComponentScan(
    basePackages = {
      "com.sonatype.insight.brain",
      "com.sonatype.insight.jaxrs" // For JaxRsExceptionMapper
    },
    includeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ANNOTATION,
          classes = Named.class)
    },
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = AdminCompatibilityConfiguration.class),
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = DropwizardManagementConnectorConfiguration.class),
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = SingleTenantAdminFilterConfiguration.class)
    })
@Import(NamedBeanRegistrationConfiguration.class)
public class InsightBrainSpringApplication
{
  private static final Logger log = LoggerFactory.getLogger(InsightBrainSpringApplication.class);

  public static void main(String[] args) {
    try {
      System.setProperty("java.awt.headless", "true");
      assertRunningAsSingleTenant();
      if (!validateTempDir()) {
        System.exit(1);
      }
      SecurityProviderBootstrap.ensureBouncyCastleProviderIsLowestPreference();

      InsightBrainCommandDispatcher commandDispatcher = new InsightBrainCommandDispatcher();
      if (commandDispatcher.handles(args)) {
        commandDispatcher.dispatch(InsightBrainSpringApplication.class, args);
        return;
      }

      SpringApplication app = new SpringApplication(InsightBrainSpringApplication.class);
      app.setKeepAlive(true);

      LaunchConfigurationResolver.LaunchConfiguration launchConfiguration =
          LaunchConfigurationResolver.resolve(args);

      File configFile = new File(launchConfiguration.configFilePath()).getAbsoluteFile();
      ApplicationLifecycle.setConfigFile(configFile);

      app.setDefaultProperties(Map.of(
          "spring.main.keep-alive", "true",
          "config.file", configFile.getAbsolutePath(),
          "config.file.implicitDefault", Boolean.toString(launchConfiguration.implicitDefaultConfigFile())));

      DropwizardConfigBootstrap.configure(app, launchConfiguration.configFilePath(),
          launchConfiguration.implicitDefaultConfigFile());

      SpdxModelFactory.init();
      DefaultModelStore.initialize(new InMemSpdxStore(), "https://spdx.org/spdxdocs/default", new ModelCopyManager());

      app.run(args);
    }
    catch (Throwable t) {
      System.err.println("Fatal error during startup: " + t.getMessage());
      t.printStackTrace(System.err);
      log.error("Fatal error during startup", t);
      System.exit(2);
    }
  }

  private static void assertRunningAsSingleTenant() {
    if (!new TenantUtil().isSingleTenant()) {
      System.err.println(
          "Fatal error: Expecting to run as SINGLE tenant, but found tenant: " + TenantThreadLocal.getTenant());
      System.exit(10);
    }
  }

  static boolean validateTempDir() {
    String tmp = System.getProperty("java.io.tmpdir");
    try {
      File dir = new File(tmp);
      if (!dir.exists()) {
        if (dir.mkdirs()) {
          log.info("Created temporary folder: {}", dir.getAbsolutePath());
        }
      }
      else if (!dir.isDirectory()) {
        log.error("The temporary location is not a folder. Please ensure that {} is a folder "
            + "or specify another folder by adding -Djava.io.tmpdir=<writeable-folder> to the command line.",
            dir.getAbsolutePath());
        return false;
      }
      File file = Files.createTempFile("clm-server-launcher", ".tmp").toFile();
      try {
        new FileCleaner().delete(file);
      }
      catch (FileDeletionException fde) {
        log.error("Unable to delete from the temporary folder. Please ensure server has access to {} "
            + "or specify another folder by adding -Djava.io.tmpdir=<writeable-folder> to the command line.",
            dir.getAbsolutePath());
        return false;
      }
    }
    catch (IOException ex) {
      log.error("Unable to write to the temporary folder. Please ensure server has access to {} "
          + "or specify another folder by adding -Djava.io.tmpdir=<writeable-folder> to the command line.", tmp);
      log.debug("Unable to validate temporary folder", ex);
      return false;
    }
    return true;
  }
}
