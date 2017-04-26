/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.yammer.metrics.annotation.Timed;

/**
 * @since 1.28
 */
@Named
@Path(FirewallMigrationResource.RESOURCE_PATH)
public class FirewallMigrationResource
{
  static final String RESOURCE_PATH = "rest/integration/repositories/migration";

  static final String SUPPORTED_PATH = "supported/{protocolVersion}";

  private final FirewallMigrationService firewallMigrationService;

  @Inject
  public FirewallMigrationResource(final FirewallMigrationService firewallMigrationService) {
    this.firewallMigrationService = firewallMigrationService;
  }

  @GET
  @Path(SUPPORTED_PATH)
  @Timed
  public void verifyMigrationSupport(@PathParam("protocolVersion") final String protocolVersion)
  {
    firewallMigrationService.verifyMigrationSupport(protocolVersion);
  }
}
