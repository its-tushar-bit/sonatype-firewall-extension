/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cluster;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterConfig
{
  private Long lastModified;

  @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
  private ClusterState state = ClusterState.UNKNOWN;

  public ClusterConfig() {
    // for jackson
  }

  public ClusterConfig(final ClusterConfig clusterConfig) {
    this.lastModified = clusterConfig.getLastModified();
    this.state = clusterConfig.getState();
  }

  public Long getLastModified() {
    return lastModified;
  }

  public void setLastModified(final Long lastModified) {
    this.lastModified = lastModified;
  }

  public ClusterState getState() {
    return state;
  }

  public void setState(final ClusterState state) {
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
