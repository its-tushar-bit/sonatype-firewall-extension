/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

/**
 * Interface for providing license display names.
 */
public interface LicenseNameProvider
{
  /**
   * Gets the short display name for a license.
   *
   * @param licenseId the license ID
   * @return the short display name of the license, or the license ID if the license is not found
   */
  String getShortDisplayName(String licenseId, boolean isMultiLicense);
}
