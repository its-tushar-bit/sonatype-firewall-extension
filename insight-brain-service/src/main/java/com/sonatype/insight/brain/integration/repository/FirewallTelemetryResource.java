/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.repository.FirewallTelemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.134.0
 */
@Named
@Path(FirewallTelemetryResource.RESOURCE_PATH)
public class FirewallTelemetryResource
{
  public static final String RESOURCE_PATH = "rest/integration/repositories/firewall-telemetry";

  private static final Logger log = LoggerFactory.getLogger(FirewallTelemetryResource.class);

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public void postFirewallTelemetryData(FirewallTelemetry firewallTelemetryDTO) {
    log.info("FirewallTelemetry called");
  }
}
