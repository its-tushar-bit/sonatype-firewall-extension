/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationStatus;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * This resource supports bulk onboarding of Source Config Management repositories
 *
 * @since 1.162
 */
@Named
@Timed
@Path(PublicApiPaths.EXPERIMENTAL_ONBOARDING_RESOURCE_PATH)
public class ApiScmOnboardingResource
{
  static final String IMPORT_REPO_PATH = "importRepositories/{organizationId}";

  static final String IMPORT_REPO_STATUS_PATH = IMPORT_REPO_PATH + "/event/{eventId}";

  private final ScmOnboardingService scmOnboardingService;

  @Inject
  public ApiScmOnboardingResource(final ScmOnboardingService scmOnboardingService) {
    this.scmOnboardingService = scmOnboardingService;
  }

  @Path(IMPORT_REPO_PATH)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.SOURCE_CONTROL_IMPORT)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public Response importRepositories(
      @PathParam("organizationId") String organizationId,
      final ImportScmOrganizationRequest importRequest)
  {
    return Response.status(Status.ACCEPTED)
        .entity(scmOnboardingService.importScmOrganization(organizationId, importRequest)).build();
  }

  @Path(IMPORT_REPO_STATUS_PATH)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  public ImportScmOrganizationStatus getImportRepositoriesStatus(
      @PathParam("organizationId") String organizationId,
      @PathParam("eventId") String eventId)
  {
    return scmOnboardingService.getImportScmOrganizationStatus(organizationId,eventId);
  }
}
