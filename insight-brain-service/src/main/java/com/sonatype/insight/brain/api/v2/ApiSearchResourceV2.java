/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiSearchServiceV2;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * Enables end users to search for components within their applications. This REST API is exposed directly to users.
 *
 * @since 1.13.0
 */
@Path(PublicApiPaths.SEARCH_RESOURCE_PATH_V2)
@Named
@Timed
public class ApiSearchResourceV2
{
  private final ApiSearchServiceV2 searchService;

  @Inject
  public ApiSearchResourceV2(final ApiSearchServiceV2 searchService) {
    this.searchService = searchService;
  }

  /**
   * Searches all currently registered applications for a component matching the given search criteria. A component can
   * be searched for by its hash or its coordinates (or its equivalent packageUrl format), the latter supporting
   * wildcards like the equivalent policy condition. The mandatory stageId parameter restricts which scans/reports
   * of the applications are inspected for the component.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.SEARCH_COMPONENT_USES)
  public ApiSearchResultsDTOV2 searchComponent(
      @QueryParam("stageId") String stageId,
      @QueryParam("hash") String hash,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier,
      @QueryParam("packageUrl") String packageUrl)
  {
    return searchService.searchComponent(stageId, hash, componentIdentifier, packageUrl);
  }
}
