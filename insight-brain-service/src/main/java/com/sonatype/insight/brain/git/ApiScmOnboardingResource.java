/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationStatus;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationTicket;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * This resource supports bulk onboarding of Source Config Management repositories
 *
 * @since 1.162
 */
@Named
@Timed
@Path(PublicApiPaths.EXPERIMENTAL_ONBOARDING_RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.SOURCE_CONTROL)
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
  @Operation(
      summary = "Import repositories for SCM organization",
      description =
          "Initiates the process to import repositories into a source control management (SCM) organization. " +
              "This is an asynchronous operation that returns a ticket for tracking the import progress.",
      responses = {
          @ApiResponse(
              responseCode = "202",
              description = "Import request accepted and processing started",
              content = @Content(
                  mediaType = MediaType.APPLICATION_JSON,
                  schema = @Schema(implementation = ImportScmOrganizationTicket.class)
              )
          ),
      }
  )
  public Response importRepositories(
      @Parameter(
          description = "Organization ID",
          required = true
      )
      @PathParam("organizationId") String organizationId,
      @RequestBody(
          description = "Configuration for the import",
          required = true,
          useParameterTypeSchema = true
      ) final ImportScmOrganizationRequest importRequest)
  {
    return Response.status(Status.ACCEPTED)
        .entity(scmOnboardingService.importScmOrganization(organizationId, importRequest)).build();
  }

  @Path(IMPORT_REPO_STATUS_PATH)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @HasFeature(SystemConfigurationPropertyFeature.SAAS_LIFECYCLE_SCM_ENABLED)
  @Operation(
      summary = "Get repository import status",
      description = "Retrieves the current status of a repository import operation",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Import status retrieved successfully",
              useReturnTypeSchema = true
          ),
          @ApiResponse(responseCode = "404", description = "Import operation not found")
      }
  )
  public ImportScmOrganizationStatus getImportRepositoriesStatus(
      @Parameter(description = "Organization ID", required = true)
      @PathParam("organizationId") String organizationId,
      @Parameter(description = "Import event ID", required = true)
      @PathParam("eventId") String eventId)
  {
    return scmOnboardingService.getImportScmOrganizationStatus(organizationId, eventId);
  }
}
