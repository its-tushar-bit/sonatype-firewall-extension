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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.dto.ImportRepositoriesRequest;
import com.sonatype.insight.brain.api.experimental.dto.SCMRepositories;
import com.sonatype.insight.brain.api.experimental.dto.ValidationResponse;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;

/**
 * This resource supports bulk onboarding of Source Config Management repositories
 *
 * @since 1.98
 */
@Named
@Timed
@Path(ApiScmOnboardingResource.RESOURCE_PATH)
public class ApiScmOnboardingResource
{
  static final String RESOURCE_PATH = PublicApiPaths.BASE_PATH + "/experimental/onboarding";

  static final String LOAD_REPO_PATH = "loadRepositories";

  static final String IMPORT_REPO_PATH = "importRepositories/{orgId}";

  static final String DEFAULT_HOST_URL = "defaultHostUrl";

  static final String VALIDATE_SCM_HOST_URL = "validate/{scmProvider}";

  private final ApiScmOnboardingService apiScmOnboardingService;

  @Inject
  public ApiScmOnboardingResource(final ApiScmOnboardingService apiScmOnboardingService) {
    this.apiScmOnboardingService = apiScmOnboardingService;
  }

  @Path(LOAD_REPO_PATH)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public SCMRepositories loadRepositories(
      @QueryParam("orgId") String orgId,
      @QueryParam("defaultHostUrl") String defaultHostUrl)
      throws IOException
  {
    return apiScmOnboardingService.loadRepositories(orgId, defaultHostUrl);
  }

  @Path(DEFAULT_HOST_URL)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> getDefaultHostUrl(
      @QueryParam("provider") String provider,
      @QueryParam("orgId") String orgId)
  {
    return ImmutableMap.of("defaultHostUrl", apiScmOnboardingService.getDefaultHostUrl(provider, orgId));
  }

  @Path(IMPORT_REPO_PATH)
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public ImportResults importRepositories(
      @PathParam("orgId") String orgId,
      final ImportRepositoriesRequest importReposRequest)
  {
    return apiScmOnboardingService.importRepositories(orgId, importReposRequest);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(VALIDATE_SCM_HOST_URL)
  public ValidationResponse validateScmHostUrl(
      @PathParam("scmProvider") String scmProvider,
      @QueryParam("scmHostUrl") String scmHostUrl)
  {
    return apiScmOnboardingService.validateScmHostUrl(scmProvider, scmHostUrl);
  }
}
