/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

import com.codahale.metrics.annotation.Timed;

import static java.util.stream.Collectors.toList;

@Named
@Timed
@Path(LicenseThreatGroupLicenseResource.RESOURCE_PATH)
public class LicenseThreatGroupLicenseResource
{
  public static final String RESOURCE_PATH = 
      "rest/licenseThreatGroupLicense/{ownerType: application|organization}/{ownerId}/{licenseThreatGroupId}";

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final LicenseDAO licenseDAO;

  @Inject
  public LicenseThreatGroupLicenseResource(
      final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO,
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final LicenseDAO licenseDAO)
  {
    this.licenseThreatGroupLicenseDAO = licenseThreatGroupLicenseDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.licenseDAO = licenseDAO;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  public List<LicenseThreatGroupLicense> getLicenseThreatGroupLicenses(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @PathParam("licenseThreatGroupId") String licenseThreatGroupId)
  {
    return licenseThreatGroupLicenseDAO.getByLicenseThreatGroupId(licenseThreatGroupId);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.WRITE)
  @Audited(AuditEvent.CONFIGURE_LICENSE_THREAT_GROUP_LICENSES)
  public List<LicenseThreatGroupLicense> setLicenseThreatGroupLicenses(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.ID) @PathParam("ownerId") String ownerId,
      @PathParam("licenseThreatGroupId") String licenseThreatGroupId,
      Set<String> licenseIds)
  {
    licenseThreatGroupLicenseDAO.setLicenses(licenseThreatGroupId, licenseIds);

    AuditData.get() //
        .setLicenseThreatGroup(licenseThreatGroupDAO.getByIdNotNull(licenseThreatGroupId)) //
        .setData("licenseNames", licenseIds.stream().map(licenseDAO::getByIdNotNull).map(License::getShortDisplayName)
            .sorted().collect(toList()));

    return licenseThreatGroupLicenseDAO.getByLicenseThreatGroupId(licenseThreatGroupId);
  }
}
