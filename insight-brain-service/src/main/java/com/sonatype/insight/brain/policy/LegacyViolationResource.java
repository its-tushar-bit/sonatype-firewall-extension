/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.policy.LegacyViolationService.LegacyViolationStatusDTO;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.168
 */
@Named
@Timed
@Path(LegacyViolationResource.RESOURCE_PATH)
public class LegacyViolationResource
{
  static final String RESOURCE_PATH = "rest/legacyViolations";

  static final String REVOKE_PATH = "revoke/{applicationPublicId}";

  static final String GRANT_PATH = "grant/{applicationPublicId}";

  static final String GET_PATH = "{ownerType: application|organization}/{ownerId}";

  private LegacyViolationService legacyViolationService;

  @Inject
  public LegacyViolationResource(LegacyViolationService legacyViolationService) {
    this.legacyViolationService = legacyViolationService;
  }

  @PUT
  @Path(REVOKE_PATH)
  @Audited(AuditEvent.REVOKE_LEGACY_VIOLATION_STATUS)
  public void revokeLegacyViolationStatus(@PathParam("applicationPublicId") String applicationPublicId) {
    legacyViolationService.revokeLegacyViolationStatus(applicationPublicId);
  }

  @PUT
  @Path(GRANT_PATH)
  @Audited(AuditEvent.APPLY_LEGACY_VIOLATION_STATUS)
  public void grantLegacyViolationStatus(@PathParam("applicationPublicId") String applicationPublicId) {
    legacyViolationService.grantLegacyViolationStatus(applicationPublicId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(GET_PATH)
  public LegacyViolationStatusDTO getLegacyViolationsStatus(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    return legacyViolationService.getLegacyViolationsStatus(ownerType, ownerId);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path(GET_PATH)
  @Audited(AuditEvent.CONFIGURE_LEGACY_VIOLATION_STATUS)
  public LegacyViolationStatusDTO setLegacyViolationStatus(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      LegacyViolationStatusDTO legacyViolationStatusDTO)
  {
    return legacyViolationService.setLegacyViolationStatus(ownerType, ownerId, legacyViolationStatusDTO);
  }
}
