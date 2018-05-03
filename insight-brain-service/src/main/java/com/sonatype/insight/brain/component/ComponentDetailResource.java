/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.insight.brain.successmetrics.ComponentCountsDTO;
import com.sonatype.insight.brain.successmetrics.OwnerFilterDTO;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.11
 */
@Named
@Timed
@Path(ComponentDetailResource.RESOURCE_PATH)
public class ComponentDetailResource
{
  public static final String RESOURCE_PATH = "rest/componentDetails";

  public static final String GET_COMPONENT_COUNTS = "componentCounts";

  private final ComponentDetailService componentDetailService;

  @Inject
  public ComponentDetailResource(ComponentDetailService componentDetailService) {
    this.componentDetailService = componentDetailService;
  }

  @GET
  @Path("applications")
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApplicationComponentDetailsDTO> getApplicationDetailsByHash(@QueryParam("hash") String hash) {
    return componentDetailService.getApplicationDetailsByHash(hash);
  }

  @GET
  @Path("name")
  @Produces(MediaType.APPLICATION_JSON)
  public ComponentDisplayName getComponentNameByHash(@QueryParam("hash") String hash) {
    return componentDetailService.getComponentNameByHash(hash);
  }

  /**
   * @since 1.35
   */
  @POST
  @Path(GET_COMPONENT_COUNTS)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getComponentCountsExceptionMeter")
  public ComponentCountsDTO getComponentCounts(OwnerFilterDTO ownerFilterDTO) {
    return componentDetailService.getComponentCounts(ownerFilterDTO.organizationIds, ownerFilterDTO.applicationIds);
  }
}
