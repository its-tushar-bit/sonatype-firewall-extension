/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.dto.FirewallConfigurationDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import static com.sonatype.insight.brain.api.experimental.ApiFirewallResource.RESOURCE_PATH;

/**
 * @since 1.106.0
 */
@Named
@Path(RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class ApiFirewallResource
{
  static final String RESOURCE_PATH = PublicApiPaths.BASE_PATH + "/experimental/firewall";

  static final String CONFIGURATION_PATH = "configuration";

  static final String RELEASE_QUARANTINE = "releaseQuarantine";

  static final String SUMMARY_PATH = "summary";

  static final String RELEASE_QUARANTINE_SUMMARY_PATH = RELEASE_QUARANTINE + "/" + SUMMARY_PATH;

  private final ApiFirewallService apiFirewallService;

  @Inject
  public ApiFirewallResource(final ApiFirewallService apiFirewallService) {
    this.apiFirewallService = apiFirewallService;
  }

  @GET
  @Path(RELEASE_QUARANTINE_SUMMARY_PATH)
  public ApiFirewallReleaseQuarantineSummaryDTO getFirewallUnquarantineSummary() {
    return apiFirewallService.getReleaseQuarantineSummary();
  }

  /**
   * @since 1.106.0
   */
  @GET
  @Path(CONFIGURATION_PATH)
  public FirewallConfigurationDTO getFirewallConfiguration() {
    return apiFirewallService.getFirewallConfiguration();
  }

  @PUT
  @Path(CONFIGURATION_PATH)
  @Audited(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING)
  public FirewallConfigurationDTO setFirewallConfiguration(final FirewallConfigurationDTO firewallConfigurationDTO) {
    return apiFirewallService.setFirewallConfiguration(firewallConfigurationDTO);
  }
}
