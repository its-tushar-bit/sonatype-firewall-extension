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
import com.sonatype.insight.brain.api.v2.dto.ApiMailConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiMailConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since MIGRATE_MAIL_CONFIG
 */
@Named
@Timed
@Path(value = PublicApiPaths.MAIL_CONFIG_RESOURCE_PATH_V2)
public class ApiMailConfigurationResource
{
  private final ApiMailConfigurationService mailConfigurationService;

  @Inject
  public ApiMailConfigurationResource(ApiMailConfigurationService mailConfigurationService) {
    this.mailConfigurationService = mailConfigurationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiMailConfigurationDTO getConfiguration() {
    return mailConfigurationService.getConfiguration();
  }

  @PUT
  @Audited(AuditEvent.CONFIGURE_MAIL)
  @Consumes(MediaType.APPLICATION_JSON)
  public void setConfiguration(ApiMailConfigurationDTO configurationDTO) {
    mailConfigurationService.setConfiguration(configurationDTO);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_MAIL)
  public void deleteConfiguration() {
    mailConfigurationService.deleteConfiguration();
  }
}
