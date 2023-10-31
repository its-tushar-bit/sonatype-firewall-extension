/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED;

/**
 * @since 1.140
 */
@Named
@Timed
@Path(value = PublicApiPaths.SOURCE_CONTROL_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "Config Source Control")
public class DefaultApiSourceControlConfigurationResource
    implements ApiSourceControlConfigurationResource
{
  private final ApiSourceControlConfigurationService service;

  @Inject
  public DefaultApiSourceControlConfigurationResource(ApiSourceControlConfigurationService service) {
    this.service = service;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @HasFeature(SAAS_LIFECYCLE_SCM_ENABLED)
  public ApiSourceControlConfigurationDTO getConfiguration() {
    return service.getConfiguration();
  }

  @Override
  @PUT
  @Audited(AuditEvent.CONFIGURE_SOURCE_CONTROL_CONFIGURATION)
  @Consumes(MediaType.APPLICATION_JSON)
  @HasFeature(SAAS_LIFECYCLE_SCM_ENABLED)
  public void setConfiguration(JsonNode jsonNode) {
    service.setConfiguration(jsonNode);
  }

  @Override
  @DELETE
  @Audited(AuditEvent.DELETE_SOURCE_CONTROL_CONFIGURATION)
  @HasFeature(SAAS_LIFECYCLE_SCM_ENABLED)
  public void deleteConfiguration() {
    service.deleteConfiguration();
  }
}
