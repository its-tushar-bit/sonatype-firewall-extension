/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.DeprecatedApiProxyConfigurationDTOV2;
import com.sonatype.insight.brain.api.v2.service.DeprecatedApiProxyConfigurationServiceV2;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.65
 * 
 * @deprecated Replaced by {@link ApiProxyServerConfigurationResource} in 1.84
 *             Scheduled for removal in July 2020.
 */
@Deprecated
@Named
@Timed
@Path(PublicApiPaths.DEPRECATED_PROXY_CONFIG_PATH_V2)
public class DeprecatedApiProxyConfigurationResourceV2
{
  private DeprecatedApiProxyConfigurationServiceV2 proxyConfigurationService;

  @Inject
  public DeprecatedApiProxyConfigurationResourceV2(DeprecatedApiProxyConfigurationServiceV2 proxyConfigurationService) {
    this.proxyConfigurationService = proxyConfigurationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public DeprecatedApiProxyConfigurationDTOV2 get() {
    return proxyConfigurationService.get();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_PROXY)
  public DeprecatedApiProxyConfigurationDTOV2 update(DeprecatedApiProxyConfigurationDTOV2 configuration) {
    return proxyConfigurationService.update(configuration);
  }
}
