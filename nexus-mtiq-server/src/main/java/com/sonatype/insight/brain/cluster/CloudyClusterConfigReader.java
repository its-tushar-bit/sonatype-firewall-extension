/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cluster;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is meant to read a Cloudy cluster configuration yaml file.
 * <p>
 * In particular, the yaml file should contain a cluster 'state' String field, which can be one of the following
 * values:
 * <ul>
 *   <li>ACTIVE: The cluster is active and serving traffic.</li>
 *   <li>FILLING: The cluster is in the process of becoming the active cluster and is filling up with traffic.</li>
 *   <li>DRAINING: The cluster is in the process of becoming the inactive cluster and is draining traffic.</li>
 *   <li>INACTIVE: The cluster is inactive and scaled down.</li>
 * </ul>
 * (see also https://github.com/sonatype/sre-cloudy-mccloudface/blob/main/docs/contributors/blue-green.md).
 */
@Named
@Singleton
public class CloudyClusterConfigReader
{
  private static final Logger log = LoggerFactory.getLogger(CloudyClusterConfigReader.class);

  private final MultiTenantInsightConfig multiTenantInsightConfig;

  private final ObjectMapper objectMapper;

  private volatile CloudyClusterConfig cloudyClusterConfig = new CloudyClusterConfig();

  @Inject
  public CloudyClusterConfigReader(final MultiTenantInsightConfig multiTenantInsightConfig) {
    this(multiTenantInsightConfig, createObjectMapper());
  }

  // Visible for testing
  public CloudyClusterConfigReader(
      final MultiTenantInsightConfig multiTenantInsightConfig,
      final ObjectMapper objectMapper)
  {
    this.multiTenantInsightConfig = multiTenantInsightConfig;
    this.objectMapper = objectMapper;
  }

  public CloudyClusterConfig getClusterConfig() {
    readClusterConfig();
    return new CloudyClusterConfig(cloudyClusterConfig);
  }

  private void readClusterConfig() {
    String clusterConfigFilePath = multiTenantInsightConfig.getCloudyClusterConfigFilePath();

    if (!isValid(clusterConfigFilePath)) {
      return;
    }

    File clusterConfigFile = newFile(clusterConfigFilePath);

    Long lastModified = clusterConfigFile.lastModified();
    if (Objects.equals(lastModified, this.cloudyClusterConfig.getLastModified())) {
      log.trace("The cluster configuration file {} has no changes.", clusterConfigFile);
      return;
    }

    CloudyClusterConfig cloudyClusterConfig = new CloudyClusterConfig();
    try {
      log.debug("Reading the cluster configuration from {}.", clusterConfigFile);
      cloudyClusterConfig = objectMapper.readValue(clusterConfigFile, CloudyClusterConfig.class);
    }
    catch (IOException e) {
      log.error("Failed to read the cluster configuration from {}.", clusterConfigFilePath, e);
    }
    finally {
      cloudyClusterConfig.setLastModified(lastModified);
      this.cloudyClusterConfig = cloudyClusterConfig;
    }

    log.debug("The cluster configuration is {}.", cloudyClusterConfig);
  }

  private boolean isValid(final String clusterConfigFilePath) {
    if (clusterConfigFilePath == null) {
      log.trace("The cluster configuration file path is null.");
      return false;
    }
    File clusterConfigFile = newFile(clusterConfigFilePath);
    if (!clusterConfigFile.exists()) {
      log.warn("The cluster configuration file {} does not exist.", clusterConfigFile);
      return false;
    }
    if (!clusterConfigFile.canRead()) {
      log.warn("The cluster configuration file {} cannot be read.", clusterConfigFile);
      return false;
    }
    return true;
  }

  // Visible for testing
  File newFile(final String pathname) {
    return new File(pathname);
  }

  // Visible for testing
  public static ObjectMapper createObjectMapper() {
    return new ObjectMapper(new YAMLFactory());
  }
}
