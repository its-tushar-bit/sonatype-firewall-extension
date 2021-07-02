/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.120
 */
@Named
@Timed
@Path(InnerSourceResource.RESOURCE_PATH)
public class InnerSourceResource
{
  static final String RESOURCE_PATH = "rest/innerSource";

  static final String COMPONENT_LATEST_VERSION_PATH = "component/latestVersion";

  private final InnerSourceService innerSourceService;

  @Inject
  public InnerSourceResource(InnerSourceService innerSourceService) {
    this.innerSourceService = innerSourceService;
  }

  @GET
  @Path(COMPONENT_LATEST_VERSION_PATH)
  @Produces(MediaType.TEXT_PLAIN)
  public String getComponentLatestVersion(@QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier) {
    return innerSourceService.getComponentLatestVersion(componentIdentifier);
  }
}
