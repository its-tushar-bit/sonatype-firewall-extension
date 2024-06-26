/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.license.LicenseThreatGroupService.ApplicableLicenseThreatGroups;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(LicenseThreatGroupResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_MANAGEMENT)
public class LicenseThreatGroupResource
{
  public static final String RESOURCE_PATH = "rest/licenseThreatGroup/{ownerType: application|organization}/{ownerId}";

  private final LicenseThreatGroupService licenseThreatGroupService;

  @Inject
  public LicenseThreatGroupResource(final LicenseThreatGroupService licenseThreatGroupService) {
    this.licenseThreatGroupService = licenseThreatGroupService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<LicenseThreatGroup> getLicenseThreatGroups(@PathParam("ownerType") OwnerType ownerType,
                                                         @PathParam("ownerId") String ownerId)
  {
    return licenseThreatGroupService.getLicenseThreatGroups(ownerType, ownerId);
  }

  /**
   * @since 1.6
   */
  @GET
  @Path("applicable")
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicableLicenseThreatGroups getApplicableLicenseThreatGroups(@PathParam("ownerType") OwnerType ownerType,
                                                                        @PathParam("ownerId") String ownerId)
  {
    return licenseThreatGroupService.getApplicableLicenseThreatGroups(ownerType, ownerId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_LICENSE_THREAT_GROUP)
  public LicenseThreatGroup addLicenseThreatGroup(@PathParam("ownerType") OwnerType ownerType,
                                                  @PathParam("ownerId") String ownerId,
                                                  LicenseThreatGroup licenseThreatGroup)
  {
    if (ownerType.equals(OwnerType.APPLICATION)) {
      throw new BadRequestException("Applications are not allowed to add license threat groups.");
    }
    return licenseThreatGroupService.addLicenseThreatGroup(ownerId, licenseThreatGroup);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_LICENSE_THREAT_GROUP)
  public LicenseThreatGroup updateLicenseThreatGroup(@PathParam("ownerType") OwnerType ownerType,
                                                     @PathParam("ownerId") String ownerId,
                                                     LicenseThreatGroup licenseThreatGroup)
  {
    return licenseThreatGroupService.updateLicenseThreatGroup(ownerType, ownerId, licenseThreatGroup);
  }

  @DELETE
  @Path("{licenseThreatGroupId}")
  @Audited(AuditEvent.DELETE_LICENSE_THREAT_GROUP)
  public void deleteLicenseThreatGroup(@PathParam("ownerType") OwnerType ownerType,
                                       @PathParam("ownerId") String ownerId,
                                       @PathParam("licenseThreatGroupId") String licenseThreatGroupId)
  {
    licenseThreatGroupService.deleteLicenseThreatGroup(ownerType, ownerId, licenseThreatGroupId);
  }
}
