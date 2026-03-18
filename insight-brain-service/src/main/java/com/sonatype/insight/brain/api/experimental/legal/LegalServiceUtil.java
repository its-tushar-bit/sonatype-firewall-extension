/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

public class LegalServiceUtil
{
  private LegalServiceUtil() {
    // Static util class
  }

  public static String getContentHash(String content) {
    return DigestUtils.sha256Hex(content);
  }

  public static void checkLicense(ProductLicense productLicense, Logger log) {
    if (!productLicense.hasFeature(LicensedFeature.ADVANCED_LEGAL_PACK)) {
      log.debug("License does not support Advanced Legal Pack features");
      throw new InvalidLicenseException();
    }
  }
}
