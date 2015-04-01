/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyViolationServiceV2;


/**
 * @since 1.13.0
 */
@Named
@Path(PublicApiPaths.POLICY_VIOLATION_SERVICE_PATH_V2)
public class ApiPolicyViolationResourceV2
{
  private ApiPolicyViolationServiceV2 apiPolicyViolationService;

  @Inject
  public ApiPolicyViolationResourceV2(final ApiPolicyViolationServiceV2 apiPolicyViolationService) {
    this.apiPolicyViolationService = apiPolicyViolationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes({MediaType.APPLICATION_JSON})
  public ApiApplicationViolationListDTOV2 getPolicyViolations(@QueryParam("p") final Set<String> policyIds) {
    return apiPolicyViolationService.getPolicyViolations(policyIds);
  }
}
