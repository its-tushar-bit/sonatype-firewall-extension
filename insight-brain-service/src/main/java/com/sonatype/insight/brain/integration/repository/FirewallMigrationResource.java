/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.repository.migration.MigrationState;

import com.yammer.metrics.annotation.Timed;

/**
 * @since 1.30
 */
@Named
// To be re-enabled by CLM-8048 // @Path(FirewallMigrationResource.RESOURCE_PATH)
public class FirewallMigrationResource
{
  static final String RESOURCE_PATH = "rest/integration/repositories/migration";

  static final String SUPPORTED_PATH = "supported/{protocolVersion}";

  static final String HISTORY_PATH = "history/{repositoryManagerInstanceId}/{repositoryPublicId}";

  private final FirewallMigrationService firewallMigrationService;

  @Inject
  public FirewallMigrationResource(final FirewallMigrationService firewallMigrationService) {
    this.firewallMigrationService = firewallMigrationService;
  }

  @POST
  @Path(SUPPORTED_PATH)
  @Timed
  public void verifyMigrationSupport(@PathParam("protocolVersion") final String protocolVersion)
  {
    firewallMigrationService.verifyMigrationSupport(protocolVersion);
  }

  @POST
  @Path(HISTORY_PATH)
  @Timed
  public void migrateRepositoryHistory(@PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
                                       @PathParam("repositoryPublicId") String repositoryPublicId,
                                       @QueryParam("sourceRepositoryManagerInstanceId")
                                           String sourceRepositoryManagerInstanceId,
                                       @QueryParam("sourceRepositoryPublicId") String sourceRepositoryPublicId,
                                       @QueryParam("lastMigratedPathname") String lastMigratedPathname)
  {
    firewallMigrationService
        .migrateRepositoryHistory(repositoryManagerInstanceId, repositoryPublicId, sourceRepositoryManagerInstanceId,
            sourceRepositoryPublicId, lastMigratedPathname);
  }

  @GET
  @Path(HISTORY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public MigrationState getRepositoryMigrationState(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId)
  {
    return firewallMigrationService.getRepositoryMigrationState(repositoryManagerInstanceId, repositoryPublicId);
  }
}
