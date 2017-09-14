/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.io.IOException;
import java.util.List;

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

/**
 * @since 1.37
 */
@Named
@Path(SuccessMetricsResource.RESOURCE_PATH)
public class SuccessMetricsResource
{
  public static final String RESOURCE_PATH = "rest/successMetrics";

  private final SuccessMetricsService successMetricsService;

  @Inject
  public SuccessMetricsResource(SuccessMetricsService successMetricsService) {
    this.successMetricsService = successMetricsService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public SuccessMetricsDTO createSuccessMetricsForCurrentUser(SuccessMetricsDTO successMetricsDTO) {
    return successMetricsService.createSuccessMetricsForCurrentUser(successMetricsDTO);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<SuccessMetricsDTO> getSuccessMetricsForCurrentUser() throws IOException {
    return successMetricsService.getSuccessMetricsForCurrentUser();
  }

  @DELETE
  @Path("{successMetricsId}")
  public void deleteSuccessMetricsForCurrentUser(@PathParam("successMetricsId") String successMetricsId) {
    successMetricsService.deleteSuccessMetricsForCurrentUser(successMetricsId);
  }
}
