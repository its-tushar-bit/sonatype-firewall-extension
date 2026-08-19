/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.47
 */
@Named
@Timed
@Path(HdsPingResource.RESOURCE_PATH)
public class HdsPingResource
{
  public static final String RESOURCE_PATH = "rest/hdsPing";

  private final HdsPingService hdsPingService;

  @Inject
  public HdsPingResource(final HdsPingService hdsPingService) {
    this.hdsPingService = hdsPingService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public PingResponseDTO pingHds() {
    return hdsPingService.pingHds();
  }
}
