/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Denotes a feature of the CLM server.
 *
 * @since 1.9
 */
public enum Feature
{
  ALLOW_EXTERNAL_HYPERLINKS,

  CI_INTEGRATION,

  CLI_INTEGRATION,

  DASHBOARD,

  ENFORCEMENT,

  FIREWALL,

  FIREWALL_FOR_ARTIFACTORY,

  IDE_INTEGRATION,

  LABELS,

  NOTIFICATIONS,

  POLICY,

  POLICY_GRANDFATHERING,

  POLICY_MONITORING,

  POLICY_VIOLATIONS,

  POLICY_VIOLATION_LOGGING_FOR_APPLICATIONS,

  POLICY_VIOLATION_LOGGING_FOR_REPOSITORIES,

  QUALITY,

  REEVALUATE_POLICY,

  RELEASE_GRAPH,

  RM_STAGING_INTEGRATION,

  ROOT_ORG,

  ROOT_ORG_MIGRATE,

  WEBHOOKS_FOR_APPLICATIONS,

  WEBHOOKS_FOR_REPOSITORIES;

  @Override
  @JsonValue
  public String toString() {
    return name().toLowerCase(Locale.ENGLISH).replace('_', '-');
  }
}
