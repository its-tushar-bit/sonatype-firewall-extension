/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentNearestFixedVersions;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;

/**
 * @since 1.144.0
 */
@Named
@Timed
@Path(ApiComponentNearestFixedVersionsResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.COMPONENT_EVALUATION)
public class ApiComponentNearestFixedVersionsResource
{
  static final String RESOURCE_PATH = "/api/experimental/components/vulnerabilities/nearestFixedVersions";

  private final ApiComponentNearestFixedVersionsService service;

  @Inject
  public ApiComponentNearestFixedVersionsResource(ApiComponentNearestFixedVersionsService service) {
    this.service = service;
  }

  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(hidden = true)
  public List<ComponentNearestFixedVersions> getNearestFixedVersions(
      ApiComponentNearestFixedVersionsRequestListDto listDto)
  {
    return service.getNearestFixedVersions(listDto);
  }
}
