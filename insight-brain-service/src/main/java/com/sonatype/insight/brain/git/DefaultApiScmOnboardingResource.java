/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.git.dto.ImportResults;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;

import com.codahale.metrics.annotation.Timed;

/**
 * This resource supports bulk onboarding of Source Config Management repositories
 *
 * @since 1.162
 */
@Named
@Timed
@Path(PublicApiPaths.EXPERIMENTAL_ONBOARDING_RESOURCE_PATH)
public class DefaultApiScmOnboardingResource
    implements ApiScmOnboardingResource
{
  static final String IMPORT_REPO_PATH = "importRepositories/{orgId}";

  private final ScmOnboardingService scmOnboardingService;

  @Inject
  public DefaultApiScmOnboardingResource(final ScmOnboardingService scmOnboardingService) {
    this.scmOnboardingService = scmOnboardingService;
  }

  @Override
  @Path(IMPORT_REPO_PATH)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ImportResults importRepositories(
      @PathParam("orgId") String orgId,
      final ImportScmOrganizationRequest importRequest) throws IOException
  {
    return scmOnboardingService.importScmOrganization(orgId, importRequest);
  }
}
