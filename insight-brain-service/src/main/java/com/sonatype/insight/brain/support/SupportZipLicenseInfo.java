/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.util.Set;

import com.sonatype.insight.brain.product.license.LicenseInfo;

/**
 * @since 1.69
 */
public class SupportZipLicenseInfo
{
  public LicenseInfo licenseInfo;

  public Set<String> features;

  public Set<String> stageIds;

  public Set<String> licensingModels;

  public Integer applicationCountLimit;

  public SupportZipLicenseInfo() {
  }

  public SupportZipLicenseInfo(
      LicenseInfo licenseInfo,
      Set<String> features,
      Set<String> stageIds,
      Set<String> licensingModels,
      Integer applicationCountLimit)
  {
    this.licenseInfo = licenseInfo;
    this.features = features;
    this.stageIds = stageIds;
    this.licensingModels = licensingModels;
    this.applicationCountLimit = applicationCountLimit;
  }
}
