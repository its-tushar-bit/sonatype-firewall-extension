/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

public class ApiArtifactoryConnectionStatusRequestDTO
{
  /**
   * Whether artifactory connections are enabled for this org/app. If null, then this will be inherited.
   */
  public Boolean enabled;

  /**
   * Whether children (orgs and apps) are allowed to override the enabled value.
   */
  public boolean allowOverride;
}
