/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Set;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;

public class ApplicationLicenseUsageTelemetry
{
  public static final String ATTRIBUTE_NAME = "application_license";

  private final String applicationId;

  private final Set<String> componentHashes;

  private final Set<String> licenseIds;

  private String realApplicationId;

  public ApplicationLicenseUsageTelemetry(
      String applicationId,
      Set<String> componentHashes,
      Set<String> licenseIds)
  {
    this.applicationId = HdsClientAnalytics.obfuscate(applicationId);
    this.componentHashes = componentHashes;
    this.licenseIds = licenseIds;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public Set<String> getComponentHashes() {
    return componentHashes;
  }

  public Set<String> getLicenseIds() {
    return licenseIds;
  }

  public String getRealApplicationId() {
    return realApplicationId;
  }

  public void setRealApplicationId(final String realApplicationId) {
    this.realApplicationId = realApplicationId;
  }
}
