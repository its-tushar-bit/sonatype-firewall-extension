/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cluster;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudyClusterConfig
{
  private Long lastModified;

  @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
  private CloudyClusterState state = CloudyClusterState.UNKNOWN;

  public CloudyClusterConfig() {
    // for jackson
  }

  public CloudyClusterConfig(final CloudyClusterConfig cloudyClusterConfig) {
    this.lastModified = cloudyClusterConfig.getLastModified();
    this.state = cloudyClusterConfig.getState();
  }

  public Long getLastModified() {
    return lastModified;
  }

  public void setLastModified(final Long lastModified) {
    this.lastModified = lastModified;
  }

  public CloudyClusterState getState() {
    return state;
  }

  public void setState(final CloudyClusterState state) {
    this.state = state;
  }

  @Override
  public String toString() {
    return "ClusterConfig{" +
        "lastModified=" + lastModified +
        ", state=" + state +
        '}';
  }
}
