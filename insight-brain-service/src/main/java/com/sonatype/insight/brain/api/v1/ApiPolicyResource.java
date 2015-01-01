/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v1.dto.ApiPolicyListDTO;
import com.sonatype.insight.brain.api.v1.service.ApiPolicyService;

/**
 *
 * @since 1.12.0
 */
@Named
@Path(PublicApiPaths.POLICY_SERVICE_PATH)
public class ApiPolicyResource
{
  private final ApiPolicyService apiPolicyService;

  @Inject
  public ApiPolicyResource(final ApiPolicyService apiPolicyService) {
    this.apiPolicyService = apiPolicyService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ApiPolicyListDTO getPolicies() {
    return apiPolicyService.getPolicies();
  }
}
