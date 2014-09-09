/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.dto.ApiApplicationViolationListDTO;
import com.sonatype.insight.brain.api.service.ApiPolicyViolationService;


/**
 * @since 1.12.0
 */
@Named
@Path(PublicApiPaths.POLICY_VIOLATION_SERVICE_PATH)
public class ApiPolicyViolationResource
{
  private ApiPolicyViolationService apiPolicyViolationService;

  @Inject
  public ApiPolicyViolationResource(final ApiPolicyViolationService apiPolicyViolationService) {
    this.apiPolicyViolationService = apiPolicyViolationService;
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes({MediaType.APPLICATION_JSON})
  public ApiApplicationViolationListDTO getPolicyViolations(@QueryParam("p") final Set<String> policyIds) {
    return apiPolicyViolationService.getPolicyViolations(policyIds);
  }
}
