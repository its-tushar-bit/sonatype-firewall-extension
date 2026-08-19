/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

/**
 * One organization that contains the requested component hash via applications in the caller's
 * readable scope.
 */
public class ComponentUsageOrganizationRowDTO
{
  public String organizationId;

  public String organizationName;

  /** Distinct applications in the caller's readable scope under this org that contain the hash. */
  public long applicationCount;

  /** Latest owner_component.time among those applications, epoch millis. */
  public Long lastSeenTime;
}
