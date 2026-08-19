/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

public class ApiArtifactoryConnectionStatusResponseDTO
{
  /**
   * Whether artifactory connections are enabled for this org/app. If null, then this will be inherited.
   */
  public Boolean enabled;

  /**
   * The id of the organization we inherit from or null if not inherited.
   */
  public String inheritedFromOrganizationId;

  /**
   * The name of the organization we inherit from or null if not inherited.
   */
  public String inheritedFromOrganizationName;

  /**
   * Whether children (orgs and apps) are allowed to override the enabled value.
   */
  public boolean allowOverride;

  /**
   * Whether the configuration of the inheriting owner is enabled/disabled.
   */
  public Boolean inheritedFromOrgEnabled;

  /**
   * Whether enabled/allowOverride can be changed for this org/app (a parent org may not allow it to be overridden).
   */
  public boolean allowChange;
}
