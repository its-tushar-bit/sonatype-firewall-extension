/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
