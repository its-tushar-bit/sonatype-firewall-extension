/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import com.sonatype.insight.brain.api.PublicApiPaths;

import com.codahale.metrics.annotation.Timed;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Named
@Timed
@Path(PublicApiPaths.EXPERIMENTAL_SAST_PATH)
public class ApiSastResource
{
  private final ApiSastService sastService;

  @Inject
  public ApiSastResource(final ApiSastService sastService) {
    this.sastService = sastService;
  }

  @GET
  @Path("/validate")
  @Produces(APPLICATION_JSON)
  public SastValidateResponseDTO validate() {
    return sastService.validate();
  }
}
