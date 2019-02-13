/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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

  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

  private final LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private final LicenseDAO licenseDAO = new LicenseDAO();

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.READ)
  @SuppressWarnings("checkstyle:LineLength")
  public List<LicenseThreatGroupLicense> getLicenseThreatGroupLicenses(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
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
  @SuppressWarnings("checkstyle:LineLength")
  public List<LicenseThreatGroupLicense> setLicenseThreatGroupLicenses(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.TYPE) @PathParam("ownerType") OwnerType ownerType,
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
