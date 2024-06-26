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
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyListDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyService;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

/**
 *
 * @since 1.12.0
 */
@Named
@Timed
@Path(PublicApiPaths.POLICY_RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.POLICY_MANAGEMENT)
public class ApiPolicyResourceV2
{
  private final ApiPolicyService apiPolicyService;

  @Inject
  public ApiPolicyResourceV2(final ApiPolicyService apiPolicyService) {
    this.apiPolicyService = apiPolicyService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiPolicyListDTO getPolicies() {
    return apiPolicyService.getPolicies();
  }
}
