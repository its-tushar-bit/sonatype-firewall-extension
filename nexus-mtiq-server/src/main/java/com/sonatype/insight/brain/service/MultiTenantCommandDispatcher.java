/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.migration.MigrateTenantsCommand;
import com.sonatype.insight.brain.migration.MultiTenantDbMigrationCommand;
import com.sonatype.insight.brain.spring.DropwizardConfigBootstrap;
import com.sonatype.insight.brain.spring.InsightBrainCommandDispatcher;
import com.sonatype.insight.brain.spring.InsightBrainCompatibilityCommand;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class MultiTenantCommandDispatcher
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantCommandDispatcher.class);

  private final InsightBrainCommandDispatcher delegate = new InsightBrainCommandDispatcher();

  public boolean handles(String[] args) {
    return args.length > 0 && getSupportedCommandNames().containsKey(args[0]);
  }

  public void dispatch(String[] args) throws Exception {
    String commandName = args[0];
    if (MigrateTenantsCommand.NAME.equals(commandName)) {
      dispatchWithoutSpringContext(args);
    }
    else {
      dispatchWithSpringContext(args);
    }
  }

  private void dispatchWithoutSpringContext(String[] args) throws Exception {
    String configFilePath = resolveConfigFilePath(args);
    log.info("Running command '{}' with config file '{}'", args[0], configFilePath);

    MultiTenantInsightConfig config = delegate.loadConfig(configFilePath, MultiTenantInsightConfig.class);

    MigrateTenantsCommand command = new MigrateTenantsCommand(config);
    command.run(delegate.extractCommandArguments(args));
  }

  private void dispatchWithSpringContext(String[] args) throws Exception {
    try (ConfigurableApplicationContext applicationContext = newApplicationBuilder(args).run()) {
      Map<String, InsightBrainCompatibilityCommand> commands = applicationContext.getBeansOfType(
          InsightBrainCompatibilityCommand.class)
          .values()
          .stream()
          .collect(Collectors.toMap(
              InsightBrainCompatibilityCommand::getName,
              command -> command,
              this::preferMultiTenantCommand,
              LinkedHashMap::new));

      InsightBrainCompatibilityCommand command = commands.get(args[0]);
      if (command == null) {
        throw new IllegalArgumentException("Unsupported command '" + args[0] + "'. Supported commands: "
            + String.join(", ", commands.keySet()));
      }
      command.run(delegate.extractCommandArguments(args));
    }
  }

  public Map<String, String> getSupportedCommandNames() {
    Map<String, String> supported = new LinkedHashMap<>(delegate.getSupportedCommandNames());
    supported.put(MigrateTenantsCommand.NAME, MigrateTenantsCommand.DESCRIPTION);
    return supported;
  }

  private InsightBrainCompatibilityCommand preferMultiTenantCommand(
      InsightBrainCompatibilityCommand left,
      InsightBrainCompatibilityCommand right)
  {
    if (right instanceof MultiTenantDbMigrationCommand) {
      return right;
    }
    if (left instanceof MultiTenantDbMigrationCommand) {
      return left;
    }
    return left;
  }

  protected SpringApplicationBuilder newApplicationBuilder(String[] args) {
    SpringApplicationBuilder builder = new SpringApplicationBuilder(MultiTenantInsightBrainService.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.main.web-application-type=none",
            "spring.main.register-shutdown-hook=false",
            "sonatype.command-mode=true",
            "sonatype.database.startup-migrations.enabled=false",
            "sonatype.mtiq.enabled=true");
    DropwizardConfigBootstrap.configure(
        builder,
        resolveConfigFilePath(args),
        MultiTenantInsightConfig.class,
        delegate.isImplicitDefaultConfigFile(args));
    return InsightBrainCommandDispatcher.applyCommandModeProperties(builder);
  }

  private String resolveConfigFilePath(String[] args) {
    return delegate.resolveConfigFilePath(args);
  }
}
