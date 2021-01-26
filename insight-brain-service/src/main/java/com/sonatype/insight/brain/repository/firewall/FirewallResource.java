/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.firewall;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @since 1.105.0
 */
@Named
@Path(FirewallResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class FirewallResource
{
  public static final String RESOURCE_PATH = "rest/repositories/firewall";

  static final String STATUS_PATH = "status";

  private final FirewallService firewallService;

  @Inject
  public FirewallResource(final FirewallService firewallService) {
    this.firewallService = firewallService;
  }

  /**
   * @since 1.105.0
   */
  @GET
  @Path(STATUS_PATH)
  public FirewallStatusDTO getStatus() {
    return firewallService.getFirewallStatus();
  }
}
