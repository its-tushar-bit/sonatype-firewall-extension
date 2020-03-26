/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
