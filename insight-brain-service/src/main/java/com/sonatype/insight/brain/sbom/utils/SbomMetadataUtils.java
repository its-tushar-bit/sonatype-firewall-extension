/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.product.license.ProductLicense;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class SbomMetadataUtils

{
  private static final Logger log = LoggerFactory.getLogger(SbomMetadataUtils.class);

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ProductLicense productLicense;

  @Inject
  public SbomMetadataUtils(
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ProductLicense productLicense)
  {
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.productLicense = productLicense;
  }

  public boolean hasMaxSbomLimitBeenReached() {
    long currentSbomFiles = thirdPartySbomMetadataDAO.getActiveSbomCount();
    if (currentSbomFiles < productLicense.getMaxSboms()) {
      return false;
    }
    else {
      log.warn(
          "SBOM Manager has reached its licensed maximum of {} files. " +
              "Contact your account team to manage all your SBOMs.",
          productLicense.getMaxSboms());
      return true;
    }
  }
}
