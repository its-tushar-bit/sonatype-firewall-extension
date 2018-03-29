/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import javax.inject.Inject;
import javax.inject.Named;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @since 1.47
 */
@Named
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
  @Produces(MediaType.TEXT_PLAIN)
  public boolean pingHds() {
    return hdsPingService.pingHds();
  }
}
