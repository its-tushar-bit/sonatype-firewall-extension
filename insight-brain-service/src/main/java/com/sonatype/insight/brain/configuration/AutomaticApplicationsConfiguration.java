/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

/**
 * @since 1.43
 */
public class AutomaticApplicationsConfiguration
{
  private boolean enabled;

  private String parentOrganizationId;

  public AutomaticApplicationsConfiguration() {
  }

  public AutomaticApplicationsConfiguration(boolean enabled, String parentOrganizationId) {
    this.enabled = enabled;
    this.parentOrganizationId = parentOrganizationId;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getParentOrganizationId() {
    return parentOrganizationId;
  }

  public void setParentOrganizationId(String parentOrganizationId) {
    this.parentOrganizationId = parentOrganizationId;
  }
}
