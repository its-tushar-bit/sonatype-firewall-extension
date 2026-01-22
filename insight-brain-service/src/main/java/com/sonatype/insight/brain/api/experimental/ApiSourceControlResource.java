/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.dto.ApiOwnerUserRateLimitsDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSourceControlService;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.162
 */
@Named
@Timed
@Path(PublicApiPaths.SOURCE_CONTROL_PATH_EXPERIMENTAL_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.SOURCE_CONTROL)
public class ApiSourceControlResource
{
  static final String OWNER_TYPE = "{ownerType:application|organization}";

  static final String OWNER_ID = "{ownerId}";

  static final String OWNER_PATH = OWNER_TYPE + "/" + OWNER_ID;

  static final String RATE_LIMITS_PATH = OWNER_PATH + "/rateLimits";

  private final ApiSourceControlService service;

  @Inject
  public ApiSourceControlResource(ApiSourceControlService service) {
    this.service = service;
  }

  @GET
  @Path(RATE_LIMITS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiOwnerUserRateLimitsDTO getRateLimits(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    return service.getRateLimits(ownerType, ownerId);
  }
}
