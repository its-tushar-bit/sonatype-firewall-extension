/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cpematching;

public class CpeMatchingConfigurationDTO
{
  /**
   * Whether CPE Matching status is enabled for this org/app. If null, then CPE matching status was never set for this
   * org/app.
   */
  public Boolean enabled;

  /**
   * Whether CPE matching is enabled for the parent org. If null, then no parent set this value.
   */
  public Boolean enabledInParent;

  /**
   * The name of the organization the cpe matching status is inherited from or null if it isn't inherited.
   */
  public String inheritedFromOrganizationName;

  /**
   * Whether children (orgs and apps) are allowed to override the CPE matching status.
   */
  public Boolean allowOverride;

  /**
   * Nearest ancestor configuration indicating whether children (orgs and apps) are allowed to override the CPE
   * matching status.
   * An explicit false value found on any ancestor would signify that overriding is not allowed.
   * A true or null value means overriding is permitted.
   */
  public Boolean inheritedFromOrganizationAllowOverride;

  public CpeMatchingConfigurationDTO() {
    // for jackson
  }
}
