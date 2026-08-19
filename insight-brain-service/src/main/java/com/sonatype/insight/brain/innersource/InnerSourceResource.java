/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

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
