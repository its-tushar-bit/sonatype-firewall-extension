/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring;

import com.sonatype.insight.brain.migration.DbMigrationCommand;
import com.sonatype.insight.brain.service.CompactCommand;
import com.sonatype.insight.brain.service.ExportEmbeddedDatabaseCommand;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.ResetAdminCommand;
import com.sonatype.insight.brain.spring.config.DropwizardConfigSourceReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.MapPropertySource;

/**
 * Launches legacy compatibility commands in a non-web Spring context.
 */
public class InsightBrainCommandDispatcher
{
  private static final Logger log = LoggerFactory.getLogger(InsightBrainCommandDispatcher.class);

  static final String COMMAND_MODE_PROPERTY = "sonatype.command-mode";

  static final String STARTUP_MIGRATIONS_ENABLED_PROPERTY = "sonatype.database.startup-migrations.enabled";

  private static final String COMMAND_MODE_PROPERTY_SOURCE = "commandMode";

  public boolean handles(String[] args) {
    if (args.length == 0) {
      return false;
    }
    return getSupportedCommandNames().containsKey(args[0]);
  }

  public void dispatch(Class<?> applicationClass, String[] args) throws Exception {
    String configFilePath = resolveConfigFilePath(args);
    log.info("Running command '{}' with config file '{}'", args[0], configFilePath);

    InsightConfig config = loadConfig(configFilePath, InsightConfig.class);
    InsightBrainCompatibilityCommand command = createCommand(args[0], config);
    command.run(extractCommandArguments(args));
  }

  public <T extends InsightConfig> T loadConfig(String configFilePath, Class<T> configClass) throws IOException {
    DropwizardConfigSourceReader reader = new DropwizardConfigSourceReader();
    Map<String, Object> configMap = reader.readConfigMap(new File(configFilePath));
    return reader.convertValue(configMap, configClass);
  }

  private InsightBrainCompatibilityCommand createCommand(String commandName, InsightConfig config) {
    return switch (commandName) {
      case DbMigrationCommand.NAME -> new DbMigrationCommand(config);
      case CompactCommand.NAME -> new CompactCommand(config);
      case ExportEmbeddedDatabaseCommand.NAME -> new ExportEmbeddedDatabaseCommand(config);
      case ResetAdminCommand.NAME -> new ResetAdminCommand(config);
      default -> throw new IllegalArgumentException("Unsupported command: " + commandName);
    };
  }

  public Map<String, String> getSupportedCommandNames() {
    return Map.of(
        DbMigrationCommand.NAME,
        DbMigrationCommand.DESCRIPTION,
        CompactCommand.NAME,
        CompactCommand.DESCRIPTION,
        ExportEmbeddedDatabaseCommand.NAME,
        ExportEmbeddedDatabaseCommand.DESCRIPTION,
        ResetAdminCommand.NAME,
        ResetAdminCommand.DESCRIPTION);
  }

  protected SpringApplicationBuilder newApplicationBuilder(Class<?> applicationClass, String[] args) {
    SpringApplicationBuilder builder = new SpringApplicationBuilder(applicationClass)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.main.web-application-type=none",
            "spring.main.register-shutdown-hook=false");

    DropwizardConfigBootstrap.configure(builder, resolveConfigFilePath(args), isImplicitDefaultConfigFile(args));
    return applyCommandModeProperties(builder);
  }

  public static SpringApplicationBuilder applyCommandModeProperties(SpringApplicationBuilder builder) {
    return builder.initializers(context -> context.getEnvironment()
        .getPropertySources()
        .addFirst(
            new MapPropertySource(COMMAND_MODE_PROPERTY_SOURCE, Map.<String, Object>of(
                COMMAND_MODE_PROPERTY, "true",
                STARTUP_MIGRATIONS_ENABLED_PROPERTY, "false"))));
  }

  public String resolveConfigFilePath(String[] args) {
    if (args.length > 1 && looksLikeConfigFile(args[1])) {
      return args[1];
    }
    return "config.yml";
  }

  public boolean isImplicitDefaultConfigFile(String[] args) {
    return !(args.length > 1 && looksLikeConfigFile(args[1]));
  }

  public String[] extractCommandArguments(String[] args) {
    int argumentOffset = 1;
    if (args.length > 1 && looksLikeConfigFile(args[1])) {
      argumentOffset = 2;
    }
    return Arrays.copyOfRange(args, argumentOffset, args.length);
  }

  protected boolean looksLikeConfigFile(String argument) {
    return argument.endsWith(".yml") || argument.endsWith(".yaml");
  }
}
