/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

/**
 * DTO for Lifecycle Repository Manager list
 *
 * @since 1.198
 */
public class ApiLifecycleRepositoryManagerDTO
{
  /**
   * Connection status of a Nexus Repository Manager instance.
   *
   * @since 1.198
   */
  public enum ConnectionStatus
  {
    CONNECTED,
    DISCONNECTED
  }

  /** NXRM deployment instance ID */
  public String instanceId;

  /** NXRM base URL (optional) */
  public String baseUrl;

  /** Count of hosted repositories */
  public int hostedRepositoryCount;

  /** Connection status of the repository manager */
  public ConnectionStatus connectionStatus;

  /**
   * Timestamp (epoch millis) of the most recent activity for this repository manager, or null if no activity recorded
   */
  public Long lastActivityTime;
}
