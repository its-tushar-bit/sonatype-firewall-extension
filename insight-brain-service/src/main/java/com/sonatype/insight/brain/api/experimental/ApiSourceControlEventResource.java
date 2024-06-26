/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(PublicApiPaths.SOURCE_CONTROL_EVENTS_RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.SOURCE_CONTROL)
public class ApiSourceControlEventResource
{
  private final ApiSourceControlEventService service;

  @Inject
  public ApiSourceControlEventResource(ApiSourceControlEventService service) {
    this.service = service;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiSourceControlEventDTO> getSourceControlEventData(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @DefaultValue("0") @QueryParam("createdOnOrAfter") long createdOnOrAfter,
      @DefaultValue("true") @QueryParam("asc") boolean ascending,
      @DefaultValue("10") @QueryParam("limit") int limit,
      @DefaultValue("0") @QueryParam("offset") int offset)
  {
    ApiSourceControlEventFilterDTO filter =
        new ApiSourceControlEventFilterDTO(createdOnOrAfter, ascending, limit, offset);
    return service.getApiSourceControlEventData(ownerType, ownerId, filter);
  }
}
