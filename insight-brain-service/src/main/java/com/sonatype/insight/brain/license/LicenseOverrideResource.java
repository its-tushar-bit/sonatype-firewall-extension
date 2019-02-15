/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.license.LicenseOverrideService.AppliedLicenseOverrides;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.jaxrs.JsonEncodedComponentIdentifier;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.6
 */
@Named
@Timed
@Path(LicenseOverrideResource.RESOURCE_PATH)
public class LicenseOverrideResource
{
  public static final String RESOURCE_PATH = "rest/licenseOverride/"
      + "{ownerType: application|organization|repository|repository_container}/{ownerId}";

  private final LicenseOverrideService licenseOverrideService;

  @Inject
  public LicenseOverrideResource(final LicenseOverrideService licenseOverrideService) {
    this.licenseOverrideService = licenseOverrideService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_COMPONENT_LICENSE)
  public LicenseOverride addLicenseOverride(@PathParam("ownerType") OwnerType ownerType,
                                            @PathParam("ownerId") String ownerId,
                                            LicenseOverride licenseOverride,
                                            @QueryParam("where") String where,
                                            @Context final HttpServletRequest request) throws IOException
  {
    return licenseOverrideService.addLicenseOverride(ownerType, ownerId, licenseOverride, where, request);
  }

  @DELETE
  @Path("{licenseOverrideId}")
  @Audited(AuditEvent.UPDATE_COMPONENT_LICENSE)
  public void deleteLicenseOverride(@PathParam("ownerType") OwnerType ownerType,
                                    @PathParam("ownerId") String ownerId,
                                    @PathParam("licenseOverrideId") String licenseOverrideId,
                                    @QueryParam("where") String where,
                                    @Context final HttpServletRequest request) throws IOException
  {
    licenseOverrideService.deleteLicenseOverride(ownerType, ownerId, licenseOverrideId, where, request);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public AppliedLicenseOverrides getAppliedLicenseOverrides(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @QueryParam("componentIdentifier") JsonEncodedComponentIdentifier componentIdentifier)
  {
    return licenseOverrideService.getAppliedLicenseOverrides(ownerType, ownerId, componentIdentifier);
  }
}
