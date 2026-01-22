/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
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
