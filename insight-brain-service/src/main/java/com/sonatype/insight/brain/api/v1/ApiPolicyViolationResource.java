/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1;

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
import com.sonatype.insight.brain.api.v1.dto.ApiApplicationViolationListDTO;
import com.sonatype.insight.brain.api.v1.service.ApiPolicyViolationService;
import com.sonatype.insight.brain.api.v2.ApiPolicyViolationResourceV2;


/**
 * @deprecated since 1.13.0, use {@link ApiPolicyViolationResourceV2}
 *
 * @since 1.12.0
 */
@Deprecated
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
