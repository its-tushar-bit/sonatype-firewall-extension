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

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.77
 */
@Named
@Timed
@Path(AutomaticSourceControlConfigurationResource.RESOURCE_PATH)
public class AutomaticSourceControlConfigurationResource
{
  public static final String RESOURCE_PATH = "rest/config/automaticScmConfiguration";

  private AutomaticSourceControlConfigurationService automaticSourceControlConfigurationService;

  @Inject
  public AutomaticSourceControlConfigurationResource(
      AutomaticSourceControlConfigurationService automaticSourceControlConfigurationService)
  {
    this.automaticSourceControlConfigurationService = automaticSourceControlConfigurationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public AutomaticSourceControlConfiguration get() {
    return automaticSourceControlConfigurationService.get();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_AUTOMATIC_SOURCE_CONTROL)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public AutomaticSourceControlConfiguration update(AutomaticSourceControlConfiguration configuration) {
    return automaticSourceControlConfigurationService.update(configuration);
  }
}
