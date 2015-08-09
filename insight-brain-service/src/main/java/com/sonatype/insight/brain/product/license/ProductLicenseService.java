/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.CLMLicenseManager.LicenseSummary;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;

import org.sonatype.licensing.LicensingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ProductLicenseService
{
  private final CLMLicenseManager licenseManager;

  private final Logger log = LoggerFactory.getLogger(ProductLicenseService.class);

  @Inject
  public ProductLicenseService(CLMLicenseManager licenseManager) {
    this.licenseManager = licenseManager;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void installLicense(InputStream is) {
    try {
      licenseManager.installLicense(is);
      log.info("CLM License successfully installed");
    }
    catch (LicensingException e) {
      // as per CLM-870, the actual exception msg is deemed inappropriate so we provide a stock msg
      String msg = "The provided license file is invalid. Please verify you selected the correct file."
          + " If the problem persists, please contact our support team.";

      // log the actual exception (especially its message which isn't otherwise revealed) to help support
      log.debug("Unable to install license", e);

      throw new BadRequestException(msg, e);
    }
    catch (IOException e) {
      String msg = "The license file was unable to install. Please ensure server has access to "
          + System.getProperty("java.io.tmpdir") + ". If the problem persists, please contact our support team.";

      log.error("Unable to install license", e);

      throw new BadRequestException(msg, e);
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void uninstallLicense() throws LicensingException {
    licenseManager.uninstallLicense();
    log.info("CLM License successfully uninstalled");
  }

  public LicenseSummary validateLicense() {
    licenseManager.validate();
    return licenseManager.getLicenseSummary();
  }
}
