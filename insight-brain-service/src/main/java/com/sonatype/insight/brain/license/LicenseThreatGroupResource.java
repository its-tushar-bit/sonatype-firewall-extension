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

import com.sonatype.insight.brain.license.LicenseThreatGroupService.ApplicableLicenseThreatGroups;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

@Named
@Path(LicenseThreatGroupResource.SERVICE_PATH)
public class LicenseThreatGroupResource
{
  public static final String SERVICE_PATH = "rest/licenseThreatGroup/{ownerType: application|organization}/{ownerId}";

  private final LicenseThreatGroupService licenseThreatGroupService;

  @Inject
  public LicenseThreatGroupResource(final LicenseThreatGroupService licenseThreatGroupService) {
    this.licenseThreatGroupService = licenseThreatGroupService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<LicenseThreatGroup> getLicenseThreatGroups(@PathParam("ownerType") String ownerType,
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
  public ApplicableLicenseThreatGroups getApplicableLicenseThreatGroups(@PathParam("ownerType") String ownerType,
      @PathParam("ownerId") String ownerId)
  {
    return licenseThreatGroupService.getApplicableLicenseThreatGroups(ownerType, ownerId);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public LicenseThreatGroup addLicenseThreatGroup(@PathParam("ownerType") String ownerType,
      @PathParam("ownerId") String ownerId, LicenseThreatGroup licenseThreatGroup)
  {
    return licenseThreatGroupService.addLicenseThreatGroup(ownerType, ownerId, licenseThreatGroup);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public LicenseThreatGroup updateLicenseThreatGroup(@PathParam("ownerType") String ownerType,
      @PathParam("ownerId") String ownerId, LicenseThreatGroup licenseThreatGroup)
  {
    return licenseThreatGroupService.updateLicenseThreatGroup(ownerType, ownerId, licenseThreatGroup);
  }

  @DELETE
  @Path("{licenseThreatGroupId}")
  public void deleteLicenseThreatGroup(@PathParam("ownerType") String ownerType, @PathParam("ownerId") String ownerId,
      @PathParam("licenseThreatGroupId") String licenseThreatGroupId)
  {
    licenseThreatGroupService.deleteLicenseThreatGroup(ownerType, ownerId, licenseThreatGroupId);
  }
}
