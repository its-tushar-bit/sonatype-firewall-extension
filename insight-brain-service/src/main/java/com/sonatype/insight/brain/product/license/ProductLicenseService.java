/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.licensing.LicensingException;

@Named
public class ProductLicenseService
{
  private final CLMLicenseManager licenseManager;

  private final ProductLicense productLicense;

  private final Logger log = LoggerFactory.getLogger(ProductLicenseService.class);

  @Inject
  public ProductLicenseService(CLMLicenseManager licenseManager, ProductLicense productLicense) {
    this.licenseManager = licenseManager;
    this.productLicense = productLicense;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void installLicense(InputStream is, String filename) {
    installLicenseNoAuthz(is, filename);
  }

  public void installLicenseNoAuthz(InputStream is, String filename) {
    try {
      licenseManager.installLicense(is);
      log.info("Nexus IQ License {} successfully installed", filename);
      licenseManager.auditLicense(filename);
    }
    catch (LicensingException e) {
      String msg;
      if (e instanceof ExternalDatabaseNotSupportedException) {
        // this exception type carries a proper (and more specific) message
        msg = e.getMessage();
      }
      else {
        // as per CLM-870, the actual exception msg is deemed inappropriate so we provide a stock msg
        msg = "The provided license file " + filename + " is invalid. Please verify you selected the correct file."
            + " If the problem persists, please contact our support team.";
      }

      // log the actual exception (especially its message which isn't otherwise revealed) to help support
      log.debug("Unable to install license {}", filename, e);

      throw new BadRequestException(msg, e);
    }
    catch (IOException e) {
      String msg = "The license file " + filename + " was unable to install. Please ensure server has access to "
          + System.getProperty("java.io.tmpdir") + ". If the problem persists, please contact our support team.";

      log.error("Unable to install license {}", filename, e);

      throw new BadRequestException(msg, e);
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void uninstallLicense() throws LicensingException {
    licenseManager.auditLicense(null);
    licenseManager.uninstallLicense();
    log.info("Nexus IQ License successfully uninstalled");
  }

  public LicenseSummary validateLicense() {
    try {
      productLicense.validate();
      return licenseManager.getLicenseSummary();
    }
    catch (InvalidLicenseException e) {
      throw new WebApplicationException(
          Response.status(402)
              .type(MediaType.TEXT_PLAIN_TYPE)
              .entity(e.getMessage())
              .build());
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public LicenseInfo getLicenseInfo() {
    return licenseManager.getLicenseInfo();
  }
}
