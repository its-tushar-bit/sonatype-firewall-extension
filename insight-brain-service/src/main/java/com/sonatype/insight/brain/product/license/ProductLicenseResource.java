/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.product.license.CLMLicenseManager.LicenseSummary;
import com.sonatype.insight.error.exception.BadRequestException;

import org.sonatype.licensing.LicensingException;

import com.sun.jersey.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ProductLicenseResource.SERVICE_PATH)
@Named
public class ProductLicenseResource
{
  public static final String SERVICE_PATH = "rest/product/license";

  private final CLMLicenseManager licenseManager;

  private final Logger log = LoggerFactory.getLogger(ProductLicenseResource.class);

  @Inject
  public ProductLicenseResource(CLMLicenseManager licenseManager) {
    this.licenseManager = licenseManager;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_PLAIN)
  @UnlicensedPath
  public String installLicense(@FormDataParam("file") InputStream is, @QueryParam("forceSuccess") boolean forceSuccess)
  {
    try {
      licenseManager.installLicense(is);
      log.info("CLM License successfully installed");
      // Note an empty string triggers success in the UI
      return "";
    }
    catch (LicensingException e) {
      // as per CLM-870, the actual exception msg is deemed inappropriate so we provide a stock msg
      String msg = "The provided license file is invalid. Please verify you selected the correct file."
          + " If the problem persists, please contact our support team.";

      // log the actual exception (especially its message which isn't otherwise revealed) to help support
      log.debug("Unable to install license", e);

      // IE<10 will only work in case of a 200 response, otherwise the response gets junked and replaced with some local
      // error page which then fails to load because of cross site scripting probs
      if (forceSuccess) {
        return msg;
      }

      throw new BadRequestException(msg, e);
    } catch (IOException e) {
      String msg = "The license file was unable to install. Please ensure server has access to "
          + System.getProperty("java.io.tmpdir") + ". If the problem persists, please contact our support team.";

      log.error("Unable to install license", e);

      if (forceSuccess) {
        return msg;
      }

      throw new BadRequestException(msg, e);
    }
  }

  @DELETE
  public void uninstallLicense() throws LicensingException {
    licenseManager.uninstallLicense();
    log.info("CLM License successfully uninstalled");
  }

  @GET
  @UnlicensedPath
  @Produces(MediaType.APPLICATION_JSON)
  public LicenseSummary validate() {
    licenseManager.validate();
    return licenseManager.getLicenseSummary();
  }

}
