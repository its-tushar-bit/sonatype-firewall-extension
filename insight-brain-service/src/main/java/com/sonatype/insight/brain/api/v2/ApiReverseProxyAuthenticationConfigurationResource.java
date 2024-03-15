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
import com.sonatype.insight.brain.api.v2.dto.ApiReverseProxyAuthenticationConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiReverseProxyAuthenticationConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.138
 */
@Named
@Timed
@Path(value = PublicApiPaths.REVERSE_PROXY_AUTHENTICATION_CONFIG_RESOURCE_PATH_V2)
@Tag(name = "Config Reverse Proxy Authentication")
public class ApiReverseProxyAuthenticationConfigurationResource
{
  private final ApiReverseProxyAuthenticationConfigurationService service;

  @Inject
  public ApiReverseProxyAuthenticationConfigurationResource(
      ApiReverseProxyAuthenticationConfigurationService service)
  {
    this.service = service;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiReverseProxyAuthenticationConfigurationDTO getConfiguration() {
    return service.getConfiguration();
  }

  @PUT
  @Audited(AuditEvent.CONFIGURE_REVERSE_PROXY_AUTHENTICATION)
  @Consumes(MediaType.APPLICATION_JSON)
  public void setConfiguration(ApiReverseProxyAuthenticationConfigurationDTO dto) {
    service.setConfiguration(dto);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_REVERSE_PROXY_AUTHENTICATION)
  public void deleteConfiguration() {
    service.deleteConfiguration();
  }
}
