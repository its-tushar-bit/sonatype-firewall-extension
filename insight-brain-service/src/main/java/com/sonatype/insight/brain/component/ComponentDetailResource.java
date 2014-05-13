/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @since 1.11
 */
@Named
@Path(ComponentDetailResource.SERVICE_PATH)
public class ComponentDetailResource
{
  public static final String SERVICE_PATH = "rest/componentDetails";

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
  public String getComponentNameByHash(@QueryParam("hash") String hash) {
    return componentDetailService.getComponentNameByHash(hash);
  }
}