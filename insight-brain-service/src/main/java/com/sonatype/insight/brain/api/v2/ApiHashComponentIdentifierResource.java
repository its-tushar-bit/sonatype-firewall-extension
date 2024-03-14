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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifierDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifiersDTO;
import com.sonatype.insight.brain.api.v2.service.ApiHashComponentIdentifierService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.85
 */
@Named
@Timed
@Path(value = PublicApiPaths.CLAIM_PATH_V2)
public class ApiHashComponentIdentifierResource
{
  private final ApiHashComponentIdentifierService apiHashComponentIdentifierService;

  @Inject
  public ApiHashComponentIdentifierResource(
      ApiHashComponentIdentifierService apiHashComponentIdentifierService)
  {
    this.apiHashComponentIdentifierService = apiHashComponentIdentifierService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{hash}")
  public ApiHashComponentIdentifierDTO get(@PathParam("hash") String hash) {
    return apiHashComponentIdentifierService.get(hash);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiHashComponentIdentifiersDTO getAll() {
    return apiHashComponentIdentifierService.getAll();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.SET_COMPONENT_IDENTITY)
  public ApiHashComponentIdentifierDTO set(ApiHashComponentIdentifierDTO hashComponentIdentifier) {
    return apiHashComponentIdentifierService.set(hashComponentIdentifier);
  }

  @DELETE
  @Path("{hash}")
  @Audited(AuditEvent.UNSET_COMPONENT_IDENTITY)
  public void delete(@PathParam("hash") String hash) {
    apiHashComponentIdentifierService.delete(hash);
  }
}
