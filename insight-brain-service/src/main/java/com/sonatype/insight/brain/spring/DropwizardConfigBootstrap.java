/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring;

import com.sonatype.insight.brain.service.ApplicationLifecycle;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.spring.config.DropwizardConfigLoader;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MapPropertySource;

public final class DropwizardConfigBootstrap
{
  private static final String CONFIG_FILE_PROPERTY = "config.file";

  private static final String CONFIG_CLASS_PROPERTY = "config.class";

  static final String CONFIG_FILE_IMPLICIT_DEFAULT_PROPERTY = "config.file.implicitDefault";

  private DropwizardConfigBootstrap() {
    // utility class
  }

  public static void configure(SpringApplication application, String configFilePath) {
    configure(application, configFilePath, InsightConfig.class, false);
  }

  public static void configure(
      SpringApplication application,
      String configFilePath,
      boolean implicitDefaultConfigFile)
  {
    configure(application, configFilePath, InsightConfig.class, implicitDefaultConfigFile);
  }

  public static void configure(
      SpringApplication application,
      String configFilePath,
      Class<? extends InsightConfig> configClass)
  {
    configure(application, configFilePath, configClass, false);
  }

  public static void configure(
      SpringApplication application,
      String configFilePath,
      Class<? extends InsightConfig> configClass,
      boolean implicitDefaultConfigFile)
  {
    application.addListeners(
        environmentPreparedListener(configFilePath, configClass, implicitDefaultConfigFile),
        applicationPreparedListener(configFilePath, configClass, implicitDefaultConfigFile));
  }

  public static void configure(SpringApplicationBuilder builder, String configFilePath) {
    configure(builder, configFilePath, InsightConfig.class, false);
  }

  public static void configure(
      SpringApplicationBuilder builder,
      String configFilePath,
      boolean implicitDefaultConfigFile)
  {
    configure(builder, configFilePath, InsightConfig.class, implicitDefaultConfigFile);
  }

  public static void configure(
      SpringApplicationBuilder builder,
      String configFilePath,
      Class<? extends InsightConfig> configClass)
  {
    configure(builder, configFilePath, configClass, false);
  }

  public static void configure(
      SpringApplicationBuilder builder,
      String configFilePath,
      Class<? extends InsightConfig> configClass,
      boolean implicitDefaultConfigFile)
  {
    File configFile = resolveConfigFile(configFilePath);
    builder.properties(
        CONFIG_FILE_PROPERTY + "=" + configFile.getAbsolutePath(),
        CONFIG_CLASS_PROPERTY + "=" + configClass.getName(),
        CONFIG_FILE_IMPLICIT_DEFAULT_PROPERTY + "=" + implicitDefaultConfigFile);
    builder.listeners(
        environmentPreparedListener(configFilePath, configClass, implicitDefaultConfigFile),
        applicationPreparedListener(configFilePath, configClass, implicitDefaultConfigFile));
  }

  private static ApplicationListener<ApplicationEnvironmentPreparedEvent> environmentPreparedListener(
      String configFilePath,
      Class<? extends InsightConfig> configClass,
      boolean implicitDefaultConfigFile)
  {
    return event -> {
      File configFile = resolveConfigFile(configFilePath);
      System.setProperty(CONFIG_FILE_PROPERTY, configFile.getAbsolutePath());
      System.setProperty(CONFIG_CLASS_PROPERTY, configClass.getName());
      System.setProperty(CONFIG_FILE_IMPLICIT_DEFAULT_PROPERTY, Boolean.toString(implicitDefaultConfigFile));
      event.getEnvironment()
          .getPropertySources()
          .addFirst(
              new MapPropertySource("dropwizardBootstrapConfig", Map.of(
                  CONFIG_FILE_PROPERTY, configFile.getAbsolutePath(),
                  CONFIG_CLASS_PROPERTY, configClass.getName(),
                  CONFIG_FILE_IMPLICIT_DEFAULT_PROPERTY, Boolean.toString(implicitDefaultConfigFile))));
      if (configFile.exists()) {
        try {
          new DropwizardConfigLoader().loadConfig(configFile, event.getEnvironment());
        }
        catch (IOException e) {
          throw new RuntimeException("Failed to load config: " + configFile.getAbsolutePath(), e);
        }
      }
    };
  }

  private static ApplicationListener<ApplicationPreparedEvent> applicationPreparedListener(
      String configFilePath,
      Class<? extends InsightConfig> configClass,
      boolean implicitDefaultConfigFile)
  {
    return event -> {
      File configFile = resolveConfigFile(configFilePath);
      ApplicationLifecycle.setConfigFile(configFile);
    };
  }

  private static File resolveConfigFile(String configFilePath) {
    try {
      return new File(configFilePath).getCanonicalFile();
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to resolve config file path: " + configFilePath, e);
    }
  }
}
