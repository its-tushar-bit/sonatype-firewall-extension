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
import com.sonatype.insight.brain.api.v2.dto.ApiProxyServerConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.84
 */
@Named
@Timed
@Path(value = PublicApiPaths.PROXY_SERVER_CONFIG_PATH_V2)
@UnlicensedPath
@Tag(name = "Config Proxy Server")
public class ApiProxyServerConfigurationResource
{
  private final ApiProxyServerConfigurationService proxyServerConfigurationService;

  @Inject
  public ApiProxyServerConfigurationResource(
      ApiProxyServerConfigurationService proxyServerConfigurationService)
  {
    this.proxyServerConfigurationService = proxyServerConfigurationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiProxyServerConfigurationDTO getConfiguration() {
    return proxyServerConfigurationService.getConfiguration();
  }

  @PUT
  @Audited(AuditEvent.CONFIGURE_PROXY)
  @Consumes(MediaType.APPLICATION_JSON)
  public void setConfiguration(ApiProxyServerConfigurationDTO configurationDTO) {
    proxyServerConfigurationService.setConfiguration(configurationDTO);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_PROXY)
  public void deleteConfiguration() {
    proxyServerConfigurationService.deleteConfiguration();
  }
}
