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
