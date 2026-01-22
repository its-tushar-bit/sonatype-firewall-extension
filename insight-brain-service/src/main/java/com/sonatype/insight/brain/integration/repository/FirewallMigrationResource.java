/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.repository.migration.MigrationDetails;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.33
 */
@Named
@Timed
@Path(FirewallMigrationResource.RESOURCE_PATH)
public class FirewallMigrationResource
{
  static final String RESOURCE_PATH = "rest/integration/repositories/migration";

  static final String SUPPORTED_PATH = "supported/{protocolVersion}";

  static final String HISTORY_PATH = "history/{targetRepositoryManagerInstanceId}/{targetRepositoryPublicId}";

  private final FirewallMigrationService firewallMigrationService;

  @Inject
  public FirewallMigrationResource(final FirewallMigrationService firewallMigrationService) {
    this.firewallMigrationService = firewallMigrationService;
  }

  @POST
  @Path(SUPPORTED_PATH)
  public void verifyMigrationSupport(@PathParam("protocolVersion") final String protocolVersion) {
    firewallMigrationService.verifyMigrationSupport(protocolVersion);
  }

  @POST
  @Path(HISTORY_PATH)
  @Audited(AuditEvent.MIGRATE_REPOSITORY)
  public void migrateRepositoryHistory(
      @PathParam("targetRepositoryManagerInstanceId") String targetRepositoryManagerInstanceId,
      @PathParam("targetRepositoryPublicId") String targetRepositoryPublicId,
      @QueryParam("sourceRepositoryManagerInstanceId") String sourceRepositoryManagerInstanceId,
      @QueryParam("sourceRepositoryPublicId") String sourceRepositoryPublicId)
  {
    firewallMigrationService.migrateRepositoryHistory(sourceRepositoryManagerInstanceId, sourceRepositoryPublicId,
        targetRepositoryManagerInstanceId, targetRepositoryPublicId);
  }

  @GET
  @Path(HISTORY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public MigrationDetails getRepositoryMigrationState(
      @PathParam("targetRepositoryManagerInstanceId") String targetRepositoryManagerInstanceId,
      @PathParam("targetRepositoryPublicId") String targetRepositoryPublicId)
  {
    return firewallMigrationService.getRepositoryMigrationState(targetRepositoryManagerInstanceId,
        targetRepositoryPublicId);
  }
}
