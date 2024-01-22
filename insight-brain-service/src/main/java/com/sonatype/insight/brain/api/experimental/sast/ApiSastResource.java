/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.sonatype.insight.brain.api.PublicApiPaths;

import com.codahale.metrics.annotation.Timed;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

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
