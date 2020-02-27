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
 * @since GLOBAL_SEARCH
 */
@Named
@Timed
@Path(FullTextSearchResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FullTextSearchResource
{
  public static final String RESOURCE_PATH = "rest/fullTextSearch";

  public static final String STATUS_PATH = "status";

  private final FullTextSearchService fullTextSearchService;

  @Inject
  public FullTextSearchResource(FullTextSearchService fullTextSearchService) {
    this.fullTextSearchService = fullTextSearchService;
  }

  @PUT
  @Path(STATUS_PATH)
  @Audited(AuditEvent.CONFIGURE_ADVANCED_SEARCH)
  public void setStatus(FullTextSearchStatusDTO fullTextSearchStatusDTO) {
    fullTextSearchService.setStatus(fullTextSearchStatusDTO);
  }

  @GET
  @Path(STATUS_PATH)
  public FullTextSearchStatusDTO getStatus() {
    return fullTextSearchService.getStatus();
  }
}
