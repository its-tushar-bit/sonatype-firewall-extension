/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring;

/**
 * Shared utility for parsing command-line arguments to extract the Dropwizard-style
 * config file path. Used by both single-tenant {@link InsightBrainSpringApplication}
 * and multi-tenant {@code MultiTenantInsightBrainService} entry points.
 */
public final class LaunchConfigurationResolver
{
  /**
   * Resolved launch configuration containing the config file path and whether
   * it was implicitly defaulted or explicitly provided.
   */
  public record LaunchConfiguration(String configFilePath, boolean implicitDefaultConfigFile)
  {
  }

  private LaunchConfigurationResolver() {
    // utility class
  }

  /**
   * Parse command line arguments to extract config file path.
   * Handles both "server config.yml" and direct "config.yml" formats.
   */
  public static LaunchConfiguration resolve(String[] args) {
    String configFilePath = "config.yml";
    boolean implicitDefaultConfigFile = true;

    if (args.length > 0) {
      String configPath = args[0];
      if (configPath.equals("server") && args.length > 1) {
        configPath = args[1];
      }
      if (configPath.endsWith(".yml") || configPath.endsWith(".yaml")) {
        configFilePath = configPath;
        implicitDefaultConfigFile = false;
      }
    }

    return new LaunchConfiguration(configFilePath, implicitDefaultConfigFile);
  }
}
