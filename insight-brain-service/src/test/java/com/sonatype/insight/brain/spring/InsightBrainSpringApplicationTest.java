/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.spring.LaunchConfigurationResolver.LaunchConfiguration;
import org.junit.Test;

public class InsightBrainSpringApplicationTest
{
  @Test
  public void shouldUseImplicitDefaultConfigWhenNoConfigArgumentIsProvided() {
    LaunchConfiguration launchConfiguration =
        LaunchConfigurationResolver.resolve(new String[0]);

    assertThat(launchConfiguration.configFilePath()).isEqualTo("config.yml");
    assertThat(launchConfiguration.implicitDefaultConfigFile()).isTrue();
  }

  @Test
  public void shouldResolveServerCommandConfigFile() {
    LaunchConfiguration launchConfiguration =
        LaunchConfigurationResolver.resolve(new String[]{"server", "custom.yml"});

    assertThat(launchConfiguration.configFilePath()).isEqualTo("custom.yml");
    assertThat(launchConfiguration.implicitDefaultConfigFile()).isFalse();
  }

  @Test
  public void shouldResolveDirectConfigFileArgument() {
    LaunchConfiguration launchConfiguration =
        LaunchConfigurationResolver.resolve(new String[]{"custom.yaml"});

    assertThat(launchConfiguration.configFilePath()).isEqualTo("custom.yaml");
    assertThat(launchConfiguration.implicitDefaultConfigFile()).isFalse();
  }
}
