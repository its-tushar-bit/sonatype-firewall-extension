/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

@Named
@Path(LicenseThreatGroupLicenseResource.SERVICE_PATH)
public class LicenseThreatGroupLicenseResource
{
  public static final String SERVICE_PATH = "rest/licenseThreatGroupLicense/{ownerType: application|organization}/{ownerId}/{licenseThreatGroupId}";

  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

  @GET
  @Produces({ MediaType.APPLICATION_JSON })
  @Authorize(permission = Permission.READ)
  public List<LicenseThreatGroupLicense> getLicenseThreatGroupLicenses(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @PathParam("licenseThreatGroupId") String licenseThreatGroupId)
  {
    return licenseThreatGroupLicenseDAO.getByLicenseThreatGroupId(licenseThreatGroupId);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  public List<LicenseThreatGroupLicense> setLicenseThreatGroupLicenses(
      @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") String ownerType,
      @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @PathParam("licenseThreatGroupId") String licenseThreatGroupId, Set<String> licenseIds)
  {
    licenseThreatGroupLicenseDAO.setLicenses(licenseThreatGroupId, licenseIds);

    return licenseThreatGroupLicenseDAO.getByLicenseThreatGroupId(licenseThreatGroupId);
  }
}
