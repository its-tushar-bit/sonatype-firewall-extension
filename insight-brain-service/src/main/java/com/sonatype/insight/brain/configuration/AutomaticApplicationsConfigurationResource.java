/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
  @SuppressWarnings("checkstyle:LineLength")
  public AutomaticApplicationsConfigurationResource(AutomaticApplicationsConfigurationService automaticApplicationsConfigurationService) {
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
