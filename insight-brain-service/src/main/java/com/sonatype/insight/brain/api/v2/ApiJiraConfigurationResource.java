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
import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.139
 */
@Named
@Timed
@Path(value = PublicApiPaths.JIRA_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "Config JIRA")
public class ApiJiraConfigurationResource
{
  private final ApiJiraConfigurationService service;

  @Inject
  public ApiJiraConfigurationResource(
      ApiJiraConfigurationService service)
  {
    this.service = service;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiJiraConfigurationDTO getConfiguration() {
    return service.getConfiguration();
  }

  @PUT
  @Audited(AuditEvent.CONFIGURE_JIRA)
  @Consumes(MediaType.APPLICATION_JSON)
  public void setConfiguration(JsonNode jsonNode) {
    service.setConfiguration(jsonNode);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_JIRA)
  public void deleteConfiguration() {
    service.deleteConfiguration();
  }
}
