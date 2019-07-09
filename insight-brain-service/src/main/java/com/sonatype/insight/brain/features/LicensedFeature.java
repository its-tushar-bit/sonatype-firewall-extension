/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

/**
 * Denotes a feature that is controlled by the product license.
 */
public enum LicensedFeature
    implements Feature
{
  CI_INTEGRATION,

  CLI_INTEGRATION,

  DASHBOARD,

  ENFORCEMENT,

  FIREWALL,

  FIREWALL_FOR_ARTIFACTORY,

  IDE_INTEGRATION,

  NOTIFICATIONS,

  POLICY_GRANDFATHERING,

  POLICY_MONITORING,

  POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,

  POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES,

  QUALITY,

  RM_STAGING_INTEGRATION,
  
  WEBHOOKS_FOR_APPLICATIONS,

  WEBHOOKS_FOR_REPOSITORIES;

  @Override
  public String toString() {
    return getId();
  }
}
