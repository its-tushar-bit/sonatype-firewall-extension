/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

/**
 * Denotes a feature that is not controlled by the product license.
 */
public enum NonLicensedFeature
    implements Feature
{
  ALLOW_EXTERNAL_HYPERLINKS,

  ENABLE_POLICY_REPORT_PREVIOUS_VERSION_LINK,

  LABELS,

  POLICY,

  POLICY_VIOLATIONS,

  REEVALUATE_POLICY,

  RELEASE_GRAPH,

  ROOT_ORG,

  ROOT_ORG_MIGRATE;

  @Override
  public String toString() {
    return getId();
  }
}
