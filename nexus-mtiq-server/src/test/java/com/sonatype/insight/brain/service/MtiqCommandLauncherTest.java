/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

public class MtiqCommandLauncherTest
{
  private final MultiTenantCommandDispatcher dispatcher = new MultiTenantCommandDispatcher();

  @Test
  public void shouldRecognizeLegacyMtiqMigrationCommand() {
    assertThat(dispatcher.handles(new String[]{"migrate-mtiq-db", "config.yml"})).isTrue();
    assertThat(dispatcher.handles(new String[]{"migrate-db", "config.yml"})).isTrue();
    assertThat(dispatcher.handles(new String[]{"reset-admin", "config.yml"})).isTrue();
    assertThat(dispatcher.handles(new String[]{"server", "config.yml"})).isFalse();
  }

  @Test
  public void shouldSetMtiqCommandPropertiesForCompatibilityCommands() {
    AtomicReference<String> commandModeProperty = new AtomicReference<>();
    AtomicReference<String> startupMigrationsProperty = new AtomicReference<>();
    AtomicReference<String> mtiqEnabledProperty = new AtomicReference<>();
    SpringApplicationBuilder builder = dispatcher.newApplicationBuilder(
        new String[]{"migrate-db", TestInsightBrainService.DEFAULT_CONFIG_FILE_PATH});
    builder.initializers(context -> {
      commandModeProperty.set(context.getEnvironment().getProperty("sonatype.command-mode"));
      startupMigrationsProperty.set(context.getEnvironment()
          .getProperty("sonatype.database.startup-migrations.enabled"));
      mtiqEnabledProperty.set(context.getEnvironment().getProperty("sonatype.mtiq.enabled"));
      throw new StopAfterPropertyCapture();
    });

    Throwable thrown = catchThrowable(builder::run);

    assertThat(thrown).isInstanceOf(StopAfterPropertyCapture.class);
    assertThat(commandModeProperty.get()).isEqualTo("true");
    assertThat(startupMigrationsProperty.get()).isEqualTo("false");
    assertThat(mtiqEnabledProperty.get()).isEqualTo("true");
  }

  private static final class StopAfterPropertyCapture
      extends RuntimeException
  {
  }
}
