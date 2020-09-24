/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.hds.HdsClient;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;

/**
 * Resource for invoking manifest scans.
 *
 * @since 1.98
 */
@Named
@Timed
@Path(ApiManifestEvaluationResource.RESOURCE_PATH)
public class ApiManifestEvaluationResource
{
  static final String RESOURCE_PATH = PublicApiPaths.BASE_PATH + "/experimental"
      + "/applications/{applicationId}/manifest-evaluation";

  private final ApiManifestEvaluationService apiManifestEvaluationService;

  @Context
  private HttpServletRequest request;

  @Inject
  public ApiManifestEvaluationResource(final ApiManifestEvaluationService apiManifestEvaluationService) {
    this.apiManifestEvaluationService = apiManifestEvaluationService;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> scanManifest(
      @PathParam("applicationId") String applicationId,
      @DefaultValue("develop") @QueryParam("stage") String stage,
      @QueryParam("branch") String branchName) throws IOException
  {
    String statusId = apiManifestEvaluationService
        .performManifestScan(applicationId, stage, branchName, HdsClient.getClientUserAgent(request));
    return ImmutableMap.of("statusId", statusId);
  }
}
