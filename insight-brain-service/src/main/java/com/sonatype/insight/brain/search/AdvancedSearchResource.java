/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.88
 */
@Named
@Timed
@Path(AdvancedSearchResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdvancedSearchResource
{
  public static final String RESOURCE_PATH = "rest/search/advanced";

  public static final String STATUS_PATH = "status";

  private final AdvancedSearchService advancedSearchService;

  @Inject
  public AdvancedSearchResource(AdvancedSearchService advancedSearchService) {
    this.advancedSearchService = advancedSearchService;
  }

  @PUT
  @Path(STATUS_PATH)
  @Audited(AuditEvent.CONFIGURE_ADVANCED_SEARCH)
  public void setStatus(AdvancedSearchStatusDTO statusDTO) {
    advancedSearchService.setStatus(statusDTO);
  }

  @GET
  @Path(STATUS_PATH)
  public AdvancedSearchStatusDTO getStatus() {
    return advancedSearchService.getStatus();
  }
}
