/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.43
 */
@Named
@Timed
@Path(AutomaticApplicationsConfigurationResource.RESOURCE_PATH)
public class AutomaticApplicationsConfigurationResource
{
  public static final String RESOURCE_PATH = "rest/config/automaticApplications";

  private AutomaticApplicationsConfigurationService automaticApplicationsConfigurationService;

  @Inject
  public AutomaticApplicationsConfigurationResource(
      AutomaticApplicationsConfigurationService automaticApplicationsConfigurationService)
  {
    this.automaticApplicationsConfigurationService = automaticApplicationsConfigurationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public AutomaticApplicationsConfiguration get() {
    return automaticApplicationsConfigurationService.get();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_AUTOMATIC_APPLICATIONS)
  public AutomaticApplicationsConfiguration update(AutomaticApplicationsConfiguration configuration) {
    return automaticApplicationsConfigurationService.update(configuration);
  }
}
