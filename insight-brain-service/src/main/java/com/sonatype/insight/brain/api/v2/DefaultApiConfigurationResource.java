/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.banning.BlockIfMultiTenant;
import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.138
 */
@Named
@Timed
@Path(value = PublicApiPaths.CONFIG_RESOURCE_PATH_V2)
@UnlicensedPath
public class DefaultApiConfigurationResource
    implements ApiConfigurationResource
{
  private final ApiConfigurationService service;

  @Inject
  public DefaultApiConfigurationResource(ApiConfigurationService service) {
    this.service = service;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> getConfiguration(@QueryParam("property") Set<String> properties) {
    return service.getConfiguration(properties);
  }

  @Override
  @PUT
  @Audited(AuditEvent.CONFIGURE_PROPERTIES)
  @Consumes(MediaType.APPLICATION_JSON)
  @BlockIfMultiTenant
  public void setConfiguration(Map<String, Object> properties) {
    service.setConfiguration(properties);
  }

  @Override
  @DELETE
  @Audited(AuditEvent.DELETE_PROPERTIES)
  @BlockIfMultiTenant
  public void deleteConfiguration(@QueryParam("property") Set<String> properties) {
    service.deleteConfiguration(properties);
  }
}
